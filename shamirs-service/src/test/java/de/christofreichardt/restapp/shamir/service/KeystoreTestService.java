/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonTracer;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import javax.json.JsonArray;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 *
 * @author Developer
 */
@Service
@Qualifier("test")
public class KeystoreTestService extends KeystoreDBService implements KeystoreService {
    
    private String failedKeystoreId;

    public void setFailedKeystoreId(String failedKeystoreId) {
        this.failedKeystoreId = failedKeystoreId;
    }

    @Override
    public void rollOver(DatabasedKeystore databasedKeystore) {
        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.entry("void", this, "rollOver(DatabasedKeystore databasedKeystore)");
        
        tracer.out().printfIndentln("this.failedKeystoreId = %s", this.failedKeystoreId);

        final JsonTracer jsonTracer = new JsonTracer() {
            @Override
            public AbstractTracer getCurrentTracer() {
                return tracer;
            }
        };
        
        databasedKeystore.trace(tracer, true);

        try {
            try {
                Map.Entry<String, JsonArray> nextPartition = databasedKeystore.nextPartition();
                jsonTracer.trace(nextPartition.getValue());
                databasedKeystore.rollover(nextPartition);
                if (Objects.equals(this.failedKeystoreId, databasedKeystore.getId())) {
                    throw new RuntimeException("This is a test.");
                }
                this.entityManager.merge(databasedKeystore);
            } catch (GeneralSecurityException | IOException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            tracer.wayout();
        }
    }
    
}
