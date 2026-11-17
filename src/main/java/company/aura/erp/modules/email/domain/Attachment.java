package company.aura.erp.modules.email.domain;

import company.aura.erp.modules.email.enumerate.AttachmentSource;
import lombok.Data;

@Data
public class Attachment {
    private AttachmentSource source;
    private String url;
}
