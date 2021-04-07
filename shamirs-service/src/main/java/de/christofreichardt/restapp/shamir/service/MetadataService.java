/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.Metadata;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public interface MetadataService {
    List<Metadata> findAllBySession(String sessionId);
}
