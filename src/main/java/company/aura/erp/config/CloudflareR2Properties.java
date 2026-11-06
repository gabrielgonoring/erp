package company.aura.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudflare.r2")
public class CloudflareR2Properties {

    private String accountId;
    private String accessKeyId;
    private String secretAccessKey;
    private String bucketName;
    private String publicUrl;
    private String endpoint;

    /**
     * Se true, ignora a validação SSL/TLS na conexão com o R2.
     * Útil em ambiente de desenvolvimento atrás de proxy corporativo com inspeção SSL.
     */
    private Boolean trustAllCertificates = false;

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
