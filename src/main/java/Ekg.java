import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import org.apache.commons.codec.digest.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

@Slf4j
public class Ekg {

    public void testValidate() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new FileInputStream(Paths.get("/home/gre/Downloads/testXML.xml").toFile().getPath())) ;
        testValidity(doc,"/home/gre/Downloads/testXSD.xsd") ;
    }

    public void run() throws Exception {
        int N = 1000;

        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(new FileInputStream(Paths.get("/home/gre/Downloads/EB0000088344_D.xml").toFile().getPath()));

        try {
            testValidity(document, "/home/gre/java/etc/stream/btl/xsd/BtlXml.xsd");
        } catch (Exception e) {
            throw new Exception("xsd validation failed", e);
        }

        XPath xPath = XPathFactory.newInstance().newXPath();

        Node node = ((NodeList) xPath.compile("/exportHeader/patient/examination/rawData/signal/wave[@lead='II']").evaluate(document,
            XPathConstants.NODESET)).item(0) ;

        if (node == null) {
            throw new Exception("can not find data for next processing");
        }

        log.info(node.getTextContent());

        String[] allValues = node.getTextContent().split(" ") ;
        if (allValues.length < N) throw new Exception("CAN NOT..") ;
        StringBuilder considered = new StringBuilder(1000);
        for (int i = 0; i < N; i++) {
            considered.append(allValues[i]) ;
            considered.append(" ") ;
        }
        String sha256Hex = DigestUtils.sha256Hex(considered.toString());

        Node parent = node.getParentNode().getParentNode().getParentNode();
        Element newElement = document.createElement("NEW") ;
        newElement.setTextContent(sha256Hex);
        parent.appendChild(newElement) ;

        saveToFile(document,new FileOutputStream(Paths.get("output.xml").toFile().getPath()) );

        log.info("DONE");

    }

    private void testValidity(Document doc, String pth) throws IOException, SAXException {
        // create a SchemaFactory capable of understanding WXS schemas
        SchemaFactory schemaFactoryfactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        // load a WXS schema, represented by a Schema instance
        Source schemaFile = new StreamSource(new File(pth));
        Schema schema = schemaFactoryfactory.newSchema(schemaFile);
        // create a Validator instance, which can be used to validate an instance document
        Validator validator = schema.newValidator();
        // validate the DOM tree
        Result r = new DOMResult() ;
        validator.validate(new DOMSource(doc),r);
    }

    private void saveToFile(Document document, OutputStream fos) throws DataboxException {
        try {
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(fos);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new DataboxException(e);
        }

    }

}
