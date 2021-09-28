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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
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
        
        LOGGER.info(String.format("%d: init(FilterConfig filterConfig = %s) ...", System.identityHashCode(this), this.config));
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
            } else {
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
                HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
                if (httpServletRequest.getRequestURI().startsWith("/shamir/v1/actuator") && 
                        !Objects.equals(this.config.getOrDefault("adminUserDN", "CN=test-user-0,L=Rodgau,ST=Hessen,C=DE"), subjectPrincipal)) {
                    final String msg = "Unauthorized call to actuator endpoint.";
                    tracer.logMessage(LogLevel.ERROR, msg, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                    error(servletResponse, Response.Status.FORBIDDEN, msg);
                } else {
                    filterChain.doFilter(servletRequest, servletResponse);
                }
            }
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
