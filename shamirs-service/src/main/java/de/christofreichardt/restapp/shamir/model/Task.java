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
@Table(name = "task")
@NamedQueries({
    @NamedQuery(name = "Task.findAll", query = "SELECT t FROM Task t"),
    @NamedQuery(name = "Task.findById", query = "SELECT t FROM Task t WHERE t.id = :id"),
    @NamedQuery(name = "Task.findByProcessingState", query = "SELECT t FROM Task t WHERE t.processingState = :processingState"),
    @NamedQuery(name = "Task.findByEffectiveTime", query = "SELECT t FROM Task t WHERE t.effectiveTime = :effectiveTime")})
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;
    
    @Size(max = 20)
    @Column(name = "processing_state")
    private String processingState;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "effective_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date effectiveTime;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "taskId")
    private Collection<KeystoreActor> keystoreActorCollection;
    
    @JoinColumn(name = "keystore_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private DatabasedKeystore keystoreId;

    public Task() {
    }

    public Task(String id) {
        this.id = id;
    }

    public Task(String id, Date effectiveTime) {
        this.id = id;
        this.effectiveTime = effectiveTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessingState() {
        return processingState;
    }

    public void setProcessingState(String processingState) {
        this.processingState = processingState;
    }

    public Date getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public Collection<KeystoreActor> getKeystoreActorCollection() {
        return keystoreActorCollection;
    }

    public void setKeystoreActorCollection(Collection<KeystoreActor> keystoreActorCollection) {
        this.keystoreActorCollection = keystoreActorCollection;
    }

    public DatabasedKeystore getKeystoreId() {
        return keystoreId;
    }

    public void setKeystoreId(DatabasedKeystore keystoreId) {
        this.keystoreId = keystoreId;
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
        if (!(object instanceof Task)) {
            return false;
        }
        Task other = (Task) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.christofreichardt.restapp.shamir.model.v2.Task[ id=" + id + " ]";
    }
    
}
