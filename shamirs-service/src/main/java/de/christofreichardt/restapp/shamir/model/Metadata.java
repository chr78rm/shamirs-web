/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import de.christofreichardt.restapp.shamir.common.MetadataAction;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "metadata")
public class Metadata implements Serializable {

    public enum Status {
        PENDING, PROCESSED, ERROR
    };

    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;

    @Size(max = 100)
    @NotNull
    @Column(name = "title")
    private String title;

    @Size(max = 20)
    @NotNull
    @Column(name = "status")
    private String state;

    @Size(max = 20)
    @NotNull
    @Column(name = "intended_action")
    private String action;

    @Size(max = 1)
    @Column(name = "validated")
    private String validated;

    @Size(max = 100)
    @NotNull
    @Column(name = "media_type")
    private String mediaType;

    @Size(max = 50)
    @NotNull
    @Column(name = "key_alias")
    private String alias;

    @Basic(optional = false)
    @NotNull
    @Column(name = "creation_time")
    private LocalDateTime creationTime;

    @Basic(optional = false)
    @NotNull
    @Column(name = "modification_time")
    private LocalDateTime modificationTime;

    @JoinColumn(name = "session_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Session session;

    @OneToOne(mappedBy = "metadata", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = false)
    private Document document;

    public Metadata() {
        this(null);
    }

    public Metadata(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.creationTime = LocalDateTime.now();
        this.modificationTime = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Status getState() {
        return Enum.valueOf(Status.class, this.state);
    }

    public void setState(Status state) {
        this.state = state.name();
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public MetadataAction getAction() {
        return Enum.valueOf(MetadataAction.class, this.action);
    }

    public void setAction(MetadataAction action) {
        this.action = action.name();
    }

    public boolean isValidated() {
        return Objects.equals("y", this.validated);
    }

    public void setValidated(boolean validated) {
        if (validated) {
            this.validated = "y";
        } else {
            this.validated = "n";
        }
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
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

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Metadata other = (Metadata) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return String.format("Metadata[id=%s, title=%s, state=%s, action=%s, alias=%s, validated=%b, mediaType=%s, creationTime=%s, modificationTime=%s]",
                this.id, this.title, this.state, this.action, this.alias, this.isValidated(), this.mediaType,
                this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.modificationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US))
        );
    }

    public JsonObject toJson() {
        return toJson(false);
    }

    public JsonObject toJson(boolean inFull) {
        JsonArrayBuilder selfTypeBuilder = Json.createArrayBuilder()
                .add("GET");
        JsonArray selfTypes = selfTypeBuilder.build();
        JsonArrayBuilder linksBuilder = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("rel", "self")
                        .add("href", String.format("/sessions/%s/metadata/%s", this.session.getId(), this.id))
                        .add("type", selfTypes)
                );
        if (inFull) {
            JsonArrayBuilder sessionTypeBuilder = Json.createArrayBuilder()
                    .add("GET");
            if (this.session.getPhase() != SessionPhase.CLOSED) {
                sessionTypeBuilder.add("PUT");
            }
            JsonArrayBuilder documentTypesBuilder = Json.createArrayBuilder()
                    .add("GET");
            if (this.session.getPhase() == SessionPhase.PROVISIONED) {
                documentTypesBuilder.add("PUT");
            }
            linksBuilder
                    .add(Json.createObjectBuilder()
                            .add("rel", "content")
                            .add("href", String.format("/sessions/%s/documents/%s", this.session.getId(), this.id))
                            .add("type", documentTypesBuilder)
                    ).add(Json.createObjectBuilder()
                            .add("rel", "session")
                            .add("href", String.format("/keystores/%s/sessions/%s", this.session.getKeystore().getId(), this.session.getId()))
                            .add("type", sessionTypeBuilder)
                    );
        }
        JsonArray links = linksBuilder.build();
        JsonObjectBuilder representationBuilder = Json.createObjectBuilder()
                .add("id", this.id)
                .add("title", this.title)
                .add("state", this.state)
                .add("action", this.action);
        if (Objects.nonNull(this.validated)) {
            representationBuilder.add("validated", this.isValidated());
        }
        representationBuilder.add("alias", this.alias)
                .add("mediaType", this.mediaType)
                .add("creationTime", this.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("modificationTime", this.modificationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("links", links);
        return representationBuilder.build();
    }

}
