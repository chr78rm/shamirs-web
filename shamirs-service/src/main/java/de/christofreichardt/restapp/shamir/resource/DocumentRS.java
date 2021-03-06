/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Document;
import de.christofreichardt.restapp.shamir.model.Metadata;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.model.XMLDocument;
import de.christofreichardt.restapp.shamir.service.DocumentService;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.MetadataService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
public class DocumentRS extends BaseRS {

    @Autowired
    SessionService sessionService;

    @Autowired
    MetadataService metadataService;

    @Autowired
    DocumentService documentService;

    @Autowired
    KeystoreService keystoreService;

    @Autowired
    @Qualifier("singleThreadExecutor")
    ExecutorService executorService;

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sessions/{sessionId}/documents")
    public Response processDocument(
            @PathParam("sessionId") String sessionId,
            @QueryParam("action") String action,
            @QueryParam("alias") String alias,
            @HeaderParam("content-type") String contentType,
            @HeaderParam("doc-title") String docTitle,
            InputStream inputStream) throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        final String methodSignature = "processDocument(String sessionId, String action, String alias, String contentType, String docTitle, InputStream inputStream)";
        tracer.entry("Response", this, methodSignature);

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            tracer.out().printfIndentln("action = %s", action);
            tracer.out().printfIndentln("alias = %s", alias);
            tracer.out().printfIndentln("contentType = %s", contentType);
            tracer.out().printfIndentln("docTitle = %s", docTitle);

            // guard clauses
            if (!contentType.equalsIgnoreCase("application/xml")) {
                return badRequest(String.format("Only 'application/xml' documents are supported yet.", sessionId));
            }
            if (!(Objects.equals(MetadataAction.SIGN.name(), action) || Objects.equals(MetadataAction.VERIFY.name(), action))) {
                return badRequest(String.format("Only signing and validating are supported.", sessionId));
            }
            Optional<Session> session = this.sessionService.findByIDWithMetadata(sessionId); // TODO: think about fetching the slices as well
            if (session.isEmpty()) {
                return badRequest(String.format("No such Session[id=%s].", sessionId));
            }
            if (session.get().isClosed() || session.get().isNew()) {
                return badRequest(String.format("Illegal state for Session[id=%s].", sessionId));
            }

            try {
                byte[] bytes = inputStream.readAllBytes();
                Metadata metadata = new Metadata(docTitle);
                String documentId = metadata.getId();
                metadata.setSession(session.get());
                metadata.pending();
                metadata.setAction(Enum.valueOf(MetadataAction.class, action));
                metadata.setAlias(alias);
                metadata.setMediaType(contentType);
                Document document = new XMLDocument(documentId);
                document.setContent(bytes);
                document.setMetadata(metadata);
                metadata.setDocument(document);
                session.get().getMetadatas().add(metadata);
                session.get().modified();
                this.sessionService.save(session.get());
                Response.Status responseStatus = Response.Status.CREATED;
                if (session.get().isActive()) {
                    Optional<DatabasedKeystore> dbKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(session.get().getKeystore().getId()); // TODO: check error conditions
                    ShamirsProtection shamirsProtection = new ShamirsProtection(dbKeystore.get().sharePoints());
                    KeyStore keyStore = dbKeystore.get().keystoreInstance();
                    DocumentProcessor documentProcessor = new DocumentProcessor(List.of(metadata), shamirsProtection, keyStore);
                    Future<?> future = CompletableFuture.supplyAsync(() -> documentProcessor.processAll(), this.executorService)
                            .thenAcceptAsync(metadatas -> this.metadataService.saveAll(metadatas), this.executorService)
                            .toCompletableFuture();
                    try {
                        final int TIMEOUT = 250; // TODO: think about configuration
                        future.get(TIMEOUT, TimeUnit.MILLISECONDS);
                    } catch (TimeoutException timeoutException) {
                        tracer.logMessage(LogLevel.INFO, String.format("Timeout expired for %s. Moving on ...", document), getClass(),
                                "processDocument(String sessionId, String action, String alias, String contentType, String docTitle, InputStream inputStream)");
                        responseStatus = Response.Status.ACCEPTED;
                    }
                }
                metadata = this.metadataService.findById(documentId).orElseThrow();

                return Response
                        .status(responseStatus)
                        .entity(metadata.toJson())
                        .type(MediaType.APPLICATION_JSON)
                        .encoding("UTF-8")
                        .build();
            } catch (IOException | GeneralSecurityException ex) {
                tracer.logException(LogLevel.ERROR, ex, getClass(), "processDocument(String sessionId, String action, String alias, InputStream inputStream)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, ex.getMessage());
                return errorResponse.build();
            } catch (ExecutionException | RuntimeException ex) {
                tracer.logException(LogLevel.ERROR, ex, getClass(), "processDocument(String sessionId, String action, String alias, InputStream inputStream)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, ex.getMessage());
                return errorResponse.build();
            }
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sessions/{sessionId}/documents")
    public Response documents(@PathParam("sessionId") String sessionId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "documents(String sessionId)");

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);

            JsonArray metadatas = this.metadataService.findAllBySession(sessionId).stream()
                    .peek(metadata -> tracer.out().printfIndentln("metadata = %s", metadata))
                    .map(metadata -> metadata.toJson())
                    .collect(new JsonValueCollector());
            JsonObject metadataInfo = Json.createObjectBuilder()
                    .add("documents", metadatas)
                    .build();

            return ok(metadataInfo);
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sessions/{sessionId}/metadata/{documentId}")
    public Response metadata(@PathParam("sessionId") String sessionId, @PathParam("documentId") String documentId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "metadata(String sessionId, String documentId)");

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            tracer.out().printfIndentln("documentId = %s", documentId);

            Optional<Metadata> metadata = this.metadataService.findById(documentId);
            if (metadata.isEmpty()) {
                String message = String.format("No such Document[id=%s].", documentId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "document(String sessionId, String documentId)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                return errorResponse.build();
            }

            return Response.status(Response.Status.OK)
                    .entity(metadata.get().toJson(true))
                    .type(MediaType.APPLICATION_JSON)
                    .encoding("UTF-8")
                    .build();
        } finally {
            tracer.wayout();
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Path("sessions/{sessionId}/documents/{documentId}")
    public Response document(@PathParam("sessionId") String sessionId, @PathParam("documentId") String documentId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "document(String sessionId, String documentId)");

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            tracer.out().printfIndentln("documentId = %s", documentId);

            Optional<Document> document = this.documentService.findById(documentId);
            if (document.isEmpty()) {
                String message = String.format("No such Document[id=%s].", documentId);
                tracer.logMessage(LogLevel.ERROR, message, getClass(), "document(String sessionId, String documentId)");
                ErrorResponse errorResponse = new ErrorResponse(Response.Status.BAD_REQUEST, message);
                return errorResponse.build();
            }

            return Response.status(Response.Status.OK)
                    .entity(document.get().getContent())
                    .type(MediaType.APPLICATION_OCTET_STREAM)
                    .build();
        } finally {
            tracer.wayout();
        }
    }

}
