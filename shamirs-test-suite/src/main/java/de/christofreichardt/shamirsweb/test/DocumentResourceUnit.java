/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.restapp.shamir.common.MetadataAction;
import de.christofreichardt.restapp.shamir.common.SessionPhase;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
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
    void postDocumentToProvisionedSession() throws IOException, SAXException, ParserConfigurationException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postDocumentToProvisionedSession()");

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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
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
            document.getDocumentElement()
                    .getElementsByTagName("timestamp")
                    .item(0)
                    .setTextContent(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // post the document to the provsioned session (for review)
            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .queryParam("action", MetadataAction.SIGN.name())
                    .queryParam("alias", "test-ec-key")
                    .request(MediaType.APPLICATION_JSON)
                    .header("doc-title", "payment-order-1")
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
    void documentsBySession() throws ParserConfigurationException, SAXException, IOException, InterruptedException, TransformerConfigurationException, TransformerException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "documentsBySession()");

        try {
            // session should be in phase 'PROVISIONED'
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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.PROVISIONED.name());
            }

            // retrieve the metadata for all documents
            JsonArray documents;
            try ( Response response = this.client.target(this.baseUrl)
                    .path("sessions")
                    .path(SESSION_ID)
                    .path("documents")
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

            // activate the provisioned session
            final int IDLE_TIME = 10;
            JsonObject sessionInstructions = Json.createObjectBuilder()
                    .add("id", SESSION_ID)
                    .add("phase", SessionPhase.ACTIVE.name())
                    .add("idleTime", IDLE_TIME)
                    .build();
            try ( Response response = this.client.target(this.baseUrl)
                    .path("keystores")
                    .path(KEYSTORE_ID)
                    .path("sessions")
                    .path(SESSION_ID)
                    .request(MediaType.APPLICATION_JSON)
                    .method("PATCH", Entity.json(sessionInstructions))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject session = response.readEntity(JsonObject.class);
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.ACTIVE.name());
                assertThat(session.getInt("idleTime")).isEqualTo(IDLE_TIME);
            }

            // fetch the metadata of the first document and wait until it is processed
            String hrefMetadata = hrefs.get(0).get();
            String hrefContent;
            String state;
            final int MAX_TRIALS = 3, PAUSE = 1;
            int trials = 0;
            do {
                try ( Response response = this.client.target(this.baseUrl)
                        .path(hrefMetadata)
                        .request(MediaType.APPLICATION_JSON)
                        .get()) {
                    trials++;
                    tracer.out().printfIndentln("response = %s", response);
                    assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                    assertThat(response.hasEntity()).isTrue();
                    JsonObject metadata = response.readEntity(JsonObject.class);
                    state = metadata.getString("state");
                    Optional<JsonObject> contentLink = metadata.getJsonArray("links").stream()
                            .map(link -> link.asJsonObject())
                            .filter(link -> Objects.equals(link.getString("rel"), "content"))
                            .findFirst();
                    assertThat(contentLink).isNotEmpty();
                    hrefContent = contentLink.get().getString("href");
                    if (Objects.equals("PENDING", state) && trials < MAX_TRIALS) {
                        Thread.sleep(Duration.ofSeconds(PAUSE).toMillis());
                    } else {
                        break;
                    }
                }
            } while (true);
            assertThat(state).isEqualTo("PROCESSED");

            // fetch the content of the first document
            tracer.out().printfIndentln("hrefContent = %s", hrefContent);
            byte[] content;
            try ( Response response = this.client.target(this.baseUrl)
                    .path(hrefContent)
                    .request(MediaType.APPLICATION_OCTET_STREAM)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                content = response.readEntity(byte[].class);
            }

            // rebuild the DOM and check for the signature
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document signedDocument = documentBuilder.parse(inputStream);
            NodeList nodeList = signedDocument.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature"); // TODO: use a constant for the namespace
            assertThat(nodeList.getLength()).isEqualTo(1);
//            TransformerFactory transformerFactory = TransformerFactory.newInstance();
//            transformerFactory.setAttribute("indent-number", 2);
//            Transformer transformer = transformerFactory.newTransformer();
//            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
//            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//            transformer.transform(new DOMSource(signedDocument), new StreamResult(tracer.out()));
        } finally {
            tracer.wayout();
        }
    }

    @Test
    @Order(3)
    void postDocumentToActiveSession() throws ParserConfigurationException, IOException, SAXException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postDocumentToActiveSession()");

        try {
            // session should be in phase 'ACTIVE'
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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.ACTIVE.name());
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
            document.getDocumentElement()
                    .getElementsByTagName("timestamp")
                    .item(0)
                    .setTextContent(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // post the document to the active session
            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .queryParam("action", MetadataAction.SIGN.name())
                    .queryParam("alias", "test-ec-key")
                    .request(MediaType.APPLICATION_JSON)
                    .header("doc-title", "payment-order-2")
                    .post(Entity.xml(document))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);
                assertThat(response.hasEntity()).isTrue();
                JsonObject metadata = response.readEntity(JsonObject.class);
                assertThat(metadata.getString("state")).isEqualTo("PROCESSED");
                assertThat(metadata.getString("action")).isEqualTo(MetadataAction.SIGN.name());
                var option = metadata.getJsonArray("links").stream()
                        .map(link -> link.asJsonObject())
                        .filter(link -> Objects.equals(link.getString("rel"), "self"))
                        .map(link -> link.getString("href"))
                        .findFirst();
                assertThat(option).isNotEmpty();
                href = option.get();
            }
            
            // retrieve the complete metadata
            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_JSON)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                JsonObject metadata = response.readEntity(JsonObject.class);
                var option = metadata.getJsonArray("links").stream()
                        .map(link -> link.asJsonObject())
                        .filter(link -> Objects.equals(link.getString("rel"), "content"))
                        .map(link -> link.getString("href"))
                        .findFirst();
                assertThat(option).isNotEmpty();
                href = option.get();
            }
            
            // retrieve the signed document
            byte[] content;
            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .request(MediaType.APPLICATION_OCTET_STREAM)
                    .get()) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.OK);
                assertThat(response.hasEntity()).isTrue();
                content = response.readEntity(byte[].class);
            }
            
            // rebuild the DOM and check for the signature
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            Document signedDocument = documentBuilder.parse(inputStream);
            NodeList nodeList = signedDocument.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature"); // TODO: use a constant for the namespace
            assertThat(nodeList.getLength()).isEqualTo(1);
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    @Order(4)
    void postDocumentForValidation() throws ParserConfigurationException, IOException, SAXException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "postDocumentForValidation()");

        try {
            // session should be in phase 'ACTIVE'
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
                assertThat(session.getString("phase")).isEqualTo(SessionPhase.ACTIVE.name());
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
            try ( InputStream inputStream = ShamirsServiceUnit.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/signed-payment-order.xml")) {
                document = documentBuilder.parse(inputStream);
            }
            
            // post the document to the active session
            try ( Response response = this.client.target(this.baseUrl)
                    .path(href)
                    .queryParam("action", MetadataAction.VERIFY.name())
                    .queryParam("alias", "test-ec-key")
                    .request(MediaType.APPLICATION_JSON)
                    .header("doc-title", "signed-payment-order")
                    .post(Entity.xml(document))) {
                tracer.out().printfIndentln("response = %s", response);
                assertThat(response.getStatusInfo().toEnum()).isEqualTo(Response.Status.CREATED);
                assertThat(response.hasEntity()).isTrue();
                JsonObject metadata = response.readEntity(JsonObject.class);
                assertThat(metadata.getString("state")).isEqualTo("PROCESSED");
                assertThat(metadata.getString("action")).isEqualTo(MetadataAction.VERIFY.name());
                assertThat(metadata.getBoolean("validated")).isTrue();
                var option = metadata.getJsonArray("links").stream()
                        .map(link -> link.asJsonObject())
                        .filter(link -> Objects.equals(link.getString("rel"), "self"))
                        .map(link -> link.getString("href"))
                        .findFirst();
                assertThat(option).isNotEmpty();
                href = option.get();
            }
        } finally {
            tracer.wayout();
        }
    }
}
