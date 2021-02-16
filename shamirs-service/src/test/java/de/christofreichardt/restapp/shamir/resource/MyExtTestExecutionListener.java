/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.junit5.MyTestExecutionListener;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;

/**
 *
 * @author Developer
 */
public class MyExtTestExecutionListener extends MyTestExecutionListener {

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        super.executionStarted(testIdentifier); //To change body of generated methods, choose Tools | Templates.
        if (testIdentifier.getParentId().isEmpty()) {
            TracerFactory.getInstance().openQueueTracer();
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult);
        if (testIdentifier.getParentId().isEmpty()) {
            TracerFactory.getInstance().closeQueueTracer();
        }
    }
    
}
