/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonAnyObjectConstraint;
import de.christofreichardt.json.JsonArrayConstraint;
import de.christofreichardt.json.JsonNumberConstraint;
import de.christofreichardt.json.JsonObjectConstraint;
import de.christofreichardt.json.JsonStringConstraint;
import de.christofreichardt.json.JsonValueConstraint;
import java.math.BigDecimal;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.json.JsonStructure;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Developer
 */
abstract public class BaseRS implements Traceable {
    
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    
    class MyJsonStringConstraint extends JsonStringConstraint {

        public MyJsonStringConstraint(String regex) {
            super(regex);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return BaseRS.this.getCurrentTracer();
        }

    }

    class MyJsonObjectConstraint extends JsonObjectConstraint {

        public MyJsonObjectConstraint(Map<String, JsonValueConstraint> constraints) {
            super(constraints);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return BaseRS.this.getCurrentTracer();
        }

    }
    
    class MyJsonAnyObjectConstraint extends JsonAnyObjectConstraint {

        public MyJsonAnyObjectConstraint() {
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return BaseRS.this.getCurrentTracer();
        }
    }

    class MyJsonNumberConstraint extends JsonNumberConstraint {

        public MyJsonNumberConstraint(String regex) {
            super(regex);
        }

        public MyJsonNumberConstraint(String regex, BigDecimal minimum, BigDecimal maximum) {
            super(regex, minimum, maximum);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return BaseRS.this.getCurrentTracer();
        }

    }
    
    class MyJsonArrayConstraint extends JsonArrayConstraint {

        public MyJsonArrayConstraint(int minSize, int maxSize, JsonValueConstraint... constraints) {
            super(minSize, maxSize, constraints);
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return BaseRS.this.getCurrentTracer();
        }
    }
    
    @PostConstruct
    protected void init() {
        LOGGER.info(String.format("Creating resource component '%s'...", getClass().getSimpleName()));
    }
    
    @PreDestroy
    protected void exit() {
        LOGGER.info(String.format("Destroying resource component '%s'...", getClass().getSimpleName()));
    }
    
    Response makeErrorResponse(String message, String method, Response.Status status) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.logMessage(LogLevel.ERROR, message, getClass(), method);
        ErrorResponse errorResponse = new ErrorResponse(status, message);
        
        return errorResponse.build();
    }
    
    Response badRequest(String message) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements.length >= 3 ? stackTraceElements[2].getMethodName() : "-";
        
        return makeErrorResponse(message, methodName, Response.Status.BAD_REQUEST);
    }
    
    Response badRequest(String message, Exception ex) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements.length >= 3 ? stackTraceElements[2].getMethodName() : "-";
        AbstractTracer tracer = getCurrentTracer();
        tracer.logException(LogLevel.ERROR, ex, getClass(), methodName);
        ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message, ex.getMessage());
        
        return errorResponse.build();
    }
    
    Response internalServerError(String message) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements.length >= 3 ? stackTraceElements[2].getMethodName() : "-";
        
        return makeErrorResponse(message, methodName, Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    Response internalServerError(String message, Exception ex) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements.length >= 3 ? stackTraceElements[2].getMethodName() : "-";
        AbstractTracer tracer = getCurrentTracer();
        tracer.logException(LogLevel.ERROR, ex, getClass(), methodName);
        ErrorResponse errorResponse = new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message, ex.getMessage());
        
        return errorResponse.build();
    }

    Response ok(JsonStructure jsonStructure) {
        return Response.status(Response.Status.OK)
                .entity(jsonStructure)
                .type(MediaType.APPLICATION_JSON)
                .encoding("UTF-8")
                .build();
    }
    
    Response created(JsonStructure jsonStructure) {
        return Response.status(Response.Status.CREATED)
                .entity(jsonStructure)
                .type(MediaType.APPLICATION_JSON)
                .encoding("UTF-8")
                .build();
    }
    
    Response notFound(String message) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String methodName = stackTraceElements.length >= 3 ? stackTraceElements[2].getMethodName() : "-";
        
        return makeErrorResponse(message, methodName, Response.Status.NOT_FOUND);
    }
    
    Response noContent() {
        return Response.noContent().build();
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
}
