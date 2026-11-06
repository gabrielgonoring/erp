package company.aura.erp.dto.invoice;

import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceCreateRequest(
        @NotNull(message = "O ID do parceiro (cliente/destinatário) é obrigatório.")
        Long partnerId,

        @NotNull(message = "O valor total da nota fiscal é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor total deve ser maior que zero.")
        BigDecimal totalAmount,

        DocumentType documentType,

        EnvironmentType environment,

        String status,

        Integer operationType,

        Integer invoiceNumber,

        Integer series,

        String accessKey,

        String authorizationProtocol,

        String paymentMethod,

        LocalDate dueDate,

        Integer freightMode,

        BigDecimal freightAmount,

        BigDecimal additionalExpenses,

        BigDecimal productsAmount,

        BigDecimal discountAmount,

        String additionalInformation,

        String externalReference,

        LocalDateTime issuedAt
) {}
