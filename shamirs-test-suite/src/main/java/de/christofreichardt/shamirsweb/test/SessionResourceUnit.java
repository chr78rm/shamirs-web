/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
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
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("idleTime", 30)
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
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
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("idleTime", 30)
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
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
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
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
    void activateSessionWithAutomaticClose() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "activateSessionWithAutomaticClose()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final int IDLE_TIME = 10;

            // session should be in phase 'PROVISIONED'
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
                assertThat(session.getString("phase")).isEqualTo("PROVISIONED");
            }
 
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("idleTime", IDLE_TIME)
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
                            )
                    )
                    .build();

            // activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo("ACTIVE");
                assertThat(session.getInt("idleTime")).isEqualTo(IDLE_TIME);
            }
            
            // session should provide a link where to put documents
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
            }
            
            // waiting for autoclosing the session
            final long FIXED_RATE = 5000L;
            Thread.sleep(IDLE_TIME*1000 + FIXED_RATE);
            
            // session should be in phase 'CLOSED' now
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
            
            // check that the rolled over keystore is loadable
            JsonObject keystoreView;
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY).isTrue();
            }
            
            // retrieve all sessions for given keystore
            Optional<JsonObject> sessionsRelation = keystoreView.getJsonArray("links").stream()
                    .map(link -> link.asJsonObject())
                    .filter(link -> Objects.equals(link.getString("rel"), "sessions"))
                    .findFirst();
            assertThat(sessionsRelation).isPresent();
            String href = sessionsRelation.get().getString("href");
            try (Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject sessionsView = response.readEntity(JsonObject.class);
                assertThat(sessionsView.getValue("/sessions").getValueType() == JsonValue.ValueType.ARRAY).isTrue();
                Optional<JsonObject> sessionView = sessionsView.getJsonArray("sessions").stream()
                        .map(session -> session.asJsonObject())
                        .filter(session -> Objects.equals(session.getString("id"), SESSION_ID))
                        .findFirst();
                assertThat(sessionView).isPresent();
                assertThat(sessionView.get().getString("phase")).isEqualTo("CLOSED");
            }
            
            // retrieve the current session for given keystore
            Optional<JsonObject> currentSessionRelation = keystoreView.getJsonArray("links").stream()
                    .map(link -> link.asJsonObject())
                    .filter(link -> Objects.equals(link.getString("rel"), "currentSession"))
                    .findFirst();
            assertThat(currentSessionRelation).isPresent();
            href = currentSessionRelation.get().getString("href");
            try (Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(6)
    void activateClosedSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "activateClosedSession()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5"; // should be closed now
            final int IDLE_TIME = 10;

            // session should be in phase 'CLOSED'
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
 
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("idleTime", IDLE_TIME)
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
                            )
                    )
                    .build();

            // try to activate the expired session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void activateSessionForKeystoreWithIncompleteSlices() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "activateSessionForKeystoreWithIncompleteSlices()");

        try {
            final String KEYSTORE_ID = "3e6b2af3-63e2-4dcb-bb71-c69f1293b072"; // the-too-few-slices-keystore
            final String SESSION_ID = "1232d4be-fa07-45d8-b741-65f60ce9ebf0";
            final int IDLE_TIME = 10;
            
            // session should be in phase 'PROVISIONED'
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
                assertThat(session.getString("phase")).isEqualTo("PROVISIONED");
            }
            
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("idleTime", IDLE_TIME)
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
                            )
                    )
                    .build();

            // try to activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(7)
    void closeActiveSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "closeActiveSession()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final int IDLE_TIME = 300;
            
            // retrieve keystore view to determine the present session
            String href;
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/links").getValueType() == JsonValue.ValueType.ARRAY).isTrue();
                Optional<JsonObject> currentSessionLink = keystoreView.getJsonArray("links").stream()
                        .map(link -> link.asJsonObject())
                        .filter(link -> Objects.equals(link.getString("rel"), "currentSession"))
                        .findFirst();
                assertThat(currentSessionLink).isNotEmpty();
                href = currentSessionLink.get().getString("href");
            }
                
            tracer.out().printfIndentln("href = %s", href);
            
            // session should be in phase 'PROVISIONED'
            try (Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo("PROVISIONED");
            }
            
            JsonObject activateSessionInstructions = Json.createObjectBuilder()
                    .add("session", Json.createObjectBuilder()
                            .add("activation", Json.createObjectBuilder()
                                    .add("automaticClose", Json.createObjectBuilder()
                                            .add("idleTime", IDLE_TIME)
                                            .add("temporalUnit", ChronoUnit.SECONDS.name())
                                    )
                            )
                    )
                    .build();

            // activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(activateSessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
}
