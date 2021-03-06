/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Developer
 */
public class KeystoreResourceUnit extends ShamirsBaseUnit {

    public KeystoreResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    void postKeystoreTemplate() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postKeystoreTemplate()");

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

            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(keystoreInstructions));

            tracer.out().printfIndentln("response = %s", response);

            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);

            JsonObject keystoreEntity = response.readEntity(JsonObject.class);
            JsonArray links = keystoreEntity.getJsonArray("links");

            assertThat(links).isNotNull();

            Optional<JsonObject> selfLink = links.stream()
                    .map(jsonValue -> jsonValue.asJsonObject())
                    .filter(link -> Objects.equals(link.getString("rel"), "self"))
                    .findFirst();

            assertThat(selfLink).isNotEmpty();
            assertThat(selfLink.get().getString("href")).isEqualTo(String.format("/keystores/%s", keystoreEntity.getString("id")));

            String href = selfLink.get().getString("href");

            tracer.out().printfIndentln("href = %s", href);

            response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            tracer.out().printfIndentln("response = %s", response);

            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
            assertThat(response.hasEntity()).isTrue();

            keystoreEntity = response.readEntity(JsonObject.class);
            JsonArray keyEntries = keystoreEntity.getJsonArray("keyEntries");

            assertThat(keyEntries).isNotNull();
            
            Set<String> aliases = keyEntries.stream()
                    .map(entry -> entry.asJsonObject())
                    .map(entry -> entry.getString("alias"))
                    .collect(Collectors.toSet());
            
            assertThat(aliases).contains(MY_ALIAS, DAGOBERTS_ALIAS, DONALDS_ALIAS, DAISIES_ALIAS);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void getKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "getKeystore()");

        try {
            final String MY_FIRST_KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(MY_FIRST_KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            tracer.out().printfIndentln("response = %s", response);

            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
            assertThat(response.hasEntity()).isTrue();
            JsonObject keystoreView = response.readEntity(JsonObject.class);
            assertThat(keystoreView.getValue("/keyEntries").getValueType() == JsonValue.ValueType.ARRAY).isTrue();

            final String THE_TOO_FEW_SLICES_KEYSTORE_ID = "3e6b2af3-63e2-4dcb-bb71-c69f1293b072"; // the-too-few-slices-keystore
            response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(THE_TOO_FEW_SLICES_KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            tracer.out().printfIndentln("response = %s", response);

            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
            assertThat(response.hasEntity()).isTrue();
            keystoreView = response.readEntity(JsonObject.class);
            assertThat(keystoreView.getString("keyEntries")).isEqualTo("unloadable");
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void unknownKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "unknownKeystore()");

        try {
            final String UNKOWN_KEYSTORE_ID = UUID.randomUUID().toString(); // with virtual certainty an unkown keystore 
            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(UNKOWN_KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            tracer.out().printfIndentln("response = %s", response);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void putKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "putKeystore()");

        try {
            JsonObject jsonObject = Json.createObjectBuilder()
                    .add("alias", "my-secret-key")
                    .add("algorithm", "AES")
                    .add("keySize", 256)
                    .build();

            Response response = this.client.target(this.baseUrl)
                    .path("keystores/123")
                    .request()
                    .put(Entity.json(jsonObject));

            tracer.out().printfIndentln("response = %s", response);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void availableKeystores() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "availableKeystores()");

        try {
            Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .request()
                    .get();

            tracer.out().printfIndentln("response = %s", response);
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void participantsByKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "participantsByKeystore()");

        try {
            final String MY_FIRST_KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(MY_FIRST_KEYSTORE_ID)
                    .path("participants")
                    .request()
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
    void keystoreInstructions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "keystoreInstructions()");

        try {
            final String MY_SECRET_KEY_ALIAS = "my-secret-key", MY_PRIVATE_EC_KEY_ALIAS = "my-private-ec-key";
            
            // some instructions with unordered sizes
            JsonObject keystoreInstructions = Json.createObjectBuilder()
                    .add("shares", 12)
                    .add("threshold", 4)
                    .add("descriptiveName", "my-posted-keystore")
                    .add("keyinfos", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("alias", MY_SECRET_KEY_ALIAS)
                                    .add("algorithm", "AES")
                                    .add("keySize", 256)
                                    .add("type", "secret-key")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("alias", MY_PRIVATE_EC_KEY_ALIAS)
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
                    )
                    .add("sizes", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-5")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 2)
                                    .add("participant", "test-user-1")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-3")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 4)
                                    .add("participant", "test-user-0")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-4")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 2)
                                    .add("participant", "test-user-2")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-6")
                            )
                    )
                    .build();
            
            // post the instructions
            JsonObject keystoreView;
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(keystoreInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);
                assertThat(response.hasEntity()).isTrue();
                keystoreView = response.readEntity(JsonObject.class);
            }
            
            // retrieve the participants for the created keystore
            JsonArray participantsView;
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(keystoreView.getString("id"))
                    .path("participants")
                    .request()
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                participantsView = response.readEntity(JsonObject.class).getJsonArray("participants");
            }
            
            boolean allMatched = participantsView.stream()
                    .map(participant -> participant.asJsonObject())
                    .map(participant -> {
                        
                        // retrieve the slice for the given participant
                        JsonObject slice;
                        try ( Response response = this.client.target(this.baseUrl)
                                .path("slices")
                                .queryParam("participantId", participant.getString("id"))
                                .queryParam("keystoreId", keystoreView.getString("id"))
                                .request(MediaType.APPLICATION_JSON)
                                .method("GET")) {
                            tracer.out().printfIndentln("response = %s", response);
                            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                            assertThat(response.hasEntity()).isTrue();
                            JsonArray slices = response.readEntity(JsonObject.class).getJsonArray("slices");
                            assertThat(slices).hasSize(1);
                            slice = slices.getJsonObject(0);
                        }
                        return new AbstractMap.SimpleImmutableEntry<>(participant.getString("id"), slice);
                        
                    })
                    .allMatch(entry -> {
                        
                        // find the user name from the id
                        String preferredName = participantsView.stream()
                                .map(participant -> participant.asJsonObject())
                                .filter(participant -> Objects.equals(participant.getString("id"), entry.getKey()))
                                .findFirst()
                                .orElseThrow()
                                .getString("descriptiveName");
                        
                        // find the size for the user name
                        int requestedSize = keystoreInstructions.getJsonArray("sizes").stream()
                                .map(size -> size.asJsonObject())
                                .filter(size -> Objects.equals(size.getString("participant"), preferredName))
                                .findFirst()
                                .orElseThrow()
                                .getInt("size");
                        
                        return requestedSize == entry.getValue().getInt("size");
                    });
            assertThat(allMatched).isTrue();
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void unknownParticipants() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "unknownParticipants()");

        try {
            final String MY_SECRET_KEY_ALIAS = "my-secret-key", MY_PRIVATE_EC_KEY_ALIAS = "my-private-ec-key";
            
            // some instructions with an unknown participant
            JsonObject keystoreInstructions = Json.createObjectBuilder()
                    .add("shares", 12)
                    .add("threshold", 4)
                    .add("descriptiveName", "my-posted-keystore")
                    .add("keyinfos", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("alias", MY_SECRET_KEY_ALIAS)
                                    .add("algorithm", "AES")
                                    .add("keySize", 256)
                                    .add("type", "secret-key")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("alias", MY_PRIVATE_EC_KEY_ALIAS)
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
                    )
                    .add("sizes", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-5")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 2)
                                    .add("participant", "test-user-1")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-3")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 4)
                                    .add("participant", "test-user-x0") // <- unknown participant
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-4")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 2)
                                    .add("participant", "test-user-x1") // <- unknown participant
                            )
                            .add(Json.createObjectBuilder()
                                    .add("size", 1)
                                    .add("participant", "test-user-6")
                            )
                    )
                    .build();
            
            // post the instructions
            try (Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(keystoreInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.BAD_REQUEST);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
}
