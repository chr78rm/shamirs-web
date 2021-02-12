/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public interface KeystoreService {
    List<DatabasedKeystore> findAll();
    Optional<DatabasedKeystore> findbyId(String id);
    DatabasedKeystore findByIdWithPostedSlices(String id);
    DatabasedKeystore findByIdWithCertainSlices(String id, String state);
    DatabasedKeystore findByIdWithActiveSlices(String id);
    DatabasedKeystore findByIdWithCurrentSlicesAndValidSession(String id);
    DatabasedKeystore findByIdAndParticipantWithPostedSlices(String id, String participantId);
    DatabasedKeystore persist(DatabasedKeystore keystore);
    DatabasedKeystore findByDescriptiveName(String descriptiveName);
    Optional<DatabasedKeystore> findByIdWithActiveSlicesAndCurrentSession(String keystoreId);
    List<DatabasedKeystore> findKeystoresWithCurrentSlicesAndIdleSessions();
    void rollOver();
    void rollOver(DatabasedKeystore databasedKeystore);
}
