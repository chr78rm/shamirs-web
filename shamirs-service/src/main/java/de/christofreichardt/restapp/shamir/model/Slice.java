/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import de.christofreichardt.restapp.shamir.common.SliceProcessingState;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "slice")
@NamedQueries({
    @NamedQuery(name = "Slice.findAll", query = "SELECT s FROM Slice s"),
    @NamedQuery(name = "Slice.findById", query = "SELECT s FROM Slice s WHERE s.id = :id"),
    @NamedQuery(name = "Slice.findByProcessingState", query = "SELECT s FROM Slice s WHERE s.processingState = :processingState"),
    @NamedQuery(name = "Slice.findByCreationTime", query = "SELECT s FROM Slice s WHERE s.creationTime = :creationTime"),
    @NamedQuery(name = "Slice.findByKeystoreId", query = "SELECT s FROM Slice s WHERE s.keystore.id = :keystoreId"),
    @NamedQuery(name = "Slice.findByParticipantId", query = "SELECT s FROM Slice s WHERE s.participant.id = :participantId"),
    @NamedQuery(name = "Slice.findByKeystoreIdAndParticipantId", query = "SELECT s FROM Slice s WHERE s.keystore.id = :keystoreId AND s.participant.id = :participantId"),})
public class Slice implements Serializable, Comparable<Slice> {

    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;

    @Lob
    @Column(name = "share")
    private byte[] share;

    @Size(max = 20)
    @Column(name = "processing_state")
    private String processingState;

    @Basic(optional = false)
    @NotNull
    @Column(name = "creation_time")
    private LocalDateTime creationTime;

    @Basic(optional = false)
    @NotNull
    @Column(name = "modification_time")
    private LocalDateTime modificationTime;

    @Basic(optional = false)
    @NotNull
    @Column(name = "partition_id")
    private String partitionId;

    @Basic(optional = false)
    @NotNull
    @Column(name = "amount")
    private int size;

    @JoinColumn(name = "keystore_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private DatabasedKeystore keystore;

    @JoinColumn(name = "participant_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Participant participant;

    public Slice() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = LocalDateTime.now();
        this.modificationTime = LocalDateTime.now();
        this.processingState = SliceProcessingState.NEW.name();
    }

    public Slice(String partitionId, int size, JsonValue share) {
        this();
        this.partitionId = partitionId;
        this.size = size;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try ( JsonWriter jsonWriter = Json.createWriter(byteArrayOutputStream)) {
            jsonWriter.write(share);
        }
        this.share = byteArrayOutputStream.toByteArray();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getShare() {
        return share;
    }

    public Optional<JsonObject> sharePoints() {
        if (Objects.nonNull(this.share)) {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(this.share);
            try ( JsonReader jsonReader = Json.createReader(inputStream)) {
                return Optional.of(jsonReader.read().asJsonObject());
            }
        } else {
            return Optional.empty();
        }
    }

    public void setShare(byte[] share) {
        this.share = share;
    }

    public void setShare(JsonValue share) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try ( JsonWriter jsonWriter = Json.createWriter(byteArrayOutputStream)) {
            jsonWriter.write(share);
        }
        setShare(byteArrayOutputStream.toByteArray());
    }

    private SliceProcessingState getProcessingState() {
        return Enum.valueOf(SliceProcessingState.class, this.processingState);
    }

    public boolean isNew() {
        return this.getProcessingState() == SliceProcessingState.NEW;
    }

    public boolean isCreated() {
        return this.getProcessingState() == SliceProcessingState.CREATED;
    }

    public boolean isFetched() {
        return this.getProcessingState() == SliceProcessingState.FETCHED;
    }

    public boolean isPosted() {
        return this.getProcessingState() == SliceProcessingState.POSTED;
    }

    public boolean isExpired() {
        return this.getProcessingState() == SliceProcessingState.EXPIRED;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(LocalDateTime modificationTime) {
        this.modificationTime = modificationTime;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void createdFor(DatabasedKeystore keystore, Participant participant) {
        if (this.isNew()) {
            this.processingState = SliceProcessingState.CREATED.name();
            this.keystore = keystore;
            this.participant = participant;
            modified();
        } else {
            throw new IllegalSliceProcessingStateException(
                    String.format("Not allowed: %s => %s.", this.getProcessingState().name(), SliceProcessingState.CREATED.name())
            );
        }
    }

    public void fetched() {
        if (this.isCreated() || this.isPosted()) {
            this.processingState = SliceProcessingState.FETCHED.name();
            this.share = null;
            modified();
        } else {
            throw new IllegalSliceProcessingStateException(
                    String.format("Not allowed: %s => %s.", this.getProcessingState().name(), SliceProcessingState.FETCHED.name())
            );
        }
    }

    public void posted(JsonObject share) {
        if (this.isFetched()) {
            this.processingState = SliceProcessingState.POSTED.name();
            setShare(share);
            modified();
        } else {
            throw new IllegalSliceProcessingStateException(
                    String.format("Not allowed: %s => %s.", this.getProcessingState().name(), SliceProcessingState.POSTED.name())
            );
        }
    }

    public void expired() {
        if (this.isCreated() || this.isPosted() || isFetched()) {
            this.processingState = SliceProcessingState.EXPIRED.name();
            modified();
        } else {
            throw new IllegalSliceProcessingStateException(
                    String.format("Not allowed: %s => %s.", this.getProcessingState().name(), SliceProcessingState.EXPIRED.name())
            );
        }
    }

    public void modified() {
        this.modificationTime = LocalDateTime.now();
    }

    @Override
    public int compareTo(Slice slice) {
        if (this.size < slice.size) {
            return -1;
        } else if (this.size > slice.size) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Slice)) {
            return false;
        }
        Slice other = (Slice) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("Slice[id=%s, state=%s, keystore=%s, participant=%s, partitionId=%s, size=%d, creationTime=%s, modificationTime=%s]",
                this.id, this.processingState, this.keystore.getDescriptiveName(), this.participant.getPreferredName(), this.partitionId, this.size,
                this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.modificationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US))
        );
    }

    public JsonObject toJson() {
        return toJson(false);
    }

    public JsonObject toJson(boolean inFull) {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("id", this.id)
                .add("partitionId", this.partitionId)
                .add("state", this.processingState)
                .add("size", this.size);
        if (inFull) {
            JsonObject sharePoints = sharePoints().orElse(JsonObject.EMPTY_JSON_OBJECT);
            jsonObjectBuilder.add("share", sharePoints);
        }
        JsonArrayBuilder selfLinkTypesBuilder = Json.createArrayBuilder();
        selfLinkTypesBuilder.add("GET");
        if (this.isCreated() || this.isPosted() || this.isFetched()) {
            selfLinkTypesBuilder.add("PATCH");
        }
        JsonArrayBuilder linksBuilder = Json.createArrayBuilder();
        linksBuilder
                .add(Json.createObjectBuilder()
                        .add("rel", "self")
                        .add("href", String.format("/slices/%s", this.id))
                        .add("type", selfLinkTypesBuilder)
                );
        if (inFull) {
            linksBuilder
                    .add(Json.createObjectBuilder()
                            .add("rel", "participant")
                            .add("href", String.format("/participant/%s", this.participant.getId()))
                            .add("type", JsonValue.EMPTY_JSON_ARRAY) // TODO: fill in the current types
                    ).add(Json.createObjectBuilder()
                            .add("rel", "keystore")
                            .add("href", String.format("/keystore/%s", this.keystore.getId()))
                            .add("type", this.keystore.selfLinkTypes())
                    );
        }
        JsonObject slice = jsonObjectBuilder
                .add("creationTime", this.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("modificationTime", this.modificationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("links", linksBuilder)
                .build();

        return slice;
    }
}
