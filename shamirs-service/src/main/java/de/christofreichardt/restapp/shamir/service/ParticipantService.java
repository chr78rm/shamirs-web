/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.Participant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public interface ParticipantService {
    Participant findByPreferredName(String preferredName);
    List<Participant> findByKeystore(String keystoreId);
}
