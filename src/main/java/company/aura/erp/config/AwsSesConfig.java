package company.aura.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.SesClientBuilder;

import java.net.URI;

@Slf4j
@Configuration
@EnableConfigurationProperties(AwsSesProperties.class)
@RequiredArgsConstructor
public class AwsSesConfig {

    private final AwsSesProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public SesClient sesClient() {
        String regionName = (properties.getRegion() != null && !properties.getRegion().isBlank())
                ? properties.getRegion().trim()
                : "us-east-1";

        log.info("Inicializando SesClient para região AWS: {}", regionName);

        AwsCredentialsProvider credentialsProvider;
        if (properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()
                && properties.getSecretAccessKey() != null && !properties.getSecretAccessKey().isBlank()) {
            log.info("Usando credenciais estáticas configuradas para Amazon SES.");
            credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKeyId().trim(), properties.getSecretAccessKey().trim())
            );
        } else {
            log.info("Credenciais estáticas não fornecidas explicitamente. Utilizando DefaultCredentialsProvider para Amazon SES.");
            credentialsProvider = DefaultCredentialsProvider.create();
        }

        SesClientBuilder builder = SesClient.builder()
                .region(Region.of(regionName))
                .credentialsProvider(credentialsProvider)
                .httpClientBuilder(ApacheHttpClient.builder());

        String endpoint = properties.getEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            log.info("Configurando endpoint customizado para Amazon SES: {}", endpoint);
            builder.endpointOverride(URI.create(endpoint.trim()));
        }

        return builder.build();
    }
}
