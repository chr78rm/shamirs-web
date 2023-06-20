/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.rs.MyClientRequestFilter;
import de.christofreichardt.rs.MyClientResponseFilter;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.json.JsonValue;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.assertj.core.api.WithAssertions;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author Developer
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class X509AuthenticationFilterUnit extends ShamirsBaseUnit implements WithAssertions {

    public X509AuthenticationFilterUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    void makeUnauthenticatedCall() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "makeUnauthenticatedCall()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            Client unauthenticatedClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .build();

            try {
                try ( Response response = unauthenticatedClient.target(this.baseUrl)
                        .path("ping")
                        .request()
                        .get()) {
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.FORBIDDEN);
                    assertThat(response.hasEntity()).isTrue();
                }
            } finally {
                unauthenticatedClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void makeAuthenticatedCall() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "makeAuthenticatedCall()");

        try {
            try ( Response response = this.client.target(this.baseUrl)
                    .path("ping")
                    .request()
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                assertThat(response.readEntity(String.class)).isEqualTo("ping");
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void makeCallWithInvalidCertificate() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "makeCallWithInvalidCertificate()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/invalid-user-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());

            Client invalidClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
                    .build();

            try {
                Throwable throwable = catchThrowable(() -> {
                    try ( Response response = invalidClient.target(this.baseUrl)
                            .path("ping")
                            .request()
                            .get()) {
                    }
                });
                assertThat(throwable).isInstanceOf(ProcessingException.class)
                        .hasCauseInstanceOf(SSLHandshakeException.class);
            } finally {
                invalidClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void makeUnauthorizedCallToActuator() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "makeUnauthorizedCallToActuator()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/test-user-1-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());

            Client unauthorizedClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
                    .build();

            try {
                try ( Response response = unauthorizedClient.target(this.baseUrl)
                        .path("actuator")
                        .path("shutdown")
                        .request()
                        .post(Entity.text(""))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.FORBIDDEN);
                    assertThat(response.hasEntity()).isTrue();
                }
            } finally {
                unauthorizedClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void bannedUser() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "bannedUser()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/banned-user-0-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());

            Client bannedClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
                    .build();
            
            try {
                try ( Response response = bannedClient.target(this.baseUrl)
                        .path("ping")
                        .request()
                        .get()) {
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.FORBIDDEN);
                    assertThat(response.hasEntity()).isTrue();
                }
            } finally {
                bannedClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(2)
    void throttlingByMinimumTimeInterval() throws GeneralSecurityException, IOException, InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "throttlingByMinimumTimeInterval()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/test-user-1-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());
            
            Client suspendedClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
                    .build();
            
            final long MINIMUM_INTERVAL = 1000, REFERENCE_FRAME = 10000, SUFFICIENT_INTERVAL = 3000; // millis
            
            try {
                Thread.sleep(REFERENCE_FRAME);
                try ( Response response = suspendedClient.target(this.baseUrl)
                        .path("ping")
                        .request()
                        .get()) {
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                    assertThat(response.hasEntity()).isTrue();
                }
                try ( Response response = suspendedClient.target(this.baseUrl)
                        .path("ping")
                        .request()
                        .get()) {
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.PAYMENT_REQUIRED);
                    assertThat(response.hasEntity()).isTrue();
                }
                for (int i=0; i<10; i++) {
                    Thread.sleep(SUFFICIENT_INTERVAL);
                    try ( Response response = suspendedClient.target(this.baseUrl)
                            .path("ping")
                            .request()
                            .get()) {
                        tracer.out().printfIndentln("response = %s", response);
                        assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                        assertThat(response.hasEntity()).isTrue();
                    }
                }
            } finally {
                suspendedClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(1)
    void throttlingByReferenceFrame() throws GeneralSecurityException, IOException, InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "throttlingByReferenceFrame()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/test-user-1-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());
            
            Client suspendedClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
                    .build();
            
            final long MINIMUM_INTERVAL = 1000, REFERENCE_FRAME = 10000; // millis
            final int MAX_CALLS = 5;
            
            try {
                for (int i=0; i<MAX_CALLS; i++) {
                    Thread.sleep(MINIMUM_INTERVAL);
                    try ( Response response = suspendedClient.target(this.baseUrl)
                            .path("ping")
                            .request()
                            .get()) {
                        tracer.out().printfIndentln("response = %s", response);
                        assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                        assertThat(response.hasEntity()).isTrue();
                    }
                }
                Thread.sleep(MINIMUM_INTERVAL);
                try ( Response response = suspendedClient.target(this.baseUrl)
                        .path("ping")
                        .request()
                        .get()) {
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.PAYMENT_REQUIRED);
                    assertThat(response.hasEntity()).isTrue();
                }
                Thread.sleep(REFERENCE_FRAME);
                    try ( Response response = suspendedClient.target(this.baseUrl)
                            .path("ping")
                            .request()
                            .get()) {
                        tracer.out().printfIndentln("response = %s", response);
                        assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                        assertThat(response.hasEntity()).isTrue();
                    }
            } finally {
                suspendedClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(3)
    void concurrentRequests() throws InterruptedException, ExecutionException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "concurrentRequests()");

        try {
            ThreadFactory threadFactory = new ThreadFactory() {
                final AtomicInteger counter = new AtomicInteger();

                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, String.format("worker-%d", this.counter.getAndDecrement()));
                }
            };

            final int CONCURRENT_CALLS = 2;
            ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_CALLS, threadFactory);
            CyclicBarrier cyclicBarrier = new CyclicBarrier(CONCURRENT_CALLS);

            class Request implements Callable<Response> {

                @Override
                public Response call() throws Exception {
                    cyclicBarrier.await();
                    try (Response response = X509AuthenticationFilterUnit.this.client.target(X509AuthenticationFilterUnit.this.baseUrl)
                            .path("ping")
                            .queryParam("delay", 100)
                            .request()
                            .get()) {
                        return response;
                    }
                }

            }
            
            final int TIME_OUT = 5;
            
            try {
                List<Future<Response>> futures = new ArrayList<>();
                for (int i = 0; i < CONCURRENT_CALLS; i++) {
                    futures.add(executorService.submit(new Request()));
                }
                List<Response> responses = futures.stream()
                        .map(future -> {
                            try {
                                return future.get(TIME_OUT, TimeUnit.SECONDS);
                            } catch (InterruptedException | ExecutionException | TimeoutException ex) {
                                throw new RuntimeException(ex);
                            }
                        })
                        .peek(response -> tracer.out().printfIndentln("response = %s", response))
                        .toList();
                assertThat(
                        responses.stream()
                                .anyMatch(response -> response.getStatus() == 429) // http 429 == too many requests
                ).isTrue();
                assertThat(
                        responses.stream()
                                .anyMatch(response -> response.getStatus() == 200) // http 200 == ok
                ).isTrue();
            } finally {
                executorService.shutdown();
                boolean terminated = executorService.awaitTermination(TIME_OUT, TimeUnit.SECONDS);
                tracer.out().printfIndentln("terminated = %b", terminated);
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void makeUnauthorizedRestartRequest() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "makeUnauthorizedRestartRequest()");

        try {
            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/test-user-1-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());

            Client unauthorizedClient = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
                    .build();

            try {
                try ( Response response = unauthorizedClient.target(this.baseUrl)
                        .path("management")
                        .path("restart")
                        .request()
                        .post(Entity.json(JsonValue.EMPTY_JSON_OBJECT))) {
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.FORBIDDEN);
                    assertThat(response.hasEntity()).isTrue();
                }
            } finally {
                unauthorizedClient.close();
            }
        } finally {
            tracer.wayout();
        }
    }
}
