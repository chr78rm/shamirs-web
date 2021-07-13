/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.Slice;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author Developer
 */
@Service
public class SliceDBService implements SliceService, Traceable {

    @PersistenceContext
    EntityManager entityManager;

    @Override
    public List<Slice> findAll() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Slice>", this, "findAll()");
        try {
            return this.entityManager.createNamedQuery("Slice.findAll", Slice.class)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public List<Slice> findByKeystoreId(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Slice>", this, "findByKeystoreId(String keystoreId)");
        try {
            return this.entityManager.createNamedQuery("Slice.findByKeystoreId", Slice.class)
                    .setParameter("keystoreId", keystoreId)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public List<Slice> findByParticipantId(String participantId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Slice>", this, "findByParticipantId(String participantId)");
        try {
            return this.entityManager.createNamedQuery("Slice.findByParticipantId", Slice.class)
                    .setParameter("participantId", participantId)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public List<Slice> findByKeystoreIdAndParticipantId(String keystoreId, String participantId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Slice>", this, "findByParticipantId(String participantId)");
        try {
            return this.entityManager.createNamedQuery("Slice.findByParticipantId", Slice.class)
                    .setParameter("keystoreId", keystoreId)
                    .setParameter("participantId", participantId)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
    
}
