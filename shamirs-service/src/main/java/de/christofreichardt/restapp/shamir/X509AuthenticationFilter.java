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
import java.util.Objects;
import java.util.stream.Stream;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.springframework.web.filter.GenericFilterBean;

/**
 *
 * @author Developer
 */
public class X509AuthenticationFilter extends GenericFilterBean implements Traceable {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");

        try {
            X509Certificate[] certChain = (X509Certificate[]) servletRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (Objects.isNull(certChain)) {
                final String MESSAGE = "Client certificate missing.";
                
                tracer.logMessage(LogLevel.ERROR, MESSAGE, getClass(), "doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)");
                
                HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                httpServletResponse.setStatus(Response.Status.FORBIDDEN.getStatusCode());
                httpServletResponse.setHeader("Content-Type", "application/json");
                httpServletResponse.setHeader("Content-Encoding", StandardCharsets.UTF_8.name());
                JsonObject errorResponse = Json.createObjectBuilder()
                        .add("status", Response.Status.FORBIDDEN.getStatusCode())
                        .add("reason", Response.Status.FORBIDDEN.getReasonPhrase())
                        .add("message", MESSAGE)
                        .build();
                JsonWriter jsonWriter = Json.createWriter(servletResponse.getOutputStream());
                jsonWriter.writeObject(errorResponse);
                servletResponse.getOutputStream().flush();
            } else {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
    
}
