/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 *
 * @author Developer
 */
public class PropertiesExtension implements ParameterResolver {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    public @interface Config {
    }

    private static final Properties CONFIG;

    static {
        CONFIG = new Properties();
        try {
            Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));
            try ( FileReader fileReader = new FileReader(baseDir.resolve(Path.of("app.properties")).toFile())) {
                CONFIG.load(fileReader);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.isAnnotated(Config.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Set<Map.Entry<Object, Object>> entries = CONFIG.entrySet();
        Map<String, String> configuration = Collections.unmodifiableMap(
                entries.stream()
                        .map(entry -> new AbstractMap.SimpleImmutableEntry<String, String>(entry.getKey().toString(), entry.getValue().toString()))
                        .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()))
        );
        
        return configuration;
    }

}
