--
-- cleanup
--
DROP TABLE IF EXISTS document;
DROP TABLE IF EXISTS metadata;
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
    shares INT NOT NULL,
    threshold INT NOT NULL,
    creation_time DATETIME NOT NULL,
    modification_time DATETIME NOT NULL,
    version INT NOT NULL
);

--
-- slice
--
CREATE TABLE slice (
    id CHAR(36) PRIMARY KEY,
    participant_id CHAR(36) NOT NULL,
    keystore_id CHAR(36) NOT NULL,
    partition_id CHAR(36) NOT NULL,
    amount INT NOT NULL,
    share BLOB,
    processing_state VARCHAR(20),
    modification_time DATETIME NOT NULL,
    creation_time DATETIME NOT NULL
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
    expiration_time DATETIME,
    version INT NOT NULL
);
ALTER TABLE csession ADD CONSTRAINT fk_session_keystore FOREIGN KEY (keystore_id) REFERENCES keystore(id);

--
-- metadata
--
CREATE TABLE metadata (
    id CHAR(36) PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    intended_action VARCHAR(20) NOT NULL,
    validated VARCHAR(1),
    media_type VARCHAR(100) NOT NULL,
    key_alias VARCHAR(50) NOT NULL,
    creation_time DATETIME NOT NULL,
    modification_time DATETIME NOT NULL
);
ALTER TABLE metadata ADD CONSTRAINT fk_metadata_session FOREIGN KEY (session_id) REFERENCES csession(id);

--
-- document
--
CREATE TABLE document (
    metadata_id CHAR(36) NOT NULL,
    content BLOB,
    creation_time DATETIME NOT NULL,
    modification_time DATETIME NOT NULL,
    doc_type VARCHAR(3) NOT NULL,
    UNIQUE KEY (metadata_id)
);
ALTER TABLE document ADD CONSTRAINT fk_document_metadata FOREIGN KEY (metadata_id) REFERENCES metadata(id);
