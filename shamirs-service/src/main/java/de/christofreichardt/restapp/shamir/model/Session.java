/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "csession")
@NamedQueries({
    @NamedQuery(name = "Session.findAll", query = "SELECT s FROM Session s"),
    @NamedQuery(name = "Session.findById", query = "SELECT s FROM Session s WHERE s.id = :id"),
    @NamedQuery(name = "Session.findByPhase", query = "SELECT s FROM Session s WHERE s.phase = :phase"),
    @NamedQuery(name = "Session.findByCreationTime", query = "SELECT s FROM Session s WHERE s.creationTime = :creationTime"),
    @NamedQuery(name = "Session.findAllByKeystore", query = "SELECT s FROM Session s WHERE s.keystore.id = :keystoreId"),
    @NamedQuery(name = "Session.findCurrentByKeystore", query = "SELECT s FROM Session s WHERE s.keystore.id = :keystoreId AND s.phase != 'CLOSED'"),
})
public class Session implements Serializable { 
    
    public enum Phase {PROVISIONED, ACTIVE, CLOSED}; // TODO: move this to shamirs-common

    private static final long serialVersionUID = 1L;
    
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;
    
    @Size(max = 20)
    @NotNull
    @Column(name = "phase")
    private String phase;
    
    @Basic
    @Column(name = "idle_time")
    private long idleTime;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "creation_time")
    private LocalDateTime creationTime;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "modification_time")
    private LocalDateTime modificationTime;
    
    @Basic
    @Column(name = "expiration_time")
    private LocalDateTime expirationTime;
    
    @Version
    @Basic(optional = false)
    @NotNull
    @Column(name = "version")
    private int version;
    
    @JoinColumn(name = "keystore_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private DatabasedKeystore keystore;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "session")
    private Collection<Metadata> metadatas = new HashSet<>();

    public Session() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = LocalDateTime.now();
        this.modificationTime = LocalDateTime.now();
    }

    public Session(String id) {
        this.id = id;
    }

    public Session(String id, LocalDateTime effectiveTime) {
        this.id = id;
        this.creationTime = effectiveTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Phase getPhase() {
        return Enum.valueOf(Phase.class, this.phase);
    }

    public void setPhase(Phase phase) {
        this.phase = phase.name();
    }

    public long getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(long idleTime) {
        this.idleTime = idleTime;
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

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public DatabasedKeystore getKeystore() {
        return keystore;
    }

    public void setKeystore(DatabasedKeystore keystore) {
        this.keystore = keystore;
    }

    public Collection<Metadata> getMetadatas() {
        return metadatas;
    }

    public void setMetadatas(Collection<Metadata> metadatas) {
        this.metadatas = metadatas;
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
        if (!(object instanceof Session)) {
            return false;
        }
        Session other = (Session) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("Session[id=%s, phase=%s, idleTime=%d, creationTime=%s, modificationTime=%s, expirationTime=%s]", this.id, this.phase, this.idleTime,
                this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.modificationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.expirationTime != null ? this.expirationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)) : "null"
        );
    }
    
    public void updateModificationTime() {
        this.modificationTime = LocalDateTime.now();
    }
    
    public JsonObject toJson() {
        return toJson(false);
    }
    
    public JsonObject toJson(boolean inFull) {
        JsonArrayBuilder selfTypeBuilder = Json.createArrayBuilder()
                .add("GET");
        if (!Objects.equals(this.phase, Phase.CLOSED.name())) {
            selfTypeBuilder.add("PUT");
        }
        JsonArray selfTypes = selfTypeBuilder.build();
        JsonArrayBuilder linksBuilder = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("rel", "self")
                        .add("href", String.format("/keystores/%s/sessions/%s", this.keystore.getId(), this.id))
                        .add("type", selfTypes)
                );
        if (inFull) {
            JsonArrayBuilder documentsTypeBuilder = Json.createArrayBuilder()
                    .add("GET");
            if (this.getPhase() != Phase.CLOSED) {
                documentsTypeBuilder.add("POST");
            }
            JsonArray documentsTypes = documentsTypeBuilder.build();
            linksBuilder
                    .add(Json.createObjectBuilder()
                            .add("rel", "documents")
                            .add("href", String.format("/sessions/%s/documents", this.id))
                            .add("type", documentsTypes)
                    );
        }
        JsonArray links = linksBuilder.build();
        return Json.createObjectBuilder()
                .add("id", this.id)
                .add("phase", this.phase)
                .add("idleTime", this.idleTime)
                .add("creationTime", this.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("modificationTime", this.modificationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("expirationTime", this.expirationTime != null ? this.expirationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "null")
                .add("links", links)
                .build();
    }
}
