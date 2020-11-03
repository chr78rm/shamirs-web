/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.QueueTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsLoadParameter;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.json.JsonTracer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 * @author Developer
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ShamirsApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class KeystoreGeneratorUnit implements Traceable {

    final JsonTracer jsonTracer = new JsonTracer() {
        @Override
        public AbstractTracer getCurrentTracer() {
            return KeystoreGeneratorUnit.this.getCurrentTracer();
        }
    };

    final JsonObject keystoreInstructions = Json.createObjectBuilder()
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

    JdbcTemplate jdbcTemplate;
    Scenario scenario;
    
    @Autowired
    DataSource dataSource;

    @BeforeAll
    void init() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "init()");

        try {
            List<String> propertyNames = new ArrayList<>(System.getProperties().stringPropertyNames());
            propertyNames.stream()
                    .sorted()
                    .forEach((propertyName) -> tracer.out().printfIndentln("%s = %s", propertyName, System.getProperties().getProperty(propertyName)));
            
            Security.addProvider(new ShamirsProvider());
            TracerFactory.getInstance().openQueueTracer();
            this.jdbcTemplate = new JdbcTemplate(this.dataSource);
            this.scenario = new Scenario(this.jdbcTemplate);
            this.scenario.setup();
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void partition() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "partition()");

        try {
            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "partition()");
            try {
                KeystoreGenerator keystoreGenerator = new KeystoreGenerator(this.keystoreInstructions);
                JsonArray sizes = this.keystoreInstructions.getJsonArray("sizes");
                Map<String, byte[]> partition = keystoreGenerator.partition();
                boolean allMatched = partition.entrySet().stream()
                        .map(entry -> {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(entry.getValue());
                            JsonReader jsonReader = Json.createReader(byteArrayInputStream);
                            JsonObject slice = jsonReader.readValue().asJsonObject();

                            return new AbstractMap.SimpleEntry<String, JsonObject>(entry.getKey(), slice);
                        })
                        .peek(entry -> {
                            tracer.out().printfIndentln("participant = %s", entry.getKey());
                            this.jsonTracer.trace(entry.getValue());
                        })
                        .allMatch(entry -> {
                            Optional<JsonObject> optionalSize = sizes.stream()
                                    .map(size -> size.asJsonObject())
                                    .filter(size -> Objects.equals(entry.getKey(), size.getString("participant")))
                                    .findFirst();
                            assertThat(optionalSize).isNotEmpty();
                            return optionalSize.get().getInt("size") == entry.getValue().getJsonArray("SharePoints").size();
                        });
                assertThat(allMatched).isTrue();
            } finally {
                qTracer.wayout();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    void keystoreBytes() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "keystoreBytes()");

        try {
            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "keystoreBytes()");
            try {
                KeystoreGenerator keystoreGenerator = new KeystoreGenerator(this.keystoreInstructions);
                byte[] keystoreBytes = keystoreGenerator.keystoreBytes();
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(keystoreBytes);
                ShamirsProtection shamirsProtection = new ShamirsProtection(keystoreGenerator.partition);
                ShamirsLoadParameter shamirsLoadParameter = new ShamirsLoadParameter(byteArrayInputStream, shamirsProtection);
                KeyStore keyStore = KeyStore.getInstance("ShamirsKeystore", Security.getProvider(ShamirsProvider.NAME));
                keyStore.load(shamirsLoadParameter);
                Set<String> aliases = new HashSet<>();
                keyStore.aliases().asIterator().forEachRemaining(alias -> aliases.add(alias));
                assertThat(aliases).contains("my-secret-key");
            } finally {
                qTracer.wayout();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
//    @Disabled
    void selectKeystores() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "selectKeystores()");

        try {
            List<Map<String, Object>> result = this.jdbcTemplate.query(
                    "SELECT k.id, k.descriptive_name, k.effective_time FROM keystore k",
                    (resultSet, rowNum) -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", resultSet.getString("id"));
                        map.put("descriptive_name", resultSet.getString("descriptive_name"));
                        map.put("effective_time", resultSet.getString("effective_time"));

                        return map;
                    });
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
        } finally {
            tracer.wayout();
        }
    }

    @AfterAll
    void exit() throws SQLException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "exit()");

        try {
            TracerFactory.getInstance().closeQueueTracer();
            Security.removeProvider(ShamirsProvider.NAME);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }

}
