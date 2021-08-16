/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import java.util.List;
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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Developer
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NextSessionResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    public NextSessionResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    @Order(1)
    void missingAndWrongId() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "missingAndWrongId()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final int IDLE_TIME = 30; // seconds

            JsonObject sessionInstructionsWithMissingId = Json.createObjectBuilder()
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructionsWithMissingId))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }

            JsonObject sessionInstructionsWithNonMatchingId = Json.createObjectBuilder()
                    .add("id", UUID.randomUUID().toString()) // with virtual certainty a non-matching sessionId
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();
            
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID) 
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructionsWithNonMatchingId))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
            
            JsonObject sessionInstructionsWithWrongIdType = Json.createObjectBuilder()
                    .add("id", 123) 
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();
            
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID) 
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructionsWithWrongIdType))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void patchInstructionsForUnknownKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "patchInstructionsForUnknownKeystore()");

        try {
            final String KEYSTORE_ID = UUID.randomUUID().toString(); // with virtual certainty an unkown keystore 
            final String SESSION_ID = UUID.randomUUID().toString();
            final int IDLE_TIME = 30; // seconds

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.NOT_FOUND);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(2)
    void patchInstructionsForUnknownSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "patchInstructionsForUnknownSession()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = UUID.randomUUID().toString(); // with virtual certainty an unknown session
            final int IDLE_TIME = 30; // seconds

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {

                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(3)
    void activateSessionWithAutomaticClose() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "activateSessionWithAutomaticClose()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final int IDLE_TIME = 5;

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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
                assertThat(Json.createPointer("/links/0/type").containsValue(session)).isTrue();
                assertThat(
                        session.getJsonArray("links").getJsonObject(0).getJsonArray("type")
                                .containsAll(List.of(Json.createValue("GET"), Json.createValue("PATCH")))
                ).isTrue();
            }

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            // activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.ACTIVE.name());
                assertThat(session.getInt("idleTime")).isEqualTo(IDLE_TIME);
                assertThat(Json.createPointer("/links/0/type").containsValue(session)).isTrue();
                assertThat(
                        session.getJsonArray("links").getJsonObject(0).getJsonArray("type")
                                .containsAll(List.of(Json.createValue("GET"), Json.createValue("PATCH")))
                ).isTrue();
            }
            
            // session should provide a link where to post documents and a link to its keystore
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
                Optional<JsonObject> documentLink = session.getJsonArray("links").stream()
                        .map(jsonValue -> jsonValue.asJsonObject())
                        .filter(link -> link.getString("rel").equals("documents"))
                        .findFirst();
                assertThat(documentLink).isNotEmpty();
                Optional<JsonObject> keystoreLink = session.getJsonArray("links").stream()
                        .map(jsonValue -> jsonValue.asJsonObject())
                        .filter(link -> link.getString("rel").equals("keystore"))
                        .findFirst();
                assertThat(keystoreLink).isNotEmpty();
            }
            
            // waiting for autoclosing the session
            final long FIXED_RATE = 2500L;
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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.CLOSED.name());
                assertThat(Json.createPointer("/links/0/type").containsValue(session)).isTrue();
                assertThat(
                        session.getJsonArray("links").getJsonObject(0).getJsonArray("type")
                                .contains(Json.createValue("GET"))  &&
                        !session.getJsonArray("links").getJsonObject(0).getJsonArray("type")
                                .contains(Json.createValue("PATCH"))
                ).isTrue();
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
            
            // retrieve all sessions for given keystore by using the provided link
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
                assertThat(sessionView.get().getString("phase")).isEqualTo(SessionPhase.CLOSED.name());
            }
            
            // retrieve the current session for given keystore by using the provided link
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
                JsonObject currentSession = response.readEntity(JsonObject.class);
                assertThat(currentSession.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(4)
    void activateClosedSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "activateClosedSession()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5"; // should be closed now
            final int IDLE_TIME = 10; // seconds

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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.CLOSED.name());
            }

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            // try to activate the expired session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {
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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
            }

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            // try to activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(5)
    void closeActiveSession() throws InterruptedException {
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
            String sessionId;
            try (Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
                sessionId = session.getString("id");
            }

            JsonObject activateSessionInstructions = Json.createObjectBuilder()
                    .add("id", sessionId)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            // activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(activateSessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
            }
            
            // waiting a second
            final long PAUSE = 1000L;
            Thread.sleep(PAUSE);
            
            JsonObject closeSessionInstructions = Json.createObjectBuilder()
                    .add("id", sessionId)
                    .add("phase", SessionPhase.CLOSED.name())
                    .build();
            
            // close the activated session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(closeSessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.CLOSED.name());
            }
            
            // retrieve keystore view to check if the key entries are loadable
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(6)
    void closeProvisionedSession() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "closeProvisionedSession()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            
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
            String sessionId;
            try (Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
                sessionId = session.getString("id");
            }
            
            JsonObject closeSessionInstructions = Json.createObjectBuilder()
                    .add("id", sessionId)
                    .add("phase", SessionPhase.CLOSED.name())
                    .build();
            
            // try to close the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(closeSessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void activateSessionWithPendingDocuments() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "activateSessionWithPendingDocuments()");

        try {
            final String KEYSTORE_ID = "28799acd-6753-451f-965a-8a0eb601eb26"; // the-pending-docs-keystore
            final int IDLE_TIME = 300; // seconds
            
            // retrieve keystore view to determine the present session
            String hrefSession;
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
                hrefSession = currentSessionLink.get().getString("href");
            }
            
            // session should be in phase 'PROVISIONED'
            String hrefDocuments;
            String sessionId;
            try (Response response = this.client.target(this.baseUrl)
                    .path(hrefSession)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
                hrefDocuments = session.getJsonArray("links").stream()
                        .map(jsonValue -> jsonValue.asJsonObject())
                        .filter(link -> link.getString("rel").equals("documents"))
                        .findFirst()
                        .orElseThrow()
                        .getString("href");
                sessionId = session.getString("id");
            }
            
            // session should have pending documents
            tracer.out().printfIndentln("hrefDocuments = %s", hrefDocuments);
            try (Response response = this.client.target(this.baseUrl)
                    .path(hrefDocuments)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonArray documents = response.readEntity(JsonObject.class).getJsonArray("documents");
                boolean allMatched = documents.stream()
                        .map(jsonValue -> jsonValue.asJsonObject())
                        .allMatch(document -> document.getString("state").equals("PENDING"));
                assertThat(allMatched).isTrue();
            }

            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", sessionId)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            // activate the provisioned session
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
            }
            
            // session should have processed documents
            final int MAX_TRIALS = 3;
            int trials = 0;
            boolean allProcessed = false;
            do {
                trials++;
                tracer.out().printfIndentln("trials = %d", trials);
                try ( Response response = this.client.target(this.baseUrl)
                        .path(hrefDocuments)
                        .request(MediaType.APPLICATION_JSON)
                        .get()) {
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                    assertThat(response.hasEntity()).isTrue();
                    JsonArray documents = response.readEntity(JsonObject.class).getJsonArray("documents");
                    allProcessed = documents.stream()
                            .map(jsonValue -> jsonValue.asJsonObject())
                            .map(document -> document.getString("state"))
                            .allMatch(state -> state.equals("PROCESSED") || state.equals("ERROR"));
                    if (allProcessed || trials >= MAX_TRIALS) {
                        break;
                    } else {
                        Thread.sleep(1000);
                    }
                }
            } while (true);
            assertThat(allProcessed).isTrue();
        } finally {
            tracer.wayout();
        }
    }
}
