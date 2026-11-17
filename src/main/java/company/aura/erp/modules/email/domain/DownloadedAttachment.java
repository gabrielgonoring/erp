package company.aura.erp.modules.email.domain;

import lombok.Data;

@Data
public class DownloadedAttachment {
    private String fileName;
    private byte[] data;
    private String contentType;
}
