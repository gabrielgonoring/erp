package company.aura.erp.modules.invoice.exception;

public class InvoiceXmlParseException extends RuntimeException {

    public InvoiceXmlParseException() {
    }

    public InvoiceXmlParseException(String message) {
        super(message);
    }

    public InvoiceXmlParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvoiceXmlParseException(Throwable cause) {
        super(cause);
    }
}
