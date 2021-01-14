/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.rs;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.json.JsonTracer;
import java.io.IOException;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;

/**
 *
 * @author Developer
 */
public class MyClientRequestFilter implements ClientRequestFilter, Traceable {

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "filter(ClientRequestContext clientRequestContext)");

        try {
            tracer.out().printfIndentln("uri = %s, method = %s", clientRequestContext.getUri(), clientRequestContext.getMethod());
            tracer.out().printfIndentln("headers = %s", clientRequestContext.getHeaders());

            String contentType = extractContentType(clientRequestContext.getStringHeaders());

            tracer.out().printfIndentln("contentType = %s", contentType);
            if (clientRequestContext.getEntityClass() != null) {
                tracer.out().printfIndentln("clientRequestContext.getEntityClass().getName() = %s, JsonObject.class.isInstance(clientRequestContext.getEntity()) = %b",
                        clientRequestContext.getEntityClass().getName(),
                        JsonObject.class.isInstance(clientRequestContext.getEntity()));
            }

            if (clientRequestContext.getEntity() != null && JsonObject.class.isInstance(clientRequestContext.getEntity())) {
                JsonTracer jsonTracer = new JsonTracer() {
                    @Override
                    public AbstractTracer getCurrentTracer() {
                        return MyClientRequestFilter.this.getCurrentTracer();
                    }
                };
                jsonTracer.trace((JsonObject) clientRequestContext.getEntity());
            }
        } finally {
            tracer.wayout();
        }
    }

    String extractContentType(MultivaluedMap<String, String> headers) {
        String contentType;
        if (headers.containsKey("Content-Type")) {
            contentType = headers.getFirst("Content-Type");
        } else if (headers.containsKey("content-type")) {
            contentType = headers.getFirst("content-type");
        } else {
            contentType = null;
        }

        return contentType;
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }

}
