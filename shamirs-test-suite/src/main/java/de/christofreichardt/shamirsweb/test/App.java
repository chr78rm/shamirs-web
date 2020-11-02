/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.christofreichardt.shamirsweb.test;

import de.christofreichardt.diagnosis.AbstractTracer;
import de.christofreichardt.diagnosis.Traceable;
import de.christofreichardt.diagnosis.TracerFactory;
import de.christofreichardt.junit5.MyTestExecutionListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Developer
 */
public class App implements Traceable {

    final Path baseDir = Path.of(System.getProperty("de.christofreichardt.shamirsweb.test.baseDir"));

    Document parseTestConfiguration() {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("Document", this, "parseTestConfiguration()");
        try {
            try {
                DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
                documentBuilderFactory.setNamespaceAware(true);
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

                tracer.out().printfIndentln("documentBuilder.isNamespaceAware() = %b", documentBuilder.isNamespaceAware());
                tracer.out().printfIndentln("documentBuilder.isValidating() = %b", documentBuilder.isValidating());

                Path configPath = this.baseDir.resolve("test-config.xml");
                Document document = documentBuilder.parse(configPath.toFile());

                return document;
            } catch (ParserConfigurationException | SAXException | IOException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            tracer.wayout();
        }
    }
    List<DiscoverySelector> configuredTests(Document configDocument) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("List<DiscoverySelector>", this, "configuredTests(Document configDocument)");
        try {
            try {
                List<DiscoverySelector> selectors = new ArrayList<>();

                XPath xpath = XPathFactory.newInstance().newXPath();
                xpath.setNamespaceContext(new NamespaceContext() {
                    @Override
                    public String getNamespaceURI(String prefix) {
                        return "http://www.christofreichardt.de/shamirs-test-suite";
                    }

                    @Override
                    public String getPrefix(String namespaceURI) {
                        return "ns";
                    }

                    @Override
                    public Iterator<String> getPrefixes(String namespaceURI) {
                        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                    }
                });
                NodeList packageNodes = (NodeList) xpath.evaluate("/ns:configuration/ns:packages/ns:package", configDocument, XPathConstants.NODESET);
                for (int i = 0; i < packageNodes.getLength(); i++) {
                    Element packageElement = (Element) packageNodes.item(i);
                    String packageName = packageElement.getAttribute("name");

                    tracer.out().printfIndentln("packageName = %s", packageName);

                    NodeList classNodes = (NodeList) xpath.evaluate("ns:classes/ns:class[@enabled='true']", packageElement, XPathConstants.NODESET);
                    for (int j = 0; j < classNodes.getLength(); j++) {
                        Element classElement = (Element) classNodes.item(j);
                        String simpleName = classElement.getAttribute("simpleName");
                        String qualifiedClassName = packageName + "." + simpleName;

                        tracer.out().printfIndentln("qualifiedClassName = %s", qualifiedClassName);

                        selectors.add(DiscoverySelectors.selectClass(Class.forName(qualifiedClassName)));
                    }
                }

                return selectors;
            } catch (XPathExpressionException | ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        } finally {
            tracer.wayout();
        }
    }

    void execute(List<DiscoverySelector> selectors) {
        AbstractTracer tracer = getCurrentTracer();
        tracer.entry("void", this, "execute(List<DiscoverySelector> selectors)");
        try {
            Launcher launcher = LauncherFactory.create();
            LauncherDiscoveryRequest launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
                    .selectors(selectors)
                    .build();
            launcher.registerTestExecutionListeners(new MyTestExecutionListener());
            launcher.execute(launcherDiscoveryRequest);
        } finally {
            tracer.wayout();
        }
    }

    @Override
    public AbstractTracer getCurrentTracer() {
        return TracerFactory.getInstance().getCurrentPoolTracer();
    }

    /**
     * @param args the command line arguments
     * @throws de.christofreichardt.diagnosis.TracerFactory.Exception
     */
    public static void main(String[] args) throws TracerFactory.Exception {
        TracerFactory.getInstance().reset();
        InputStream resourceAsStream = App.class.getClassLoader().getResourceAsStream("de/christofreichardt/shamirsweb/test/trace-config.xml");
        if (resourceAsStream != null) {
            TracerFactory.getInstance().readConfiguration(resourceAsStream);
        }
        TracerFactory.getInstance().openPoolTracer();

        try {
            System.out.printf("%s: Hallo!\n", Thread.currentThread().getName());

            AbstractTracer tracer = TracerFactory.getInstance().getCurrentPoolTracer();
            tracer.initCurrentTracingContext();
            tracer.entry("void", App.class, "main(String[] args)");

            System.getenv().keySet().stream()
                    .sorted()
                    .forEach(key -> tracer.out().printfIndentln("%s = %s", key, System.getenv(key)));
            tracer.out().printfIndentln("---");
            List<String> propertyNames = new ArrayList<>(System.getProperties().stringPropertyNames());
            propertyNames.stream()
                    .sorted()
                    .forEach((propertyName) -> tracer.out().printfIndentln("%s = %s", propertyName, System.getProperties().getProperty(propertyName)));
            
            try {
                App app = new App();
                Document configDocument = app.parseTestConfiguration();
                List<DiscoverySelector> configuredTests = app.configuredTests(configDocument);
                app.execute(configuredTests);
            } finally {
                tracer.wayout();
            }
        } finally {
            TracerFactory.getInstance().closePoolTracer();
        }
    }

}
