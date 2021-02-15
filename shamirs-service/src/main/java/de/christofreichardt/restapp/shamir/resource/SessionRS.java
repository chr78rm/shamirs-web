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
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
@Path("")
public class SessionRS implements Traceable {
    
    @Autowired
    SessionService sessionService;

    @Autowired
    KeystoreService keystoreService;
    
    @Autowired
    ScheduledExecutorService scheduledExecutorService;

    final JsonTracer jsonTracer = new JsonTracer() {
        @Override
        public AbstractTracer getCurrentTracer() {
            return SessionRS.this.getCurrentTracer();
        }
    };

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{keystoreId}/sessions")
    public Response sessionsByKeystore(@PathParam("keystoreId") String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "sessionsByKeystore()");

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);

            JsonArray sessions = this.sessionService.findAllByKeystore(keystoreId)
                    .stream()
                    .peek(session -> tracer.out().printfIndentln("session = %s", session))
                    .map(session -> session.toJson())
                    .collect(new JsonValueCollector());
            JsonObject sessionsInfo = Json.createObjectBuilder()
                    .add("sessions", sessions)
                    .build();

            return Response.status(Response.Status.OK)
                    .entity(sessionsInfo)
                    .type(MediaType.APPLICATION_JSON)
                    .encoding("UTF-8")
                    .build();
        } finally {
            tracer.wayout();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{keystoreId}/sessions/{sessionId}")
    // TODO: phase transition from 'ACTIVE' to 'CLOSED'
    public Response updateSession(@PathParam("keystoreId") String keystoreId, @PathParam("sessionId") String sessionId, JsonObject sessionInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "updateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            tracer.out().printfIndentln("sessionId = %s", sessionId);

            JsonPointer sessionPointer = Json.createPointer("/session");
            JsonPointer activationPointer = Json.createPointer("/session/activation");
            JsonPointer closurePointer = Json.createPointer("/session/closure");
            String errorMessage = "Invalid session instructions.";
            
            if (!sessionPointer.containsValue(sessionInstructions) || sessionPointer.getValue(sessionInstructions).getValueType() != JsonValue.ValueType.OBJECT) {
                tracer.logMessage(LogLevel.ERROR, errorMessage, getClass(), "updateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, errorMessage);
                return errorResponse.build();
            }
            
            Response response;
            if (activationPointer.containsValue(sessionInstructions) && activationPointer.getValue(sessionInstructions).getValueType() == JsonValue.ValueType.OBJECT) {
                JsonPointer jsonPointer = Json.createPointer("/session/activation/automaticClose/idleTime");
                if (!jsonPointer.containsValue(sessionInstructions) || jsonPointer.getValue(sessionInstructions).getValueType() != JsonValue.ValueType.NUMBER) {
                    tracer.logMessage(LogLevel.ERROR, errorMessage, getClass(), "updateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                    ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, errorMessage);
                    response = errorResponse.build();
                } else {
                    response = activateSession(keystoreId, sessionId, sessionInstructions.getJsonObject("session"));
                }
            } else if (closurePointer.containsValue(sessionInstructions) && closurePointer.getValue(sessionInstructions).getValueType() == JsonValue.ValueType.OBJECT) {
                response = closeSession(keystoreId, sessionId, sessionInstructions);
            } else {
                tracer.logMessage(LogLevel.ERROR, errorMessage, getClass(), "updateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, errorMessage);
                response = errorResponse.build();
            }
            
            return response;
        } finally {
            tracer.wayout();
        }
    }
    
    Response activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");

        try {
            Response response;
            Optional<DatabasedKeystore> optionalKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(keystoreId);
            if (optionalKeystore.isPresent()) {
                DatabasedKeystore keystore = optionalKeystore.get();
                Session currentSession = keystore.getSessions().iterator().next();
                if (Objects.equals(currentSession.getId(), sessionId)) {
                    try {
                        ShamirsProtection shamirsProtection = new ShamirsProtection(keystore.sharePoints());
                        currentSession.setPhase(Session.Phase.ACTIVE.name());
                        JsonObject automaticClose = sessionInstructions.getJsonObject("activation").getJsonObject("automaticClose");
                        int idleTime = automaticClose.getInt("idleTime");
                        TemporalUnit temporalUnit = ChronoUnit.valueOf(automaticClose.getString("temporalUnit", "SECONDS"));
                        Duration duration = Duration.of(idleTime, temporalUnit);
                        currentSession.setIdleTime(duration.getSeconds());
                        currentSession.setModificationTime(LocalDateTime.now());
                        currentSession.setExpirationTime(currentSession.getModificationTime().plusSeconds(duration.getSeconds()));
                        this.sessionService.save(currentSession);

                        response = Response.status(Response.Status.OK)
                                .entity(currentSession.toJson())
                                .type(MediaType.APPLICATION_JSON)
                                .encoding("UTF-8")
                                .build();
                    } catch (IllegalArgumentException ex) {
                        String message = String.format("Cannot activate session for Keystore[id=%s].", keystoreId);
                        tracer.logException(LogLevel.ERROR, ex, getClass(), "activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                        ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message, ex.getMessage());
                        response = errorResponse.build();
                    }
                } else {
                    String message = String.format("No current Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId);
                    tracer.logMessage(LogLevel.ERROR, message, getClass(), "activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                    ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                    response = errorResponse.build();
                }
            } else {
                String message = String.format("No such Keystore[id=%s].", keystoreId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                response = errorResponse.build();
            }

            return response;
        } finally {
            tracer.wayout();
        }
    }
    
    class SessionClosureService implements Runnable, Traceable {
        
        final DatabasedKeystore keystore;

        public SessionClosureService(DatabasedKeystore keystore) {
            this.keystore = keystore;
        }

        @Override
        public void run() {
            AbstractTracer tracer = getCurrentTracer();
            tracer.entry("void", this, "run()");

            try {
                SessionRS.this.keystoreService.rollOver(this.keystore);
            } finally {
                tracer.wayout();
            }
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return TracerFactory.getInstance().getCurrentPoolTracer();
        }
        
    }
    
    Response closeSession(String keystoreId, String sessionId, JsonObject sessionInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "closeSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");

        try {
            Optional<DatabasedKeystore> databasedKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(keystoreId);
            if (databasedKeystore.isEmpty()) {
                String message = String.format("No such Keystore[id=%s].", keystoreId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "closeSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                return errorResponse.build();
            }
            if (databasedKeystore.get().getSessions().isEmpty()) {
                String message = String.format("Couldn't retrieve a session for Keystore[id=%s].", keystoreId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "closeSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message);
                return errorResponse.build();
            }
            Session currentSession = databasedKeystore.get().getSessions().iterator().next();
            if (!Objects.equals(currentSession.getId(), sessionId)) {
                String message = String.format("No such Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                return errorResponse.build();
            }
            
            this.scheduledExecutorService.schedule(new SessionClosureService(databasedKeystore.get()), 0, TimeUnit.SECONDS);
            
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{keystoreId}/sessions/{sessionId}")
    public Response session(@PathParam("keystoreId") String keystoreId, @PathParam("sessionId") String sessionId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "session(String keystoreId, String sessionId)");

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            tracer.out().printfIndentln("sessionId = %s", sessionId);

            Response response;

            Optional<Session> session = this.sessionService.findByID(sessionId);
            if (session.isPresent()) {
                if (session.get().getKeystore().getId().equals(keystoreId)) {
                    response = Response.status(Response.Status.OK)
                        .entity(session.get().toJson(true))
                        .type(MediaType.APPLICATION_JSON)
                        .encoding("UTF-8")
                        .build();
                } else {
                    String message = String.format("No Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId);
                    tracer.logMessage(LogLevel.ERROR, message, getClass(), "session(String keystoreId, String sessionId)");
                    ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                    response = errorResponse.build();
                }
            } else {
                String message = String.format("No such Session[id=%s].", sessionId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "session(String keystoreId, String sessionId)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                response = errorResponse.build();
            }
            
            return response;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
