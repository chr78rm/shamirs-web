/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.rs.MyClientRequestFilter;
import de.christofreichardt.rs.MyClientResponseFilter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import static org.assertj.core.api.Assertions.assertThat;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author Developer
 */
@ExtendWith(PropertiesExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ShamirsBaseUnit implements Traceable {

    final Map<String, String> config;
    final String baseUrl;
    final boolean externalService;
    final int maxTrials;
    final int pause;
    final String dbClassName;

    Client client;
    Process process;

    public ShamirsBaseUnit(Map<String, String> config) {
        this.config = config;
        this.baseUrl = config.getOrDefault("de.christofreichardt.shamirsweb.test.baseUrl", "https://localhost:8443/shamir/v1");
        this.externalService = Boolean.parseBoolean(config.getOrDefault("de.christofreichardt.shamirsweb.test.externalService", "false"));
        this.maxTrials = Integer.parseInt(config.getOrDefault("de.christofreichardt.shamirsweb.test.maxTrials", "10"));
        this.pause = Integer.parseInt(config.getOrDefault("de.christofreichardt.shamirsweb.test.pause", "1"));
        this.dbClassName = config.getOrDefault("de.christofreichardt.shamirsweb.test.dbClassName", "de.christofreichardt.shamirsweb.test.NativeMariaDB");
    }
    

    @BeforeAll
    void init() throws InterruptedException, GeneralSecurityException, IOException, ClassNotFoundException, NoSuchMethodException, ReflectiveOperationException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "init()");

        try {
            this.config.entrySet().forEach(entry -> tracer.out().printfIndentln("%s = %s", entry.getKey(), entry.getValue()));

            Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));
            
            File batch = baseDir.resolve(Path.of("..", "sql", "mariadb", "setup-scenario.sql")).toFile();
            Database database = (Database) Class.forName(this.dbClassName).getDeclaredConstructor().newInstance();
            database.execute(batch);

            if (!this.externalService) {
                ProcessBuilder processBuilder = new ProcessBuilder("java", "-Djava.security.egd=file:/dev/urandom", "-jar", "target/shamirs-service.jar");
                File workingDir = baseDir.resolve(Path.of("..", "shamirs-service")).toFile();
                File logFile = baseDir.resolve(this.config.getOrDefault("de.christofreichardt.shamirsweb.test.spring.log", "log/spring-boot.log")).toFile();
                this.process = processBuilder.directory(workingDir)
                        .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                        .start();
                tracer.out().printfIndentln("this.process.pid() = %d", this.process.pid());
            }

            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/service-id-trust.p12");
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());
            this.client = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
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
            do {
                try {
                    response = this.client.target(this.baseUrl)
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
                        if (trials >= this.maxTrials) {
                            break;
                        } else {
                            tracer.out().printfIndentln("Waiting %d second(s) ...", this.pause);
                            Thread.sleep(TimeUnit.SECONDS.toMillis(this.pause));
                        }
                    } else {
                        break;
                    }
                }
            } while (true);
            
            if (trials >= this.maxTrials) {
                tracer.logMessage(LogLevel.SEVERE, "Cannot establish connection to the service. Aborting ...", getClass(), "ping()");
                this.process.destroy();
                boolean terminated = this.process.waitFor(5, TimeUnit.SECONDS);
                tracer.out().printfIndentln("terminated = %b", terminated);
                TracerFactory.getInstance().closePoolTracer();
                System.exit(1);
            }

            tracer.out().printfIndentln("response = %s", response);
            assertThat(Objects.equals("ping", response)).isTrue();
        } finally {
            tracer.wayout();
        }
    }

    @AfterAll
    void exit() throws InterruptedException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "exit()");

        try {
            if (!this.externalService) {
                try ( Response response = this.client.target(this.baseUrl)
                        .path("actuator")
                        .path("shutdown")
                        .request()
                        .post(Entity.text(""))) {
                    String message = response.readEntity(String.class);

                    tracer.out().printfIndentln("message = %s", message);
                }
                boolean terminated = this.process.waitFor(2500, TimeUnit.MILLISECONDS);
                tracer.out().printfIndentln("terminated = %b", terminated);
            }

            if (Objects.nonNull(this.client)) {
                this.client.close();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }
    
}
