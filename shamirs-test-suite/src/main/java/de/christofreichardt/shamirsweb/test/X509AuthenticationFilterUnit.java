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
import java.util.Map;
import java.util.Objects;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.assertj.core.api.WithAssertions;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Developer
 */
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

            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/invalid-user.p12");
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

            try ( Response response = unauthorizedClient.target(this.baseUrl)
                    .path("actuator")
                    .path("shutdown")
                    .request()
                    .post(Entity.text(""))) {
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.FORBIDDEN);
                assertThat(response.hasEntity()).isTrue();
            }
        } finally {
            tracer.wayout();
        }
    }
}
