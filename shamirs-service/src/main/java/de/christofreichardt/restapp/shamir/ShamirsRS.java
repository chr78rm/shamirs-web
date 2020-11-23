/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsLoadParameter;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.json.JsonValueCollector;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.service.KeystoreService;
import de.christofreichardt.restapp.shamir.service.ParticipantService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *
 * @author Developer
 */
@Component
@Path("")
public class ShamirsRS implements Traceable {

    @Autowired
    KeystoreService keystoreService;

    @Autowired
    ParticipantService participantService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("ping")
    public String ping() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "ping()");

        try {
            List<DatabasedKeystore> keystores = this.keystoreService.findAll();
            tracer.out().printfIndentln("keystores.size() = %d", keystores.size());
            keystores.forEach(
                    keystore -> tracer.out().printfIndentln("keystore = %s", keystore)
            );

            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
            DatabasedKeystore keystore = this.keystoreService.findByIdWithPostedSlices(KEYSTORE_ID);
            keystore.getSlices().forEach(
                    slice -> tracer.out().printfIndentln("slice = %s", slice)
            );

            final String PARTICIPANT_ID = "8844dd34-c836-4060-ba73-c6d86ad1275d"; // christof
            keystore = this.keystoreService.findByIdAndParticipantWithPostedSlices(KEYSTORE_ID, PARTICIPANT_ID);
            keystore.getSlices().forEach(
                    slice -> tracer.out().printfIndentln("slice = %s", slice)
            );
            JsonArray sharePoints = keystore.getSlices().stream()
                    .map(slice -> new ByteArrayInputStream(slice.getShare()))
                    .map(in -> {
                        try ( JsonReader jsonReader = Json.createReader(in)) {
                            return jsonReader.read();
                        }
                    })
                    .collect(new JsonValueCollector());
            ShamirsProtection shamirsProtection = new ShamirsProtection(sharePoints);
            ByteArrayInputStream in = new ByteArrayInputStream(keystore.getStore());
            ShamirsLoadParameter shamirsLoadParameter = new ShamirsLoadParameter(in, shamirsProtection);
            KeyStore shamirsKeystore = KeyStore.getInstance("ShamirsKeystore", Security.getProvider(ShamirsProvider.NAME));
            shamirsKeystore.load(shamirsLoadParameter);
            shamirsKeystore.aliases().asIterator().forEachRemaining(alias -> tracer.out().printfIndentln("alias = %s", alias));

            return "ping";
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }

}
