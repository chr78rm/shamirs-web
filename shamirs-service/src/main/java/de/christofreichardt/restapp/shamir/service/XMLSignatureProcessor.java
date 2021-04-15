/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.restapp.shamir.service;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Developer
 */
@Component
public class XMLSignatureProcessor implements Traceable {

    public Document sign(Document document, PrivateKey privateKey) throws GeneralSecurityException, MarshalException, XMLSignatureException, ParserConfigurationException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Document", this, "sign(Document document, PrivateKey privateKey)");

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document clonedDocument = documentBuilder.newDocument();
            Node clonedDocumentElement = clonedDocument.importNode(document.getDocumentElement(), true);
            clonedDocument.appendChild(clonedDocumentElement);

            XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance();
            DigestMethod digestMethod = xmlSignatureFactory.newDigestMethod(DigestMethod.SHA256, null);
            Transform transform = xmlSignatureFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
            Reference reference = xmlSignatureFactory.newReference("", digestMethod, List.of(transform), null, null);
            CanonicalizationMethod canonicalizationMethod = xmlSignatureFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null);
            SignatureMethod signatureMethod = xmlSignatureFactory.newSignatureMethod(SignatureMethod.ECDSA_SHA256, null);
            SignedInfo signedInfo = xmlSignatureFactory.newSignedInfo(canonicalizationMethod, signatureMethod, List.of(reference));
            DOMSignContext domSignContext = new DOMSignContext(privateKey, document.getDocumentElement());
            XMLSignature xmlSignature = xmlSignatureFactory.newXMLSignature(signedInfo, null);
            xmlSignature.sign(domSignContext);

            return document;
        } finally {
            tracer.wayout();
        }
    }

    public boolean validate(Document document, PublicKey publicKey) throws MarshalException, XMLSignatureException {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("boolean", this, "validate(Document document, PublicKey publicKey)");

        try {
            NodeList nodeList = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (nodeList.getLength() == 0) {
                throw new XMLSignatureException("No Signature found.");
            }

            XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory.getInstance();
            DOMValidateContext domValidateContext = new DOMValidateContext(publicKey, nodeList.item(0));
            XMLSignature xmlSignature = xmlSignatureFactory.unmarshalXMLSignature(domValidateContext);

            return xmlSignature.validate(domValidateContext);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentQueueTracer();
    }
}
