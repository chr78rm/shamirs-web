/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Developer
 */
public class SliceResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    public SliceResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    void dummy() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "dummy()");

        try {
            JsonObject slice = Json.createObjectBuilder()
                    .add("state", "FETCHED")
                    .build();

            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path("5ae7570d-3f3b-43b5-94e6-b23f24d60093")
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(slice))) {
                tracer.out().printfIndentln("response = %s", response);
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void slicesByKeystore() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "slicesByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final int IDLE_TIME = 1;

            // session should be in phase 'PROVISIONED'
            try ( Response response = this.client.target(this.baseUrl)
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

            // retrieve the slice views for the keystore
            JsonArray slices;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("keystoreId", KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }

            // assert that all slices have status POSTED and the type array of the self link contains GET and PATCH
            boolean allMatched = slices.stream()
                    .map((slice -> slice.asJsonObject()))
                    .map(slice -> Enum.valueOf(SliceProcessingState.class, slice.getString("state")))
                    .allMatch(state -> state == SliceProcessingState.POSTED);
            assertThat(allMatched).isTrue();
            allMatched = slices.stream()
                    .map((slice -> slice.asJsonObject()))
                    .map(slice -> Json.createPointer("/links/0/type").getValue(slice))
                    .map(type -> type.asJsonArray())
                    .allMatch(type -> type.containsAll(List.of(Json.createValue("GET"), Json.createValue("PATCH"))));
            assertThat(allMatched).isTrue();

            // store the slice ids for later use
            Set<String> sliceIds = slices.stream()
                    .map((slice -> slice.asJsonObject()))
                    .map(slice -> slice.getString("id"))
                    .collect(Collectors.toSet());

            // activate the session and wait for the automatic closure
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
            try ( Response response = this.client.target(this.baseUrl)
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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.ACTIVE.name());
                assertThat(session.getInt("idleTime")).isEqualTo(IDLE_TIME);
            }

            // waiting for autoclosing the session
            final long FIXED_RATE = 2500L;
            Thread.sleep(IDLE_TIME * 1000 + FIXED_RATE);

            // session should be in phase 'CLOSED' now
            try ( Response response = this.client.target(this.baseUrl)
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

            // retrieve the slice views for the keystore again
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("keystoreId", KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }

            // assert that the previous fetched slices are now expired
            allMatched = slices.stream()
                    .map((slice -> slice.asJsonObject()))
                    .filter(slice -> sliceIds.contains(slice.getString("id")))
                    .map(slice -> Enum.valueOf(SliceProcessingState.class, slice.getString("state")))
                    .allMatch(state -> state == SliceProcessingState.EXPIRED);
            assertThat(allMatched).isTrue();
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void slicesByParticipant() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "slicesByParticipant()");

        try {
            final String PARTICIPANT_ID = "8844dd34-c836-4060-ba73-c6d86ad1275d"; // christof

            // retrieve the slice views for the participant
            JsonArray slices;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("participantId", PARTICIPANT_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void slicesByKeystoreAndParticipant() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "slicesByKeystoreAndParticipant()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String PARTICIPANT_ID = "8844dd34-c836-4060-ba73-c6d86ad1275d"; // christof

            // retrieve the slice views for keystore and participant
            JsonArray slices;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("participantId", PARTICIPANT_ID)
                    .queryParam("keystoreId", KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void allSlices() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "slicesByKeystoreAndParticipant()");

        try {
            // retrieve all slice views
            JsonArray slices;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void querySingleSlice() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "querySingleSlice()");

        try {
            final String SLICE_ID = "9a83d398-35d6-4959-aea2-1c930a936b43"; // christofs slice from 'my-first-keystore'
            
            // retrieve all slice views
            JsonObject slice;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(SLICE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slice = response.readEntity(JsonObject.class);
            }
            assertThat(slice.getInt("size")).isEqualTo(
                    slice.getJsonObject("share").getJsonArray("SharePoints").size()
            );
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void queryNotExistingSlice() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "queryNotExistingSlice()");

        try {
            final String SLICE_ID = UUID.randomUUID().toString(); // with virtual certainty a not existing slice id
            
            // retrieve all slice views
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(SLICE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.NOT_FOUND);
                assertThat(response.hasEntity()).isTrue();
                response.readEntity(JsonObject.class);
            }
        } finally {
            tracer.wayout();
        }
    }

//    @Test
//    void fetchSlice() {
//        AbstractTracer tracer = getCurrentTracer();
//        tracer.entry("void", this, "fetchSlice()");
//
//        try {
//            JsonObject instruction = Json.createObjectBuilder()
//                    .add("slice", Json.createObjectBuilder()
//                            .add("retrieval", JsonValue.EMPTY_JSON_OBJECT)
//                    )
//                    .build();
//            
//            try ( Response response = this.client.target(this.baseUrl)
//                    .path("slices")
//                    .path("5ae7570d-3f3b-43b5-94e6-b23f24d60093")
//                    .request(MediaType.APPLICATION_JSON)
//                    .method("PATCH", Entity.json(slice))) {
//                tracer.out().printfIndentln("response = %s", response);
//            }
//        } finally {
//            tracer.wayout();
//        }
//    }
}
