--
-- cleanup
--
DROP TABLE IF EXISTS document;
DROP TABLE IF EXISTS csession;
DROP TABLE IF EXISTS slice;
DROP TABLE IF EXISTS participant;
DROP TABLE IF EXISTS keystore;

--
-- participant
--
CREATE TABLE participant (
    id CHAR(36) PRIMARY KEY,
    preferred_name VARCHAR(100) NOT NULL,
    effective_time DATETIME NOT NULL,
    UNIQUE KEY (preferred_name)
);

--
-- keystore
--
CREATE TABLE keystore (
    id CHAR(36) PRIMARY KEY,
    descriptive_name VARCHAR(100) NOT NULL,
    store BLOB,
    current_partition_id CHAR(36) NOT NULL,
    creation_time DATETIME NOT NULL,
    modification_time DATETIME NOT NULL
);

--
-- slice
--
CREATE TABLE slice (
    id CHAR(36) PRIMARY KEY,
    participant_id CHAR(36) NOT NULL,
    keystore_id CHAR(36) NOT NULL,
    partition_id CHAR(36) NOT NULL,
    share BLOB,
    processing_state VARCHAR(20),
    effective_time DATETIME NOT NULL
);
ALTER TABLE slice ADD CONSTRAINT fk_participant FOREIGN KEY (participant_id) REFERENCES participant(id);
ALTER TABLE slice ADD CONSTRAINT fk_slice_keystore FOREIGN KEY (keystore_id) REFERENCES keystore(id);

--
-- session
--
CREATE TABLE csession (
    id CHAR(36) PRIMARY KEY,
    keystore_id CHAR(36) NOT NULL,
    phase VARCHAR(20) NOT NULL,
    idle_time INTEGER UNSIGNED,
    creation_time DATETIME NOT NULL,
    modification_time DATETIME NOT NULL,
    expiration_time DATETIME
);
ALTER TABLE csession ADD CONSTRAINT fk_session_keystore FOREIGN KEY (keystore_id) REFERENCES keystore(id);

--
-- document
--
CREATE TABLE document (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    content BLOB,
    effective_time DATETIME NOT NULL
);
ALTER TABLE document ADD CONSTRAINT fk_document_session FOREIGN KEY (session_id) REFERENCES csession(id);
