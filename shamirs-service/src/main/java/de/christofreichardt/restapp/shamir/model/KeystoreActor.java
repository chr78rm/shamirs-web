/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "keystore_actor")
@NamedQueries({
    @NamedQuery(name = "KeystoreActor.findAll", query = "SELECT k FROM KeystoreActor k"),
    @NamedQuery(name = "KeystoreActor.findById", query = "SELECT k FROM KeystoreActor k WHERE k.id = :id"),
    @NamedQuery(name = "KeystoreActor.findByEntryAlias", query = "SELECT k FROM KeystoreActor k WHERE k.entryAlias = :entryAlias"),
    @NamedQuery(name = "KeystoreActor.findByActorType", query = "SELECT k FROM KeystoreActor k WHERE k.actorType = :actorType"),
    @NamedQuery(name = "KeystoreActor.findByEffectiveTime", query = "SELECT k FROM KeystoreActor k WHERE k.effectiveTime = :effectiveTime")})
public class KeystoreActor implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "entry_alias")
    private String entryAlias;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "actor_type")
    private String actorType;
    @Basic(optional = false)
    @NotNull
    @Column(name = "effective_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date effectiveTime;
    @JoinColumn(name = "task_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Task taskId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "actorId")
    private Collection<Document> documentCollection;

    public KeystoreActor() {
    }

    public KeystoreActor(String id) {
        this.id = id;
    }

    public KeystoreActor(String id, String entryAlias, String actorType, Date effectiveTime) {
        this.id = id;
        this.entryAlias = entryAlias;
        this.actorType = actorType;
        this.effectiveTime = effectiveTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEntryAlias() {
        return entryAlias;
    }

    public void setEntryAlias(String entryAlias) {
        this.entryAlias = entryAlias;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public Date getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Task getTaskId() {
        return taskId;
    }

    public void setTaskId(Task taskId) {
        this.taskId = taskId;
    }

    public Collection<Document> getDocumentCollection() {
        return documentCollection;
    }

    public void setDocumentCollection(Collection<Document> documentCollection) {
        this.documentCollection = documentCollection;
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
        if (!(object instanceof KeystoreActor)) {
            return false;
        }
        KeystoreActor other = (KeystoreActor) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.christofreichardt.restapp.shamir.model.v2.KeystoreActor[ id=" + id + " ]";
    }
    
}
