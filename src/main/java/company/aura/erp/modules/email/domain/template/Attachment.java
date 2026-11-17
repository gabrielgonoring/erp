package company.aura.erp.modules.email.domain.template;

import company.aura.erp.modules.email.enumerate.AttachmentSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    private AttachmentSource source;
    private String url;
}
