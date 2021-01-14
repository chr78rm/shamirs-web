/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
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
            JsonObject keystoreInstructions = Json.createObjectBuilder()
                    .add("shares", 12)
                    .add("threshold", 4)
                    .add("descriptiveName", "my-posted-keystore")
                    .add("keyinfos", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                    .add("alias", "my-secret-key")
                                    .add("algorithm", "AES")
                                    .add("keySize", 256)
                                    .add("type", "secret-key")
                            )
                            .add(Json.createObjectBuilder()
                                    .add("alias", "my-private-key")
                                    .add("algorithm", "EC")
                                    .add("type", "private-key")
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
            
            final String THE_TOO_FEW_SLICES_KEYSTORE_ID = "3e6b2af3-63e2-4dcb-bb71-c69f1293b072"; // the-too-few-slices-keystore
            response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(THE_TOO_FEW_SLICES_KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get();

            tracer.out().printfIndentln("response = %s", response);

            assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
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

    
}
