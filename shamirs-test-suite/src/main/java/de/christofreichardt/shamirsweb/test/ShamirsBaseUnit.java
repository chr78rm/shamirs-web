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
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.net.ssl.SSLHandshakeException;
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
    
    enum ServiceType {NATIVE, DOCKER}

    final Map<String, String> config;
    final String baseUrl;
    final boolean externalService;
    final int maxTrials;
    final int pause;
    final String dbClassName;
    final boolean nativeService;
    final String keystoreName;
    final String alias;
    final String trustStoreName;

    Client client;
    Process process;

    public ShamirsBaseUnit(Map<String, String> config) {
        this.config = config;
        this.baseUrl = config.getOrDefault("de.christofreichardt.shamirsweb.test.baseUrl", "https://localhost:8443/shamir/v1");
        this.externalService = Boolean.parseBoolean(config.getOrDefault("de.christofreichardt.shamirsweb.test.externalService", "false"));
        this.maxTrials = Integer.parseInt(config.getOrDefault("de.christofreichardt.shamirsweb.test.maxTrials", "10"));
        this.pause = Integer.parseInt(config.getOrDefault("de.christofreichardt.shamirsweb.test.pause", "1"));
        this.dbClassName = config.getOrDefault("de.christofreichardt.shamirsweb.test.dbClassName", "de.christofreichardt.shamirsweb.test.NativeMariaDB");
        this.nativeService = ServiceType.valueOf(config.getOrDefault("de.christofreichardt.shamirsweb.test.serviceType", "native").toUpperCase()) == ServiceType.NATIVE;
        this.keystoreName = config.getOrDefault("de.christofreichardt.shamirsweb.test.keystore", "service-id.p12");
        this.alias = config.getOrDefault("de.christofreichardt.shamirsweb.test.alias", "local-shamirs-service-id");
        this.trustStoreName = config.getOrDefault("de.christofreichardt.shamirsweb.test.trustStore", "service-id-trust.p12");
    }
    

    @BeforeAll
    void init() throws InterruptedException, GeneralSecurityException, IOException, ClassNotFoundException, NoSuchMethodException, ReflectiveOperationException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "init()");

        try {
            this.config.entrySet().forEach(entry -> tracer.out().printfIndentln("%s = %s", entry.getKey(), entry.getValue()));

            Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));
            
            File batch = baseDir.resolve(Path.of("sql", "mariadb", "setup-scenario.sql")).toFile();
            Database database = (Database) Class.forName(this.dbClassName).getDeclaredConstructor().newInstance();
            database.execute(batch);

            if (!this.externalService) {
                if (this.nativeService) {
                    ProcessBuilder processBuilder = new ProcessBuilder("java", "-Djava.security.egd=file:/dev/urandom", "-jar", "target/shamirs-service.jar");
                    File workingDir = baseDir.resolve(Path.of("..", "shamirs-service")).toFile();
                    File logFile = baseDir.resolve(this.config.getOrDefault("de.christofreichardt.shamirsweb.test.spring.log", "log/spring-boot.log")).toFile();
                    this.process = processBuilder.directory(workingDir)
                            .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                            .start();
                } else {
                    Path logDir = Path.of(System.getProperty("user.dir"), "..", "data", "log").toRealPath();
                    ProcessBuilder processBuilder = new ProcessBuilder(
                            "docker", "run", "--interactive", "--tty", "--rm", "--network=shamirs-network", "--hostname=shamirs-service", "--name=shamirs-service",
                            "--publish", "127.0.0.1:8443:8443", "--mount", String.format("type=bind,src=%s,dst=/home/vodalus/shamirs-service/log", logDir),
                            "--detach", "shamirs-service:latest", String.format("--keystore=%s", this.keystoreName), String.format("--alias=%s", this.alias)
                    );
                    this.process = processBuilder.start();
                }
                tracer.out().printfIndentln("this.process.pid() = %d", this.process.pid());
            }

            InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream(String.format("de/christofreichardt/shamirsweb/test/%s", this.trustStoreName));
            Objects.requireNonNull(inputStream, "No InputStream for truststore.");
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(inputStream, "changeit".toCharArray());
            
            inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/test-user-0-id.p12");
            Objects.requireNonNull(inputStream, "No InputStream for keystore.");
            KeyStore keystore = KeyStore.getInstance("pkcs12");
            keystore.load(inputStream, "changeit".toCharArray());
            
            this.client = ClientBuilder
                    .newBuilder()
                    .withConfig(new ClientConfig().connectorProvider(new ApacheConnectorProvider()))
                    .register(MyClientRequestFilter.class)
                    .register(MyClientResponseFilter.class)
                    .trustStore(trustStore)
                    .keyStore(keystore, "changeit".toCharArray())
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
            boolean abort = false;
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

                    tracer.out().printfIndentln("%d. Connection attempt failed: ex.getCause() = %s", trials, ex.getCause());

                    if (ex.getCause() != null && ((ex.getCause() instanceof ConnectException) || ex.getCause() instanceof SSLHandshakeException)) {
                        if (trials >= this.maxTrials) {
                            abort = true;
                            break;
                        } else {
                            tracer.out().printfIndentln("Waiting %d second(s) ...", this.pause);
                            Thread.sleep(TimeUnit.SECONDS.toMillis(this.pause));
                        }
                    } else if (ex.getCause() != null && (ex.getCause() instanceof IOException)) {
                        abort = true;
                        break;
                    } else {
                        tracer.logMessage(LogLevel.WARNING, String.format("Something went wrong. Check pid=%d", this.process.pid()), getClass(), "ping()");
                        break;
                    }
                }
            } while (true);
            
            if (abort) {
                tracer.logMessage(LogLevel.SEVERE, "Cannot establish connection to the service. Aborting ...", getClass(), "ping()");
                if (!this.externalService) {
                    this.process.destroy();
                    boolean terminated = this.process.waitFor(5, TimeUnit.SECONDS);
                    tracer.out().printfIndentln("terminated = %b", terminated);
                }
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
            } else {
                try ( Response response = this.client.target(this.baseUrl)
                        .path("management")
                        .path("restart")
                        .request()
                        .post(Entity.json(JsonValue.EMPTY_JSON_OBJECT))) {
                    response.readEntity(JsonObject.class);
                }
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
