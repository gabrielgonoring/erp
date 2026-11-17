package company.aura.erp.modules.email.enumerate;

import company.aura.erp.modules.email.domain.template.Customer;
import company.aura.erp.modules.email.domain.template.NotificationRequest;
import company.aura.erp.modules.email.domain.template.Payload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;


@Getter
@AllArgsConstructor
public enum Field {
    CUSTOMER_NAME("customerName", request -> Field.getStringInCustomer(request, Customer::getName, "Cliente")),
    ORDER_ID("orderId", request -> Field.getNumberInPayload(request, Payload::getOrderId)),
    AMOUNT("amount", request -> Field.getAmountInPayload(request, Payload::getAmount)),
    INSTALLMENT_NUMBER("installmentNumber", request -> Field.getNumberInPayload(request, Payload::getInstallmentNumber)),
    INSTALLMENT_DUE_DATE("installmentDueDate", request -> Field.getDateInPayload(request, Payload::getInstallmentDueDate)),
    PAYMENT_METHOD("paymentMethod", request->  getStringInPayload(request, Payload::getPaymentMethod, "Não informado")),
    RESET_PASSWORD_URL("changePasswordUrl", request->  getStringInPayload(request, Payload::getResetPasswordUrl, ""));

    private final String replaceCode;
    private Function<NotificationRequest, String> retriveValue;

    public String retrieveValueInRequest(NotificationRequest notificationRequest){
        return retriveValue.apply(notificationRequest);
    }

    private static String getAmountInPayload(NotificationRequest notificationRequest, Function<Payload, BigDecimal> getValueFunction){
        if (notificationRequest.getPayload()==null) {
            return "Não informado";
        }

        BigDecimal retrievedValue = getValueFunction.apply(notificationRequest.getPayload());

        if (retrievedValue==null) {
            return "Não informado";
        }

        return formatNumber(retrievedValue);
    }

    private static String getNumberInPayload(NotificationRequest notificationRequest, Function<Payload, Number> getValueFunction){
        if (notificationRequest.getPayload()==null) {
            return "Não informado";
        }

        Number retrievedValue = getValueFunction.apply(notificationRequest.getPayload());

        if (retrievedValue==null) {
            return "Não informado";
        }

        return retrievedValue.toString();
    }

    private static String getDateInPayload(NotificationRequest notificationRequest, Function<Payload, LocalDate> getValueFunction){
        if (notificationRequest.getPayload()==null) {
            return "Não informado";
        }

        LocalDate retrievedValue = getValueFunction.apply(notificationRequest.getPayload());

        if (retrievedValue==null) {
            return "Não informado";
        }

        return formatDate(retrievedValue);
    }

    private static String getStringInPayload(NotificationRequest notificationRequest, Function<Payload, String> getValueFunction, String defaultValue){
        if (notificationRequest.getPayload()==null)
            return defaultValue;

        String retrievedValue = getValueFunction.apply(notificationRequest.getPayload());

        return StringUtils.defaultIfBlank(retrievedValue, defaultValue);
    }

    private static String getStringInCustomer(NotificationRequest notificationRequest, Function<Customer, String> getValueFunction, String defaultValue){
        if (notificationRequest.getCustomer()==null)
            return defaultValue;

        String retrievedValue = getValueFunction.apply(notificationRequest.getCustomer());

        return StringUtils.defaultIfBlank(retrievedValue, defaultValue);
    }


    private static String formatNumber(BigDecimal decimal){
        return String.format(Locale.GERMAN, "%.2f", decimal);
    }

    private static String formatDate(LocalDate date){
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}