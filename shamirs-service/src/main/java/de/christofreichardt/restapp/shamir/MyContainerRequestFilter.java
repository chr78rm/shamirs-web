/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonTracer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Developer
 */
public class MyContainerRequestFilter implements ContainerRequestFilter, Traceable {

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "filter(ContainerRequestContext containerRequestContext)");

        try {
            tracer.out().printfIndentln("method = %s", containerRequestContext.getMethod());
            tracer.out().printfIndentln("headers = %s", containerRequestContext.getHeaders());

            if (Objects.equals(MediaType.APPLICATION_JSON, containerRequestContext.getHeaderString("content-type"))) {
                try {
                    byte[] bytes = containerRequestContext.getEntityStream().readAllBytes();
                    tracer.out().printfIndentln("payload = %s", new String(bytes, StandardCharsets.UTF_8));
                    try {
                        JsonTracer jsonTracer = new JsonTracer() {
                            @Override
                            public AbstractTracer getCurrentTracer() {
                                return MyContainerRequestFilter.this.getCurrentTracer();
                            }
                        };
                        JsonStructure jsonStructure;
                        try (JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(bytes))) {
                            jsonStructure = jsonReader.read();
                        }
                        jsonTracer.trace(jsonStructure);
                    } finally {
                        containerRequestContext.setEntityStream(new ByteArrayInputStream(bytes));
                    }
                } catch (IOException ex) {
                    tracer.logException(LogLevel.ERROR, ex, getClass(), "filter(ContainerRequestContext containerRequestContext)");
                }
            }
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
