/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Developer
 */
@TestMethodOrder(OrderAnnotation.class)
public class SessionResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    public SessionResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    @Disabled
    void postSessionInstructions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postSessionInstructions()");

        try {
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("automaticClose", Json.createObjectBuilder()
                                    .add("idleTime", 30)
                                    .add("temporalUnit", ChronoUnit.SECONDS.name())
                            )
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
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);

            response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(sessionInstructions));

            tracer.out().printfIndentln("response = %s", response);
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Disabled
    void postInstructionsForUnknownKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postInstructionsForUnknownKeystore()");

        try {
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("automaticClose", Json.createObjectBuilder()
                                    .add("idleTime", 30)
                                    .add("temporalUnit", ChronoUnit.SECONDS.name())
                            )
                    )
                    .build();

            final String KEYSTORE_ID = UUID.randomUUID().toString(); // with virtual certainty an unkown keystore 

            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(sessionInstructions));

            tracer.out().printfIndentln("response = %s", response);
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(1)
    @Disabled
    void emptyInstructions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "emptyInstructions()");

        try {
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .addNull("session")
                    .build();

            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore

            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(sessionInstructions));

            tracer.out().printfIndentln("response = %s", response);
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(2)
    @Disabled
    void incompleteInstructions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "incompleteInstructions()");

        try {
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("automaticClose", Json.createObjectBuilder()
                                    .add("temporalUnit", ChronoUnit.SECONDS.name())
                            )
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
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void sessionsByKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "sessionsByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore

            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            tracer.out().printfIndentln("response = %s", response);
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void updateSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "sessionsByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("automaticClose", Json.createObjectBuilder()
                                    .add("idleTime", 30)
                                    .add("temporalUnit", ChronoUnit.SECONDS.name())
                            )
                    )
                    .build();
            
            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions));

            tracer.out().printfIndentln("response = %s", response);
            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.NO_CONTENT);
        } finally {
            tracer.wayout();
        }
    }
}
