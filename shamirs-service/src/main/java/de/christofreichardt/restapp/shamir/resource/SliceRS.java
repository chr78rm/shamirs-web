/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.Slice;
import de.christofreichardt.restapp.shamir.service.SliceService;
import java.util.List;
import java.util.Objects;
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

    @PATCH
    @Path("/slices/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateSlice(@PathParam("id") String id, JsonObject jsonObject) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "updateSlice(String id, JsonObject jsonObject)");

        try {
            tracer.out().printfIndentln("id = %s", id);
            tracer.out().printfIndentln("jsonObject = %s", jsonObject);

            JsonObject slice = Json.createObjectBuilder()
                    .add("state", "FETCHED")
                    .build();

            return ok(slice);
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
}
