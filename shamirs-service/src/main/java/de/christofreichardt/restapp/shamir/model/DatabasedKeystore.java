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
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "keystore", uniqueConstraints = @UniqueConstraint(columnNames = {"descriptive_name"}))
@NamedQueries({
    @NamedQuery(name = "DatabasedKeystore.findAll", query = "SELECT k FROM DatabasedKeystore k"),
    @NamedQuery(name = "DatabasedKeystore.findById", query = "SELECT k FROM DatabasedKeystore k WHERE k.id = :id"),
    @NamedQuery(name = "DatabasedKeystore.findByIdWithPostedSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED'"),
    @NamedQuery(name = "DatabasedKeystore.findByIdAndParticipantWithPostedSlices",
            query = "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND s.processingState = 'POSTED' AND s.participant.id = :participantId"),
    @NamedQuery(name = "DatabasedKeystore.findByEffectiveTime", query = "SELECT k FROM DatabasedKeystore k WHERE k.effectiveTime = :effectiveTime")})
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
    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystore")
    private Collection<Slice> slices;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "keystore")
    private Collection<Session> sessions;

    public DatabasedKeystore() {
        this.id = UUID.randomUUID().toString();
        this.effectiveTime = LocalDateTime.now();
    }

    public DatabasedKeystore(String id) {
        this.id = id;
    }

    public DatabasedKeystore(String id, LocalDateTime effectiveTime) {
        this.id = id;
        this.effectiveTime = effectiveTime;
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

    public LocalDateTime getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(LocalDateTime effectiveTime) {
        this.effectiveTime = effectiveTime;
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
        return String.format("DatabasedKeystore[id=%s, descriptiveName=%s, effectiveTime=%s]",
                this.id, this.descriptiveName, this.effectiveTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
    }

    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("id", this.id)
                .add("descriptiveName", this.descriptiveName)
                .add("effectiveTime", this.effectiveTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

}
