/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Developer
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DocumentResourceUnit extends ShamirsBaseUnit implements WithAssertions {

    final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43"; // my-first-keystore
    final String SESSION_ID = "8bff8ac6-fc31-40de-bd6a-eca4348171c5"; // belonging to 'my-first-keystore'

    public DocumentResourceUnit(@PropertiesExtension.Config Map<String, String> config) {
        super(config);
    }

    @Test
    @Order(1)
    void postDocument() throws IOException, SAXException, ParserConfigurationException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postDocument()");

        try {
            // session should be in phase 'PROVISIONED'
            String href;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo("PROVISIONED");
                Optional<JsonObject> documentsLink = session.getJsonArray("links").stream()
                        .map(link -> link.asJsonObject())
                        .filter(link -> Objects.equals(link.getString("rel"), "documents"))
                        .findFirst();
                assertThat(documentsLink).isPresent();
                href = documentsLink.get().getString("href");
            }

            tracer.out().printfIndentln("href = %s", href);

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

            tracer.out().printfIndentln("documentBuilder.isNamespaceAware() = %b", documentBuilder.isNamespaceAware());
            tracer.out().printfIndentln("documentBuilder.isValidating() = %b", documentBuilder.isValidating());

            Document document;
            try ( InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/payment-order.xml")) {
                document = documentBuilder.parse(inputStream);
            }

            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .queryParam("action", MetadataAction.SIGN.name())
                    .queryParam("alias", "test-ec-key")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.xml(document))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);
                assertThat(response.hasEntity()).isTrue();
                JsonObject metadata = response.readEntity(JsonObject.class);
                assertThat(metadata.getString("state")).isEqualTo("PENDING");
                assertThat(metadata.getString("action")).isEqualTo(MetadataAction.SIGN.name());
            }
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(2)
    void documentsBySession() throws ParserConfigurationException, SAXException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "documentsBySession()");

        try {
            // retrieve the metadata of the documents
            JsonArray documents;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("sessions")
                    .path(SESSION_ID)
                    .path("metadata")
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                documents = response.readEntity(JsonObject.class).getJsonArray("documents");
            }

            // extract the self links and the hrefs
            List<Optional<String>> hrefs = documents.stream()
                    .map(document -> document.asJsonObject())
                    .map(document -> document.getJsonArray("links"))
                    .map(links -> {
                        return links.stream()
                                .map(link -> link.asJsonObject())
                                .filter(link -> Objects.equals("self", link.getString("rel")))
                                .findFirst()
                                .map(link -> link.getString("href"));
                    })
                    .collect(Collectors.toList());

            tracer.out().printfIndentln("hrefs = %s", hrefs);
            assertThat(hrefs).isNotEmpty();
            assertThat(hrefs.get(0)).isNotEmpty();
            
            // fetch the content of the first document
            byte[] content;
            String href = hrefs.get(0).get();
            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_OCTET_STREAM)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                content = response.readEntity(byte[].class);
            }
                
            // rebuild the DOM
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            documentBuilder.parse(inputStream);
        } finally {
            tracer.wayout();
        }
    }
}
