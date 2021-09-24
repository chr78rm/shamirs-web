package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
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
public class PingRS extends BaseRS {

    @Autowired
    KeystoreService keystoreService;

    final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("ping")
    public Response ping() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "ping()");

        try {
            Optional<DatabasedKeystore> dbKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(KEYSTORE_ID);
            if (dbKeystore.isEmpty()) {
                return internalServerError("Something went wrong.");
            }
            dbKeystore.get().trace(tracer, true);

            return Response.status(Response.Status.OK)
                    .entity("ping")
                    .type(MediaType.TEXT_PLAIN)
                    .encoding("UTF-8")
                    .build();
        } finally {
            tracer.wayout();
        }
    }
}
