/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 *
 * @author Developer
 */
@Configuration
@EnableScheduling
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);
    
    @Autowired
    SessionSanitizer sessionSanitizer;
    
    @Autowired
    ScheduledExecutorService scheduledExecutorService;
    
    @Scheduled(fixedRate = 5000L)
    void trigger() {
        LOGGER.info(String.format("%s: Watching sessions ...", Thread.currentThread().getName()));
        this.sessionSanitizer.cleanup();
    }
    
    @PreDestroy
    void cleanup() throws InterruptedException {
        LOGGER.info("Terminating scheduling service ...");
        
        final long TIMEOUT = 5;
        this.scheduledExecutorService.shutdown();
        boolean terminated = this.scheduledExecutorService.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
        
        LOGGER.info(String.format("terminated = %b", terminated));
    }
    
}
