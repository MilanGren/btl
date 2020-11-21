import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.MarshalException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import com.sun.xml.ws.util.StringUtils;
import cz.abclinuxu.datoveschranky.common.ByteArrayAttachmentStorer;
import cz.abclinuxu.datoveschranky.common.Config;
import cz.abclinuxu.datoveschranky.common.DataBoxEnvironment;
import cz.abclinuxu.datoveschranky.common.DataBoxException;
import cz.abclinuxu.datoveschranky.common.Utils;
import cz.abclinuxu.datoveschranky.common.entities.Message;
import cz.abclinuxu.datoveschranky.common.entities.content.ByteContent;
import cz.abclinuxu.datoveschranky.common.entities.content.FileContent;
import cz.abclinuxu.datoveschranky.common.interfaces.AttachmentStorer;
import cz.abclinuxu.datoveschranky.impl.MessageValidator;
import cz.abclinuxu.datoveschranky.impl.Validator;
import cz.abclinuxu.datoveschranky.ws.dm.TMessDownOutput;
import cz.abclinuxu.datoveschranky.ws.dm.TReturnedMessage;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class App {

    public <T> void LOG(T t) {
        System.out.println((String)t);
    }

    public class X {
        public List<String> list = new ArrayList<>() ;
        public void set() {
            list.add("0") ;
            list.add("1") ;
            list.add("2") ;
            list.add("3") ;
            list.add("4") ;
        }
    }

    public void TestStream() throws Exception {
        App.X app = new App.X() ;
        app.set();
        String x = app.list.stream().filter(t -> t.compareTo("2") == 0).findAny().orElseThrow(() -> new Exception("MSG"));
        System.out.println(x) ;
        List<Boolean> blist = new ArrayList<>() ;
        blist.add(new Boolean(true)) ;
        blist.add(new Boolean(false)) ;
        blist.add(new Boolean(true)) ;
        List<Boolean> selectedTrues = blist.stream().filter(t -> t.equals(Boolean.valueOf(true))).collect(Collectors.toList());
        boolean noVirus = blist.stream().filter(t -> t.equals(Boolean.valueOf(false))).findAny().isEmpty() ;
    }


    @XmlType(propOrder = { "name","salary","info" })
    //@XmlRootElement
    public static class Employee {
        private int id;
        private String name;
        private float salary;
        private String info ;

        public Employee() {}
        public Employee(int id, String name, float salary) {
            super();
            this.id = id;
            this.name = name;
            this.salary = salary;
        }
        @XmlAttribute
        public int getId() {
            return id;
        }
        public void setId(int id) {
            this.id = id;
        }
        @XmlElement(name="TEST", namespace = "nejmspejs")
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        @XmlElement
        public float getSalary() {
            return salary;
        }
        public void setSalary(float salary) {
            this.salary = salary;
        }
        @XmlElement
        public String getInfo() {
            return info;
        }
        public void setInfo(String info) {
            this.info = info;
        }
    }
    public void TestCreateXMLbasic() throws Exception {
        JAXBContext contextObj = JAXBContext.newInstance(Employee.class);
        Marshaller marshallerObj = contextObj.createMarshaller();
        marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        Employee emp1 = new Employee(1,"Vimal Jaiswal",50000);
        try {
            marshallerObj.marshal(emp1, new FileOutputStream("employee.xml"));
        } catch (MarshalException e) {
            LOG(e.toString()) ;
            LOG("will add root element to fix it") ;
            QName qName = new QName("root");
            JAXBElement<Employee> root = new JAXBElement<>(qName, Employee.class, emp1);
            marshallerObj.marshal(root, new FileOutputStream("employee.xml"));

            LOG("converting to Document") ;
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            marshallerObj.marshal(root, document) ;

            LOG("printing root attributes") ;
            NamedNodeMap attributes = document.getDocumentElement().getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node node = attributes.item(i);
                    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                        String name = node.getNodeName();
                        System.out.println(name + " " + node.getNamespaceURI());
                    }
                }
            }

            LOG("now will delete selected elements") ;
            NodeList nodeList = document.getElementsByTagName("ns2:TEST") ;
            if (nodeList.getLength() == 0) throw new Exception("NO SUCH AN ELEMENT") ;
            for(int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i) ;
                Node parent = element.getParentNode();
                parent.removeChild(element);
                parent.normalize();
            }

            LOG("set conversion to file and set some attributes") ;
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            DOMSource source = new DOMSource(document) ;
            FileWriter writer = new FileWriter(new File("employee-changed.xml"));
            StreamResult result = new StreamResult(writer);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            LOG("finalising") ;
            transformer.transform(source, result);

            LOG("done") ;
        }
    }


    static class Http {

        private String getHeader (String domain, String username, String password){
            String auth;
            if (StringUtils.isBlank(domain)) {
                auth = String.format("%s:%s", username, password);
            } else {
                auth = String.format("%s@%s:%s", username, domain, password);  // milan@client1:password
            }
            System.out.println(auth);
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.US_ASCII));
            return "Basic " + new String(encodedAuth);
        }

        public org.springframework.http.HttpHeaders getJsonHeaders (String domain, String username, String password){
            org.springframework.http.HttpHeaders jsonHeaders = new org.springframework.http.HttpHeaders();
            String authHeader = getHeader(domain, username, password);
            jsonHeaders.set("Authorization", authHeader);
            jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
            return jsonHeaders;
        }

    }

    static class RunCmd {

        void run() throws Exception {

            ProcessBuilder builder = new ProcessBuilder();
            builder.command("bash", "-c", "gs -h");
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();
            log.info("exit code: " + exitCode);

            if (exitCode != 0) {
                try (final BufferedReader b = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    if ((line = b.readLine()) != null)
                        System.out.println(line);
                } catch (final IOException ee) {
                    ee.printStackTrace();
                }
                throw new Exception("shell command run error");
            }

        }

        private static class StreamGobbler implements Runnable {
            private InputStream inputStream;
            private Consumer<String> consumer;

            public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
                this.inputStream = inputStream;
                this.consumer = consumer;
            }

            @Override
            public void run() {
                new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(consumer);
            }
        }


    }

    public static void main(String[] args) throws Exception {
/*        (new App()).TestCreateXMLbasic();
        XMLService xmlService = new XMLService() ;

        Http http = new Http() ;
        http.getJsonHeaders("client1","mgren","heslo") ;

        xmlService.process("src/main/resources/examples/ODZ_with_xml_attachment.zfo",999999);

        new RunCmd().run();*/




        new Ekg().run();



    }
}
