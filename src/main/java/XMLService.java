import cz.abclinuxu.datoveschranky.common.Config;
import cz.abclinuxu.datoveschranky.common.DataBoxEnvironment;
import cz.abclinuxu.datoveschranky.common.Utils;
import cz.abclinuxu.datoveschranky.common.entities.content.FileContent;
import cz.abclinuxu.datoveschranky.impl.MessageValidator;
import cz.abclinuxu.datoveschranky.ws.dm.TReturnedMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URISyntaxException;

public class XMLService {

    public <T> void LOG(T t) {
        System.out.println((String)t);
    }
    public void process(String zfopth, int attachmentSize) throws DataboxException {

        try {
            File zfo = new File(zfopth);
            FileContent fc = new FileContent(zfo);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Utils.copy(fc.getInputStream(), bos);
            TReturnedMessage tmsg = (new MessageValidator(new Config(DataBoxEnvironment.TEST))).getTReturnedMessage(bos.toByteArray());

            // swap content of two nodes
            String dmSender = tmsg.getDmDm().getDmSender() ;
            tmsg.getDmDm().setDmSender(tmsg.getDmDm().getDmID());
            tmsg.getDmDm().setDmID(dmSender);

            // set count of contents manually
            tmsg.setDmAttachmentSize(BigInteger.valueOf(attachmentSize)); ;

            // start
            JAXBContext context = JAXBContext.newInstance(TReturnedMessage.class);
            Marshaller marshaller = context.createMarshaller();

            // add root
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            QName qName = new QName("root");
            JAXBElement<TReturnedMessage> root = new JAXBElement<>(qName, TReturnedMessage.class, tmsg);

            // converting to Document
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            marshaller.marshal(root, document);

            // remove not required elements
            deleteElementsByTagNameNS("dmFiles",document) ;
            deleteElementsByTagNameNS("dmQTimestamp",document);

            // saving to file
            saveToFile("dmdm.xml",document) ;

        } catch (Exception e) {
            throw new DataboxException(e) ;
        }

    }

    private void deleteElementsByTagNameNS(String name, Document document) throws DataboxException {
        NodeList nodeList = document.getElementsByTagNameNS("*",name) ;
        if (nodeList.getLength() == 0) throw new DataboxException("NO SUCH AN ELEMENT") ;
        for(int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i) ;
            Node parent = element.getParentNode();
            parent.removeChild(element);
            parent.normalize();
        }
    }

    private void saveToFile(String name, Document document) throws DataboxException {
        try {
            DOMSource source = new DOMSource(document);
            FileWriter writer = new FileWriter(new File(name));
            StreamResult result = new StreamResult(writer);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
/*            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");*/
            transformer.transform(source, result);
        } catch (Exception e) {
            throw new DataboxException(e) ;
        }

    }


}
