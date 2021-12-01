/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.json.JsonValueConstraint;
import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import de.christofreichardt.restapp.shamir.model.IllegalSliceProcessingStateException;
import de.christofreichardt.restapp.shamir.model.Slice;
import de.christofreichardt.restapp.shamir.service.SliceService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
public class SliceRS extends BaseRS {

    @Autowired
    SliceService sliceService;
    
    @Autowired
    SliceInstructionsValidator sliceInstructionValidator;
    
    @Autowired
    ShareValidator shareValidator;

    @PATCH
    @Path("/slices/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSlice(@PathParam("id") String id, JsonObject instructions) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "updateSlice(String id, JsonObject instructions)");

        try {
            tracer.out().printfIndentln("id = %s", id);

            // check if slice exists
            Optional<Slice> slice = this.sliceService.findById(id);
            if (slice.isEmpty()) {
                return notFound(String.format("No such Slice[id=%s].", id));
            }

            tracer.out().printfIndentln("slice = %s", slice.get());
            
            try {
                this.sliceInstructionValidator.check(instructions);

                // match the id against the path parameter
                if (!Objects.equals(id, instructions.getString("id"))) {
                    return badRequest(String.format("The transmitted id=%s, doesn't match the id of the resource [id=%s].", instructions.getString("id"), id));
                }

                // dispatch
                try {
                    if (SliceProcessingState.valueOf(instructions.getString("state")) == SliceProcessingState.FETCHED) {
                        if (!instructions.get("share").asJsonObject().entrySet().isEmpty()) {
                            return badRequest("Malformed patch.");
                        }
                        return fetchSlice(slice.get());
                    } else if (SliceProcessingState.valueOf(instructions.getString("state")) == SliceProcessingState.POSTED) {
                        return postSlice(slice.get(), instructions.getJsonObject("share"));
                    } else {
                        return badRequest("Malformed patch.");
                    }
                } catch (IllegalSliceProcessingStateException ex) {
                    return badRequest("Illegal processing state transition.", ex);
                } catch (Exception ex) {
                    return internalServerError("Something went wrong.");
                }
            } catch (JsonValueConstraint.Exception ex) {
                return badRequest("Malformed patch.", ex);
            }
        } finally {
            tracer.wayout();
        }
    }

    Response fetchSlice(Slice slice) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "fetchSlice(Slice slice)");

        try {
            slice.fetched();
            slice = this.sliceService.save(slice);

            return ok(slice.toJson(true));
        } finally {
            tracer.wayout();
        }
    }

    Response postSlice(Slice slice, JsonObject share) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "postSlice(Slice slice, JsonObject share)");

        try {
            try {
                this.shareValidator.check(share);
                
                if (!Objects.equals(share.getString("PartitionId"), slice.getPartitionId())) {
                    return badRequest(String.format("Unmatched partitionId '%s'.", slice.getPartitionId()));
                }
                if (slice.getSize() != share.getJsonArray("SharePoints").size()) {
                    return badRequest(String.format("Expected %d sharepoints but got %d.", slice.getSize(), share.getJsonArray("SharePoints").size()));
                }
                
                slice.posted(share);
                slice = this.sliceService.save(slice);
                
                return ok(slice.toJson(true));
            } catch (JsonValueConstraint.Exception ex) {
                return badRequest("Malformed share.", ex);
            }
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Path("/slices")
    @Produces(MediaType.APPLICATION_JSON)
    public Response querySlices(@QueryParam("keystoreId") String keystoreId, @QueryParam("participantId") String participantId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "querySlices(String keystoreId, String participantId)");

        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            tracer.out().printfIndentln("participantId = %s", participantId);

            List<Slice> slices;
            if (Objects.isNull(keystoreId) && Objects.isNull(participantId)) {
                slices = this.sliceService.findAll();
            } else if (Objects.isNull(participantId)) {
                slices = this.sliceService.findByKeystoreId(keystoreId);
            } else if (Objects.isNull(keystoreId)) {
                slices = this.sliceService.findByParticipantId(participantId);
            } else {
                slices = this.sliceService.findByKeystoreIdAndParticipantId(keystoreId, participantId);
            }
            JsonObject slicesInfo = Json.createObjectBuilder()
                    .add("slices", slices.stream()
                            .map(slice -> slice.toJson())
                            .collect(new JsonValueCollector()))
                    .build();

            return ok(slicesInfo);
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Path("/slices/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response singleSlice(@PathParam("id") String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "singleSlice(String id)");

        try {
            tracer.out().printfIndentln("id = %s", id);

            Optional<Slice> slice = this.sliceService.findById(id);
            if (slice.isEmpty()) {
                return notFound(String.format("No such Slice[id=%s].", id));
            }

            return ok(slice.get().toJson(true));
        } finally {
            tracer.wayout();
        }
    }
}
