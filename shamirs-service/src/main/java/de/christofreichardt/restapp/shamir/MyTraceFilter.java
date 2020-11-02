/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.TracerFactory;
import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author reichardt
 */
public class MyTraceFilter implements Filter {

    static final Logger LOGGER = LoggerFactory.getLogger(MyTraceFilter.class);
    
    final AtomicInteger requestCounter = new AtomicInteger();
    FilterConfig filterConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        Iterator<String> iter = filterConfig.getInitParameterNames().asIterator();
        while (iter.hasNext()) {
            String name = iter.next();
            stringBuilder.append(name);
            stringBuilder.append("->");
            stringBuilder.append(filterConfig.getInitParameter(name));
            if (iter.hasNext()) {
                stringBuilder.append(", ");
            }
        }
        stringBuilder.append("]");
        
        this.filterConfig = filterConfig;
        
        LOGGER.info(String.format("%d: init(FilterConfig filterConfig = %s) ...", System.identityHashCode(this), stringBuilder.toString()));
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        LOGGER.info(String.format("%d: doFilter(), url = %s", System.identityHashCode(this), httpServletRequest.getRequestURL()));
        if (true) {
            int counter = this.requestCounter.getAndIncrement();
            AbstractTracer tracer = TracerFactory.getInstance().takeTracer();
            tracer.initCurrentTracingContext();
            tracer.entry("void", this, "doFilter(ServletRequest request, ServletResponse response, FilterChain chain)");
            try {
                tracer.out().printfIndentln("method = %s", httpServletRequest.getMethod());
                tracer.out().printfIndentln("contextPath = %s", httpServletRequest.getContextPath());
                tracer.out().printfIndentln("requestCounter = %d", counter);
                if (counter == 0) {
                    traceProperties();
                }
                traceMemory();
                traceRequest(httpServletRequest);
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                tracer.wayout();
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
    
    void traceProperties() {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentQueueTracer();
        tracer.entry("void", this, "traceProperties()");

        try {
            System.getProperties().stringPropertyNames().stream()
                    .sorted()
                    .forEach(name -> tracer.out().printfIndentln("%s = %s", name, System.getProperty(name)));
        } finally {
            tracer.wayout();
        }
    }
    
    void traceMemory() {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentQueueTracer();
        tracer.entry("void", this, "traceMemory()");

        try {
            final long MEGA_BYTE = 1024 * 1024;

            long maxMemory = Runtime.getRuntime().maxMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;

            tracer.out().printfIndentln("maxMemory = %dMB", maxMemory / MEGA_BYTE);
            tracer.out().printfIndentln("totalMemory = %dMB", totalMemory / MEGA_BYTE);
            tracer.out().printfIndentln("freeMemory = %dMB", freeMemory / MEGA_BYTE);
            tracer.out().printfIndentln("usedMemory = %dMB", usedMemory / MEGA_BYTE);
        } finally {
            tracer.wayout();
        }
    }
    
    void traceRequest(HttpServletRequest request) {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentQueueTracer();
        tracer.entry("void", this, "traceRequest(HttpServletRequest request)");

        try {
            tracer.out().printfIndentln("(*) Request properties");
            tracer.out().printfIndentln("RequestURL = %s", request.getRequestURL().toString());
            tracer.out().printfIndentln("RequestURI = %s", request.getRequestURI());
            tracer.out().printfIndentln("ContextPath = %s", request.getContextPath());
            tracer.out().printfIndentln("ServletPath = %s", request.getServletPath());
            tracer.out().printfIndentln("RemoteAddr = %s", request.getRemoteAddr());
            tracer.out().printfIndentln("RemoteHost = %s", request.getRemoteHost());
            tracer.out().printfIndentln("RemotePort = %s", request.getRemotePort());
            tracer.out().printfIndentln("RemoteUser = %s", request.getRemoteUser());
            tracer.out().printfIndentln("AuthType = %s", request.getAuthType());
            tracer.out().printfIndentln("Method = %s", request.getMethod());
            tracer.out().printfIndentln("CharacterEncoding = %s", request.getCharacterEncoding());
            tracer.out().printfIndentln("RequestedSessionId = %s", request.getRequestedSessionId());
            tracer.out().printfIndentln("RequestedSessionIdValid = %b", request.isRequestedSessionIdValid());
            if (request.getUserPrincipal() != null) {
                tracer.out().printfIndentln("request.getUserPrincipal().getName() = %s", request.getUserPrincipal().getName());
            }

            tracer.out().printfIndentln("(*) Request header");
            if (request.getHeaderNames() != null) {
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    tracer.out().printfIndentln("header[%s] = %s", headerName, request.getHeader(headerName));
                }
            }

            tracer.out().printfIndentln("(*) Session");
            HttpSession httpSession = request.getSession(false);
            if (httpSession != null) {
                tracer.out().printfIndentln("httpSession.isNew() = %b", httpSession.isNew());
                tracer.out().printfIndentln("id = %s", httpSession.getId());
                tracer.out().printfIndentln("creationTime = %tT", new Date(httpSession.getCreationTime()));
                tracer.out().printfIndentln("lastAccessedTime = %tT", new Date(httpSession.getLastAccessedTime()));
                tracer.out().printfIndentln("maxInactiveInterval = %s seconds", httpSession.getMaxInactiveInterval());

                Enumeration<String> attributNames = httpSession.getAttributeNames();
                while (attributNames.hasMoreElements()) {
                    String attributName = attributNames.nextElement();
                    tracer.out().printfIndentln("httpSession.getAttribute(%s).getClass().getName() = %s", attributName, httpSession.getAttribute(attributName).getClass().getName());
                }
            }
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public void destroy() {
        LOGGER.info(String.format("%d: destroy() ...", System.identityHashCode(this)));
    }
    
}
