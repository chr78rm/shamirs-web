/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.resource;

import java.util.Objects;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Developer
 */
public class ErrorResponse {

    final Response.Status status;
    final String message;
    final String hint;

    public ErrorResponse(Response.Status status, String message) {
        this.status = status;
        this.message = message;
        this.hint = null;
    }

    public ErrorResponse(Response.Status status, String message, String hint) {
        this.status = status;
        this.message = message;
        this.hint = hint;
    }

    Response build() {
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder()
                .add("status", this.status.getStatusCode())
                .add("reason", this.status.getReasonPhrase())
                .add("message", this.message);
        if (Objects.nonNull(this.hint)) {
            jsonObjectBuilder.add("hint", this.hint);
        }
        JsonObject error = jsonObjectBuilder.build();
        return Response.status(Response.Status.BAD_REQUEST)
                            .entity(error)
                            .type(MediaType.APPLICATION_JSON)
                            .encoding("UTF-8")
                            .build();
    }
}
