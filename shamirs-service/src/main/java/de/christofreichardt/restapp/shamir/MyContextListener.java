/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

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
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.info(String.format("%d: contextDestroyed(ServletContextEvent servletContextEvent) ...", System.identityHashCode(this)));
    }
    
}
