/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.model;

import de.christofreichardt.restapp.shamir.service.XMLSignatureProcessor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Developer
 */
@Entity
@DiscriminatorValue("xml")
public class XMLDocument extends Document {

    private static final long serialVersionUID = 1L;

    public XMLDocument() {
    }

    public XMLDocument(String id) {
        super(id);
    }

    @Override
    public boolean verify(PublicKey publicKey) {
        boolean verified = false;
        try {
            XMLSignatureProcessor xmlSignatureProcessor = new XMLSignatureProcessor();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.getContent());
            org.w3c.dom.Document parsedDocument = documentBuilder.parse(byteArrayInputStream);
            verified = xmlSignatureProcessor.validate(parsedDocument, publicKey);
            this.getMetadata().setValidated(verified);
            this.setModificationTime(LocalDateTime.now());
            this.getMetadata().processed();
            this.getMetadata().setModificationTime(this.getModificationTime());
        } catch (SAXException | IOException ex) {
            this.getMetadata().fault(ex.getMessage());
        } catch (ParserConfigurationException | MarshalException | XMLSignatureException ex) {
            throw new RuntimeException(this.getMetadata().toString(), ex);
        }
        
        return verified;
    }

    @Override
    public Document sign(PrivateKey privateKey) {
        try {
            XMLSignatureProcessor xmlSignatureProcessor = new XMLSignatureProcessor();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.getContent());
            org.w3c.dom.Document parsedDocument = documentBuilder.parse(byteArrayInputStream);
            NodeList nodeList = parsedDocument.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature"); // TODO: use a constant for the namespace
            if (nodeList.getLength() == 0) {
                org.w3c.dom.Document signedDocument = xmlSignatureProcessor.sign(parsedDocument, privateKey);
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.transform(new DOMSource(signedDocument), new StreamResult(byteArrayOutputStream));
                this.setContent(byteArrayOutputStream.toByteArray());
                this.setModificationTime(LocalDateTime.now());
                this.getMetadata().processed();
                this.getMetadata().setModificationTime(this.getModificationTime());
            } else {
                this.getMetadata().fault("Document is signed already.");
            }
        } catch (SAXException | IOException ex) {
            this.getMetadata().fault(ex.getMessage());
        } catch (ParserConfigurationException | GeneralSecurityException | MarshalException | XMLSignatureException | TransformerException ex) {
            throw new RuntimeException(this.getMetadata().toString(), ex);
        }
        
        return this;
    }

}
