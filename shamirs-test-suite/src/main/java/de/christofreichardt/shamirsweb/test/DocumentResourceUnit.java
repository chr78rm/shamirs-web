/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Developer
 */
public class DocumentResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    public DocumentResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    void dummy() throws IOException, SAXException, ParserConfigurationException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "dummy()");

        try {
            final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5";
            
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            tracer.out().printfIndentln("documentBuilder.isNamespaceAware() = %b", documentBuilder.isNamespaceAware());
            tracer.out().printfIndentln("documentBuilder.isValidating() = %b", documentBuilder.isValidating());
            
            Document document;
            try (InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/payment-order.xml")) {
                document = documentBuilder.parse(inputStream);
            }
            
            try (Response response = this.client.target(this.baseUrl)
                    .path("sessions")
                    .path(SESSION_ID)
                    .path("documents")
                    .queryParam("action", MetadataAction.SIGN.name())
                    .queryParam("alias", "test-ec-key")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.xml(document))) {

                tracer.out().printfIndentln("response = %s", response);
            }
        } finally {
            tracer.wayout();
        }
    }
}
