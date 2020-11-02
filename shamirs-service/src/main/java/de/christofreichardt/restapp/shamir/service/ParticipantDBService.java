/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.Participant;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Developer
 */
@Service
public class ParticipantDBService implements ParticipantService, Traceable {
    
    @PersistenceContext
    EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Participant findByPreferredName(String preferredName) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Participant", this, "findByPreferredName(String preferredName)");
        try {
            tracer.out().printfIndentln("preferredName = %s", preferredName);
            
            return this.entityManager
                    .createNamedQuery("Participant.findByPreferredName", Participant.class)
                    .setParameter("preferredName", preferredName)
                    .getSingleResult();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
    
}
