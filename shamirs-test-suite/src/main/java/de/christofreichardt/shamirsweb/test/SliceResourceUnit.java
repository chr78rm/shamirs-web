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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.junit.jupiter.api.Disabled;
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
    @Order(2)
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

            // session instructions for activation
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();

            // activate the provisioned session
            try ( Response response = this.client.target(this.baseUrl)
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
    @Order(1)
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
                    .get()) {
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
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                createdSlice = response.readEntity(JsonObject.class);
            }
            assertThat(createdSlice.getJsonObject("share")).isNotEmpty();
            this.passwordShares = createdSlice.getJsonObject("share");

            // try to change the state to FETCHED (missing id)
            JsonObject instruction = Json.createObjectBuilder()
                    .add("state", Json.createValue(SliceProcessingState.FETCHED.name()))
                    .add("share", JsonValue.EMPTY_JSON_OBJECT)
                    .build();
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(createdSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(instruction))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }

            // change the state to FETCHED
            JsonObject instructionWithId = Json.createObjectBuilder(instruction)
                    .add("id", createdSlice.getString("id"))
                    .build();
            JsonObject fetchedSlice;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(createdSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(instructionWithId))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                fetchedSlice = response.readEntity(JsonObject.class);
            }
            assertThat(fetchedSlice.getString("state")).isEqualTo(SliceProcessingState.FETCHED.name());
            assertThat(fetchedSlice.getJsonObject("share")).isEqualTo(JsonValue.EMPTY_JSON_OBJECT);

            // try to change the state again to FETCHED, this should give a bad request
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path(fetchedSlice.getString("id"))
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(instructionWithId))) {
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
                    .add("id", fetchedSlice.getString("id"))
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
                    .add("share", JsonValue.EMPTY_JSON_OBJECT)
                    .build();
            Map<String, JsonObject> fetchedSlices = selectedSlices.stream()
                    .map(selectedSlice -> selectedSlice.asJsonObject())
                    .map(selectedSlice -> {
                        JsonObject fetchedSlice;
                        JsonObject fetchInstructionWithId = Json.createObjectBuilder(fetchInstruction)
                                .add("id", selectedSlice.getString("id"))
                                .build();
                        try ( Response response = this.client.target(this.baseUrl)
                                .path("slices")
                                .path(selectedSlice.getString("id"))
                                .request(MediaType.APPLICATION_JSON)
                                .method("PATCH", Entity.json(fetchInstructionWithId))) {
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
                            .add("id", fetchedShare.getKey())
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

    @Test
    @Order(6)
    void threshold_2() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "threshold_2()");

        try {
            final String MY_ALIAS = "my-secret-key", DONALDS_ALIAS = "donalds-private-ec-key", DAGOBERTS_ALIAS = "dagoberts-private-dsa-key", DAISIES_ALIAS = "daisies-private-rsa-key";

            JsonObject keystoreInstructions = Json.createObjectBuilder()
                    .add("shares", 12)
                    .add("threshold", 4)
                    .add("descriptiveName", "my-posted-keystore")
                    .add("keyinfos", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("alias", MY_ALIAS)
                                    .add("algorithm", "AES")
                                    .add("keySize", 256)
                                    .add("type", "secret-key")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("alias", DONALDS_ALIAS)
                                    .add("algorithm", "EC")
                                    .add("type", "private-key")
                                    .add("x509", Json.createObjectBuilder()
                                            .add("validity", 100)
                                            .add("commonName", "Donald Duck")
                                            .add("locality", "Entenhausen")
                                            .add("state", "Bayern")
                                            .add("country", "Deutschland")
                                    )
                            )
                            .add(Json.createObjectBuilder()
                                    .add("alias", DAGOBERTS_ALIAS)
                                    .add("algorithm", "DSA")
                                    .add("type", "private-key")
                                    .add("x509", Json.createObjectBuilder()
                                            .add("validity", 100)
                                            .add("commonName", "Dagobert Duck")
                                            .add("locality", "Entenhausen")
                                            .add("state", "Bayern")
                                            .add("country", "Deutschland")
                                    )
                            )
                            .add(Json.createObjectBuilder()
                                    .add("alias", DAISIES_ALIAS)
                                    .add("algorithm", "RSA")
                                    .add("type", "private-key")
                                    .add("x509", Json.createObjectBuilder()
                                            .add("validity", 100)
                                            .add("commonName", "Daisy Duck")
                                            .add("locality", "Entenhausen")
                                            .add("state", "Bayern")
                                            .add("country", "Deutschland")
                                    )
                            )
                    )
                    .add("sizes", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("size", 4)
                                    .add("participant", "christof")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 2)
                                    .add("participant", "test-user-1")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 2)
                                    .add("participant", "test-user-2")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-3")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-4")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-5")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-6")
                            )
                    )
                    .build();

            String keystoreId;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(keystoreInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                keystoreId = keystoreView.getString("id");
            }

            // assert that the keystore is loadable
            int threshold, shares;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreId)
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
                    .queryParam("keystoreId", keystoreId)
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

            // change the state to FETCHED
            final JsonObject fetchInstruction = Json.createObjectBuilder()
                    .add("state", Json.createValue(SliceProcessingState.FETCHED.name()))
                    .add("share", JsonValue.EMPTY_JSON_OBJECT)
                    .build();
            selectedSlices.stream()
                    .map(selectedSlice -> selectedSlice.asJsonObject())
                    .forEach(selectedSlice -> {
                        JsonObject fetchInstructionWithId = Json.createObjectBuilder(fetchInstruction)
                                .add("id", selectedSlice.getString("id"))
                                .build();
                        try ( Response response = this.client.target(this.baseUrl)
                                .path("slices")
                                .path(selectedSlice.getString("id"))
                                .request(MediaType.APPLICATION_JSON)
                                .method("PATCH", Entity.json(fetchInstructionWithId))) {
                            tracer.out().printfIndentln("response = %s", response);
                            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                            assertThat(response.hasEntity()).isTrue();
                        }
                    });

            // assert that the keystore is unloadable
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreId)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.STRING);
                assertThat(keystoreView.getString("keyEntries")).isEqualTo("unloadable");
            }

            // retransmit shares until the threshold is exceeded
            {
                Iterator<Map.Entry<String, JsonObject>> iter = fetchedShares.entrySet().iterator();
                while (sum > (shares - threshold) && iter.hasNext()) {
                    Map.Entry<String, JsonObject> fetchedShare = iter.next();
                    JsonObject postInstruction = Json.createObjectBuilder()
                            .add("id", fetchedShare.getKey())
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

            // assert that the keystore is loadable again and extract the link of the current session
            String hrefCurrentSession;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreId)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY);
                JsonObject currentSessionlink = keystoreView.getJsonArray("links").stream()
                        .map(link -> link.asJsonObject())
                        .filter(link -> Objects.equals("currentSession", link.getString("rel")))
                        .findFirst()
                        .orElseThrow();
                hrefCurrentSession = currentSessionlink.getString("href");
            }
            tracer.out().printfIndentln("hrefCurrentSession = %s", hrefCurrentSession);

            // assert that the session is in status PROVISONED and retrieve the session id
            String sessionId;
            try ( Response response = this.client.target(this.baseUrl)
                    .path(hrefCurrentSession)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject sessionView = response.readEntity(JsonObject.class);
                assertThat(SessionPhase.isValid(sessionView.getString("phase"))).isTrue();
                assertThat(SessionPhase.valueOf(sessionView.getString("phase"))).isEqualTo(SessionPhase.PROVISIONED);
                sessionId = sessionView.getString("id");
            }
            
            // retrieve the Ids of slice views for the keystore
            final Set<String> sliceIds;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("keystoreId", keystoreId)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
                sliceIds = slices.stream()
                        .map(slice -> slice.asJsonObject())
                        .map(slice -> slice.getString("id"))
                        .collect(Collectors.toSet());
            }
            
            // activate the provisioned session
            final int IDLE_TIME = 300; //seconds
            final int PAUSE = 2500; // milliseconds
            JsonObject activateSessionInstructions = Json.createObjectBuilder()
                    .add("id", sessionId)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreId)
                    .path("sessions")
                    .path(sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(activateSessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
            }
            
            // no haste
            Thread.sleep(PAUSE);
            
            // close the activated session
            JsonObject closeSessionInstructions = Json.createObjectBuilder()
                    .add("id", sessionId)
                    .add("phase", SessionPhase.CLOSED.name())
                    .build();
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreId)
                    .path("sessions")
                    .path(sessionId)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(closeSessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
            }
            
            // check that the keystore is loadable
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreId)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject keystoreView = response.readEntity(JsonObject.class);
                assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY);
            }
            
            // retrieve the slice views for the keystore and check that the old ones are all EXPIRED and the new ones are CREATED
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("keystoreId", keystoreId)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                slices = response.readEntity(JsonObject.class).getJsonArray("slices");
                slices.stream()
                        .map(slice -> slice.asJsonObject())
                        .filter(slice -> sliceIds.contains(slice.getString("id")))
                        .allMatch(slice -> Objects.equals(slice.getString("state"), SliceProcessingState.EXPIRED.name()));
                slices.stream()
                        .map(slice -> slice.asJsonObject())
                        .filter(slice -> !sliceIds.contains(slice.getString("id")))
                        .allMatch(slice -> Objects.equals(slice.getString("state"), SliceProcessingState.CREATED.name()));
            }
        } finally {
            tracer.wayout();
        }
    }
}
