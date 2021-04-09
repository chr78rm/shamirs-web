/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.Metadata;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Developer
 */
@Service
public class MetadataDBService implements MetadataService, Traceable {

    @PersistenceContext
    EntityManager entityManager;
    
    @Autowired
    MetadataRepository metadataRepository;

    @Override
    public List<Metadata> findAllBySession(String sessionId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Metadata>", this, "findAllBySession(String sessionId)");

        try {
            tracer.out().printfIndentln("sessionId = %s", sessionId);
            
            return this.entityManager.createQuery(
                    "SELECT m FROM Metadata m WHERE m.session.id = :sessionId", 
                    Metadata.class)
                    .setParameter("sessionId", sessionId)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public Optional<Metadata> findById(String documentId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Optional<Metadata>", this, "findById(String documentId)");

        try {
            tracer.out().printfIndentln("documentId = %s", documentId);
            
            return this.metadataRepository.findById(documentId);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
    
}
