package company.aura.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Slf4j
@Configuration
@EnableConfigurationProperties(CloudflareR2Properties.class)
@RequiredArgsConstructor
public class CloudflareR2Config {

    private final CloudflareR2Properties properties;

    @Bean
    public S3Client s3Client() {
        String accessKey = properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()
                ? properties.getAccessKeyId()
                : "default-access-key";
        String secretKey = properties.getSecretAccessKey() != null && !properties.getSecretAccessKey().isBlank()
                ? properties.getSecretAccessKey()
                : "default-secret-key";

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        String endpointUri = properties.getEndpointUri();
        if (endpointUri != null && !endpointUri.isBlank()) {
            builder.endpointOverride(URI.create(endpointUri));
        } else {
            log.warn("Cloudflare R2 endpoint ou accountId não configurado no application.yml / variáveis de ambiente.");
        }

        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        String accessKey = properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()
                ? properties.getAccessKeyId()
                : "default-access-key";
        String secretKey = properties.getSecretAccessKey() != null && !properties.getSecretAccessKey().isBlank()
                ? properties.getSecretAccessKey()
                : "default-secret-key";

        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        String endpointUri = properties.getEndpointUri();
        if (endpointUri != null && !endpointUri.isBlank()) {
            builder.endpointOverride(URI.create(endpointUri));
        }

        return builder.build();
    }
}
