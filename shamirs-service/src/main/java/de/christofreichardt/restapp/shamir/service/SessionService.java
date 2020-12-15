/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.Session;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public interface SessionService {
    Optional<Session> findLatestByKeystore(String keystoreId);
    Session save(Session session);
    List<Session> findAllByKeystore(String keystoreId);
    Optional<Session> findCurrentSessionByKeystore(String keystoreId);
}
