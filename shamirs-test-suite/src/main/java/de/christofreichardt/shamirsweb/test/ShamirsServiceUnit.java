/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.rs.MyClientResponseFilter;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Developer
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShamirsServiceUnit implements Traceable {

    Client client;

    @BeforeAll
    void init() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "init()");

        try {
            this.client = ClientBuilder
                    .newBuilder()
                    .register(MyClientResponseFilter.class)
                    //                    .trustStore(trustStore)
                    .build();
            ping();
        } finally {
            tracer.wayout();
        }
    }

    void ping() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "ping()");

        try {
            String response = null;
            int trials = 0;
            final int MAX_TRIALS = 5, PAUSE = 1;
            do {
                try {
                    response = this.client.target("http://localhost:8080/shamir/v1")
                            .path("ping")
                            .request()
                            .get(String.class);
                    if (response != null) {
                        break;
                    }
                } catch (ProcessingException ex) {
                    trials++;

                    tracer.out().printfIndentln("%d: ex.getCause() = %s", trials, ex.getCause());

                    if (ex.getCause() != null && (ex.getCause() instanceof ConnectException)) {
                        if (trials >= MAX_TRIALS) {
                            break;
                        } else {
                            tracer.out().printfIndentln("Waiting %d second(s) ...", PAUSE);
                            Thread.sleep(TimeUnit.SECONDS.toMillis(PAUSE));
                        }
                    } else {
                        break;
                    }
                }
            } while (true);

            tracer.out().printfIndentln("response = %s", response);
            assertThat(Objects.equals("ping", response)).isTrue();
        } finally {
            tracer.wayout();
        }
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

            Response response = this.client.target("http://localhost:8080/shamir/v1")
                    .path("keystores")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(keystoreInstructions));

            tracer.out().printfIndentln("response = %s", response);

            JsonStructure jsonStructure = response.readEntity(JsonStructure.class);
            JsonTracer jsonTracer = new JsonTracer() {
                @Override
                public AbstractTracer getCurrentTracer() {
                    return ShamirsServiceUnit.this.getCurrentTracer();
                }
            };
            jsonTracer.trace(jsonStructure);
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

            Response response = this.client.target("http://localhost:8080/shamir/v1")
                    .path("keystores/123")
                    .request()
                    .put(Entity.json(jsonObject));

            tracer.out().printfIndentln("response = %s", response);
        } finally {
            tracer.wayout();
        }
    }

    @AfterAll
    void exit() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "exit()");

        try {
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }

}
