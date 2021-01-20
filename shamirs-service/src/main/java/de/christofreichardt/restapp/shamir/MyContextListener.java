/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.TracerFactory;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author reichardt
 */
public class MyContextListener implements ServletContextListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.info(String.format("%d: contextInitialized(ServletContextEvent servletContextEvent) ...", System.identityHashCode(this)));
        
        InputStream resourceAsStream = MyContextListener.class.getClassLoader().getResourceAsStream("de/christofreichardt/restapp/shamir/trace-config.xml");
        if (resourceAsStream != null) {
            try {
                try {
                    TracerFactory.getInstance().reset();
                    TracerFactory.getInstance().readConfiguration(resourceAsStream);
                    TracerFactory.getInstance().openQueueTracer();
                    TracerFactory.getInstance().openPoolTracer();
                } catch (TracerFactory.Exception ex) {
                    LOGGER.warn(String.format("Problems when evaluating the configuration resource: %s", ex.getMessage()));
                }
            } finally {
                try {
                    resourceAsStream.close();
                } catch (IOException ex) {
                }
            }
        } else {
            LOGGER.warn(String.format("No tracer configuration found."));
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.info(String.format("%d: contextDestroyed(ServletContextEvent servletContextEvent) ...", System.identityHashCode(this)));
        
        TracerFactory.getInstance().closeQueueTracer();
        TracerFactory.getInstance().closePoolTracer();
    }
    
}
