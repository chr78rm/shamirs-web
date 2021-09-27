/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.json.JsonArray;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Developer
 */
@Service
@Primary
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
    public Optional<DatabasedKeystore> findByIdWithActiveSlicesAndCurrentSession(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Optional<DatabasedKeystore>", this, "findByIdWithActiveSlicesAndCurrentSession(String keystoreId)");
        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);

            Optional<DatabasedKeystore> optional;
            try {
                DatabasedKeystore keystore = this.entityManager
                        .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s LEFT JOIN FETCH s.participant WHERE k.id = :keystoreId AND k.currentPartitionId = s.partitionId", DatabasedKeystore.class)
                        .setParameter("keystoreId", keystoreId)
                        .getSingleResult();
                
                keystore = this.entityManager.createQuery(
                        "SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.sessions s "
                        + "WHERE k = :keystore "
                        + "AND s.phase != '" + SessionPhase.CLOSED.name() + "'",
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
                    .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.slices s LEFT JOIN FETCH s.participant WHERE k.currentPartitionId = s.partitionId", DatabasedKeystore.class)
                    .getResultList();
            LocalDateTime currentTime = LocalDateTime.now();
            
            tracer.out().printfIndentln("currentTime = %s", currentTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss.SSS").withLocale(Locale.US)));
            
            keystores =  this.entityManager
                    .createQuery("SELECT k FROM DatabasedKeystore k LEFT JOIN FETCH k.sessions s WHERE k IN :keystores AND s.phase = 'ACTIVE' AND s.expirationTime < :currentTime", DatabasedKeystore.class)
                    .setParameter("keystores", keystores)
                    .setParameter("currentTime", currentTime)
                    .getResultList();
            
            keystores.forEach(keystore -> tracer.out().printfIndentln("keystore = %s", keystore));
            
            return keystores;
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional
    public void rollOver(DatabasedKeystore databasedKeystore) {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.entry("void", this, "rollOver(DatabasedKeystore databasedKeystore)");

        final JsonTracer jsonTracer = new JsonTracer() {
            @Override
            public AbstractTracer getCurrentTracer() {
                return tracer;
            }
        };
        
        databasedKeystore.trace(tracer, true);

        try {
            try {
                Map.Entry<String, JsonArray> nextPartition = databasedKeystore.nextPartition();
                jsonTracer.trace(nextPartition.getValue());
                databasedKeystore.rollover(nextPartition); 
                this.entityManager.merge(databasedKeystore);
            } catch (GeneralSecurityException | IOException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
