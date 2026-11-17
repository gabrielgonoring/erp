package company.aura.erp.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.job")
public class JobSecurityProperties {

    /**
     * Chave de API para autenticação nos endpoints de Jobs (/api/jobs/**).
     */
    private String apiKey = "aura-erp-job-secret-key-default";

    /**
     * Nome do cabeçalho HTTP esperado para a API Key. Padrão: X-API-KEY.
     */
    private String headerName = "X-API-KEY";
}
