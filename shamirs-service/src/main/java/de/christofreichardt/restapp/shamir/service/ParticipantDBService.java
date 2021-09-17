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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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
    @Transactional(readOnly = true)
    public List<Participant> findByKeystore(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Participant>", this, "findByKeystore(String keystoreId)");
        try {
            return this.entityManager.createNamedQuery("Participant.findByKeystore", Participant.class)
                    .setParameter("keystoreId", keystoreId)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Participant> findByPreferredNames(Set<String> preferredNames) throws ParticipantNotFoundException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Participant>", this, "findByPreferredNames(Set<String> preferredNames)");
        try {
            tracer.out().printfIndentln("preferredNames = %s", preferredNames);

            Map<String, Participant> participants = preferredNames.stream()
                    .map((String preferredName) -> {
                        SimpleImmutableEntry<String, Participant> entry;
                        try {
                            entry = new SimpleImmutableEntry<>(preferredName, findByPreferredName(preferredName));
                        } catch (NoResultException ex) {
                            entry = new SimpleImmutableEntry<>(preferredName, null);
                        }
                        return entry;
                    })
                    .collect(HashMap::new, (map,entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
            Set<String> unknownNames = participants.entrySet().stream()
                    .filter(entry -> Objects.isNull(entry.getValue()))
                    .map(entry -> entry.getKey())
                    .collect(Collectors.toSet());
            if (!unknownNames.isEmpty()) {
                throw new ParticipantNotFoundException(String.format("Not found: %s", unknownNames));
            }
            
            return participants;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
