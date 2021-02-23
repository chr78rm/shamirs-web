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
import de.christofreichardt.restapp.shamir.ShamirsApp;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.model.Slice;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(1)
    void rollover() throws InterruptedException, SQLException, GeneralSecurityException, IOException {
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

                Session currentSession = databasedKeystore.get().getSessions().iterator().next();
                tracer.out().printfIndentln("currentSession = %s", currentSession);
                assertThat(currentSession.getPhase()).isEqualTo(Session.Phase.PROVISIONED.name());
                assertThat(currentSession.getId()).isEqualTo(SESSION_ID);
                assertThat(currentSession.getKeystore().getId()).isEqualTo(KEYSTORE_ID);

                Duration duration = Duration.of(IDLE_TIME, ChronoUnit.SECONDS);
                currentSession.setPhase(Session.Phase.ACTIVE.name());
                currentSession.setIdleTime(duration.getSeconds());
                currentSession.setModificationTime(LocalDateTime.now());
                currentSession.setExpirationTime(currentSession.getModificationTime().plusSeconds(duration.getSeconds()));
                this.sessionService.save(currentSession);

                String selectKeystoreWithSession = 
                        "SELECT k.id, k.descriptive_name, k.modification_time, k.current_partition_id, k.version, s.id AS session_id, s.phase, s.idle_time, s.modification_time\n"
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
                
                String selectKeystoreWithSlices =
                        "SELECT k.id, k.descriptive_name, k.current_partition_id, k.modification_time, s.id, s.processing_state, s.modification_time, s.partition_id, s.amount\n"
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
                        String.format(selectKeystoreWithSession, KEYSTORE_ID, Session.Phase.ACTIVE.name()), 
                        keystoreWithSessionRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(1);
                tracer.out().printfIndentln("------------");
                tracer.out().flush();

                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSlices, KEYSTORE_ID, Slice.ProcessingState.POSTED.name()), 
                        keystoreWithSlicesRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                tracer.out().flush();
                assertThat(result.size()).isEqualTo(7);
                tracer.out().printfIndentln("------------");

                final long FIXED_RATE = 2500L;
                Thread.sleep((IDLE_TIME + 1) * 1000 + FIXED_RATE);

                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSession, KEYSTORE_ID, Session.Phase.CLOSED.name()), 
                        keystoreWithSessionRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                tracer.out().flush();
                assertThat(result.size()).isEqualTo(1);
                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSession, KEYSTORE_ID, Session.Phase.PROVISIONED.name()), 
                        keystoreWithSessionRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(1);
                tracer.out().printfIndentln("------------");

                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSlices, KEYSTORE_ID, Slice.ProcessingState.EXPIRED.name()), 
                        keystoreWithSlicesRowMapper
                );
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
                assertThat(result.size()).isEqualTo(7);
                result = this.jdbcTemplate.query(
                        String.format(selectKeystoreWithSlices, KEYSTORE_ID, Slice.ProcessingState.CREATED.name()), 
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

                Session currentSession = databasedKeystore.get().getSessions().iterator().next();
                tracer.out().printfIndentln("currentSession = %s", currentSession);
                assertThat(currentSession.getPhase()).isEqualTo(Session.Phase.PROVISIONED.name());
                
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
