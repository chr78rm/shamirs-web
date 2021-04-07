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
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import de.christofreichardt.restapp.shamir.model.Document;
import de.christofreichardt.restapp.shamir.model.Metadata;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.model.XMLDocument;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
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
public class DocumentRS implements Traceable {
    
    @Autowired
    SessionService sessionService;

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
        tracer.entry("Response", this, "processDocument(String sessionId, String action, String alias, InputStream inputStream)");

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            tracer.out().printfIndentln("action = %s", action);
            tracer.out().printfIndentln("alias = %s", alias);
            
            Optional<Session> session = this.sessionService.findByID(sessionId);
            if (session.isEmpty()) {
                String message = String.format("No such Session[id=%s].", sessionId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "processDocument(String sessionId, String action, String alias, InputStream inputStream)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                return errorResponse.build();
            }
            if (session.get().getPhase() != Session.Phase.PROVISIONED) {
                String message = "Currently, only provisioned sessions are supported.";
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "processDocument(String sessionId, String action, String alias, InputStream inputStream)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                return errorResponse.build();
            }
            
            try {
                byte[] bytes = inputStream.readAllBytes();
                Metadata metadata = new Metadata();
                metadata.setSession(session.get());
                metadata.setState(Metadata.Status.PENDING);
                metadata.setAction(Enum.valueOf(MetadataAction.class, action));
                metadata.setAlias(alias);
                Document document = new XMLDocument(metadata.getId());
                document.setContent(bytes);
                document.setMetadata(metadata);
                metadata.setDocument(document);
                List<Metadata> metadatas = new ArrayList<>();
                metadatas.add(metadata);
                session.get().setMetadatas(metadatas);
                this.sessionService.save(session.get());
            } catch (IOException ex) {
                tracer.logException(LogLevel.ERROR, ex, getClass(), "processDocument(String sessionId, String action, String alias, InputStream inputStream)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, ex.getMessage());
                return errorResponse.build();
            }
            
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
