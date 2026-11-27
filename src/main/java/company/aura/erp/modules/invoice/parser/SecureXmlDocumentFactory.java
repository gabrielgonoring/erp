package company.aura.erp.modules.invoice.parser;

import company.aura.erp.modules.invoice.exception.InvoiceXmlParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecureXmlDocumentFactory {

    private static final String TAG_NFE = "NFe";
    private static final String TAG_INF_NFE = "infNFe";
    private static final String TAG_PROT_NFE = "protNFe";

    private final XmlElementReader elementReader;

    public InvoiceXmlContext createContext(MultipartFile xmlFile) {
        log.info("createContext() - START - source=MultipartFile, fileName={}", xmlFile != null ? xmlFile.getOriginalFilename() : null);
        if (xmlFile == null || xmlFile.isEmpty()) {
            throw new InvoiceXmlParseException("O arquivo XML da nota fiscal é obrigatório e não pode estar vazio.");
        }
        try (InputStream inputStream = xmlFile.getInputStream()) {
            InvoiceXmlContext context = createContext(inputStream);
            log.info("createContext() - END - source=MultipartFile, fileName={}", xmlFile.getOriginalFilename());
            return context;
        } catch (IOException ex) {
            log.error("createContext() - ERROR - Failed to read MultipartFile stream - error: {}", ex.getMessage(), ex);
            throw new InvoiceXmlParseException("Não foi possível ler o arquivo XML enviado.", ex);
        }
    }

    public InvoiceXmlContext createContext(String xmlContent) {
        log.info("createContext() - START - source=String, length={}", xmlContent != null ? xmlContent.length() : 0);
        if (StringUtils.isBlank(xmlContent)) {
            throw new InvoiceXmlParseException("O conteúdo XML da nota fiscal não pode ser nulo ou vazio.");
        }
        String sanitized = stripUtf8Bom(xmlContent).trim();
        InvoiceXmlContext context = createContext(sanitized.getBytes(StandardCharsets.UTF_8));
        log.info("createContext() - END - source=String");
        return context;
    }

    public InvoiceXmlContext createContext(byte[] xmlBytes) {
        log.info("createContext() - START - source=byte[], size={}", xmlBytes != null ? xmlBytes.length : 0);
        if (xmlBytes == null || xmlBytes.length == 0) {
            throw new InvoiceXmlParseException("Os bytes do XML da nota fiscal não podem ser nulos ou vazios.");
        }
        InvoiceXmlContext context = createContext(new ByteArrayInputStream(xmlBytes));
        log.info("createContext() - END - source=byte[]");
        return context;
    }

    public InvoiceXmlContext createContext(InputStream inputStream) {
        log.info("createContext() - START - source=InputStream");
        if (inputStream == null) {
            throw new InvoiceXmlParseException("O stream de entrada do XML da nota fiscal não pode ser nulo.");
        }

        try {
            byte[] rawBytes = inputStream.readAllBytes();
            if (rawBytes.length == 0) {
                throw new InvoiceXmlParseException("O XML da nota fiscal está vazio.");
            }
            byte[] cleanBytes = stripBomBytes(rawBytes);

            DocumentBuilder builder = createSecureDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(cleanBytes));
            document.getDocumentElement().normalize();

            Element nfeElement = findElementByLocalName(document, TAG_NFE);
            Element infNFeElement = findElementByLocalName(document, TAG_INF_NFE);
            if (infNFeElement == null) {
                throw new InvoiceXmlParseException("XML inválido: tag obrigatória <infNFe> não encontrada na nota fiscal.");
            }
            Element protNFeElement = findElementByLocalName(document, TAG_PROT_NFE);

            InvoiceXmlContext context = InvoiceXmlContext.builder()
                    .document(document)
                    .nfeElement(nfeElement)
                    .infNFeElement(infNFeElement)
                    .protNFeElement(protNFeElement)
                    .reader(elementReader)
                    .build();

            log.info("createContext() - END - infNFeId={}, hasProtNFe={}",
                    elementReader.getAttribute(infNFeElement, "Id"), protNFeElement != null);
            return context;
        } catch (InvoiceXmlParseException ex) {
            throw ex;
        } catch (SAXException | IOException | ParserConfigurationException ex) {
            log.error("createContext() - ERROR - Failed to parse invoice XML - error: {}", ex.getMessage(), ex);
            throw new InvoiceXmlParseException("Falha ao realizar o parsing do XML da nota fiscal: " + ex.getMessage(), ex);
        }
    }

    private DocumentBuilder createSecureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder();
    }

    private Element findElementByLocalName(Document document, String localName) {
        NodeList byNs = document.getElementsByTagNameNS("*", localName);
        if (byNs.getLength() > 0) {
            return (Element) byNs.item(0);
        }
        NodeList byTag = document.getElementsByTagName(localName);
        if (byTag.getLength() > 0) {
            return (Element) byTag.item(0);
        }
        return null;
    }

    private String stripUtf8Bom(String content) {
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    private byte[] stripBomBytes(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            byte[] stripped = new byte[bytes.length - 3];
            System.arraycopy(bytes, 3, stripped, 0, stripped.length);
            return stripped;
        }
        return bytes;
    }
}
