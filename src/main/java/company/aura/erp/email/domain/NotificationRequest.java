package company.aura.erp.email.domain;

import lombok.Data;

@Data
public class NotificationRequest {
    private String receiveEmail;
    private String receivePhoneNumber;
    private Company company;
    private Customer customer;
    private Payload payload;
}
