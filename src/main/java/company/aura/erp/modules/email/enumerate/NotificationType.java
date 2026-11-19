package company.aura.erp.modules.email.enumerate;

import company.aura.erp.modules.email.domain.template.NotificationRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static company.aura.erp.modules.email.enumerate.Field.*;

@Getter
@AllArgsConstructor
public enum NotificationType {
    DEFAULT(false, "Padrão", Collections.emptyList()),
    INSTALLMENT_NOTIFY(false, "Cobrança em breve", List.of(CUSTOMER_NAME, ORDER_ID, AMOUNT, INSTALLMENT_NUMBER, INSTALLMENT_DUE_DATE, PAYMENT_METHOD)),
    FORGOT_PASSWORD(true, "Recuperação de senha", List.of(CUSTOMER_NAME, RESET_PASSWORD_URL)),
    INVOICE_LINKED(false, "Nota fiscal vinculada", List.of(CUSTOMER_NAME, COMPANY_NAME, ORDER_ID, INVOICE_NUMBER, DOCUMENT_TYPE, AMOUNT));

    private final Boolean internal;
    private final String humanDescription;
    private final List<Field> fields;

    public Map<String, String> buildReplacer(NotificationRequest notificationRequest) {
        return this.fields.stream().collect(Collectors.toMap(Field::getReplaceCode, field -> field.retrieveValueInRequest(notificationRequest)));
    }
}
