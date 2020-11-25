/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Participant;
import de.christofreichardt.restapp.shamir.model.Slice;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.ParticipantService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
@Path("keystores")
public class KeystoreRS implements Traceable {

    @Autowired
    KeystoreService keystoreService;

    @Autowired
    ParticipantService participantService;
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
    public Response createKeystore(JsonObject keystoreInstructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "createKeystore(JsonObject keystoreInstructions)");

        try {
            Response response;
            String descriptiveName = keystoreInstructions.getString("descriptiveName");
            try {
                KeystoreGenerator keystoreGenerator = new KeystoreGenerator(keystoreInstructions);

                DatabasedKeystore keystore = new DatabasedKeystore();
                keystore.setDescriptiveName(descriptiveName);
                keystore.setStore(keystoreGenerator.keystoreBytes());

                Map<String, byte[]> partition = keystoreGenerator.partition();
                Set<Slice> slices = partition.entrySet().stream()
                        .map(entry -> {
                            Participant participant = this.participantService.findByPreferredName(entry.getKey());
                            tracer.out().printfIndentln("participant = %s", participant);
                            Slice slice = new Slice();
                            slice.setParticipant(participant);
                            slice.setPartitionId(keystoreGenerator.partitionId());
                            slice.setShare(entry.getValue());
                            slice.setProcessingState("CREATED");
                            slice.setKeystore(keystore);

                            return slice;
                        })
                        .collect(Collectors.toSet());

                keystore.setSlices(slices);
                this.keystoreService.persist(keystore);

                response = Response.status(Response.Status.CREATED)
                        .entity(keystore.toJson())
                        .type(MediaType.APPLICATION_JSON)
                        .encoding("UTF-8")
                        .build();
            } catch (GeneralSecurityException | IOException ex) {
                JsonObject hint = Json.createObjectBuilder()
                        .add("status", 500)
                        .add("reason", "Internal Server Error")
                        .add("message", ex.getMessage())
                        .build();

                response = Response.status(Response.Status.CREATED)
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
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("")
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
    @Path("{id}")
    public Response updateKeystore(@PathParam("id") String id, JsonObject jsonObject) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("JsonObject", this, "updateKeystore(String id, JsonObject jsonObject)");

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
    @Path("{id}")
    public Response keystore(@PathParam("id") String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "keystore()");

        try {
            tracer.out().printfIndentln("id = %s", id);
            
            JsonObject dummy = Json.createObjectBuilder()
                    .add("dummy", "test")
                    .build();
            return Response.status(Response.Status.OK)
                    .entity(dummy)
                    .type(MediaType.APPLICATION_JSON)
                    .encoding("UTF-8")
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