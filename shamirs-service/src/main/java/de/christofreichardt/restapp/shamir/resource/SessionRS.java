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
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.util.Objects;
import java.util.Optional;
import javax.json.JsonObject;
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

            Optional<DatabasedKeystore> keystore = this.keystoreService.findbyId(keystoreId); // TODO: load with active slices to determine the phase of the to be created session
            keystore.ifPresentOrElse(k -> {
                Optional<Session> latestSession = this.sessionService.findLatestByKeystore(keystoreId)
                        .filter(s -> Objects.equals(s.getPhase(), Session.Phase.PENDING.name()) || Objects.equals(s.getPhase(), Session.Phase.ACTIVE.name()));
                if (latestSession.isEmpty()) {
                    Session session = new Session();
                    session.setPhase(Session.Phase.PENDING.name());
                    session.setKeystore(k);
                    this.sessionService.save(session);
                } else {
                    tracer.logMessage(LogLevel.WARNING, 
                            String.format("Active or pending session found for keystore with id=%s.", keystoreId), 
                            SessionRS.class, 
                            "createSesssion(String keystoreId, JsonObject sessionInstructions)");
                }
            }, () -> tracer.logMessage(LogLevel.WARNING, 
                    String.format("No keystore found for id=%s.", keystoreId), 
                    SessionRS.class, 
                    "createSesssion(String keystoreId, JsonObject sessionInstructions)")
            );

            return Response.noContent()
                    .status(Response.Status.NO_CONTENT)
                    .build();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
