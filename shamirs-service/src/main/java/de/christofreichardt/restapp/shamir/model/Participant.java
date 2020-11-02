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
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
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
@Table(name = "participant", uniqueConstraints = @UniqueConstraint(columnNames = {"preferred_name"}))
@NamedQueries({
    @NamedQuery(name = "Participant.findAll", query = "SELECT p FROM Participant p"),
    @NamedQuery(name = "Participant.findById", query = "SELECT p FROM Participant p WHERE p.id = :id"),
    @NamedQuery(name = "Participant.findByPreferredName", query = "SELECT p FROM Participant p WHERE p.preferredName = :preferredName"),
    @NamedQuery(name = "Participant.findByEffectiveTime", query = "SELECT p FROM Participant p WHERE p.effectiveTime = :effectiveTime")})
public class Participant implements Serializable {

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
    @Column(name = "preferred_name")
    private String preferredName;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "participant")
    private Collection<Slice> sliceCollection;

    public Participant() {
    }

    public Participant(String id) {
        this.id = id;
    }

    public Participant(String id, String preferredName, LocalDateTime effectiveTime) {
        this.id = id;
        this.preferredName = preferredName;
        this.effectiveTime = effectiveTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public LocalDateTime getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(LocalDateTime effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Collection<Slice> getSliceCollection() {
        return sliceCollection;
    }

    public void setSliceCollection(Collection<Slice> sliceCollection) {
        this.sliceCollection = sliceCollection;
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
        if (!(object instanceof Participant)) {
            return false;
        }
        Participant other = (Participant) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("Participant[id=%s, preferredName=%s, effectiveTime=%s]",
                this.id, this.preferredName, this.effectiveTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
    }
    
}
