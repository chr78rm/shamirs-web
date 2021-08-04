/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.Slice;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public interface SliceService {
    List<Slice> findAll();
    List<Slice> findByKeystoreId(String keystoreId);
    List<Slice> findByParticipantId(String participantId);
    List<Slice> findByKeystoreIdAndParticipantId(String keystoreId, String participantId);
    Optional<Slice> findById(String sliceId);
    Slice save(Slice slice);
}
