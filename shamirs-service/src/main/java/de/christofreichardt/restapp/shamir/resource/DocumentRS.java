/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
@Path("")
public class DocumentRS implements Traceable {

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sessions/{sessionId}/documents")
    public Response processDocument(
            @PathParam("sessionId") String sessionId, 
            @QueryParam("action") String action,
            @QueryParam("alias") String alias,
            InputStream inputStream) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "processDocument(String sessionId, InputStream inputStream)");

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            tracer.out().printfIndentln("action = %s", action);
            tracer.out().printfIndentln("alias = %s", alias);
            
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
