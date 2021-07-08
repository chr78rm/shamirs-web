/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.PATCH;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
@Path("")
public class SliceRS extends BaseRS {

    @PATCH
    @Path("/slices/{id}")
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
}
