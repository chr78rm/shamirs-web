/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.jca.shamir.ShamirsProvider;
import java.security.Security;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 *
 * @author Developer
 */
@SpringBootApplication
@EnableJpaRepositories("de.christofreichardt.restapp.shamir.service")
public class ShamirsApp {

    @Bean
    @Order(0)
    FilterRegistrationBean<MyTraceFilter> tracingFilter() {
        FilterRegistrationBean<MyTraceFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new MyTraceFilter());
        filterRegistrationBean.setInitParameters(Map.of("blacklist", "/test", "key2", "value2"));
        filterRegistrationBean.setUrlPatterns(List.of("/shamir/v1/*"));

        return filterRegistrationBean;
    }

    @Bean
    ServletListenerRegistrationBean<MyContextListener> contextListener() {
        ServletListenerRegistrationBean<MyContextListener> servletListenerRegistrationBean = new ServletListenerRegistrationBean<>();
        servletListenerRegistrationBean.setListener(new MyContextListener());

        return servletListenerRegistrationBean;
    }

    @Bean
    @ConfigurationProperties(prefix="spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create()
//                .url("jdbc:mariadb://localhost:3306/shamirs_db")
//                .password("Msiw47Ut129")
//                .username("shamir")
                .build();
    }
    
    @Bean
    SessionSanitizer sessionSanitizer() {
        return new SessionSanitizer();
    }
    
    @Bean
    ScheduledExecutorService singleThreadScheduledExecutor() {
        
        ThreadFactory myThreadFactory = new ThreadFactory() {
            
            AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "scheduling-" + counter.getAndIncrement());
            }
        };
        
        return Executors.newSingleThreadScheduledExecutor(myThreadFactory);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Security.addProvider(new ShamirsProvider());
        SpringApplication.run(ShamirsApp.class);
    }
    
}
