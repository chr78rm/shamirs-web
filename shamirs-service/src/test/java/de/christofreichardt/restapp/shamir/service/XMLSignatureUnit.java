/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.QueueTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.jca.shamir.ShamirsProtection;
import de.christofreichardt.jca.shamir.ShamirsProvider;
import de.christofreichardt.restapp.shamir.ShamirsApp;
import de.christofreichardt.restapp.shamir.model.DatabasedKeystore;
import de.christofreichardt.restapp.shamir.resource.Scenario;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Developer
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ShamirsApp.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XMLSignatureUnit implements Traceable, WithAssertions {

    JdbcTemplate jdbcTemplate;
    Scenario scenario;

    @Autowired
    DataSource dataSource;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Autowired
    KeystoreService keystoreService;

    @BeforeAll
    void init() throws GeneralSecurityException, IOException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "init()");

        try {
            List<String> propertyNames = new ArrayList<>(System.getProperties().stringPropertyNames());
            propertyNames.stream()
                    .sorted()
                    .forEach((propertyName) -> tracer.out().printfIndentln("%s = %s", propertyName, System.getProperties().getProperty(propertyName)));

            Security.addProvider(new ShamirsProvider());

            this.jdbcTemplate = new JdbcTemplate(this.dataSource);
            this.scenario = new Scenario(this.jdbcTemplate);
            this.scenario.setup();
            this.entityManagerFactory.getCache().evictAll();
        } finally {
            tracer.wayout();
        }
    }

    @Autowired
    XMLSignatureProcessor xmlSignatureProcessor;

    @Test
    void signEnveloped() throws ParserConfigurationException, SAXException, IOException, GeneralSecurityException, MarshalException, XMLSignatureException, TransformerConfigurationException, TransformerException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "signEnveloped()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43";

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document paymentOrder = documentBuilder.parse(Path.of("xml", "in", "payment-order.xml").toFile());

            Optional<DatabasedKeystore> dbKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(KEYSTORE_ID);
            assertThat(dbKeystore).isNotEmpty();
            KeyStore keyStore = dbKeystore.get().keystoreInstance();
            Iterator<String> iter = keyStore.aliases().asIterator();
            iter.forEachRemaining(alias -> tracer.out().printfIndentln("alias = %s", alias));
            ShamirsProtection shamirsProtection = new ShamirsProtection(dbKeystore.get().sharePoints());
            KeyStore.Entry entry = keyStore.getEntry("test-ec-key", shamirsProtection);
            assertThat(entry).isNotNull();
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

            Document signedDocument;
            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "signEnveloped()");
            try {
                signedDocument = this.xmlSignatureProcessor.sign(paymentOrder, privateKeyEntry.getPrivateKey());
                Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
                transformer.transform(new DOMSource(signedDocument), new StreamResult(Path.of("xml", "out", "signed-payment-order.xml").toFile()));
                boolean validated = this.xmlSignatureProcessor.validate(signedDocument, privateKeyEntry.getCertificate().getPublicKey());
                assertThat(validated).isTrue();
            } finally {
                qTracer.wayout();
            }
        } finally {
            tracer.wayout();
        }
    }
    
    @Test
    void validate() throws ParserConfigurationException, SAXException, IOException, GeneralSecurityException, MarshalException, XMLSignatureException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "validate()");

        try {
            final String KEYSTORE_ID = "5adab38c-702c-4559-8a5f-b792c14b9a43";

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document signedPaymentOrder = documentBuilder.parse(Path.of("xml", "in", "signed-payment-order.xml").toFile());
            Document fakedPaymentOrder = documentBuilder.parse(Path.of("xml", "in", "faked-payment-order.xml").toFile());

            Optional<DatabasedKeystore> dbKeystore = this.keystoreService.findByIdWithActiveSlicesAndCurrentSession(KEYSTORE_ID);
            assertThat(dbKeystore).isNotEmpty();
            KeyStore keyStore = dbKeystore.get().keystoreInstance();
            Iterator<String> iter = keyStore.aliases().asIterator();
            iter.forEachRemaining(alias -> tracer.out().printfIndentln("alias = %s", alias));
            ShamirsProtection shamirsProtection = new ShamirsProtection(dbKeystore.get().sharePoints());
            KeyStore.Entry entry = keyStore.getEntry("test-ec-key", shamirsProtection);
            assertThat(entry).isNotNull();
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
            
            QueueTracer<?> qTracer = TracerFactory.getInstance().takeTracer();
            qTracer.initCurrentTracingContext();
            qTracer.entry("void", this, "signEnveloped()");
            try {
                boolean validated = this.xmlSignatureProcessor.validate(signedPaymentOrder, privateKeyEntry.getCertificate().getPublicKey());
                assertThat(validated).isTrue();
                validated = this.xmlSignatureProcessor.validate(fakedPaymentOrder, privateKeyEntry.getCertificate().getPublicKey());
                assertThat(validated).isFalse();
            } finally {
                qTracer.wayout();
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
