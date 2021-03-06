/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.SessionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Developer
 */
public class SessionSanitizer implements Runnable, Traceable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSanitizer.class);

    @Autowired
    SessionService sessionService;

    KeystoreService keystoreService;

    @Autowired
    public void setKeystoreService(KeystoreService keystoreService) {
        this.keystoreService = keystoreService;
    }

    @Override
    public void run() {
        LOGGER.info(String.format("Watching sessions ..."));
        cleanup();
    }

    void cleanup() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.initCurrentTracingContext();
        tracer.entry("void", this, "cleanup()");

        try {
            try {
                List<DatabasedKeystore> databasedKeystores = this.keystoreService.findKeystoresWithCurrentSlicesAndIdleSessions();
                databasedKeystores.forEach(databasedKeystore -> {
                    try {
                        this.keystoreService.rollOver(databasedKeystore);
                    } catch (Throwable ex) {
                        tracer.logException(LogLevel.ERROR, ex, getClass(), "cleanup()");
                    }
                });
            } catch (Exception ex) {
                tracer.logException(LogLevel.ERROR, ex, getClass(), "cleanup()");
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
