/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import de.christofreichardt.restapp.shamir.model.Metadata;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;

/**
 *
 * @author Developer
 */
public class DocumentProcessor implements Traceable {

    final List<Metadata> pendingDocuments;
    final ShamirsProtection shamirsProtection;
    final KeyStore keyStore;

    public DocumentProcessor(List<Metadata> pendingDocuments, ShamirsProtection shamirsProtection, KeyStore keyStore) {
        this.pendingDocuments = pendingDocuments;
        this.shamirsProtection = shamirsProtection;
        this.keyStore = keyStore;
    }

    public List<Metadata> getPendingDocuments() {
        return pendingDocuments;
    }

    public List<Metadata> processAll() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.initCurrentTracingContext();
        tracer.entry("void", this, "processAll()");

        try {
            tracer.out().printfIndentln("pendingDocuments = %s", this.pendingDocuments);
            
            try {
                this.pendingDocuments.forEach(metadata -> processPendingDocument(metadata));
            } catch (Exception ex) {
                tracer.logException(LogLevel.ERROR, ex, getClass(), "run()");
            }
                
            return this.pendingDocuments;
        } finally {
            tracer.wayout();
        }
    }

    void processPendingDocument(Metadata metadata) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "processPendingDocuments(Metadata metadata)");

        try {
            try {
                String alias = metadata.getAlias();
                if (this.keyStore.containsAlias(alias)) {
                    if (metadata.getAction() == MetadataAction.SIGN || metadata.getAction() == MetadataAction.VERIFY) {
                        if (this.keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) this.keyStore.getEntry(alias, this.shamirsProtection);
                            if (metadata.getAction() == MetadataAction.SIGN) {
                                metadata.getDocument().sign(privateKeyEntry.getPrivateKey());
                            } else if (metadata.getAction() == MetadataAction.VERIFY) {
                                metadata.getDocument().verify(privateKeyEntry.getCertificate().getPublicKey());
                            }
                        } else {
                            metadata.setState(Metadata.Status.ERROR);
                        }
                    }
                } else {
                    tracer.logMessage(LogLevel.ERROR, String.format("No such key entry: %s", alias), getClass(), "processPendingDocument(Metadata metadata)"); // TODO: set metadata state to faulty
                }
            } catch (GeneralSecurityException ex) {
                throw new RuntimeException(ex); // TODO: rethink error handling, processing of other documents should proceed if one fails for specific reasons
            }
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }

}
