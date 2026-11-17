package company.aura.erp.modules.email.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadedAttachment {
    private String fileName;
    private byte[] data;
    private String contentType;
}
