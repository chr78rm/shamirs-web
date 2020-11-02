/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 *
 * @author Developer
 */
@Entity
@Table(name = "document")
@NamedQueries({
    @NamedQuery(name = "Document.findAll", query = "SELECT d FROM Document d"),
    @NamedQuery(name = "Document.findById", query = "SELECT d FROM Document d WHERE d.id = :id"),
    @NamedQuery(name = "Document.findByEffectiveTime", query = "SELECT d FROM Document d WHERE d.effectiveTime = :effectiveTime")})
public class Document implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;
    @Lob
    @Column(name = "content")
    private byte[] content;
    @Basic(optional = false)
    @NotNull
    @Column(name = "effective_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date effectiveTime;
    @JoinColumn(name = "actor_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private KeystoreActor actorId;

    public Document() {
    }

    public Document(String id) {
        this.id = id;
    }

    public Document(String id, Date effectiveTime) {
        this.id = id;
        this.effectiveTime = effectiveTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public Date getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(Date effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public KeystoreActor getActorId() {
        return actorId;
    }

    public void setActorId(KeystoreActor actorId) {
        this.actorId = actorId;
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
        if (!(object instanceof Document)) {
            return false;
        }
        Document other = (Document) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "de.christofreichardt.restapp.shamir.model.v2.Document[ id=" + id + " ]";
    }
    
}
