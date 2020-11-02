--
-- cleanup
--
DROP TABLE IF EXISTS document;
DROP TABLE IF EXISTS keystore_actor;
DROP TABLE IF EXISTS task;
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
    effective_time DATETIME NOT NULL,
    UNIQUE KEY (descriptive_name)
);

--
-- slice
--
CREATE TABLE slice (
    id CHAR(36) PRIMARY KEY,
    participant_id CHAR(36) NOT NULL,
    keystore_id CHAR(36) NOT NULL,
    share BLOB,
    processing_state VARCHAR(20),
    effective_time DATETIME NOT NULL
);
ALTER TABLE slice ADD CONSTRAINT fk_participant FOREIGN KEY (participant_id) REFERENCES participant(id);
ALTER TABLE slice ADD CONSTRAINT fk_slice_keystore FOREIGN KEY (keystore_id) REFERENCES keystore(id);

--
-- task
--
CREATE TABLE task (
    id CHAR(36) PRIMARY KEY,
    keystore_id CHAR(36) NOT NULL,
    processing_state VARCHAR(20),
    effective_time DATETIME NOT NULL
);
ALTER TABLE task ADD CONSTRAINT fk_task_keystore FOREIGN KEY (keystore_id) REFERENCES keystore(id);

--
-- keystore_actor
--
CREATE TABLE keystore_actor (
    id CHAR(36) PRIMARY KEY,
    task_id CHAR(36) NOT NULL,
    entry_alias VARCHAR(50) NOT NULL,
    actor_type VARCHAR(20) NOT NULL,
    effective_time DATETIME NOT NULL
);
ALTER TABLE keystore_actor ADD CONSTRAINT fk_actor_task FOREIGN KEY (task_id) REFERENCES task(id);

--
-- document
--
CREATE TABLE document (
    id CHAR(36) PRIMARY KEY,
    actor_id CHAR(36) NOT NULL,
    content BLOB,
    effective_time DATETIME NOT NULL
);
ALTER TABLE document ADD CONSTRAINT fk_document_actor FOREIGN KEY (actor_id) REFERENCES keystore_actor(id);
