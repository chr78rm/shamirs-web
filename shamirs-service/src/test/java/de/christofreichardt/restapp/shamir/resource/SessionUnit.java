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
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.restapp.shamir.ShamirsApp;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    void rollover() throws InterruptedException, SQLException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "rollover()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            final long IDLE_TIME = 1; // seconds

            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "activate()");
            try {
                Optional<DatabasedKeystore> keystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(KEYSTORE_ID);

                assertThat(keystore).isNotEmpty();
                tracer.out().printfIndentln("keystore = %s", keystore.get());
                assertThat(keystore.get().getSessions().isEmpty()).isFalse();

                Session currentSession = keystore.get().getSessions().iterator().next();

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

                String sql = String.format(
                        "SELECT k.id, k.descriptive_name, k.modification_time, k.current_partition_id, k.version, s.id AS session_id, s.phase, s.idle_time, s.modification_time\n"
                        + "FROM keystore k\n"
                        + "LEFT JOIN csession s ON k.id = s.keystore_id\n"
                        + "WHERE k.id = '%s'\n"
                        + "ORDER BY k.id, s.modification_time",
                        KEYSTORE_ID
                );

                List<Map<String, Object>> result = this.jdbcTemplate.query(
                        sql,
                        (resultSet, rowNum) -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("keystore_id", resultSet.getString("k.id"));
                            map.put("descriptive_name", resultSet.getString("k.descriptive_name"));
                            map.put("modification_time", resultSet.getString("k.modification_time"));
                            map.put("current_partition_id", resultSet.getString("k.current_partition_id"));
                            map.put("session_id", resultSet.getString("session_id"));
                            map.put("phase", resultSet.getString("s.phase"));
                            return map;
                        });
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));

                assertThat(result).isNotEmpty();
                assertThat(result.get(0).get("phase")).isEqualTo(Session.Phase.ACTIVE.name());

                final long FIXED_RATE = 5000L;
                Thread.sleep((IDLE_TIME + 1) * 1000 + FIXED_RATE);

                result = this.jdbcTemplate.query(
                        sql,
                        (resultSet, rowNum) -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("keystore_id", resultSet.getString("id"));
                            map.put("descriptive_name", resultSet.getString("descriptive_name"));
                            map.put("modification_time", resultSet.getString("modification_time"));
                            map.put("current_partition_id", resultSet.getString("current_partition_id"));
                            map.put("session_id", resultSet.getString("session_id"));
                            map.put("phase", resultSet.getString("phase"));
                            return map;
                        });
                result.forEach(row -> tracer.out().printfIndentln("row = %s", row));
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
