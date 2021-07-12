/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.restapp.shamir.ShamirsApp;
import de.christofreichardt.restapp.shamir.model.Participant;
import de.christofreichardt.restapp.shamir.service.ParticipantService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class ParticipantUnit implements Traceable, WithAssertions {

    JdbcTemplate jdbcTemplate;
    Scenario scenario;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    ParticipantService participantService;

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
    void selectParticipantsByKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "selectParticipantsByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore

            String selectParticipantsByKeystore
                    = "SELECT p.id, p.preferred_name, p.creation_time\n"
                    + "FROM keystore k JOIN (slice s, participant p) ON (k.id = s.keystore_id AND s.participant_id = p.id)\n"
                    + "WHERE k.id = '%s'";
            List<Map<String, Object>> result = this.jdbcTemplate.query(
                    String.format(selectParticipantsByKeystore, KEYSTORE_ID),
                    (resultSet, rowNum) -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", resultSet.getString("p.id"));
                        map.put("preferred_name", resultSet.getString("p.preferred_name"));
                        map.put("creation_time", resultSet.getString("p.creation_time"));

                        return map;
                    });
            result.forEach(row -> tracer.out().printfIndentln("row = %s", row));

            List<Participant> participants = this.participantService.findByKeystore(KEYSTORE_ID);
            tracer.out().printfIndentln("participants = %s", participants);
            assertThat(participants.size()).isEqualTo(result.size());
            boolean allMatched = participants.stream()
                    .allMatch(participant -> {
                        return result.stream()
                                .filter(row -> row.get("preferred_name").equals(participant.getPreferredName()))
                                .findFirst()
                                .isPresent();
                    });
            assertThat(allMatched).isTrue();
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
