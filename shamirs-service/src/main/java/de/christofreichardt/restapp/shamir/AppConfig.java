/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
public class AppConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppConfig.class);

    @Autowired
    SessionSanitizer sessionSanitizer;

    @Autowired
    ScheduledExecutorService scheduledExecutorService;

    @Autowired
    Environment environment;
    
    @PostConstruct
    void init() {
        boolean scheduleSanitizer = Boolean.valueOf(this.environment.getProperty("de.christofreichardt.restapp.shamir.scheduleSanitizer", "true"));
        if (scheduleSanitizer) {
            long initialDelay = Long.parseLong(this.environment.getProperty("de.christofreichardt.restapp.shamir.initialDelay"));
            long period = Long.parseLong(this.environment.getProperty("de.christofreichardt.restapp.shamir.period"));
            TimeUnit timeUnit = TimeUnit.of(ChronoUnit.valueOf(this.environment.getProperty("de.christofreichardt.restapp.shamir.temporalUnit")));

            LOGGER.info(String.format("Scheduling sanitizer service [initialDelay=%d, period=%d, timeUnit=%s] ...", initialDelay, period, timeUnit));

            this.scheduledExecutorService.scheduleAtFixedRate(this.sessionSanitizer, initialDelay, period, timeUnit);
        } else {
            LOGGER.info(String.format("Scheduling sanitizer service skipped ..."));
        }
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
