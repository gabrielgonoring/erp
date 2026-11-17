package company.aura.erp.modules.email.domain;

import lombok.Data;

import java.util.List;

@Data
public class NotificationRequest {
    private String receiveEmail;
    private String receivePhoneNumber;
    private Company company;
    private Customer customer;
    private Payload payload;
    private List<Attachment> attachment;
}
