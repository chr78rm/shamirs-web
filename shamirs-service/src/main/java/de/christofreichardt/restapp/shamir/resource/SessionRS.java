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
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Objects;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.NoResultException;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
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
@Path("keystores/{keystoreId}/sessions")
public class SessionRS implements Traceable {

    @Autowired
    SessionService sessionService;

    @Autowired
    KeystoreService keystoreService;

    final JsonTracer jsonTracer = new JsonTracer() {
        @Override
        public AbstractTracer getCurrentTracer() {
            return SessionRS.this.getCurrentTracer();
        }
    };

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response createSesssion(@PathParam("keystoreId") String keystoreId, JsonObject sessionInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "createSesssion(String keystoreId, JsonObject sessionInstructions)");

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);

            final int DEFAULT_IDLE_TIME = 1800; // seconds

            Response response;

            try {
                DatabasedKeystore keystore = this.keystoreService.findByIdWithActiveSlices(keystoreId);
                
                Optional<Session> latestSession = this.sessionService.findLatestByKeystore(keystoreId)
                        .filter(s -> Objects.equals(s.getPhase(), Session.Phase.PENDING.name()) || Objects.equals(s.getPhase(), Session.Phase.ACTIVE.name()));
                if (latestSession.isEmpty()) {
                    if (sessionInstructions.isNull("session")) {
                        throw new IllegalArgumentException("No session instructions found.");
                    }
                    Session session = new Session();
                    try {
                        ShamirsProtection shamirsProtection = new ShamirsProtection(keystore.sharePoints());
                        session.setPhase(Session.Phase.ACTIVE.name());
                    } catch (IllegalArgumentException ex) {
                        session.setPhase(Session.Phase.PENDING.name());
                    }
                    session.setKeystore(keystore);
                    int idleTime;
                    TemporalUnit temporalUnit;
                    if (Objects.nonNull(sessionInstructions.getJsonObject("session")
                            .getJsonObject("automaticClose"))) {
                        JsonObject automaticClose = sessionInstructions.getJsonObject("session")
                                .getJsonObject("automaticClose");
                        if (!automaticClose.containsKey("idleTime")) {
                            throw new IllegalArgumentException("Incomplete instructions.");
                        }
                        idleTime = automaticClose.getInt("idleTime");
                        temporalUnit = ChronoUnit.valueOf(automaticClose.getString("temporalUnit", "SECONDS"));
                    } else {
                        idleTime = DEFAULT_IDLE_TIME;
                        temporalUnit = ChronoUnit.SECONDS;
                    }
                    Duration duration = Duration.of(idleTime, temporalUnit);
                    session.setIdleTime(duration.getSeconds());
                    this.sessionService.save(session);
                    
                    response = Response.status(Response.Status.CREATED)
                            .entity(session.toJson())
                            .type(MediaType.APPLICATION_JSON)
                            .encoding("UTF-8")
                            .build();
                } else {
                    tracer.logMessage(LogLevel.WARNING,
                            String.format("Active or pending session found for keystore with id=%s.", keystoreId),
                            SessionRS.class,
                            "createSesssion(String keystoreId, JsonObject sessionInstructions)");
                    
                    JsonObject hint = Json.createObjectBuilder()
                            .add("status", Response.Status.BAD_REQUEST.getStatusCode())
                            .add("reason", Response.Status.BAD_REQUEST.getReasonPhrase())
                            .add("message", String.format("Active or pending session found for keystore with id=%s.", keystoreId))
                            .build();
                    
                    response = Response.status(Response.Status.BAD_REQUEST)
                            .entity(hint)
                            .type(MediaType.APPLICATION_JSON)
                            .encoding("UTF-8")
                            .build();
                }
            } catch (IllegalArgumentException | NoResultException ex) {
                tracer.logException(LogLevel.ERROR, ex, SessionRS.class, "createSesssion(String keystoreId, JsonObject sessionInstructions)");
                
                JsonObject hint = Json.createObjectBuilder()
                        .add("status", Response.Status.BAD_REQUEST.getStatusCode())
                        .add("reason", Response.Status.BAD_REQUEST.getReasonPhrase())
                        .add("message", ex.getMessage())
                        .build();

                response = Response.status(Response.Status.BAD_REQUEST)
                        .entity(hint)
                        .type(MediaType.APPLICATION_JSON)
                        .encoding("UTF-8")
                        .build();
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
