/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Participant;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.ParticipantService;
import de.christofreichardt.restapp.shamir.service.ParticipantService.ParticipantNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.persistence.PersistenceException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
public class KeystoreRS extends BaseRS {

    @Autowired
    KeystoreService keystoreService;

    @Autowired
    ParticipantService participantService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores")
    public Response createKeystore(JsonObject keystoreInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "createKeystore(JsonObject keystoreInstructions)");

        try {
            try {
                KeystoreGenerator keystoreGenerator = new KeystoreGenerator(keystoreInstructions);
                
                Map<String, Participant> participants = this.participantService.findByPreferredNames(keystoreGenerator.participantNames());
                DatabasedKeystore keystore = keystoreGenerator.makeKeystore(participants);
                keystore = this.keystoreService.persist(keystore);
                
                return created(keystore.toJson());
            } catch (GeneralSecurityException | IOException | RuntimeException ex) {
                return internalServerError("Something went wrong.", ex);
            } catch(ParticipantNotFoundException ex) {
                return badRequest("Unknown participants.", ex);
            }
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores")
    public Response availableKeystores() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "availableKeystores()");

        try {
            JsonArray keystores = this.keystoreService.findAll().stream()
                    .peek(keystore -> tracer.out().printfIndentln("keystore = %s", keystore))
                    .map(keystore -> keystore.toJson())
                    .collect(new JsonValueCollector());
            JsonObject keystoresInfo = Json.createObjectBuilder()
                    .add("keystores", keystores)
                    .build();

            return Response.status(Response.Status.OK)
                    .entity(keystoresInfo)
                    .type(MediaType.APPLICATION_JSON)
                    .encoding("UTF-8")
                    .build();
        } finally {
            tracer.wayout();
        }
    }

    @PUT
    @Path("keystores/{id}")
    public Response updateKeystore(@PathParam("id") String id, JsonObject jsonObject) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "updateKeystore(String id, JsonObject jsonObject)");

        try {
            tracer.out().printfIndentln("id = %s", id);
            tracer.out().printfIndentln("jsonObject = %s", jsonObject);

            return Response.noContent()
                    .status(Response.Status.NO_CONTENT)
                    .build();
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{id}")
    public Response keystore(@PathParam("id") String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "keystore()");

        try {
            tracer.out().printfIndentln("id = %s", id);

            Response response;
            try {
                Optional<DatabasedKeystore> keystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(id);
                if (keystore.isEmpty()) {
                    tracer.logMessage(LogLevel.ERROR, String.format("No such Keystore[id=%s] found.", id), getClass(), "keystore(@PathParam(\"id\") String id)");
                    String message = String.format("No such Keystore[id=%s] found.", id);
                    ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                    return errorResponse.build();
                }

                keystore.get().trace(tracer, true);

                JsonObject jsonKeystore = keystore.get().toJson(true);
                response = Response.status(Response.Status.OK)
                        .entity(jsonKeystore)
                        .type(MediaType.APPLICATION_JSON)
                        .encoding("UTF-8")
                        .build();
            } catch (PersistenceException ex) {
                tracer.logException(LogLevel.ERROR, ex, getClass(), "keystore(@PathParam(\"id\") String id)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
                response = errorResponse.build();
            }

            return response;

        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("keystores/{id}/participants")
    public Response participantsByKeystore(@PathParam("id") String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "participantsByKeystore(String id)");

        try {
            tracer.out().printfIndentln("id = %s", id);

            JsonArray participants = this.participantService.findByKeystore(id).stream()
                    .map(participant -> participant.toJson())
                    .collect(new JsonValueCollector());

            JsonObject participantsInfo = Json.createObjectBuilder()
                    .add("participants", participants)
                    .build();

            return ok(participantsInfo);
        } finally {
            tracer.wayout();
        }
    }
}
