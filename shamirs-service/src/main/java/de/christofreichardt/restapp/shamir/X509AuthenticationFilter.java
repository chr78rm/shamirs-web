/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Developer
 */
public class X509AuthenticationFilter implements Filter, Traceable {

    static final Logger LOGGER = LoggerFactory.getLogger(X509AuthenticationFilter.class);
    Map<String, String> config;
    final List<String> excludeDNs = new ArrayList<>();
    final Map<String, Deque<Instant>> callsPerUser = new ConcurrentHashMap<>();
    final Set<String> activeUsers = new HashSet<>();
    final Lock lock = new ReentrantLock();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Map<String, String> temp = new HashMap<>();
        Iterator<String> iter = filterConfig.getInitParameterNames().asIterator();
        while (iter.hasNext()) {
            String parameterName = iter.next();
            String parameterValue = filterConfig.getInitParameter(parameterName);
            temp.put(parameterName, parameterValue);
        }
        this.config = Collections.unmodifiableMap(temp);

        LOGGER.info(String.format("%d: config = %s ...", System.identityHashCode(this), this.config));

        this.excludeDNs.addAll(
                Arrays.stream(this.config.get("excludeDNs").split(";"))
                        .map(distinguishedName -> distinguishedName.strip())
                        .toList()
        );

        LOGGER.info(String.format("%d: excludeDNs = %s ...", System.identityHashCode(this), this.excludeDNs.stream().map(dn -> "'" + dn + "'").toList()));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");

        try {
            X509Certificate[] certChain = (X509Certificate[]) servletRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (Objects.isNull(certChain)) {
                final String msg = "Client certificate missing.";
                tracer.logMessage(LogLevel.ERROR, msg, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                error(servletResponse, Response.Status.FORBIDDEN, msg);
                return;
            }

            tracer.out().printfIndentln("certChain.length = %d", certChain.length);
            for (int i = 0; i < certChain.length; i++) {
                X509Certificate certificate = certChain[i];
                certificate.getNotBefore().toInstant();
                ZonedDateTime notBefore = ZonedDateTime.ofInstant(certificate.getNotBefore().toInstant(), ZoneId.systemDefault());
                ZonedDateTime notAfter = ZonedDateTime.ofInstant(certificate.getNotAfter().toInstant(), ZoneId.systemDefault());
                tracer.out().printfIndentln("(%d) serialNr=%d, subject=%s, issuer=%s, from=%s, until=%s", i,
                        certificate.getSerialNumber(), certificate.getSubjectX500Principal(), certificate.getIssuerX500Principal(),
                        notBefore.format(DateTimeFormatter.ISO_ZONED_DATE_TIME), notAfter.format(DateTimeFormatter.ISO_ZONED_DATE_TIME));
            }

            String subjectPrincipal = certChain[0].getSubjectX500Principal().getName();
            tracer.out().printfIndentln("subjectPrincipal = %s", subjectPrincipal);
            
            this.lock.lock();
            try {
                if (this.activeUsers.contains(subjectPrincipal)) {
                    final String msg = String.format("User '%s' has submitted an unfinished call.", subjectPrincipal);
                    tracer.logMessage(LogLevel.ERROR, msg, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                    error(servletResponse, Response.Status.TOO_MANY_REQUESTS, msg);
                    return;
                }
                this.activeUsers.add(subjectPrincipal);
            } finally {
                this.lock.unlock();
            }
                
            try {
                if (this.excludeDNs.contains(subjectPrincipal)) {
                    final String msg = String.format("User[%s] has been banned.", subjectPrincipal);
                    tracer.logMessage(LogLevel.ERROR, msg, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                    error(servletResponse, Response.Status.FORBIDDEN, msg);
                    return;
                }
                
                HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
                String requestURI = httpServletRequest.getRequestURI();
                if ((requestURI.startsWith("/shamir/v1/actuator") || requestURI.startsWith("/shamir/v1/management")) && !isAdminUser(subjectPrincipal)) {
                    final String msg = "Unauthorized call to actuator or management endpoint.";
                    tracer.logMessage(LogLevel.ERROR, msg, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                    error(servletResponse, Response.Status.FORBIDDEN, msg);
                    return;
                }
                
                boolean throttlingEnabled = Boolean.parseBoolean(this.config.getOrDefault("throttling", "true"));
                if (!isAdminUser(subjectPrincipal) && throttlingEnabled) {
                    if (isSuspended(subjectPrincipal)) {
                        final String msg = String.format("User '%s' is temporarily suspended.", subjectPrincipal);
                        tracer.logMessage(LogLevel.ERROR, msg, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                        error(servletResponse, Response.Status.PAYMENT_REQUIRED, msg);
                        return;
                    }
                }
                
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                this.lock.lock();
                try {
                    boolean removed = this.activeUsers.remove(subjectPrincipal);
                    tracer.out().printfIndentln("Invocation for user '%s' is complete.", subjectPrincipal);
                    if (!removed) {
                        tracer.logMessage(LogLevel.WARNING, String.format("User '%s' hasn't been found within the set of active invocations.", subjectPrincipal), getClass(), 
                                "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                    }
                } finally {
                    this.lock.unlock();
                }
            }
        } finally {
            tracer.wayout();
        }
    }

    boolean isAdminUser(String subjectPrincipal) {
        return Objects.equals(this.config.getOrDefault("adminUserDN", "CN=test-user-0,L=Rodgau,ST=Hessen,C=DE"), subjectPrincipal);
    }

    boolean isSuspended(String subjectPrincipal) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "isSuspended(String subjectPrincipal)");

        try {
            boolean suspended;
            
            final Duration minInterval = Duration.of(
                    Long.parseLong(this.config.getOrDefault("minInterval", "10")), 
                    ChronoUnit.valueOf(this.config.getOrDefault("minInterval.temporalUnit", "SECONDS"))
            );
            final Duration timeFrame = Duration.of(
                    Long.parseLong(this.config.getOrDefault("timeFrame", "10")), 
                    ChronoUnit.valueOf(this.config.getOrDefault("timeFrame.temporalUnit", "MINUTES"))
            );
            final int maxCalls = Integer.parseInt(this.config.getOrDefault("timeFrame.maxCalls", "20"));
            
            tracer.out().printfIndentln("minInterval = %s", minInterval);
            tracer.out().printfIndentln("timeFrame = %s", timeFrame);
            tracer.out().printfIndentln("maxCalls = %d", maxCalls);
            
            Instant now = Instant.now();
            if (!this.callsPerUser.containsKey(subjectPrincipal)) {
                this.callsPerUser.put(subjectPrincipal, new LinkedList<>());
            }
            Deque<Instant> calls = this.callsPerUser.get(subjectPrincipal);
            Duration interval = null;
            if (calls.peekFirst() != null) {
                interval = Duration.between(calls.peekFirst(), now);
            }
            
            tracer.out().printfIndentln("now = %s", now);
            tracer.out().printfIndentln("interval = %s", interval);
            
            if (interval == null) {
                calls.addFirst(now);
                suspended = false;
            } else if (interval.compareTo(minInterval) >= 0) {
                while (calls.size() > maxCalls) {
                    calls.removeLast();
                }
                Duration frame = Duration.between(calls.peekLast(), now);
                
                tracer.out().printfIndentln("calls.size() = %d, calls = %s", calls.size(), calls);
                tracer.out().printfIndentln("frame = %s", frame);
                
                if (timeFrame.isZero()) {
                    tracer.out().printfIndentln("Checking the maximal permitted calls within a timeframe is disabled.");
                    calls.addFirst(now);
                    suspended = false;
                } else if (calls.size() < maxCalls || frame.compareTo(timeFrame) > 0) {
                    calls.addFirst(now);
                    suspended = false;
                } else {
                    tracer.out().printfIndentln("Too much calls within the reference frame.");
                    suspended = true;
                }
            } else {
                tracer.out().printfIndentln("Minimum interval hasn't been satisfied.");
                suspended = true;
            }

            return suspended;
        } finally {
            tracer.wayout();
        }
    }

    void error(ServletResponse servletResponse, Response.Status status, String message) throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "error(ServletResponse servletResponse, Response.Status status)");

        try {
            tracer.out().printfIndentln("status = %s", status);

            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
            httpServletResponse.setStatus(status.getStatusCode());
            httpServletResponse.setHeader("Content-Type", "application/json");
            httpServletResponse.setHeader("Content-Encoding", StandardCharsets.UTF_8.name());
            JsonObject errorResponse = Json.createObjectBuilder()
                    .add("status", status.getStatusCode())
                    .add("reason", status.getReasonPhrase())
                    .add("message", message)
                    .build();
            JsonWriter jsonWriter = Json.createWriter(servletResponse.getOutputStream());
            jsonWriter.writeObject(errorResponse);
            servletResponse.getOutputStream().flush();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
