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

    /**
     * Account ID da Cloudflare.
     * Utilizado para compor o endpoint padrão: https://<accountId>.r2.cloudflarestorage.com
     */
    private String accountId;

    /**
     * R2 Access Key ID (chave de acesso da API R2).
     */
    private String accessKeyId;

    /**
     * R2 Secret Access Key (segredo da API R2).
     */
    private String secretAccessKey;

    /**
     * Nome do bucket no Cloudflare R2.
     */
    private String bucketName;

    /**
     * URL pública ou domínio customizado associado ao bucket (opcional).
     * Exemplo: https://cdn.aura.company ou https://pub-xxx.r2.dev
     */
    private String publicUrl;

    /**
     * Endpoint customizado (opcional). Se informado, sobrescreve a URL base do endpoint S3/R2.
     */
    private String endpoint;

    /**
     * Retorna a URI resolvida do endpoint do Cloudflare R2.
     */
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
