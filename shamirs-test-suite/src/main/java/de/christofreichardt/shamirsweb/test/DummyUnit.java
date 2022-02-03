/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author reichardt
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(PropertiesExtension.class)
public class DummyUnit implements Traceable {

    @BeforeAll
    void init() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "init()");

        try {
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void dummy(@PropertiesExtension.Config Map<String, String> config) throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "dummy(Map<String, String> config)");

        try {
            config.entrySet()
                    .stream()
                    .sorted((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()))
                    .forEach(entry -> tracer.out().printfIndentln("%s = %s", entry.getKey(), entry.getValue()));
            
            Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));
            Path logPath = baseDir.resolve(config.getOrDefault("de.christofreichardt.shamirsweb.test.spring.log", "log/spring-boot.log"));
            Files.deleteIfExists(logPath);
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void paths() throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "paths()");

        try {
            Path logDir = Path.of(System.getProperty("user.dir"), "..", "data", "log").toRealPath();
            tracer.out().printfIndentln("logDir = %s", logDir);
        } finally {
            tracer.wayout();
        }
    }

    @AfterAll
    void exit() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "exit()");

        try {
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }
    
}
