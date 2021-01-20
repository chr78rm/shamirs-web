/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.restapp.shamir.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Developer
 */
public class SessionSanitizer implements Traceable {

    @Autowired
    SessionService sessionService;

    void cleanup() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.initCurrentTracingContext();
        tracer.entry("void", this, "cleanup()");

        try {
            int updated = this.sessionService.closeIdleSessions();
            
            tracer.out().printfIndentln("Closing %d idle sessions.", updated);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }
}
