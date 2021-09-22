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
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.restapp.shamir.SessionSanitizer;
import de.christofreichardt.restapp.shamir.ShamirsApp;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Participant;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.KeystoreTestService;
import de.christofreichardt.restapp.shamir.service.ParticipantService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 * @author Developer
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ShamirsApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SessionUnit implements Traceable, WithAssertions {

    JdbcTemplate jdbcTemplate;
    Scenario scenario;

    @Autowired
    DataSource dataSource;

    @Autowired
    KeystoreService keystoreService;

    @Autowired
    SessionService sessionService;

    @Autowired
    ParticipantService participantService;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    ScheduledExecutorService scheduledExecutorService;

    @Autowired
    SessionSanitizer sessionSanitizer;

    @Autowired
    @Qualifier("test")
    KeystoreService keystoreTestService;

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

    @Test
    @Order(1)
    void rollover() throws InterruptedException, SQLException, GeneralSecurityException, IOException, ExecutionException, TimeoutException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "rollover()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final long IDLE_TIME = 1; // seconds

            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "rollover()");
            try {
                Optional<DatabasedKeystore> databasedKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(KEYSTORE_ID);
                assertThat(databasedKeystore).isNotEmpty();
                tracer.out().printfIndentln("keystore = %s", databasedKeystore.get());
                assertThat(databasedKeystore.get().getSessions().isEmpty()).isFalse();

                Session currentSession = databasedKeystore.get().currentSession();
                tracer.out().printfIndentln("currentSession = %s", currentSession);
                assertThat(currentSession.isProvisioned()).isTrue();
                assertThat(currentSession.getId()).isEqualTo(SESSION_ID);
                assertThat(currentSession.getKeystore().getId()).isEqualTo(KEYSTORE_ID);

                Duration duration = Duration.of(IDLE_TIME, ChronoUnit.SECONDS);
                currentSession.activated(duration);
                this.sessionService.save(currentSession);

                String selectKeystoreWithSession
                        = "SELECT k.id, k.descriptive_name, k.modification_time, k.current_partition_id, k.version, s.id AS session_id, s.phase, s.idle_time, s.modification_time\n"
                        + "FROM keystore k\n"
                        + "LEFT JOIN csession s ON k.id = s.keystore_id\n"
                        + "WHERE k.id = '%s' AND s.phase = '%s'";
                RowMapper<Map<String, Object>> keystoreWithSessionRowMapper = (ResultSet resultSet, int rowNum) -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("keystore_id", resultSet.getString("k.id"));
                    map.put("k.descriptive_name", resultSet.getString("k.descriptive_name"));
                    map.put("k.modification_time", resultSet.getString("k.modification_time"));
                    map.put("k.current_partition_id", resultSet.getString("k.current_partition_id"));
                    map.put("session_id", resultSet.getString("session_id"));
                    map.put("s.phase", resultSet.getString("s.phase"));
                    map.put("s.idle_time", resultSet.getString("s.idle_time"));
                    map.put("s.modification_time", resultSet.getString("s.modification_time"));
                    return map;
                };

                String selectKeystoreWithSlices
                        = "SELECT k.id, k.descriptive_name, k.current_partition_id, k.modification_time, s.id, s.processing_state, s.modification_time, s.partition_id, s.amount\n"
                        + "FROM keystore k\n"
                        + "LEFT JOIN slice s ON s.keystore_id = k.id\n"
                        + "WHERE k.id = '%s' AND s.processing_state = '%s'";
                RowMapper<Map<String, Object>> keystoreWithSlicesRowMapper = (ResultSet resultSet, int rowNum) -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("keystore_id", resultSet.getString("k.id"));
                    map.put("k.descriptive_name", resultSet.getString("k.descriptive_name"));
                    map.put("k.modification_time", resultSet.getString("k.modification_time"));
                    map.put("k.current_partition_id", resultSet.getString("k.current_partition_id"));
                    map.put("slice_id", resultSet.getString("s.id"));
                    map.put("s.processing_state", resultSet.getString("s.processing_state"));
                    map.put("s.modification_time", resultSet.getString("s.modification_time"));
                    map.put("s.partition_id", resultSet.getString("s.partition_id"));
                    map.put("s.amount", resultSet.getString("s.amount"));
                    return map;
                };

                List<Map<String, Object>> result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSession, KEYSTORE_ID, SessionPhase.ACTIVE.name()),
                        keystoreWithSessionRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(1);
                tracer.out().printfIndentln("------------");
                tracer.out().flush();

                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSlices, KEYSTORE_ID, SliceProcessingState.POSTED.name()),
                        keystoreWithSlicesRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                tracer.out().flush();
                assertThat(result.size()).isEqualTo(7);
                tracer.out().printfIndentln("------------");

                ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.schedule(this.sessionSanitizer, IDLE_TIME + 2, TimeUnit.SECONDS);
                scheduledFuture.get(IDLE_TIME + 5, TimeUnit.SECONDS);

                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSession, KEYSTORE_ID, SessionPhase.CLOSED.name()),
                        keystoreWithSessionRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                tracer.out().flush();
                assertThat(result.size()).isEqualTo(1);
                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSession, KEYSTORE_ID, SessionPhase.PROVISIONED.name()),
                        keystoreWithSessionRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(1);
                tracer.out().printfIndentln("------------");

                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSlices, KEYSTORE_ID, SliceProcessingState.EXPIRED.name()),
                        keystoreWithSlicesRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(7);
                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSlices, KEYSTORE_ID, SliceProcessingState.CREATED.name()),
                        keystoreWithSlicesRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(7);

                databasedKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(KEYSTORE_ID);
                assertThat(databasedKeystore).isNotEmpty();
                tracer.out().printfIndentln("keystore = %s", databasedKeystore.get());
                KeyStore keyStore = databasedKeystore.get().keystoreInstance();
                ShamirsProtection shamirsProtection = new ShamirsProtection(databasedKeystore.get().sharePoints());
                Iterator<String> iter = keyStore.aliases().asIterator();
                while (iter.hasNext()) {
                    String alias = iter.next();
                    KeyStore.Entry entry = keyStore.getEntry(alias, shamirsProtection);
                    tracer.out().printfIndentln("entry.getAttributes() = %s", entry.getAttributes());
                }
            } finally {
                qTracer.wayout();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(2)
    void idleKeystore() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "idleKeystore()");

        try {
            final String THE_IDLE_KEYSTORE_ID = "e509eaf0-3fec-4972-9e32-48e6911710f7";

            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "idleKeystore()");
            try {
                Optional<DatabasedKeystore> databasedKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(THE_IDLE_KEYSTORE_ID);
                assertThat(databasedKeystore).isNotEmpty();
                tracer.out().printfIndentln("keystore = %s", databasedKeystore.get());
                assertThat(databasedKeystore.get().getSessions().isEmpty()).isFalse();

                Session currentSession = databasedKeystore.get().currentSession();
                tracer.out().printfIndentln("currentSession = %s", currentSession);
                assertThat(currentSession.isProvisioned()).isTrue();

                KeyStore keyStore = databasedKeystore.get().keystoreInstance();
                ShamirsProtection shamirsProtection = new ShamirsProtection(databasedKeystore.get().sharePoints());
                Iterator<String> iter = keyStore.aliases().asIterator();
                while (iter.hasNext()) {
                    String alias = iter.next();
                    KeyStore.Entry entry = keyStore.getEntry(alias, shamirsProtection);
                    tracer.out().printfIndentln("entry.getAttributes() = %s", entry.getAttributes());
                }
            } finally {
                qTracer.wayout();
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(3)
    void rolloverMultipleKeystores() throws GeneralSecurityException, IOException, ParticipantService.ParticipantNotFoundException, InterruptedException, ExecutionException, TimeoutException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "rolloverMultipleKeystores()");

        try {
            tracer.out().printfIndentln("this.keystoreTestService.getClass().getName() = %s", this.keystoreTestService.getClass().getName());

            final String MY_SECRET_KEY_ALIAS = "my-secret-key", MY_PRIVATE_EC_KEY_ALIAS = "my-private-ec-key";
            final int KEYSTORE_COUNT = 3;
            final long IDLE_TIME = 1, TIME_OUT = 5; // seconds

            // same instructions for all keystores under test
            JsonObject keystoreInstructions = Json.createObjectBuilder()
                    .add("shares", 12)
                    .add("threshold", 4)
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

            // create and persists the keystores, prepare an IN expression with their ids
            String keystoreIdsExpr;
            StringBuilder stringBuilder = new StringBuilder("(");
            for (int i = 0; i < KEYSTORE_COUNT; i++) {
                JsonObject instructions = Json.createObjectBuilder(keystoreInstructions)
                        .add("descriptiveName", "keystore-" + i)
                        .build();
                KeystoreGenerator keystoreGenerator = new KeystoreGenerator(instructions);
                Map<String, Participant> participants = this.participantService.findByPreferredNames(keystoreGenerator.participantNames());
                DatabasedKeystore keystore = keystoreGenerator.makeDBKeystore(participants);
                keystore = this.keystoreTestService.persist(keystore);
                stringBuilder.append("'");
                stringBuilder.append(keystore.getId());
                stringBuilder.append("'");
                if (i < KEYSTORE_COUNT - 1) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append(")");
            keystoreIdsExpr = stringBuilder.toString();

            tracer.out().printfIndentln("keystoreIdsExpr = %s", keystoreIdsExpr);

            String selectKeystoresWithSlicesAndParticipants
                    = "SELECT k.id, k.descriptive_name, k.modification_time, k.current_partition_id, sl.id, sl.processing_state, sl.modification_time, sl.partition_id, sl.amount, p.id, p.preferred_name\n"
                    + "FROM keystore k\n"
                    + "LEFT JOIN slice sl ON sl.keystore_id = k.id\n"
                    + "LEFT JOIN participant p ON sl.participant_id = p.id\n"
                    + "WHERE k.id IN %s\n"
                    + "ORDER BY k.descriptive_name, p.preferred_name";

            RowMapper<Map<String, String>> keystoreWithSlicesAndParticipantsRowMapper = (ResultSet resultSet, int rowNum) -> {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("keystore_id", resultSet.getString("k.id"));
                map.put("k.descriptive_name", resultSet.getString("k.descriptive_name"));
                map.put("k.modification_time", resultSet.getString("k.modification_time"));
                map.put("k.current_partition_id", resultSet.getString("k.current_partition_id"));
                map.put("slice_id", resultSet.getString("sl.id"));
                map.put("sl.processing_state", resultSet.getString("sl.processing_state"));
                map.put("sl.modification_time", resultSet.getString("sl.modification_time"));
                map.put("sl.partition_id", resultSet.getString("sl.partition_id"));
                map.put("sl.amount", resultSet.getString("sl.amount"));
                map.put("participant_id", resultSet.getString("p.id"));
                map.put("p.preferred_name", resultSet.getString("p.preferred_name"));
                return map;
            };

            String selectKeystoresWithSessions
                    = "SELECT k.id, k.descriptive_name, k.modification_time, k.current_partition_id, s.id AS session_id, s.phase, s.idle_time, s.expiration_time, s.modification_time\n"
                    + "FROM keystore k\n"
                    + "LEFT JOIN csession s ON k.id = s.keystore_id\n"
                    + "WHERE k.id IN %s\n";

            RowMapper<Map<String, String>> keystoreWithSessionRowMapper = (ResultSet resultSet, int rowNum) -> {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("keystore_id", resultSet.getString("k.id"));
                map.put("k.descriptive_name", resultSet.getString("k.descriptive_name"));
                map.put("k.modification_time", resultSet.getString("k.modification_time"));
                map.put("k.current_partition_id", resultSet.getString("k.current_partition_id"));
                map.put("session_id", resultSet.getString("session_id"));
                map.put("s.phase", resultSet.getString("s.phase"));
                map.put("s.idle_time", resultSet.getString("s.idle_time"));
                map.put("s.expiration_time", resultSet.getString("s.expiration_time"));
                map.put("s.modification_time", resultSet.getString("s.modification_time"));
                return map;
            };

            // assert that the keystores have been properly stored
            tracer.out().printfIndentln("--------- selectKeystoresWithSlicesAndParticipants ---------");
            List<Map<String, String>> result = this.jdbcTemplate.query(String.format(selectKeystoresWithSlicesAndParticipants, keystoreIdsExpr), keystoreWithSlicesAndParticipantsRowMapper);
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
            tracer.out().println();
            assertThat(result.size()).isEqualTo(KEYSTORE_COUNT * keystoreInstructions.getJsonArray("sizes").size());
            tracer.out().printfIndentln("---------       selectKeystoresWithSessions       ---------");
            result = this.jdbcTemplate.query(String.format(selectKeystoresWithSessions, keystoreIdsExpr), keystoreWithSessionRowMapper);
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
            tracer.out().println();
            assertThat(result.size()).isEqualTo(KEYSTORE_COUNT);
            assertThat(
                    result.stream()
                            .allMatch(row -> Objects.equals(row.get("s.phase"), SessionPhase.PROVISIONED.name()))
            ).isTrue();

            // extract the keystore ids from the result set
            Set<String> keystoreIds = result.stream()
                    .map(row -> row.get("keystore_id"))
                    .collect(Collectors.toUnmodifiableSet());

            tracer.out().printfIndentln("keystoreIds = %s", keystoreIds);

            // activate the provisioned sessions
            final Duration duration = Duration.of(IDLE_TIME, ChronoUnit.SECONDS);
            keystoreIds.stream()
                    .map(keystoreId -> this.keystoreTestService.findByIdWithActiveSlicesAndCurrentSession(keystoreId))
                    .map(keystore -> keystore.orElseThrow())
                    .peek(keystore -> tracer.out().printfIndentln("keystore = %s", keystore))
                    .map(keystore -> keystore.currentSession())
                    .peek(session -> tracer.out().printfIndentln("session = %s", session))
                    .forEach(session -> {
                        session.activated(duration);
                        this.sessionService.save(session);
                    });
            tracer.out().println();

            // assert that all sessions have been activated
            tracer.out().printfIndentln("---------       selectKeystoresWithSessions       ---------");
            result = this.jdbcTemplate.query(String.format(selectKeystoresWithSessions, keystoreIdsExpr), keystoreWithSessionRowMapper);
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
            tracer.out().println();
            assertThat(result.size()).isEqualTo(KEYSTORE_COUNT);
            assertThat(
                    result.stream()
                            .allMatch(row -> Objects.equals(row.get("s.phase"), SessionPhase.ACTIVE.name()))
            ).isTrue();

            // chose one of the keystores and set it up for failure
            Random random = new Random();
            String randomKeystoreId = keystoreIds.toArray(new String[keystoreIds.size()])[random.nextInt(keystoreIds.size())];
            ((KeystoreTestService) this.keystoreTestService).setFailedKeystoreId(randomKeystoreId);

            // trigger rollover
            this.sessionSanitizer.setKeystoreService(this.keystoreTestService);
            ScheduledFuture<?> scheduledFuture = this.scheduledExecutorService.schedule(this.sessionSanitizer, IDLE_TIME + 2, TimeUnit.SECONDS);
            scheduledFuture.get(TIME_OUT, TimeUnit.SECONDS);

            // assert that the chosen keystore has failed to rollover
            tracer.out().printfIndentln("---------       selectKeystoresWithSessions       ---------");
            result = this.jdbcTemplate.query(String.format(selectKeystoresWithSessions, keystoreIdsExpr), keystoreWithSessionRowMapper);
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
            tracer.out().println();
            assertThat(
                    result.stream()
                            .filter(row -> Objects.equals(row.get("keystore_id"), randomKeystoreId))
                            .allMatch(row -> Objects.equals(row.get("s.phase"), SessionPhase.ACTIVE.name()))
            ).isTrue();
            assertThat(
                    result.stream()
                            .filter(row -> Objects.equals(row.get("keystore_id"), randomKeystoreId))
                            .collect(Collectors.toList())
            ).hasSize(1);

            // assert that the failed keystore is still loadable
            Optional<DatabasedKeystore> failedKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(randomKeystoreId);
            assertThat(failedKeystore).isNotEmpty();
            failedKeystore.get().keystoreInstance();

            // trigger rollover again
            this.sessionSanitizer.setKeystoreService(this.keystoreService);
            scheduledFuture = this.scheduledExecutorService.schedule(this.sessionSanitizer, 0, TimeUnit.SECONDS);
            scheduledFuture.get(TIME_OUT, TimeUnit.SECONDS);

            // assert that all keystores have been rolled over
            tracer.out().printfIndentln("---------       selectKeystoresWithSessions       ---------");
            result = this.jdbcTemplate.query(String.format(selectKeystoresWithSessions, keystoreIdsExpr), keystoreWithSessionRowMapper);
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
            assertThat(result.size()).isEqualTo(KEYSTORE_COUNT * 2);
            assertThat(
                    result.stream()
                            .allMatch(row -> Objects.equals(row.get("s.phase"), SessionPhase.CLOSED.name()) || Objects.equals(row.get("s.phase"), SessionPhase.PROVISIONED.name()))
            ).isTrue();
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
