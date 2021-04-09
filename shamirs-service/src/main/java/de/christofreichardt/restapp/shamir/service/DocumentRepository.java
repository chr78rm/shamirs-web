/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.restapp.shamir.model.Document;
import org.springframework.data.repository.CrudRepository;

/**
 *
 * @author Developer
 */
public interface DocumentRepository extends CrudRepository<Document, String> {
}
