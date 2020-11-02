/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.jsonp.JsonProcessingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;

/**
 *
 * @author reichardt
 */
@Component
@ApplicationPath("/shamir/v1")
public class MyJerseyConfig extends ResourceConfig {

    public MyJerseyConfig() {
        register(ShamirsRS.class);
        register(JsonProcessingFeature.class);
        register(MyContainerRequestFilter.class);
    }
    
}
