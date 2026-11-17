package company.aura.erp.modules.email.enumerate;

import company.aura.erp.modules.email.domain.NotificationRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static company.aura.erp.modules.email.enumerate.Field.*;
@Getter
@AllArgsConstructor
public enum NotificationType {
    INSTALLMENT_NOTIFY(List.of(CUSTOMER_NAME,ORDER_ID, AMOUNT,INSTALLMENT_NUMBER,INSTALLMENT_DUE_DATE,PAYMENT_METHOD)),
    FORGOT_PASSWORD(List.of(CUSTOMER_NAME, RESET_PASSWORD_URL));

    private final List<Field> fields;

    public Map<String, String> buildReplacer(NotificationRequest notificationRequest){
        return this.fields.stream().collect(Collectors.toMap(Field::getReplaceCode, field -> field.retrieveValueInRequest(notificationRequest)));
    }
}
