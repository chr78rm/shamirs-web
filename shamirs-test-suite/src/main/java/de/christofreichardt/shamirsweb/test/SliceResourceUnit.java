/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Developer
 */
public class SliceResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    public SliceResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    void dummy() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "dummy()");

        try {
            JsonObject slice = Json.createObjectBuilder()
                    .add("state", "FETCHED")
                    .build();

            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .path("5ae7570d-3f3b-43b5-94e6-b23f24d60093")
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(slice))) {
                tracer.out().printfIndentln("response = %s", response);
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void slicesByKeystore() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "slicesByKeystore()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            
            try ( Response response = this.client.target(this.baseUrl)
                    .path("slices")
                    .queryParam("keystoreId", KEYSTORE_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("GET")) {
                tracer.out().printfIndentln("response = %s", response);
            }
        } finally {
            tracer.wayout();
        }
    }

}
