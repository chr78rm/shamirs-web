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
import java.util.List;
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
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
}