package company.aura.erp.modules.invoice.parser;

@FunctionalInterface
public interface InvoiceXmlSectionExtractor<T> {
    T extract(InvoiceXmlContext context);
}
