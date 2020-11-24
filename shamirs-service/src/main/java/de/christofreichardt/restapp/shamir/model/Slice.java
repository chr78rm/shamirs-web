/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
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
    @NamedQuery(name = "Slice.findByEffectiveTime", query = "SELECT s FROM Slice s WHERE s.effectiveTime = :effectiveTime")})
public class Slice implements Serializable {

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
    @Column(name = "effective_time")
    private LocalDateTime effectiveTime;
    
    @Basic(optional = false)
    @NotNull
    @Column(name = "partition_id")
    private String partitionId;
    
    @JoinColumn(name = "keystore_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private DatabasedKeystore keystore;
    
    @JoinColumn(name = "participant_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private Participant participant;

    public Slice() {
        this.id = UUID.randomUUID().toString();
        this.effectiveTime = LocalDateTime.now();
    }

    public Slice(String id) {
        this.id = id;
    }

    public Slice(String id, LocalDateTime effectiveTime) {
        this.id = id;
        this.effectiveTime = effectiveTime;
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

    public void setShare(byte[] share) {
        this.share = share;
    }

    public String getProcessingState() {
        return processingState;
    }

    public void setProcessingState(String processingState) {
        this.processingState = processingState;
    }

    public LocalDateTime getEffectiveTime() {
        return effectiveTime;
    }

    public void setEffectiveTime(LocalDateTime effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

    public DatabasedKeystore getKeystore() {
        return keystore;
    }

    public void setKeystore(DatabasedKeystore keystore) {
        this.keystore = keystore;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
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
        return String.format("Slice[id=%s, state=%s, keystore=%s, participant=%s, effectiveTime=%s]", 
                this.id, this.processingState, this.keystore.getDescriptiveName(), this.participant.getPreferredName(), 
                this.effectiveTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
    }
    
}
