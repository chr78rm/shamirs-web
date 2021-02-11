/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.PasswordGenerator;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.model.Session;
import de.christofreichardt.restapp.shamir.model.Slice;
import de.christofreichardt.scala.shamir.SecretSharing;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
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
            
            keystores.forEach(keystore -> {
                tracer.out().printfIndentln("keystore = %s", keystore);
                keystore.getSessions().forEach(session -> tracer.out().printfIndentln("session = %s", session));
                keystore.getSlices().forEach(slice -> tracer.out().printfIndentln("slice = %s", slice));
                tracer.out().printfIndentln("(*---*)");
            });
            
            return keystores;
        } finally {
            tracer.wayout();
        }
    }
    
    class JsonSliceComparator implements Comparator<JsonObject> {

        @Override
        public int compare(JsonObject slice1, JsonObject slice2) {
            int size1 = slice1.getJsonArray("SharePoints").size();
            int size2 = slice2.getJsonArray("SharePoints").size();
            if (size1 < size2) {
                return -1;
            } else if (size1 > size2) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    @Transactional
    public void rollOver() {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.entry("void", this, "rollOver()");

        final JsonTracer jsonTracer = new JsonTracer() {
            @Override
            public AbstractTracer getCurrentTracer() {
                return tracer;
            }
        };
        
        List<DatabasedKeystore> databasedKeystores = findKeystoresWithCurrentSlicesAndIdleSessions();
        try {
            databasedKeystores.forEach(databasedKeystore -> {
                final int DEFAULT_PASSWORD_LENGTH = 32;
                try {
                    PasswordGenerator passwordGenerator = new PasswordGenerator(DEFAULT_PASSWORD_LENGTH);
                    CharSequence passwordSequence = passwordGenerator.generate().findFirst().get();
                    SecretSharing secretSharing = new SecretSharing(databasedKeystore.getShares(), databasedKeystore.getThreshold(), passwordSequence);
                    JsonArray nextPartition = secretSharing.partitionAsJson(databasedKeystore.sizes()).stream()
                            .map(slice -> slice.asJsonObject())
                            .sorted(new JsonSliceComparator())
                            .collect(new JsonValueCollector());

                    jsonTracer.trace(nextPartition);
                    
                    ShamirsProtection nextProtection = new ShamirsProtection(nextPartition);
                    byte[] nextKeystoreBytes = databasedKeystore.nextKeystoreInstance(nextProtection);
                    String nextPartitionId = nextPartition.getJsonObject(0).getString("PartitionId");
                    Iterator<JsonValue> iter = nextPartition.iterator();
                    Set<Slice> nextSlices = databasedKeystore.getSlices().stream()
                            .filter(slice -> Objects.equals(slice.getPartitionId(), databasedKeystore.getCurrentPartitionId()))
                            .sorted((Slice slice1, Slice slice2) -> {
                                if (slice1.getSize() < slice2.getSize()) {
                                    return -1;
                                } else if (slice1.getSize() > slice2.getSize()) {
                                    return 1;
                                } else {
                                    return 0;
                                }
                            })
                            .peek(slice -> tracer.out().printfIndentln("slice = %s", slice))
                            .map(slice -> {
                                slice.setProcessingState(Slice.ProcessingState.EXPIRED.name());
                                Slice nextSlice = new Slice();
                                nextSlice.setKeystore(databasedKeystore);
                                nextSlice.setParticipant(slice.getParticipant());
                                nextSlice.setPartitionId(nextPartitionId);
                                nextSlice.setProcessingState(Slice.ProcessingState.CREATED.name());
                                nextSlice.setSize(slice.getSize());
                                JsonValue share = iter.next();
                                jsonTracer.trace(share.asJsonObject());
                                nextSlice.setShare(share);
                                return nextSlice;
                            })
                            .collect(Collectors.toSet());
                    databasedKeystore.setSlices(nextSlices);
                    databasedKeystore.setCurrentPartitionId(nextPartitionId);
                    databasedKeystore.getSessions().stream()
                            .filter(session -> Objects.equals(Session.Phase.ACTIVE.name(), session.getPhase()))
                            .forEach(session -> session.setPhase(Session.Phase.CLOSED.name()));
                    Session session = new Session();
                    session.setPhase(Session.Phase.PROVISIONED.name());
                    session.setKeystore(databasedKeystore);
                    databasedKeystore.getSessions().add(session);
                    databasedKeystore.setStore(nextKeystoreBytes);
                    this.entityManager.merge(databasedKeystore);
                } catch (GeneralSecurityException | IOException ex) {
                    throw new RuntimeException(ex);
                }
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
