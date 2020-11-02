/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author Developer
 */
public class Scenario implements Traceable {

    final JdbcTemplate jdbcTemplate;

    public Scenario(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    void cleanup() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "cleanup()");

        try {
            int[] affectedRows = this.jdbcTemplate.batchUpdate(
                    "DELETE FROM slice",
                    "DELETE FROM keystore",
                    "DELETE FROM participant"
            );
            for (int rows : affectedRows) {
                tracer.out().printfIndentln("Affected rows = %s", rows);
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
