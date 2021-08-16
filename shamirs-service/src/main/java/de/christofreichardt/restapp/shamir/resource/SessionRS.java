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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
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

    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{keystoreId}/sessions/{sessionId}")
    public Response patchSession(@PathParam("keystoreId") String keystoreId, @PathParam("sessionId") String sessionId, JsonObject sessionInstructions) throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "patchSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            
            // guard clauses
            if (!sessionInstructions.containsKey("id") || sessionInstructions.get("id").getValueType() != JsonValue.ValueType.STRING) {
                return badRequest("Malformed patch.");
            }
            if (!Objects.equals(sessionId, sessionInstructions.getString("id"))) {
                return badRequest("SessionIds don't match.");
            }
            if (!sessionInstructions.containsKey("phase") || sessionInstructions.get("phase").getValueType() != JsonValue.ValueType.STRING) {
                return badRequest("Malformed patch.");
            }
            String phase = sessionInstructions.getString("phase");
            if (!SessionPhase.isValid(phase)) {
                return badRequest("Malformed patch.");
            }
            Optional<DatabasedKeystore> dbKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(keystoreId);
            if (dbKeystore.isEmpty()) {
                return notFound(String.format("No such Keystore[id=%s].", keystoreId));
            }
            Session currentSession = dbKeystore.get().currentSession();
            if (!Objects.equals(currentSession.getId(), sessionId)) {
                return badRequest(String.format("No present Session[id=%s] found for Keystore[id=%s].", sessionId, keystoreId));
            }

            // dispatch
            if (SessionPhase.valueOf(phase) == SessionPhase.ACTIVE) {
                return activateSession(dbKeystore.get(), currentSession, sessionInstructions);
            } else if (SessionPhase.valueOf(phase) == SessionPhase.CLOSED) {
                return closeSession(dbKeystore.get(), currentSession);
            }

            return internalServerError("Something went wrong.");
        } finally {
            tracer.wayout();
        }
    }
    
    private Response activateSession(DatabasedKeystore dbKeystore, Session currentSession, JsonObject sessionInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "activateSession(String keystoreId, String sessionId, JsonObject sessionInstructions)");

        try {
            if (!sessionInstructions.containsKey("idleTime") || sessionInstructions.get("idleTime").getValueType() != JsonValue.ValueType.NUMBER) {
                return badRequest("Malformed patch.");
            }
            try {
                ShamirsProtection shamirsProtection = new ShamirsProtection(dbKeystore.sharePoints()); // TODO: think about checking if enough sharepoints are available beforehand
                KeyStore keyStore = dbKeystore.keystoreInstance();
                int idleTime = sessionInstructions.getInt("idleTime");
                Duration duration = Duration.of(idleTime, ChronoUnit.SECONDS);
                currentSession.activated(duration);
                this.sessionService.save(currentSession);
                List<Metadata> pendingDocuments = this.metadataService.findPendingBySession(currentSession.getId());
                DocumentProcessor documentProcessor = new DocumentProcessor(pendingDocuments, shamirsProtection, keyStore);
                CompletableFuture.supplyAsync(() -> documentProcessor.processAll(), this.executorService)
                        .thenAcceptAsync(metadatas -> this.metadataService.saveAll(metadatas), this.executorService);

                return ok(currentSession.toJson());
            } catch (GeneralSecurityException | IOException | IllegalArgumentException ex) {
                return badRequest(String.format("Cannot activate session for Keystore[id=%s].", dbKeystore.getId()), ex);
            }
        } finally {
            tracer.wayout();
        }
    }
    
    private Response closeSession(DatabasedKeystore dbKeystore, Session currentSession) throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "closeSession(DatabasedKeystore dbKeystore, Session currentSession)");

        try {
            if (!currentSession.isActive()) {
                return badRequest(String.format("Session[id=%s] isn't active.", currentSession.getId()));
            }

            ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.schedule(new SessionClosureService(dbKeystore), 0, TimeUnit.SECONDS);
            try {
                scheduledFuture.get(5, TimeUnit.SECONDS);
                Optional<Session> closedSession = this.sessionService.findByID(currentSession.getId());
                if (closedSession.isEmpty()) {
                    return internalServerError("Something went wrong.");
                }
                return ok(closedSession.get().toJson());
            } catch (ExecutionException ex) {
                return internalServerError("Something went wrong.");
            } catch (TimeoutException ex) {
                tracer.logException(LogLevel.WARNING, ex, getClass(), "nextCloseSession(DatabasedKeystore dbKeystore, Session currentSession, JsonObject sessionInstructions)");
                Response.noContent().build();
            }
            
            return internalServerError("Something went wrong.");
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
