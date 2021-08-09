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
    @NamedQuery(name = "Participant.findByEffectiveTime", query = "SELECT p FROM Participant p WHERE p.creationTime = :creationTime"),
    @NamedQuery(name = "Participant.findByKeystore", query = "SELECT p FROM Participant p JOIN p.slices s WHERE s.keystore.id = :keystoreId"),
})
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
    @Column(name = "creation_time")
    private LocalDateTime creationTime;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "participant")
    private Collection<Slice> slices;

    public Participant() {
        this.id = UUID.randomUUID().toString();
        this.creationTime = LocalDateTime.now();
    }

    public Participant(String preferredName) {
        this();
        this.preferredName = preferredName;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public String getId() {
        return id;
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
        return String.format("Participant[id=%s, preferredName=%s, creationTime=%s]",
                this.id, this.preferredName, this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
    }
    
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("id", this.id)
                .add("descriptiveName", this.preferredName)
                .add("creationTime", this.creationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
