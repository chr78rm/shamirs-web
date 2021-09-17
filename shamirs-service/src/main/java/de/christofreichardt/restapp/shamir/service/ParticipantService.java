/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.Participant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public interface ParticipantService {

    static class ParticipantNotFoundException extends java.lang.Exception {

        private static final long serialVersionUID = 1L;

        public ParticipantNotFoundException(String message) {
            super(message);
        }
    }
    
    Participant findByPreferredName(String preferredName);
    List<Participant> findByKeystore(String keystoreId);
    Map<String, Participant> findByPreferredNames(Set<String> preferredNames) throws ParticipantNotFoundException;
}
