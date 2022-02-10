/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

/**
 *
 * @author Developer
 */
@SpringBootApplication
@EnableJpaRepositories("de.christofreichardt.restapp.shamir.service")
public class ShamirsApp {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ShamirsApp.class);
    private static ConfigurableApplicationContext configurableApplicationContext; 

    public ShamirsApp() {
    }

    @PostConstruct
    void init() {
        System.out.printf("%s: ShamirsApp has been constructed ...\n", Thread.currentThread().getName());
    }

    @PreDestroy
    void exit() {
        System.out.printf("%s: ShamirsApp is shutting down ...\n", Thread.currentThread().getName());

        TracerFactory.getInstance().closeQueueTracer();
        TracerFactory.getInstance().closePoolTracer();
    }

    @Autowired
    Environment environment;

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
    @Order(1)
    FilterRegistrationBean<X509AuthenticationFilter> x509AuthenticationFilter() {
        FilterRegistrationBean<X509AuthenticationFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(new X509AuthenticationFilter());
        boolean x509AuthFilterEnabled = Boolean.parseBoolean(this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.enabled", "false"));
        filterRegistrationBean.setEnabled(x509AuthFilterEnabled);
        filterRegistrationBean.setUrlPatterns(List.of("/shamir/v1/*"));
        String adminUserDN = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.adminUserDN", "CN=test-user-0,L=Rodgau,ST=Hessen,C=DE");
        String excludeDNs = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.excludeDNs", "");
        String throttling = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.throttling", "true");
        String minInterval = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.minInterval", "10");
        String minIntervalTempUnit = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.minInterval.temporalUnit", "SECONDS");
        String timeFrame = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.timeFrame", "10");
        String timeFrameTempUnit = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.timeFrame.temporalUnit", "MINUTES");
        String maxCalls = this.environment.getProperty("de.christofreichardt.restapp.shamir.x509AuthenticationFilter.timeFrame.maxCalls", "20");
        filterRegistrationBean.setInitParameters(
                Map.of(
                        "adminUserDN", adminUserDN, "excludeDNs", excludeDNs, "throttling", throttling, "minInterval", minInterval, "minInterval.temporalUnit", minIntervalTempUnit,
                        "timeFrame", timeFrame, "timeFrame.temporalUnit", timeFrameTempUnit, "timeFrame.maxCalls", maxCalls
                )
        );

        return filterRegistrationBean;
    }

    @Bean
    ServletListenerRegistrationBean<MyContextListener> contextListener() {
        ServletListenerRegistrationBean<MyContextListener> servletListenerRegistrationBean = new ServletListenerRegistrationBean<>();
        servletListenerRegistrationBean.setListener(new MyContextListener());

        return servletListenerRegistrationBean;
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
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

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(EntityManagerFactoryBuilder entityManagerFactoryBuilder) {
        return entityManagerFactoryBuilder.dataSource(dataSource())
                .packages("de.christofreichardt.restapp.shamir.model")
                .build();
    }

    @Bean
    Lock lock() {
        return new ReentrantLock();
    }

    @Bean("singleThreadExecutor")
    ExecutorService singleThreadExecutor() {
        ThreadFactory myThreadFactory = new ThreadFactory() {

            AtomicInteger counter = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                return new Thread(runnable, "executing-" + counter.getAndIncrement());
            }
        };

        return Executors.newSingleThreadExecutor(myThreadFactory);
    }

    static class EnvironmentPreparedEventListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Traceable {

        @Override
        public void onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
            AbstractTracer tracer = getCurrentTracer();
            tracer.entry("void", this, "onApplicationEvent(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent)");

            try {
                String[] args = applicationEnvironmentPreparedEvent.getArgs();
                for (int i=0; i<args.length; i++) {
                    tracer.out().printfIndentln("arg[%d] = %s", i, args[i]);
                }
                ConfigurableEnvironment configurableEnvironment = applicationEnvironmentPreparedEvent.getEnvironment();
                MutablePropertySources mutablePropertySources = configurableEnvironment.getPropertySources();
                mutablePropertySources.stream()
                        .filter(propertySource -> propertySource instanceof EnumerablePropertySource)
                        .peek(propertySource -> tracer.out().printfIndentln("---> propertySource = %s", propertySource))
                        .map(propertySource -> (EnumerablePropertySource) propertySource)
                        .forEach(propertySource -> {
                            String[] propertyNames = propertySource.getPropertyNames();
                            Arrays.sort(propertyNames);
                            for(String propertyName : propertyNames) {
                                String resolvedProperty = configurableEnvironment.getProperty(propertyName);
                                String property = propertySource.getProperty(propertyName).toString();
                                if (Objects.equals(resolvedProperty, property)) {
                                    tracer.out().printfIndentln("%s = %s", propertyName, resolvedProperty);
                                } else {
                                    tracer.out().printfIndentln("%1$s = %2$s => %1$s = %3$s", propertyName, property, resolvedProperty);
                                }
                            }
                        });
            } finally {
                tracer.wayout();
            }
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return TracerFactory.getInstance().getCurrentPoolTracer();
        }

    }
    
    static class ReadyEventListener implements ApplicationListener<ApplicationReadyEvent>, Traceable {

        @Override
        public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
            LOGGER.info("onApplicationEvent(ApplicationReadyEvent applicationReadyEvent)");
        }

        @Override
        public AbstractTracer getCurrentTracer() {
            return TracerFactory.getInstance().getCurrentPoolTracer();
        }

    }

    /**
     * @param args the command line arguments
     * @throws java.io.FileNotFoundException
     * @throws de.christofreichardt.diagnosis.TracerFactory.Exception
     */
    public static void main(String[] args) throws FileNotFoundException, TracerFactory.Exception {
        InputStream resourceAsStream = MyContextListener.class.getClassLoader().getResourceAsStream("de/christofreichardt/restapp/shamir/trace-config.xml");
        if (resourceAsStream == null) {
            throw new FileNotFoundException("Missing tracer configuration.");
        }

        try {
            TracerFactory.getInstance().reset();
            TracerFactory.getInstance().readConfiguration(resourceAsStream);
            TracerFactory.getInstance().openQueueTracer();
            TracerFactory.getInstance().openPoolTracer();
        } finally {
            try {
                resourceAsStream.close();
            } catch (IOException ex) {
            }
        }

        AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
        tracer.initCurrentTracingContext();
        tracer.entry("void", ShamirsApp.class, "main(String[] args)");

        try {
            Security.addProvider(new ShamirsProvider());
            SpringApplication springApplication = new SpringApplication(ShamirsApp.class);
            springApplication.addListeners(new EnvironmentPreparedEventListener(), new ReadyEventListener());
            ShamirsApp.configurableApplicationContext = springApplication.run(args);
        } finally {
            tracer.wayout();
        }
    }
    
    static class RestartThreadFactory implements ThreadFactory {
        
        AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, String.format("restarter-%d", this.counter.getAndIncrement()));
            thread.setDaemon(false);
            return thread;
         }
        
    }
    
    static final private RestartThreadFactory restartThreadFactory = new RestartThreadFactory();

    public static void restart(CountDownLatch countDownLatch) {
        ApplicationArguments applicationArguments = ShamirsApp.configurableApplicationContext.getBean(ApplicationArguments.class);
        Thread thread = ShamirsApp.restartThreadFactory.newThread(() -> {
            try {
                countDownLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
            }
            LOGGER.info("Restarting ...");
            ShamirsApp.configurableApplicationContext.close();
            SpringApplication springApplication = new SpringApplication(ShamirsApp.class);
            springApplication.addListeners(new EnvironmentPreparedEventListener(), new ReadyEventListener());
            ShamirsApp.configurableApplicationContext = springApplication.run(applicationArguments.getSourceArgs());
        });
        thread.start();
    }
}
