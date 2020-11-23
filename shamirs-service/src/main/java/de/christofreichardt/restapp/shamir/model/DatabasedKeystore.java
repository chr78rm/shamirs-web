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
import java.util.Locale;
import java.util.UUID;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "keystore")
@NamedQueries({
            @NamedQuery(name = "DatabasedKeystore.findAll", query = "SELECT k FROM DatabasedKeystore k"),
            @NamedQuery(name = "DatabasedKeystore.findById", query = "SELECT k FROM DatabasedKeystore k WHERE k.id = :id"),
            @NamedQuery(name = "DatabasedKeystore.findByDescriptiveName", query = "SELECT k FROM DatabasedKeystore k WHERE k.descriptiveName = :descriptiveName"),
            @NamedQuery(name = "DatabasedKeystore.findByIdWithPostedSlices",
                    query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED'"),
            @NamedQuery(name = "DatabasedKeystore.findByIdAndParticipantWithPostedSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED' AND s.participant.id = :participantId"),
    })
public class DatabasedKeystore implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "descriptive_name")
    private String descriptiveName;

    @Lob
    @Column(name = "store")
    private byte[] store;

    @Basic(optional = false)
    @NotNull
    @Column(name = "creation_time")
    private LocalDateTime creationTime;

    @Basic(optional = false)
    @NotNull
    @Column(name = "modification_time")
    private LocalDateTime mofificationTime;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystore")
    private Collection<Slice> slices;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystore")
    private Collection<Session> sessions;

    public DatabasedKeystore() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = LocalDateTime.now();
        this.mofificationTime = LocalDateTime.now();
    }

    public DatabasedKeystore(String id) {
        this.id = id;
    }

    public DatabasedKeystore(String id, LocalDateTime creationTime) {
        this.id = id;
        this.creationTime = creationTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescriptiveName() {
        return descriptiveName;
    }

    public void setDescriptiveName(String descriptiveName) {
        this.descriptiveName = descriptiveName;
    }

    public byte[] getStore() {
        return store;
    }

    public void setStore(byte[] store) {
        this.store = store;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(LocalDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public LocalDateTime getMofificationTime() {
        return mofificationTime;
    }

    public void setMofificationTime(LocalDateTime mofificationTime) {
        this.mofificationTime = mofificationTime;
    }

    public Collection<Slice> getSlices() {
        return slices;
    }

    public void setSlices(Collection<Slice> slices) {
        this.slices = slices;
    }

    public Collection<Session> getSessions() {
        return sessions;
    }

    public void setSessions(Collection<Session> sessions) {
        this.sessions = sessions;
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
        if (!(object instanceof DatabasedKeystore)) {
            return false;
        }
        DatabasedKeystore other = (DatabasedKeystore) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("DatabasedKeystore[id=%s, descriptiveName=%s, creationTime=%s, mofificationTime=%s]",
                this.id, this.descriptiveName,
                this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.mofificationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("id", this.id)
                .add("descriptiveName", this.descriptiveName)
                .add("creationTime", this.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("mofificationTime", this.mofificationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .add("links", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("rel", "self")
                                .add("href", String.format("/keystores/%s", this.id))
                                .add("type", "GET")
                        )
                )
                .build();
    }

}
