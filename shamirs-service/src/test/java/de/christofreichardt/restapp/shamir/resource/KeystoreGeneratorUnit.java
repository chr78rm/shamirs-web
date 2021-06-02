/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.QueueTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsLoadParameter;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.restapp.shamir.ShamirsApp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.assertj.core.api.WithAssertions;
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
public class KeystoreGeneratorUnit implements Traceable, WithAssertions {

    final JsonTracer jsonTracer = new JsonTracer() {
        @Override
        public AbstractTracer getCurrentTracer() {
            return KeystoreGeneratorUnit.this.getCurrentTracer();
        }
    };

    final String MY_ALIAS = "my-secret-key", DONALDS_ALIAS = "donalds-private-ec-key", DAGOBERTS_ALIAS = "dagoberts-private-dsa-key", DAISIES_ALIAS = "daisies-private-rsa-key";
    final JsonObject keystoreInstructions = Json.createObjectBuilder()
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

    JdbcTemplate jdbcTemplate;
    Scenario scenario;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;

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

            this.jdbcTemplate = new JdbcTemplate(this.dataSource);
            this.scenario = new Scenario(this.jdbcTemplate);
            this.scenario.setup();
            this.entityManagerFactory.getCache().evictAll();
        } finally {
            tracer.wayout();
        }
    }

    static class JsonByteReader {

        final byte[] bytes;

        public JsonByteReader(byte[] bytes) {
            this.bytes = bytes;
        }

        JsonValue readValue() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.bytes);
            JsonReader jsonReader = Json.createReader(byteArrayInputStream);

            return jsonReader.readValue();
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
                            JsonObject slice = new JsonByteReader(entry.getValue())
                                    .readValue()
                                    .asJsonObject();

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

                Iterator<Map.Entry<String, byte[]>> iter = partition.entrySet().iterator();
                while (iter.hasNext()) {
                    byte[] slice = iter.next().getValue();
                    assertThat(Objects.equals(keystoreGenerator.partitionId(),
                            new JsonByteReader(slice)
                                    .readValue()
                                    .asJsonObject()
                                    .getString("PartitionId")
                    )).isTrue();
                }
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
                Iterator<String> iter = keyStore.aliases().asIterator();
                while (iter.hasNext()) {
                    String alias = iter.next();
                    KeyStore.Entry entry = keyStore.getEntry(alias, shamirsProtection);
                    tracer.out().printfIndentln("entry[%s].getAttributes() = %s", alias, entry.getAttributes());
                    aliases.add(alias);
                }
                assertThat(aliases).contains(MY_ALIAS, DONALDS_ALIAS, DAGOBERTS_ALIAS, DAISIES_ALIAS);
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
                    "SELECT k.id, k.descriptive_name, k.current_partition_id, k.creation_time, k.modification_time FROM keystore k",
                    (resultSet, rowNum) -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", resultSet.getString("id"));
                        map.put("descriptive_name", resultSet.getString("descriptive_name"));
                        map.put("current_partition_id", resultSet.getString("current_partition_id"));
                        map.put("creation_time", resultSet.getString("creation_time"));
                        map.put("modification_time", resultSet.getString("modification_time"));

                        return map;
                    });
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void invalidInstructions() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "invalidInstructions()");

        try {
            Map<String, JsonPointer> jsonPointers = Map.of(
                    "shares", KeystoreGenerator.SHARES_POINTER,
                    "threshold", KeystoreGenerator.THRESHOLD_POINTER,
                    "descriptiveName", KeystoreGenerator.DESCRIPTIVE_NAME_POINTER,
                    "keyinfos", KeystoreGenerator.KEY_INFOS_POINTER,
                    "sizes", KeystoreGenerator.SIZES_POINTER
            );
            jsonPointers.forEach((name, pointer) -> {
                JsonObject invalidInstructions = pointer.remove(this.keystoreInstructions);
                assertThatIllegalArgumentException().isThrownBy(() -> new KeystoreGenerator(invalidInstructions))
                        .withMessage(String.format("Missing JSON value '%s'.", name));
            });
            jsonPointers.forEach((name, pointer) -> {
                JsonObject invalidInstructions = pointer.replace(this.keystoreInstructions, JsonValue.NULL);
                this.jsonTracer.trace(invalidInstructions);
                assertThatIllegalArgumentException().isThrownBy(() -> new KeystoreGenerator(invalidInstructions))
                        .withMessage(String.format("Invalid value type for '%s'.", name));
            });
        } finally {
            tracer.wayout();
        }
    }

    @AfterAll
    void exit() throws SQLException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "exit()");

        try {
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
