package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.ShamirsApp;
import java.util.concurrent.CountDownLatch;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
@Path("/management")
public class ManagementRS extends BaseRS {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("restart")
    public Response restart(JsonObject jsonObject) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Response", this, "restart(JsonObject jsonObject)");

        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            try {
                JsonObject message = Json.createObjectBuilder()
                        .add("message", "Restarting the service ...")
                        .build();
                
                ShamirsApp.restart(countDownLatch);
                
                return ok(message);
            } finally {
                countDownLatch.countDown();
            }
        } finally {
            tracer.wayout();
        }
    }

}
