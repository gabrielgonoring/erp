package company.aura.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "cloudflare.r2")
public class CloudflareR2Properties {

    private String accountId;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;
    private String publicUrl;
    private String endpoint;

    public String getEndpointUri() {
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint.trim();
        }
        if (accountId != null && !accountId.isBlank()) {
            return "https://" + accountId.trim() + ".r2.cloudflarestorage.com";
        }
        return null;
    }
}
