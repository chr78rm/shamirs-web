/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    final Path keystoreBaseDir = Path.of("..", "sql", "keystores");

    final String MY_FIRST_KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43";
    final String THE_TOO_FEW_SLICES_KEYSTORE_ID = "3e6b2af3-63e2-4dcb-bb71-c69f1293b072";
    final String THE_IDLE_KEYSTORE_ID = "e509eaf0-3fec-4972-9e32-48e6911710f7";
    final String THE_PENDING_DOCS_KEYSTORE_ID = "28799acd-6753-451f-965a-8a0eb601eb26";

    static class HexFormatter {

        static String format(byte[] bytes) {
            StringBuilder stringBuilder = new StringBuilder("0x");
            for (byte currentByte : bytes) {
                stringBuilder.append(String.format("%02x", currentByte));
            }

            return stringBuilder.toString();
        }
    }

    public Scenario(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    void cleanup() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "cleanup()");

        try {
            int[] affectedRows = this.jdbcTemplate.batchUpdate(
                    "DELETE FROM document",
                    "DELETE FROM metadata",
                    "DELETE FROM csession",
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

    public void setup() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "setup()");

        try {
            cleanup();
            insertKeystore();
            insertParticipants();
            insertSlices();
            insertSessions();
            insertMetadata();
            insertDocuments();
        } finally {
            tracer.wayout();
        }
    }

    void insertKeystore() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertKeystore()");

        try {
            String sql = "INSERT INTO keystore (id, descriptive_name, store, current_partition_id, shares, threshold, creation_time, modification_time, version) \n"
                    + "VALUES (\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    CURRENT_TIMESTAMP,\n"
                    + "    CURRENT_TIMESTAMP,\n"
                    + "    1\n"
                    + ")";

            final byte[] myFirstKeystoreBytes = Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "my-keys.p12")));
            tracer.out().printfIndentln("myFirstKeystoreBytes = %s", HexFormatter.format(myFirstKeystoreBytes));
            DefaultLobHandler defaultLobHandler = new DefaultLobHandler();
            Integer result = this.jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(defaultLobHandler) {
                @Override
                protected void setValues(PreparedStatement preparedStatement, LobCreator lobCreator) throws SQLException, DataAccessException {
                    preparedStatement.setString(1, MY_FIRST_KEYSTORE_ID);
                    preparedStatement.setString(2, "my-first-keystore");
                    lobCreator.setBlobAsBytes(preparedStatement, 3, myFirstKeystoreBytes);
                    preparedStatement.setString(4, "467b268d-1a7f-4f00-993c-672b82494822");
                    preparedStatement.setInt(5, 12);
                    preparedStatement.setInt(6, 4);
                }
            });
            tracer.out().printfIndentln("result = %d", result);

            final byte[] theTooFewSlicesKeystoreBytes = Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_TOO_FEW_SLICES_KEYSTORE_ID, "my-keys.p12")));
            tracer.out().printfIndentln("theTooFewSlicesKeystoreBytes = %s", HexFormatter.format(theTooFewSlicesKeystoreBytes));
            result = this.jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(defaultLobHandler) {
                @Override
                protected void setValues(PreparedStatement preparedStatement, LobCreator lobCreator) throws SQLException, DataAccessException {
                    preparedStatement.setString(1, THE_TOO_FEW_SLICES_KEYSTORE_ID);
                    preparedStatement.setString(2, "the-too-few-slices-keystore");
                    lobCreator.setBlobAsBytes(preparedStatement, 3, theTooFewSlicesKeystoreBytes);
                    preparedStatement.setString(4, "467b268d-1a7f-4f00-993c-672b82494822");
                    preparedStatement.setInt(5, 12);
                    preparedStatement.setInt(6, 4);
                }
            });

            tracer.out().printfIndentln("result = %d", result);

            final byte[] theIdleKeystoreBytes = Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "my-keys.p12")));
            tracer.out().printfIndentln("theIdleKeystoreBytes = %s", HexFormatter.format(theIdleKeystoreBytes));
            result = this.jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(defaultLobHandler) {
                @Override
                protected void setValues(PreparedStatement preparedStatement, LobCreator lobCreator) throws SQLException, DataAccessException {
                    preparedStatement.setString(1, THE_IDLE_KEYSTORE_ID);
                    preparedStatement.setString(2, "the-idle-keystore");
                    lobCreator.setBlobAsBytes(preparedStatement, 3, theIdleKeystoreBytes);
                    preparedStatement.setString(4, "171e7454-a441-467c-994f-9f879f6008d2");
                    preparedStatement.setInt(5, 12);
                    preparedStatement.setInt(6, 4);
                }
            });

            tracer.out().printfIndentln("result = %d", result);

            final byte[] thePendingDocsKeystoreBytes = Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-keys.p12")));
            tracer.out().printfIndentln("thePendingDocsKeystoreBytes = %s", HexFormatter.format(thePendingDocsKeystoreBytes));
            result = this.jdbcTemplate.execute(sql, new AbstractLobCreatingPreparedStatementCallback(defaultLobHandler) {
                @Override
                protected void setValues(PreparedStatement preparedStatement, LobCreator lobCreator) throws SQLException, DataAccessException {
                    preparedStatement.setString(1, THE_PENDING_DOCS_KEYSTORE_ID);
                    preparedStatement.setString(2, "the-pending-docs-keystore");
                    lobCreator.setBlobAsBytes(preparedStatement, 3, thePendingDocsKeystoreBytes);
                    preparedStatement.setString(4, "576f2709-e0b2-4a5f-af3a-c031597960c7");
                    preparedStatement.setInt(5, 12);
                    preparedStatement.setInt(6, 4);
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
                    new Object[]{"7a8a75d9-e4ac-4f88-9bbd-75d6582c4577", "test-user-0"},
                    new Object[]{"f6cdb2e5-ea3e-405f-ad0a-14c034497e23", "test-user-1"},
                    new Object[]{"337dd2bd-508d-423d-84ca-81770d8ac30d", "test-user-2"},
                    new Object[]{"48ef6c98-0e04-49bc-9f7f-01f2cec3ccac", "test-user-3"},
                    new Object[]{"222185fb-6cbc-45e6-90d1-e5390fb2f9f9", "test-user-4"},
                    new Object[]{"b78d63a0-e365-4934-93e4-ec1ea713cba8", "test-user-5"},
                    new Object[]{"54ce43ce-c335-47a2-98b8-1bd1fc4f93a4", "test-user-6"}
            );

            String sql = "INSERT INTO participant (id, preferred_name, creation_time)\n"
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
                    
                    //
                    // setup slices for keystore[id='5adab38c-702c-4559-8a5f-b792c14b9a43', descriptive_name='my-first-keystore']
                    //

                    new Object[]{
                        "9a83d398-35d6-4959-aea2-1c930a936b43",
                        "8844dd34-c836-4060-ba73-c6d86ad1275d", // christof
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        4,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-0.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "4f66bb7d-417d-48d8-a269-e0d2011715f1",
                        "f6cdb2e5-ea3e-405f-ad0a-14c034497e23", // test-user-1
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        2,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-1.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "35650def-5d15-40d8-a707-21ecf9799d1d",
                        "337dd2bd-508d-423d-84ca-81770d8ac30d", // test-user-2
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        2,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-2.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "187b30af-65f6-4bb1-8feb-68263dcdffa7",
                        "48ef6c98-0e04-49bc-9f7f-01f2cec3ccac", // test-user-3
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-3.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "6dc636e7-efa8-4e30-9ee3-a373e8063e30",
                        "222185fb-6cbc-45e6-90d1-e5390fb2f9f9", // test-user-4
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-4.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "6ef561a2-020a-492e-abb3-106a467a4908",
                        "b78d63a0-e365-4934-93e4-ec1ea713cba8", // test-user-5
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-5.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "7b5ab104-a05c-4103-9582-303be0dcb173",
                        "54ce43ce-c335-47a2-98b8-1bd1fc4f93a4", // test-user-6
                        MY_FIRST_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(MY_FIRST_KEYSTORE_ID, "test-6.json"))),
                        "POSTED"
                    },
                    
                    //
                    // setup slices for keystore[id='3e6b2af3-63e2-4dcb-bb71-c69f1293b072', descriptive_name='the-too-few-slices-keystore']
                    //

                    new Object[]{
                        "ce9a98d9-3237-4949-9de3-327fc2f21d26",
                        "8844dd34-c836-4060-ba73-c6d86ad1275d", // christof
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        4,
                        null,
                        "FETCHED"
                    },
                    new Object[]{
                        "c216e2c5-6bc3-4840-aa42-05564b40f0cd",
                        "f6cdb2e5-ea3e-405f-ad0a-14c034497e23", // test-user-1
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        2,
                        null,
                        "FETCHED"
                    },
                    new Object[]{
                        "de1a09c0-2dc9-4430-bd07-a693aa9a3abb",
                        "337dd2bd-508d-423d-84ca-81770d8ac30d", // test-user-2
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        2,
                        null,
                        "FETCHED"
                    },
                    new Object[]{
                        "56b4db85-1b70-49b8-a934-73b50a0e352a",
                        "48ef6c98-0e04-49bc-9f7f-01f2cec3ccac", // test-user-3
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        null,
                        "FETCHED"
                    },
                    new Object[]{
                        "f23b7254-3cf5-4e21-b7cb-d709edf01d9f",
                        "222185fb-6cbc-45e6-90d1-e5390fb2f9f9", // test-user-4
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_TOO_FEW_SLICES_KEYSTORE_ID, "test-4.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "d126d03b-8802-45e5-8d0b-90644fed3775",
                        "b78d63a0-e365-4934-93e4-ec1ea713cba8", // test-user-5
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_TOO_FEW_SLICES_KEYSTORE_ID, "test-5.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "282782ec-98a2-41f0-a5b9-427f6860de0d",
                        "54ce43ce-c335-47a2-98b8-1bd1fc4f93a4", // test-user-6
                        THE_TOO_FEW_SLICES_KEYSTORE_ID,
                        "467b268d-1a7f-4f00-993c-672b82494822",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_TOO_FEW_SLICES_KEYSTORE_ID, "test-6.json"))),
                        "POSTED"
                    },
                    
                    //
                    // setup slices for keystore[id='e509eaf0-3fec-4972-9e32-48e6911710f7', descriptive_name='the-idle-keystore']
                    //

                    new Object[]{
                        "c9092c53-f9a3-4e35-9ff0-5555c91f1882",
                        "8844dd34-c836-4060-ba73-c6d86ad1275d", // christof
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        4,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-0.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "475bfa68-9e30-46c7-9787-1e4645bba762",
                        "f6cdb2e5-ea3e-405f-ad0a-14c034497e23", // test-user-1
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        2,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-1.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "5bde04da-f62e-40bc-8829-532ada99e529",
                        "337dd2bd-508d-423d-84ca-81770d8ac30d", // test-user-2
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        2,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-2.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "e9b2cc92-1e44-4e23-8b2c-2d03995526e2",
                        "48ef6c98-0e04-49bc-9f7f-01f2cec3ccac", // test-user-3
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-3.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "288b61db-d529-4762-96ec-4e94e25f1af6",
                        "222185fb-6cbc-45e6-90d1-e5390fb2f9f9", // test-user-4
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-4.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "f242a695-d2ee-4bdd-ad82-cc354b89f809",
                        "b78d63a0-e365-4934-93e4-ec1ea713cba8", // test-user-5
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-5.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "0dcb2522-558f-40e2-9d41-97d378f2a47b",
                        "54ce43ce-c335-47a2-98b8-1bd1fc4f93a4", // test-user-6
                        THE_IDLE_KEYSTORE_ID,
                        "171e7454-a441-467c-994f-9f879f6008d2",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_IDLE_KEYSTORE_ID, "test-6.json"))),
                        "POSTED"
                    },
                    
                    //
                    // setup slices for keystore[id='28799acd-6753-451f-965a-8a0eb601eb26', descriptive_name='the-pending_docs-keystore']
                    //

                    new Object[]{
                        "31fe5c5d-3936-4f87-9c9a-c1214b7a7ffa",
                        "8844dd34-c836-4060-ba73-c6d86ad1275d", // christof
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        4,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-0.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "bd801f24-2bbc-4ce2-8bf4-0a39347598df",
                        "f6cdb2e5-ea3e-405f-ad0a-14c034497e23", // test-user-1
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        2,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-1.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "7c590867-3c9e-4f03-87c3-7978ee68e3be",
                        "337dd2bd-508d-423d-84ca-81770d8ac30d", // test-user-2
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        2,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-2.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "3c5ecae8-0934-4088-8b01-0f8183e658be",
                        "48ef6c98-0e04-49bc-9f7f-01f2cec3ccac", // test-user-3
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-3.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "d7089473-b86f-4e77-9ead-1b45f722334d",
                        "222185fb-6cbc-45e6-90d1-e5390fb2f9f9", // test-user-4
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-4.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "e95eed52-0739-4d75-a8a3-604da1e338c2",
                        "b78d63a0-e365-4934-93e4-ec1ea713cba8", // test-user-5
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-5.json"))),
                        "POSTED"
                    },
                    new Object[]{
                        "60407f6a-4861-472f-90cd-c054bbceee58",
                        "54ce43ce-c335-47a2-98b8-1bd1fc4f93a4", // test-user-6
                        THE_PENDING_DOCS_KEYSTORE_ID,
                        "576f2709-e0b2-4a5f-af3a-c031597960c7",
                        1,
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "test-6.json"))),
                        "POSTED"
                    }
            );

            String sql = "INSERT INTO slice (id, participant_id, keystore_id, partition_id, amount, share, processing_state, creation_time, modification_time)\n"
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            int[] affectedRows = this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    tracer.out().printfIndentln("i = %d", i);
                    Object[] row = batchArgs.get(i);
                    String id = (String) row[0];
                    preparedStatement.setString(1, id);
                    preparedStatement.setString(2, (String) row[1]);
                    preparedStatement.setString(3, (String) row[2]);
                    preparedStatement.setString(4, (String) row[3]);
                    preparedStatement.setInt(5, (int) row[4]);
                    if (row[5] != null) {
                        byte[] share = (byte[]) row[5];
                        tracer.out().printfIndentln("slice(%s) = %s", id, HexFormatter.format(share));
                        preparedStatement.setBlob(6, new SerialBlob(share));
                    } else {
                        tracer.out().printfIndentln("slice(%s) = null", id);
                        preparedStatement.setNull(6, Types.BLOB);
                    }
                    preparedStatement.setString(7, (String) row[6]);
                }

                @Override
                public int getBatchSize() {
                    return batchArgs.size();
                }
            });

            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %d", rows);
            }
        } finally {
            tracer.wayout();
        }
    }

    void insertSessions() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertSessions()");

        try {
            final String CURRENT_TIME = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            tracer.out().printfIndentln("CURRENT_TIME = %s", CURRENT_TIME);

            List<Object[]> batchArgs = List.of(
                    new Object[]{"1232d4be-fa07-45d8-b741-65f60ce9ebf0", THE_TOO_FEW_SLICES_KEYSTORE_ID, "PROVISIONED", 0, CURRENT_TIME, CURRENT_TIME, null},
                    new Object[]{"8bff8ac6-fc31-40de-bd6a-eca4348171c5", MY_FIRST_KEYSTORE_ID, "PROVISIONED", 0, CURRENT_TIME, CURRENT_TIME, null},
                    new Object[]{"09f6f079-cd70-4221-a44e-45862a4fb777", THE_IDLE_KEYSTORE_ID, "ACTIVE", 5, CURRENT_TIME, CURRENT_TIME, LocalDateTime.now().plusSeconds(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))},
                    new Object[]{"90d38be1-78f1-417f-99e4-12a082c60141", THE_PENDING_DOCS_KEYSTORE_ID, "PROVISIONED", 0, CURRENT_TIME, CURRENT_TIME, null}
            );

            String sql = "INSERT INTO csession (id, keystore_id, phase, idle_time, creation_time, modification_time, expiration_time, version)\n"
                    + "VALUES (\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    1\n"
                    + ")";

            int[] affectedRows = this.jdbcTemplate.batchUpdate(sql, batchArgs);

            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %s", rows);
            }
        } finally {
            tracer.wayout();
        }
    }

    void insertMetadata() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertMetadata()");

        try {
            final String CURRENT_TIME = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            tracer.out().printfIndentln("CURRENT_TIME = %s", CURRENT_TIME);

            List<Object[]> batchArgs = List.of(
                    new Object[]{"b73ac5ed-81e3-4cdd-94e9-7e2925a9b388", "90d38be1-78f1-417f-99e4-12a082c60141", "pending-doc-1", "PENDING", MetadataAction.SIGN.name(),
                        null, "application/xml", "testers-private-ec-key", CURRENT_TIME, CURRENT_TIME},
                    new Object[]{"374d0eb2-06e7-497e-9eec-7934c6ba7a58", "90d38be1-78f1-417f-99e4-12a082c60141", "pending-doc-2", "PENDING", MetadataAction.SIGN.name(),
                        null, "application/xml", "testers-private-ec-key", CURRENT_TIME, CURRENT_TIME},
                    new Object[]{"74e00512-d3bf-4a6d-b4c8-f2ec3415fd93", "90d38be1-78f1-417f-99e4-12a082c60141", "faulty-pending-doc-1", "PENDING", MetadataAction.SIGN.name(),
                        null, "application/xml", "testers-private-ec-key", CURRENT_TIME, CURRENT_TIME},
                    new Object[]{"04d364de-775a-4d4f-b4b0-7bf7d6cde2ee", "90d38be1-78f1-417f-99e4-12a082c60141", "faulty-pending-doc-2", "PENDING", MetadataAction.SIGN.name(),
                        null, "application/xml", "test-aes-key", CURRENT_TIME, CURRENT_TIME}
            );

            String sql = "INSERT INTO metadata (id, session_id, title, status, intended_action, validated, media_type, key_alias, creation_time, modification_time)\n"
                    + "VALUES (\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?\n"
                    + ")";

            int[] affectedRows = this.jdbcTemplate.batchUpdate(sql, batchArgs);

            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %s", rows);
            }

        } finally {
            tracer.wayout();
        }
    }
    
    void insertDocuments() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "insertDocuments()");

        try {
            final String CURRENT_TIME = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            tracer.out().printfIndentln("CURRENT_TIME = %s", CURRENT_TIME);

            List<Object[]> batchArgs = List.of(
                    new Object[]{
                        "b73ac5ed-81e3-4cdd-94e9-7e2925a9b388", 
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "pending-doc-1.xml"))),
                        "xml"
                    },
                    new Object[]{
                        "374d0eb2-06e7-497e-9eec-7934c6ba7a58", 
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "pending-doc-2.xml"))),
                        "xml"
                    },
                    new Object[]{
                        "74e00512-d3bf-4a6d-b4c8-f2ec3415fd93", 
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "faulty-pending-doc-1.xml"))),
                        "xml"
                    },
                    new Object[]{
                        "04d364de-775a-4d4f-b4b0-7bf7d6cde2ee", 
                        Files.readAllBytes(this.keystoreBaseDir.resolve(Path.of(THE_PENDING_DOCS_KEYSTORE_ID, "pending-doc-1.xml"))),
                        "xml"
                    }
            );
            
            String sql = "INSERT INTO document (metadata_id, content, doc_type, creation_time, modification_time)\n"
                    + "VALUES (\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    ?,\n"
                    + "    CURRENT_TIMESTAMP,\n"
                    + "    CURRENT_TIMESTAMP\n"
                     + ")";
            
            int[] affectedRows = this.jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    tracer.out().printfIndentln("i = %d", i);
                    Object[] row = batchArgs.get(i);
                    preparedStatement.setString(1, (String) row[0]);
                    byte[] content = (byte[]) row[1];
                    tracer.out().printfIndentln("content(%d) = %s", i, HexFormatter.format(content));
                    preparedStatement.setBlob(2, new SerialBlob(content));
                    preparedStatement.setString(3, (String) row[2]);
                }

                @Override
                public int getBatchSize() {
                    return batchArgs.size();
                }
            });

            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %d", rows);
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
