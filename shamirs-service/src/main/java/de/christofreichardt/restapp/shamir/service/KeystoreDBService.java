/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.model.Slice;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Developer
 */
@Service
public class KeystoreDBService implements KeystoreService, Traceable {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    KeystoreRepository keystoreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DatabasedKeystore> findAll() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<DatabasedKeystore>", this, "findAll()");
        try {
            return this.entityManager
                    .createNamedQuery("DatabasedKeystore.findAll", DatabasedKeystore.class)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DatabasedKeystore> findbyId(String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findbyId(String id)");
        try {
            tracer.out().printfIndentln("id = %s", id);

            return this.keystoreRepository.findById(id);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatabasedKeystore findByIdWithPostedSlices(String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findByIdWithPostedSlices(String id)");
        try {
            tracer.out().printfIndentln("id = %s", id);

            return this.entityManager
                    .createNamedQuery("DatabasedKeystore.findByIdWithPostedSlices", DatabasedKeystore.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatabasedKeystore findByIdWithCertainSlices(String id, String state) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findByIdWithCertainSlices(String id, String state)");
        try {
            tracer.out().printfIndentln("id = %s", id);
            tracer.out().printfIndentln("state = %s", state);

            return this.entityManager
                    .createNamedQuery("DatabasedKeystore.findByIdWithCertainSlices", DatabasedKeystore.class)
                    .setParameter("id", id)
                    .setParameter("state", state)
                    .getSingleResult();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatabasedKeystore findByIdWithActiveSlices(String id) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findByIdWithActiveSlices(String id)");
        try {
            tracer.out().printfIndentln("id = %s", id);

            return this.entityManager.createQuery( // TODO: think about the current_partition_id
                    "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :id AND "
                    + "s.processingState = '" + Slice.ProcessingState.POSTED.name()
                    + "' OR s.processingState = '" + Slice.ProcessingState.CREATED.name() + "'",
                    DatabasedKeystore.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatabasedKeystore findByIdWithCurrentSlicesAndValidSession(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findByIdWithCurrentSlicesAndValidSession(String keystoreId)");
        try {
            tracer.out().printfIndentln("id = %s", keystoreId);

            DatabasedKeystore keystore = this.entityManager
                    .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :keystoreId AND k.currentPartitionId = s.partitionId", DatabasedKeystore.class)
                    .setParameter("keystoreId", keystoreId)
                    .getSingleResult();

            keystore = this.entityManager.createQuery(
                    "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.sessions s "
                    + "WHERE k = :keystore "
                    + "AND s.phase != '" + Session.Phase.CLOSED.name() + "'",
                    DatabasedKeystore.class)
                    .setParameter("keystore", keystore)
                    .getSingleResult();

            return keystore;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatabasedKeystore findByIdAndParticipantWithPostedSlices(String id, String participantId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findByIdWithPostedSlices(String id)");
        try {
            tracer.out().printfIndentln("id = %s", id);
            tracer.out().printfIndentln("participantId = %s", participantId);

            return this.entityManager
                    .createNamedQuery("DatabasedKeystore.findByIdAndParticipantWithPostedSlices", DatabasedKeystore.class)
                    .setParameter("id", id)
                    .setParameter("participantId", participantId)
                    .getSingleResult();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional
    public DatabasedKeystore persist(DatabasedKeystore keystore) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "persist(DatabasedKeystore keystore)");
        try {
            tracer.out().printfIndentln("keystore = %s", keystore);

            return this.keystoreRepository.save(keystore);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DatabasedKeystore findByDescriptiveName(String descriptiveName) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("DatabasedKeystore", this, "findByIdWithPostedSlices(String id)");
        try {
            tracer.out().printfIndentln("id = %s", descriptiveName);

            DatabasedKeystore databasedKeystore;
            List<DatabasedKeystore> resultList = this.entityManager
                    .createNamedQuery("DatabasedKeystore.findByDescriptiveName", DatabasedKeystore.class)
                    .setParameter("descriptiveName", descriptiveName)
                    .getResultList();
            if (resultList.isEmpty()) {
                databasedKeystore = null;
            } else if (resultList.size() == 1) {
                databasedKeystore = resultList.get(0);
            } else {
                throw new NonUniqueResultException();
            }

            return databasedKeystore;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DatabasedKeystore> findByIdWithActiveSlicesAndCurrentSession(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Optional<DatabasedKeystore>", this, "findByIdWithActiveSlicesAndCurrentSession(String keystoreId)");
        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);

            Optional<DatabasedKeystore> optional;
            try {
                DatabasedKeystore keystore = this.entityManager
                        .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.id = :keystoreId AND k.currentPartitionId = s.partitionId", DatabasedKeystore.class)
                        .setParameter("keystoreId", keystoreId)
                        .getSingleResult();
                
                keystore = this.entityManager.createQuery(
                        "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.sessions s "
                        + "WHERE k = :keystore "
                        + "AND s.phase != '" + Session.Phase.CLOSED.name() + "'",
                        DatabasedKeystore.class)
                        .setParameter("keystore", keystore)
                        .getSingleResult();
                
                optional = Optional.of(keystore);
            } catch (NoResultException ex) {
                optional = Optional.empty();
            }
            
            return optional;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatabasedKeystore> findKeystoresWithCurrentSlicesAndIdleSessions() {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.entry("List<DatabasedKeystore>", this, "findKeystoresWithCurrentSlicesAndIdleSessions()");

        try {
            List<DatabasedKeystore> keystores = this.entityManager
                    .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s WHERE k.currentPartitionId = s.partitionId", DatabasedKeystore.class)
                    .getResultList();
            LocalDateTime currentTime = LocalDateTime.now();
            keystores =  this.entityManager
                    .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.sessions s WHERE k IN :keystores AND s.phase = 'ACTIVE' AND s.expirationTime < :currentTime", DatabasedKeystore.class)
                    .setParameter("keystores", keystores)
                    .setParameter("currentTime", currentTime)
                    .getResultList();
            
            return keystores;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional
    public void rollOver() {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.entry("void", this, "rollOver()");

        try {
            List<DatabasedKeystore> keystores = findKeystoresWithCurrentSlicesAndIdleSessions();
            keystores.forEach(keystore -> {
                tracer.out().printfIndentln("keystore = %s", keystore);
                keystore.getSessions().forEach(session -> tracer.out().printfIndentln("session = %s", session));
                keystore.getSlices().forEach(slice -> tracer.out().printfIndentln("slice = %s", slice));
                tracer.out().printfIndentln("(*---*)");
            });
            
            String nextPartitionId = UUID.randomUUID().toString();
            keystores.forEach(keystore -> {
                Set<Slice> nextSlices = keystore.getSlices().stream()
                        .filter(slice -> Objects.equals(slice.getPartitionId(), keystore.getCurrentPartitionId()))
                        .map(slice -> {
                            slice.setProcessingState(Slice.ProcessingState.EXPIRED.name());
                            Slice nextSlice = new Slice();
                            nextSlice.setKeystore(keystore);
                            nextSlice.setParticipant(slice.getParticipant());
                            nextSlice.setPartitionId(nextPartitionId);
                            nextSlice.setProcessingState(Slice.ProcessingState.CREATED.name());
                            nextSlice.setShare(null);
                            return nextSlice;
                        })
                        .collect(Collectors.toSet());
                keystore.setSlices(nextSlices);
                keystore.setCurrentPartitionId(nextPartitionId);
                keystore.getSessions().stream()
                        .filter(session -> Objects.equals(Session.Phase.ACTIVE.name(), session.getPhase()))
                        .forEach(session -> session.setPhase(Session.Phase.CLOSED.name()));
                Session session = new Session();
                session.setPhase(Session.Phase.PROVISIONED.name());
                session.setKeystore(keystore);
                keystore.getSessions().add(session);
                this.entityManager.merge(keystore);
            });
            
        } finally {
            tracer.wayout();
        }
    }
    
    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
