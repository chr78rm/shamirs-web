/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.Session;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Developer
 */
@Service
public class SessionDBService implements SessionService, Traceable {

    @PersistenceContext
    EntityManager entityManager;

    @Autowired
    SessionRepository sessionRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> findLatestByKeystore(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Optional<Session>", this, "findLatestByKeystore(String keystoreId)");
        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);

            List<Session> sessions = this.entityManager.createQuery(
                    "SELECT s1 FROM Session s1 WHERE s1.keystore.id = :keystoreId AND s1.creationTime = (SELECT MAX(s2.creationTime) FROM Session s2 WHERE s2.id = s1.id)",
                    Session.class)
                    .setParameter("keystoreId", keystoreId)
                    .getResultList();

            return sessions.isEmpty() ? Optional.empty() : Optional.of(sessions.get(0));
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional
    public Session save(Session session) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Session", this, "save(Session session)");
        try {
            tracer.out().printfIndentln("session = %s", session);

            return this.sessionRepository.save(session);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findAllByKeystore(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<Session>", this, "findAllByKeystore(String keystoreId)");
        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            
            return this.entityManager.createNamedQuery("Session.findAllByKeystore", Session.class)
                    .setParameter("keystoreId", keystoreId)
                    .getResultList();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Session findCurrentSessionByKeystore(String keystoreId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Session", this, "findCurrentSessionByKeystore(String keystoreId)");
        try {
            tracer.out().printfIndentln("keystoreId = %s", keystoreId);
            
            return this.entityManager.createQuery("SELECT s "
                    + "FROM Session s "
                    + "WHERE s.keystore.id = :keystoreId AND s.phase != '" + Session.Phase.CLOSED.name() + "'", Session.class)
                    .setParameter("keystoreId", keystoreId)
                    .getSingleResult();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    @Transactional
    public int closeIdleSessions() {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.entry("List<Session>", this, "closeIdleSessions()");
        try {
            LocalDateTime currentTime = LocalDateTime.now();
            
            tracer.out().printfIndentln("currentTime = %s", currentTime.format(DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss").withLocale(Locale.US)));
            
            return this.entityManager.createQuery("UPDATE Session s "
                    + "SET s.phase ='" + Session.Phase.CLOSED.name() + "' "
                    + "WHERE s.phase = '" + Session.Phase.ACTIVE.name() + "' AND s.expirationTime < :currentTime")
                    .setParameter("currentTime", currentTime)
                    .executeUpdate();
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
}
