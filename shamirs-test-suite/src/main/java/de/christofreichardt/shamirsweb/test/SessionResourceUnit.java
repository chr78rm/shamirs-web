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
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.api.WithAssertions;
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
    void putInstructionsForUnknownKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "putInstructionsForUnknownKeystore()");

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
            final String SESSION_ID = UUID.randomUUID().toString();

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(4)
    void putInstructionsForUnknownSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "putInstructionsForUnknownSession()");

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
            final String SESSION_ID = UUID.randomUUID().toString(); // with virtual certainty an unknown session

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(1)
    void emptyInstructions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "emptyInstructions()");

        try {
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .addNull("session")
                    .build();

            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(2)
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
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(3)
    void sessionsByKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "sessionsByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonArray sessions = response.readEntity(JsonObject.class).getJsonArray("sessions");
                assertThat(sessions.size() == 1).isTrue();
                assertThat(sessions.getJsonObject(0).getString("phase")).isEqualTo("PROVISIONED");
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(5)
    void updateSession() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "updateSession()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final int IDLE_TIME = 10;

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("automaticClose", Json.createObjectBuilder()
                                    .add("idleTime", IDLE_TIME)
                                    .add("temporalUnit", ChronoUnit.SECONDS.name())
                            )
                    )
                    .build();

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo("ACTIVE");
                assertThat(session.getInt("idleTime")).isEqualTo(IDLE_TIME);
            }
            
            final long FIXED_RATE = 5000L;
            Thread.sleep(IDLE_TIME*1000 + FIXED_RATE);
            
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo("CLOSED");
            }
            
        } finally {
            tracer.wayout();
        }
    }
}
