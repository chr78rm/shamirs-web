/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import java.time.temporal.ChronoUnit;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
public class SliceResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    JsonObject passwordShares;

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
    @Order(1)
    void slicesByKeystore() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "slicesByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5"; // provisioned session for my-first-keystore
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

            // assert that the previous fetched slices are now expired and the type array of the self link contains only GET
            allMatched = slices.stream()
                    .map((slice -> slice.asJsonObject()))
                    .filter(slice -> sliceIds.contains(slice.getString("id")))
                    .map(slice -> Enum.valueOf(SliceProcessingState.class, slice.getString("state")))
                    .allMatch(state -> state == SliceProcessingState.EXPIRED);
            assertThat(allMatched).isTrue();
            allMatched = slices.stream()
                    .map((slice -> slice.asJsonObject()))
                    .filter(slice -> sliceIds.contains(slice.getString("id")))
                    .map(slice -> Json.createPointer("/links/0/type").getValue(slice))
                    .map(type -> type.asJsonArray())
                    .allMatch(type -> type.contains(Json.createValue("GET")) && !type.contains(Json.createValue("PATCH")));
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
        tracer.entry("void", this, "allSlices()");

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
    @Order(2)
    void querySingleSlice() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "querySingleSlice()");

        try {
            final String SLICE_ID = "9a83d398-35d6-4959-aea2-1c930a936b43"; // christofs slice from 'my-first-keystore'

            // retrieve particular slice views
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

            // retrieve particular slice views
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

    @Test
    @Order(3)
    void fetchSlice() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "fetchSlice()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String PARTICIPANT_ID = "8844dd34-c836-4060-ba73-c6d86ad1275d"; // christof

            // retrieve a created slice view for keystore and participant
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
            JsonObject createdSlice = slices.stream()
                    .map(slice -> slice.asJsonObject())
                    .filter(slice -> Objects.equals(slice.getString("state"), SliceProcessingState.CREATED.name()))
                    .findFirst()
                    .orElseThrow();

            // fetch the password shares
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(createdSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                createdSlice = response.readEntity(JsonObject.class);
            }
            assertThat(createdSlice.getJsonObject("share")).isNotEmpty();
            this.passwordShares = createdSlice.getJsonObject("share");

            // change the state to FETCHED
            JsonObject instruction = Json.createObjectBuilder()
                    .add("state", Json.createValue(SliceProcessingState.FETCHED.name()))
                    .build();
            JsonObject fetchedSlice;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(createdSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(instruction))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                fetchedSlice = response.readEntity(JsonObject.class);
            }
            assertThat(fetchedSlice.getString("state")).isEqualTo(SliceProcessingState.FETCHED.name());
            assertThat(fetchedSlice.getJsonObject("share")).isEqualTo(JsonValue.EMPTY_JSON_OBJECT);

            // try to change the state again to FETCHED
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(fetchedSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(instruction))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(4)
    void postSlice() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postSlice()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String PARTICIPANT_ID = "8844dd34-c836-4060-ba73-c6d86ad1275d"; // christof

            // retrieve a fetched slice view for keystore and participant
            JsonArray slices;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("participantId", PARTICIPANT_ID)
                    .queryParam("keystoreId", KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }
            JsonObject fetchedSlice = slices.stream()
                    .map(slice -> slice.asJsonObject())
                    .filter(slice -> Objects.equals(slice.getString("state"), SliceProcessingState.FETCHED.name()))
                    .findFirst()
                    .orElseThrow();

            // retransmit the shares
            JsonObject instruction = Json.createObjectBuilder()
                    .add("state", Json.createValue(SliceProcessingState.POSTED.name()))
                    .add("share", this.passwordShares)
                    .build();
            JsonObject postedSlice;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(fetchedSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(instruction))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                postedSlice = response.readEntity(JsonObject.class);
            }
            assertThat(postedSlice.getString("state")).isEqualTo(SliceProcessingState.POSTED.name());
            assertThat(postedSlice.getJsonObject("share")).isNotEmpty();
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(5)
    void threshold() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "threshold()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore

            // assert that the keystore is loadable
            int threshold, shares;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY);
                threshold = keystoreView.getInt("threshold");
                shares = keystoreView.getInt("shares");
            };
            tracer.out().printfIndentln("shares = %d", shares);
            tracer.out().printfIndentln("threshold = %d", threshold);

            // retrieve the slice views for the keystore
            JsonArray slices;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("keystoreId", KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
            }

            // filter for slices which are CREATED or POSTED
            List<JsonObject> currentSlices = slices.stream()
                    .map(slice -> slice.asJsonObject())
                    .filter(slice -> {
                        SliceProcessingState processingState = SliceProcessingState.valueOf(slice.getString("state"));
                        return processingState == SliceProcessingState.CREATED || processingState == SliceProcessingState.POSTED;
                    })
                    .collect(Collectors.toList());
            tracer.out().printfIndentln("currentSlices.size() = %d", currentSlices.size());
            JsonTracer jsonTracer = new JsonTracer() {
                @Override
                public AbstractTracer getCurrentTracer() {
                    return SliceResourceUnit.this.getCurrentTracer();
                }
            };
            tracer.out().println();
            tracer.out().printfIndentln("--- currentSlices ---");
            tracer.out().println();
            currentSlices.forEach(slice -> jsonTracer.trace(slice));

            // collect a subset of slices such that the remaining slices fall below the threshold
            int sum = 0;
            List<JsonObject> selectedSlices = new ArrayList<>();
            {
                Iterator<JsonObject> iter = currentSlices.iterator();
                while (sum <= (shares - threshold) && iter.hasNext()) {
                    JsonObject currentSlice = iter.next();
                    sum += currentSlice.getInt("size");
                    selectedSlices.add(currentSlice);
                }
            }
            tracer.out().printfIndentln("sum = %d", sum);
            assertThat(shares - sum).isLessThan(threshold);
            tracer.out().println();
            tracer.out().printfIndentln("--- selectedSlices ---");
            tracer.out().println();
            selectedSlices.forEach(slice -> jsonTracer.trace(slice));

            // fetch the shares from the selected slices
            Map<String, JsonObject> fetchedShares = selectedSlices.stream()
                    .map(selectedSlice -> selectedSlice.asJsonObject())
                    .map(selectedSlice -> {
                        JsonObject fetchedShare;
                        try ( Response response = this.client.target(this.baseUrl)
                                .path("slices")
                                .path(selectedSlice.getString("id"))
                                .request(MediaType.APPLICATION_JSON)
                                .method("GET")) {
                            tracer.out().printfIndentln("response = %s", response);
                            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                            assertThat(response.hasEntity()).isTrue();
                            fetchedShare = response.readEntity(JsonObject.class).getJsonObject("share");
                        }
                        return new SimpleImmutableEntry<>(selectedSlice.getString("id"), fetchedShare);
                    })
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
            tracer.out().println();
            tracer.out().printfIndentln("--- fetchedShares ---");
            tracer.out().println();
            fetchedShares.forEach((id, share) -> {
                tracer.out().printfIndentln("id = %s", id);
                jsonTracer.trace(share);
                tracer.out().println();
            });

            // change the state to FETCHED
            final JsonObject fetchInstruction = Json.createObjectBuilder()
                    .add("state", Json.createValue(SliceProcessingState.FETCHED.name()))
                    .build();
            Map<String, JsonObject> fetchedSlices = selectedSlices.stream()
                    .map(selectedSlice -> selectedSlice.asJsonObject())
                    .map(selectedSlice -> {
                        JsonObject fetchedSlice;
                        try ( Response response = this.client.target(this.baseUrl)
                                .path("slices")
                                .path(selectedSlice.getString("id"))
                                .request(MediaType.APPLICATION_JSON)
                                .method("PATCH", Entity.json(fetchInstruction))) {
                            tracer.out().printfIndentln("response = %s", response);
                            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                            assertThat(response.hasEntity()).isTrue();
                            fetchedSlice = response.readEntity(JsonObject.class);
                        }
                        return new SimpleImmutableEntry<>(fetchedSlice.getString("id"), fetchedSlice);
                    })
                    .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
            tracer.out().println();
            tracer.out().printfIndentln("--- fetchedSlices ---");
            tracer.out().println();
            fetchedSlices.forEach((id, fetchedSlice) -> {
                tracer.out().printfIndentln("id = %s", id);
                jsonTracer.trace(fetchedSlice);
                tracer.out().println();
            });

            // assert that the keystore is unloadable
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.STRING);
            }

            // retransmit shares until the threshold is exceeded
            {
                Iterator<Map.Entry<String, JsonObject>> iter = fetchedShares.entrySet().iterator();
                while (sum > (shares - threshold) && iter.hasNext()) {
                    Map.Entry<String, JsonObject> fetchedShare = iter.next();
                    JsonObject postInstruction = Json.createObjectBuilder()
                            .add("state", Json.createValue(SliceProcessingState.POSTED.name()))
                            .add("share", fetchedShare.getValue())
                            .build();
                    try ( Response response = this.client.target(this.baseUrl)
                            .path("slices")
                            .path(fetchedShare.getKey())
                            .request(MediaType.APPLICATION_JSON)
                            .method("PATCH", Entity.json(postInstruction))) {
                        tracer.out().printfIndentln("response = %s", response);
                        assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                        assertThat(response.hasEntity()).isTrue();
                    }
                    sum -= fetchedShare.getValue().getJsonArray("SharePoints").size();
                }
            }

            // assert that the keystore is loadable again
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY);
            }
        } finally {
            tracer.wayout();
        }
    }
}
