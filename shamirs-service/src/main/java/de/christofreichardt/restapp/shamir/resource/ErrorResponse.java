/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Developer
 */
public class ErrorResponse {

    final Response.Status status;
    final String message;

    public ErrorResponse(Response.Status status, String message) {
        this.status = status;
        this.message = message;
    }

    Response build() {
        JsonObject hint = Json.createObjectBuilder()
                .add("status", this.status.getStatusCode())
                .add("reason", this.status.getReasonPhrase())
                .add("message", this.message)
                .build();
        return Response.status(Response.Status.BAD_REQUEST)
                            .entity(hint)
                            .type(MediaType.APPLICATION_JSON)
                            .encoding("UTF-8")
                            .build();
    }
}
