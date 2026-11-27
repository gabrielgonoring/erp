package company.aura.erp.modules.invoice.parser;

import lombok.Builder;
import lombok.Getter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Getter
@Builder
public class InvoiceXmlContext {
    private final Document document;
    private final Element nfeElement;
    private final Element infNFeElement;
    private final Element protNFeElement;
    private final XmlElementReader reader;
}
