/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.rs;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.LogLevel;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonTracer;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;

/**
 *
 * @author Developer
 */
public class MyClientResponseFilter implements ClientResponseFilter, Traceable {

    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)");

        try {
            tracer.out().printfIndentln("uri = %s, method = %s", clientRequestContext.getUri(), clientRequestContext.getMethod());
            tracer.out().printfIndentln("statusInfo = %s, status = %d", clientResponseContext.getStatusInfo(), clientResponseContext.getStatus());
            tracer.out().printfIndentln("headers = %s", clientResponseContext.getHeaders());

            String contentType = clientResponseContext.getHeaderString("Content-Type");
            String contentEncoding = clientResponseContext.getHeaderString("Content-Encoding");

            tracer.out().printfIndentln("contentType = %s", contentType);
            tracer.out().printfIndentln("contentEncoding = %s", contentEncoding);

            if (Objects.nonNull(contentType)) {
                if (contentType.contains(MediaType.APPLICATION_JSON)) {
                    try {
                        byte[] bytes = clientResponseContext.getEntityStream().readAllBytes();
                        tracer.out().printfIndentln("payload = %s", new String(bytes, StandardCharsets.UTF_8));
                        try {
                            JsonTracer jsonTracer = new JsonTracer() {
                                @Override
                                public AbstractTracer getCurrentTracer() {
                                    return MyClientResponseFilter.this.getCurrentTracer();
                                }
                            };
                            JsonStructure jsonStructure;
                            try ( JsonReader jsonReader = Json.createReader(new ByteArrayInputStream(bytes))) {
                                jsonStructure = jsonReader.read();
                            }
                            jsonTracer.trace(jsonStructure);
                        } finally {
                            clientResponseContext.setEntityStream(new ByteArrayInputStream(bytes));
                        }
                    } catch (IOException ex) {
                        tracer.logException(LogLevel.ERROR, ex, getClass(), "filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)");
                    }
                } else if (contentType.contains(MediaType.TEXT_HTML)) {
                    try {
                        byte[] bytes = clientResponseContext.getEntityStream().readAllBytes();
                        try {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                            try (InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8);
                                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                                bufferedReader.lines().forEach(line -> tracer.out().printfIndentln("line = %s", line));
                            }
                        } finally {
                            clientResponseContext.setEntityStream(new ByteArrayInputStream(bytes));
                        }
                    } catch (IOException ex) {
                        tracer.logException(LogLevel.ERROR, ex, getClass(), "filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext)");
                    }
                }
            }

        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }

}
