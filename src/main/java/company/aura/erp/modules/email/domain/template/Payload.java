package company.aura.erp.modules.email.domain.template;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Payload {
    Long orderId;
    Integer installmentNumber;
    BigDecimal amount;
    LocalDate installmentDueDate;
    String paymentMethod;
    String resetPasswordUrl;
    Long invoiceId;
    String invoiceNumber;
    String documentType;
}
