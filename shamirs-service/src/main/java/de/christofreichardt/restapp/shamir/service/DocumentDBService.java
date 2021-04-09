/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.Document;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Developer
 */
@Service
public class DocumentDBService implements DocumentService, Traceable {
    
    @Autowired
    DocumentRepository documentRepository;

    @Override
    public Optional<Document> findById(String documentId) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Optional<Document>", this, "findById(String documentId)");

        try {
            tracer.out().printfIndentln("documentId = %s", documentId);
            
            return this.documentRepository.findById(documentId);
        } finally {
            tracer.wayout();
        }
    }
    
    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
}
