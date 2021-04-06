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
import java.util.Objects;
import java.util.UUID;
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
    
    public enum Status {PENDING, PROCESSED};
    public enum Action {SIGN, VERIFY, EMCRYPT, DECRYPT}; // Move this to shamirs-common

    private static final long serialVersionUID = 1L;

    @Id
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 36)
    @Column(name = "id")
    private String id;

    @Size(max = 20)
    @NotNull
    @Column(name = "status")
    private String state;

    @Size(max = 20)
    @NotNull
    @Column(name = "intended_action")
    private String action;

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
        this.id = UUID.randomUUID().toString();
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

    public Status getState() {
        return Enum.valueOf(Status.class, this.state);
    }

    public void setState(Status state) {
        this.state = state.name();
    }

    public Action getAction() {
        return Enum.valueOf(Action.class, this.action);
    }

    public void setAction(Action action) {
        this.action = action.name();
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
        return String.format("Metadata[id=%s, state=%s, action=%s, alias=%s, creationTime=%s, modificationTime=%s]",
                this.id, this.state, this.action, this.alias,
                this.creationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)),
                this.modificationTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US))
        );
    }

}
