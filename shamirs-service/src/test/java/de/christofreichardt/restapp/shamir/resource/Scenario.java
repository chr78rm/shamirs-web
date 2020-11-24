/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.sql.rowset.serial.SerialBlob;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractLobCreatingPreparedStatementCallback;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;

/**
 *
 * @author Developer
 */
public class Scenario implements Traceable {

    final JdbcTemplate jdbcTemplate;

    public Scenario(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void cleanup() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "cleanup()");

        try {
            int[] affectedRows = this.jdbcTemplate.batchUpdate(
                    "DELETE FROM slice",
                    "DELETE FROM keystore",
                    "DELETE FROM participant"
            );
            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %s", rows);
            }
        } finally {
            tracer.wayout();
        }
    }

    void setup() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "setup()");

        try {
            cleanup();
            insertKeystore();
            insertParticipants();
            insertSlices();
        } finally {
            tracer.wayout();
        }
    }

    void insertKeystore() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertKeystore()");

        try {
            String sql = "INSERT INTO keystore (id, descriptive_name, store, creation_time, modification_time) \n"
                    + "VALUES (\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    CURRENT_TIMESTAMP,\n"
                    + "    CURRENT_TIMESTAMP\n"
                    + ")";

            final byte[] keystoreBytes = Files.readAllBytes(Path.of("..", "sql", "my-keys.p12"));

            DefaultLobHandler defaultLobHandler = new DefaultLobHandler();
            Integer result = this.jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(defaultLobHandler) {
                @Override
                protected void setValues(PreparedStatement preparedStatement, LobCreator lobCreator) throws SQLException, DataAccessException {
                    preparedStatement.setString(1, "5adab38c-702c-4559-8a5f-b792c14b9a43");
                    preparedStatement.setString(2, "my-first-keystore");
                    lobCreator.setBlobAsBytes(preparedStatement, 3, keystoreBytes);
                }
            });
            
            tracer.out().printfIndentln("result = %d", result);
        } finally {
            tracer.wayout();
        }
    }

    void insertParticipants() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertParticipants()");

        try {
            List<Object[]> batchArgs = List.of(
                    new Object[]{"8844dd34-c836-4060-ba73-c6d86ad1275d", "christof"},
                    new Object[]{"f6cdb2e5-ea3e-405f-ad0a-14c034497e23", "test-user-1"},
                    new Object[]{"337dd2bd-508d-423d-84ca-81770d8ac30d", "test-user-2"},
                    new Object[]{"48ef6c98-0e04-49bc-9f7f-01f2cec3ccac", "test-user-3"},
                    new Object[]{"222185fb-6cbc-45e6-90d1-e5390fb2f9f9", "test-user-4"},
                    new Object[]{"b78d63a0-e365-4934-93e4-ec1ea713cba8", "test-user-5"},
                    new Object[]{"54ce43ce-c335-47a2-98b8-1bd1fc4f93a4", "test-user-6"}
            );

            String sql = "INSERT INTO participant (id, preferred_name, effective_time)\n"
                    + "VALUES (\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    CURRENT_TIMESTAMP\n"
                    + ")";

            int[] affectedRows = this.jdbcTemplate.batchUpdate(sql, batchArgs);
            
            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %s", rows);
            }
        } finally {
            tracer.wayout();
        }
    }

    void insertSlices() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertSlices()");

        try {
            List<Object[]> batchArgs = List.of(
                    new Object[]{
                        "9a83d398-35d6-4959-aea2-1c930a936b43",
                        "8844dd34-c836-4060-ba73-c6d86ad1275d",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-0.json")),
                        "POSTED"
                    },
                    new Object[]{
                        "4f66bb7d-417d-48d8-a269-e0d2011715f1",
                        "f6cdb2e5-ea3e-405f-ad0a-14c034497e23",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-1.json")),
                        "POSTED"
                    },
                    new Object[]{
                        "35650def-5d15-40d8-a707-21ecf9799d1d",
                        "337dd2bd-508d-423d-84ca-81770d8ac30d",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-2.json")),
                        "POSTED"
                    },
                    new Object[]{
                        "187b30af-65f6-4bb1-8feb-68263dcdffa7",
                        "48ef6c98-0e04-49bc-9f7f-01f2cec3ccac",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-3.json")),
                        "POSTED"
                    },
                    new Object[]{
                        "6dc636e7-efa8-4e30-9ee3-a373e8063e30",
                        "222185fb-6cbc-45e6-90d1-e5390fb2f9f9",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-4.json")),
                        "POSTED"
                    },
                    new Object[]{
                        "6ef561a2-020a-492e-abb3-106a467a4908",
                        "b78d63a0-e365-4934-93e4-ec1ea713cba8",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-5.json")),
                        "POSTED"
                    },
                    new Object[]{
                        "7b5ab104-a05c-4103-9582-303be0dcb173",
                        "54ce43ce-c335-47a2-98b8-1bd1fc4f93a4",
                        "5adab38c-702c-4559-8a5f-b792c14b9a43",
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        Files.readAllBytes(Path.of("..", "sql", "test-6.json")),
                        "POSTED"
                    }
            );

            String sql = "INSERT INTO slice (id, participant_id, keystore_id, partition_id, share, processing_state, effective_time)\n"
                    + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
            
            int[] affectedRows = this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    Object[] row = batchArgs.get(i);
                    preparedStatement.setString(1, (String) row[0]);
                    preparedStatement.setString(2, (String) row[1]);
                    preparedStatement.setString(3, (String) row[2]);
                    preparedStatement.setString(4, (String) row[3]);
                    preparedStatement.setBlob(5, new SerialBlob((byte[]) row[4]));
                    preparedStatement.setString(6, (String) row[5]);
                }
                @Override
                public int getBatchSize() {
                    return batchArgs.size();
                }
            });
            
            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %s", rows);
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
