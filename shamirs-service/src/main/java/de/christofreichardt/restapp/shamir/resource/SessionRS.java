/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Metadata;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.MetadataService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
@Path("")
public class SessionRS extends BaseRS {

    @Autowired
    SessionService sessionService;

    @Autowired
    KeystoreService keystoreService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    ScheduledExecutorService scheduledExecutorService;

    @Autowired
    @Qualifier("singleThreadExecutor")
    ExecutorService executorService;

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

            return ok(sessionsInfo);
        } finally {
            tracer.wayout();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{keystoreId}/sessions/{sessionId}")
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
                return badRequest(errorMessage);
            }

            Response response;
            if (activationPointer.containsValue(sessionInstructions) && activationPointer.getValue(sessionInstructions).getValueType() == JsonValue.ValueType.OBJECT) {
                JsonPointer jsonPointer = Json.createPointer("/session/activation/automaticClose/idleTime");
                if (!jsonPointer.containsValue(sessionInstructions) || jsonPointer.getValue(sessionInstructions).getValueType() != JsonValue.ValueType.NUMBER) {
                    response = badRequest(errorMessage);
                } else {
                    response = activateSession(keystoreId, sessionId, sessionInstructions.getJsonObject("session"));
                }
            } else if (closurePointer.containsValue(sessionInstructions) && closurePointer.getValue(sessionInstructions).getValueType() == JsonValue.ValueType.OBJECT) {
                response = closeSession(keystoreId, sessionId, sessionInstructions);
            } else {
                response = badRequest(errorMessage);
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
            Optional<DatabasedKeystore> dbKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(keystoreId);
            if (dbKeystore.isEmpty()) {
                return badRequest(String.format("No such Keystore[id=%s].", keystoreId));
            }
            if (dbKeystore.get().getSessions().isEmpty()) {
                return internalServerError(String.format("Couldn't retrieve a session for Keystore[id=%s].", keystoreId));
            }
            Session currentSession = dbKeystore.get().getSessions().iterator().next();
            if (!Objects.equals(currentSession.getId(), sessionId)) {
                return badRequest(String.format("No active Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId));
            }

            try {
                ShamirsProtection shamirsProtection = new ShamirsProtection(dbKeystore.get().sharePoints());
                KeyStore keyStore = dbKeystore.get().keystoreInstance();
                currentSession.setPhase(SessionPhase.ACTIVE);
                JsonObject automaticClose = sessionInstructions.getJsonObject("activation").getJsonObject("automaticClose");
                int idleTime = automaticClose.getInt("idleTime");
                TemporalUnit temporalUnit = ChronoUnit.valueOf(automaticClose.getString("temporalUnit", "SECONDS"));
                Duration duration = Duration.of(idleTime, temporalUnit);
                currentSession.setIdleTime(duration.getSeconds());
                currentSession.setModificationTime(LocalDateTime.now());
                currentSession.setExpirationTime(currentSession.getModificationTime().plusSeconds(duration.getSeconds()));
                this.sessionService.save(currentSession);
                List<Metadata> pendingDocuments = this.metadataService.findPendingBySession(sessionId);
                DocumentProcessor documentProcessor = new DocumentProcessor(pendingDocuments, shamirsProtection, keyStore);
                CompletableFuture.supplyAsync(() -> documentProcessor.processAll(), this.executorService)
                        .thenAcceptAsync(metadatas -> this.metadataService.saveAll(metadatas), this.executorService);

                response = ok(currentSession.toJson());
            } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
                response = badRequest(String.format("Cannot activate session for Keystore[id=%s].", keystoreId), ex);
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
                return badRequest(String.format("No such Keystore[id=%s].", keystoreId));
            }
            if (databasedKeystore.get().getSessions().isEmpty()) {
                return internalServerError(String.format("Couldn't retrieve a session for Keystore[id=%s].", keystoreId));
            }
            Session currentSession = databasedKeystore.get().getSessions().iterator().next();
            if (!Objects.equals(currentSession.getId(), sessionId)) {
                return badRequest(String.format("No such Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId));
            }
            if (SessionPhase.ACTIVE != currentSession.getPhase()) {
                return badRequest(String.format("Session[id=%s] isn't active.", sessionId));
            }

            this.scheduledExecutorService.schedule(new SessionClosureService(databasedKeystore.get()), 0, TimeUnit.SECONDS);

            return Response.noContent().build();
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{keystoreId}/sessions/{sessionId}")
    public Response session(@PathParam("keystoreId") String keystoreId, @PathParam("sessionId") String sessionId) {
        AbstractTracer tracer = getCurrentTracer();
        String method = "session(String keystoreId, String sessionId)";
        tracer.entry("Response", this, method);

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            tracer.out().printfIndentln("sessionId = %s", sessionId);

            Optional<Session> session = this.sessionService.findByID(sessionId);
            if (session.isEmpty()) {
                return badRequest(String.format("No such Session[id=%s].", sessionId));
            }
            if (!Objects.equals(session.get().getKeystore().getId(), keystoreId)) {
                return badRequest(String.format("No Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId));
            }

            return ok(session.get().toJson(true));
        } finally {
            tracer.wayout();
        }
    }

}
