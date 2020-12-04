/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Developer
 */
public class SessionResourceUnit extends ShamirsBaseUnit {

    public SessionResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    void postSessionInstructions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postSessionInstructions()");

        try {
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("automaticClose", Json.createObjectBuilder()
                            .add("idleTime", 30)
                    )
                    .build();
            
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            
            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(sessionInstructions));

            tracer.out().printfIndentln("response = %s", response);
        } finally {
            tracer.wayout();
        }
    }

}
