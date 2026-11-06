package company.aura.erp.service;

import company.aura.erp.config.CloudflareR2Properties;
import company.aura.erp.dto.storage.R2FileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudflareR2Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final CloudflareR2Properties properties;

    
    private String getBucketName() {
        String bucket = properties.getBucketName();
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("O nome do bucket Cloudflare R2 não foi configurado (CLOUDFLARE_R2_BUCKET_NAME).");
        }
        return bucket.trim();
    }
    
    public String upload(MultipartFile file) {
        return upload(file, null);
    }
    
    public String upload(MultipartFile file, String folder) {

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O arquivo para upload não pode estar vazio.");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );


        String uniqueKey = UUID.randomUUID() + "-" + originalFilename;

        if (folder != null && !folder.isBlank()) {
            String sanitizedFolder = folder.replaceAll("^/+|/+$", "");
            uniqueKey = sanitizedFolder + "/" + uniqueKey;
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        try {
            upload(uniqueKey, file.getInputStream(), file.getSize(), contentType);
            return uniqueKey;
        } catch (IOException e) {
            log.error("Erro ao ler o InputStream do arquivo para upload: {}", originalFilename, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao processar o arquivo para upload.", e);
        }
    }
    
    public String upload(String key, byte[] content, String contentType) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chave do arquivo não pode ser nula ou vazia.");
        }
        if (content == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O conteúdo do arquivo não pode ser nulo.");
        }

        String mimeType = (contentType != null && !contentType.isBlank())
                ? contentType
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .contentType(mimeType)
                    .contentLength((long) content.length)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(content));
            log.info("Upload realizado com sucesso no R2: bucket='{}', key='{}', tamanho={} bytes", getBucketName(), key, content.length);
            return key;
        } catch (S3Exception e) {
            log.error("Erro no upload para o Cloudflare R2 (key='{}'): {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar arquivo", e);
        }
    }
    
    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chave do arquivo não pode ser nula ou vazia.");
        }
        if (inputStream == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O InputStream do arquivo não pode ser nulo.");
        }

        String mimeType = (contentType != null && !contentType.isBlank())
                ? contentType
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .contentType(mimeType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            log.info("Upload de stream realizado com sucesso no R2: bucket='{}', key='{}', tamanho={} bytes", getBucketName(), key, contentLength);
            return key;
        } catch (S3Exception e) {
            log.error("Erro no upload de stream para o Cloudflare R2 (key='{}'): {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar arquivo", e);
        }
    }
    
    public byte[] download(String key) {
        validateKey(key);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObjectAsBytes(request).asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("Arquivo não encontrado no Cloudflare R2: key='{}'", key);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no bucket: " + key, e);
        } catch (S3Exception e) {
            log.error("Erro ao buscar arquivo '{}' no Cloudflare R2: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar arquivo", e);
        }
    }


    public ResponseInputStream<GetObjectResponse> getFileInputStream(String key) {
        validateKey(key);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        } catch (NoSuchKeyException e) {
            log.warn("Arquivo não encontrado no Cloudflare R2: key='{}'", key);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no bucket: " + key, e);
        } catch (S3Exception e) {
            log.error("Erro ao obter stream do arquivo '{}' no Cloudflare R2: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao obter stream do arquivo", e);
        }
    }
    
    public List<R2FileInfo> searchFiles(String prefix) {
        try {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(getBucketName());

            if (prefix != null && !prefix.isBlank()) {
                builder.prefix(prefix.trim());
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(builder.build());
            if (response.contents() == null || response.contents().isEmpty()) {
                return Collections.emptyList();
            }

            return response.contents().stream()
                    .map(obj -> new R2FileInfo(
                            obj.key(),
                            obj.size(),
                            obj.lastModified(),
                            obj.eTag()
                    ))
                    .toList();
        } catch (S3Exception e) {
            log.error("Erro ao listar arquivos com prefixo '{}'", prefix, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao listar arquivos", e);
        }
    }


    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("Erro ao verificar existência do arquivo '{}' no Cloudflare R2: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao verificar existência do arquivo", e);
        }
    }


    public String getFileUrl(String key) {
        validateKey(key);
        String publicUrl = properties.getPublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            String baseUrl = publicUrl.replaceAll("/+$", "");
            String cleanKey = key.replaceAll("^/+", "");
            return baseUrl + "/" + cleanKey;
        }
        return generatePresignedUrl(key, Duration.ofMinutes(60));
    }

    public String generatePresignedUrl(String key, Duration duration) {
        validateKey(key);
        Duration expiration = (duration != null && !duration.isNegative() && !duration.isZero())
                ? duration
                : Duration.ofMinutes(60);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (S3Exception e) {
            log.error("Erro ao gerar URL pré-assinada para a chave '{}': {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar URL pré-assinada para o arquivo", e);
        }
    }

   
    public void delete(String key) {
        validateKey(key);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("Arquivo '{}' excluído com sucesso do bucket '{}'", key, getBucketName());
        } catch (S3Exception e) {
            log.error("Erro ao excluir arquivo '{}' do Cloudflare R2: {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir arquivo", e);
        }
    }

   
    public void deleteMany(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }

        try {
            List<ObjectIdentifier> objectIdentifiers = keys.stream()
                    .filter(k -> k != null && !k.isBlank())
                    .map(k -> ObjectIdentifier.builder().key(k).build())
                    .toList();

            if (objectIdentifiers.isEmpty()) {
                return;
            }

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(getBucketName())
                    .delete(Delete.builder().objects(objectIdentifiers).build())
                    .build();

            s3Client.deleteObjects(request);
            log.info("Lote de {} arquivos excluído com sucesso do bucket '{}'", objectIdentifiers.size(), getBucketName());
        } catch (S3Exception e) {
            log.error("Erro ao excluir lote de arquivos do Cloudflare R2: {}", e.awsErrorDetails().errorMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir lote de arquivos", e);
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chave do arquivo não pode ser nula ou vazia.");
        }
    }
}
