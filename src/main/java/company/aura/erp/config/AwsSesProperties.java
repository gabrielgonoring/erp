package company.aura.erp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aws.ses")
public class AwsSesProperties {

    /**
     * AWS Access Key ID. Se vazio, o SDK usará a cadeia de credenciais padrão (DefaultCredentialsProvider:
     * variáveis de ambiente AWS_ACCESS_KEY_ID, IAM Roles, ~/.aws/credentials, etc).
     */
    private String accessKeyId;

    /**
     * AWS Secret Access Key.
     */
    private String secretAccessKey;

    /**
     * Região da AWS onde o SES está configurado (ex: us-east-1, sa-east-1).
     */
    private String region = "us-east-1";

    /**
     * Endereço de e-mail do remetente padrão (deve ser verificado no Amazon SES).
     */
    private String fromEmail;

    /**
     * Nome de exibição do remetente (ex: Aura ERP).
     */
    private String fromName;

    /**
     * Configuration Set opcional do SES para rastreamento de métricas e reputação.
     */
    private String configurationSet;

    /**
     * Endpoint customizado opcional (útil para LocalStack ou testes locais).
     */
    private String endpoint;

    /**
     * Habilita ou desabilita o envio real via Amazon SES (se false, apenas loga a mensagem).
     */
    private Boolean enabled = true;

    /**
     * Retorna o endereço formatado no padrão "Nome <email@dominio.com>" ou apenas "email@dominio.com".
     */
    public String getFormattedFrom() {
        if (fromEmail == null || fromEmail.isBlank()) {
            return null;
        }
        if (fromName != null && !fromName.isBlank()) {
            return String.format("%s <%s>", fromName.trim(), fromEmail.trim());
        }
        return fromEmail.trim();
    }
}
