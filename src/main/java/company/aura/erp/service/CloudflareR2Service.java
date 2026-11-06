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
        log.info("upload() - START - originalFilename={}", file != null ? file.getOriginalFilename() : null);
        String key = upload(file, null);
        log.info("upload() - END - key={}", key);
        return key;
    }

    public String upload(MultipartFile file, String folder) {
        log.info("upload() - START - originalFilename={}, folder={}",
                file != null ? file.getOriginalFilename() : null, folder);

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
            log.info("upload() - END - key={}", uniqueKey);
            return uniqueKey;
        } catch (IOException e) {
            log.error("upload() - ERROR - Error when reading file input stream - originalFilename: {}, error: {}", originalFilename, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao processar o arquivo para upload.", e);
        }
    }

    public String upload(String key, byte[] content, String contentType) {
        log.info("upload() - START - key={}, size={}, contentType={}", key, content != null ? content.length : null, contentType);

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
            log.info("upload() - END - key={}", key);
            return key;
        } catch (S3Exception e) {
            log.error("upload() - ERROR - Error when uploading bytes to bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar arquivo", e);
        }
    }

    public String upload(String key, InputStream inputStream, long contentLength, String contentType) {
        log.info("upload() - START - key={}, contentLength={}, contentType={}", key, contentLength, contentType);

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
            log.info("upload() - END - key={}", key);
            return key;
        } catch (S3Exception e) {
            log.error("upload() - ERROR - Error when uploading stream to bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao enviar arquivo", e);
        }
    }

    public byte[] download(String key) {
        log.info("download() - START - key={}", key);
        validateKey(key);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            byte[] bytes = s3Client.getObjectAsBytes(request).asByteArray();
            log.info("download() - END - key={}, size={}", key, bytes.length);
            return bytes;
        } catch (NoSuchKeyException e) {
            log.warn("download() - ERROR - File not found in bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no bucket: " + key, e);
        } catch (S3Exception e) {
            log.error("download() - ERROR - Error when downloading file from bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao buscar arquivo", e);
        }
    }

    public ResponseInputStream<GetObjectResponse> getFileInputStream(String key) {
        log.info("getFileInputStream() - START - key={}", key);
        validateKey(key);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(request);
            log.info("getFileInputStream() - END - key={}", key);
            return responseInputStream;
        } catch (NoSuchKeyException e) {
            log.warn("getFileInputStream() - ERROR - File not found in bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no bucket: " + key, e);
        } catch (S3Exception e) {
            log.error("getFileInputStream() - ERROR - Error when getting file stream from bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao obter stream do arquivo", e);
        }
    }

    public List<R2FileInfo> searchFiles(String prefix) {
        log.info("searchFiles() - START - prefix={}", prefix);
        try {
            ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(getBucketName());

            if (prefix != null && !prefix.isBlank()) {
                builder.prefix(prefix.trim());
            }

            ListObjectsV2Response response = s3Client.listObjectsV2(builder.build());
            if (response.contents() == null || response.contents().isEmpty()) {
                log.info("searchFiles() - END - prefix={}, totalFiles=0", prefix);
                return Collections.emptyList();
            }

            List<R2FileInfo> files = response.contents().stream()
                    .map(obj -> new R2FileInfo(
                            obj.key(),
                            obj.size(),
                            obj.lastModified(),
                            obj.eTag()
                    ))
                    .toList();

            log.info("searchFiles() - END - prefix={}, totalFiles={}", prefix, files.size());
            return files;
        } catch (S3Exception e) {
            log.error("searchFiles() - ERROR - Error when searching files in bucket - prefix: {}, error: {}", prefix, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao listar arquivos", e);
        }
    }

    public boolean exists(String key) {
        log.info("exists() - START - key={}", key);
        if (key == null || key.isBlank()) {
            log.info("exists() - END - key={}, exists=false", key);
            return false;
        }
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(request);
            log.info("exists() - END - key={}, exists=true", key);
            return true;
        } catch (NoSuchKeyException e) {
            log.info("exists() - END - key={}, exists=false", key);
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.info("exists() - END - key={}, exists=false", key);
                return false;
            }
            log.error("exists() - ERROR - Error when checking if file exists in bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao verificar existência do arquivo", e);
        }
    }

    public String getFileUrl(String key) {
        log.info("getFileUrl() - START - key={}", key);
        validateKey(key);
        String publicUrl = properties.getPublicUrl();
        if (publicUrl != null && !publicUrl.isBlank()) {
            String baseUrl = publicUrl.replaceAll("/+$", "");
            String cleanKey = key.replaceAll("^/+", "");
            String fileUrl = baseUrl + "/" + cleanKey;
            log.info("getFileUrl() - END - key={}, url={}", key, fileUrl);
            return fileUrl;
        }
        String presignedUrl = generatePresignedUrl(key, Duration.ofMinutes(60));
        log.info("getFileUrl() - END - key={}, url={}", key, presignedUrl);
        return presignedUrl;
    }

    public String generatePresignedUrl(String key, Duration duration) {
        log.info("generatePresignedUrl() - START - key={}, duration={}", key, duration);
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
            String url = presigned.url().toString();
            log.info("generatePresignedUrl() - END - key={}, url={}", key, url);
            return url;
        } catch (S3Exception e) {
            log.error("generatePresignedUrl() - ERROR - Error when generating presigned URL - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar URL pré-assinada para o arquivo", e);
        }
    }

    public void delete(String key) {
        log.info("delete() - START - key={}", key);
        validateKey(key);
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("delete() - END - key={}", key);
        } catch (S3Exception e) {
            log.error("delete() - ERROR - Error when deleting file from bucket - key: {}, error: {}", key, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir arquivo", e);
        }
    }

    public void deleteMany(List<String> keys) {
        log.info("deleteMany() - START - totalKeys={}", keys != null ? keys.size() : 0);
        if (keys == null || keys.isEmpty()) {
            log.info("deleteMany() - END - totalDeleted=0");
            return;
        }

        try {
            List<ObjectIdentifier> objectIdentifiers = keys.stream()
                    .filter(k -> k != null && !k.isBlank())
                    .map(k -> ObjectIdentifier.builder().key(k).build())
                    .toList();

            if (objectIdentifiers.isEmpty()) {
                log.info("deleteMany() - END - totalDeleted=0");
                return;
            }

            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(getBucketName())
                    .delete(Delete.builder().objects(objectIdentifiers).build())
                    .build();

            s3Client.deleteObjects(request);
            log.info("deleteMany() - END - totalDeleted={}", objectIdentifiers.size());
        } catch (S3Exception e) {
            log.error("deleteMany() - ERROR - Error when deleting multiple files from bucket - error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao excluir lote de arquivos", e);
        }
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A chave do arquivo não pode ser nula ou vazia.");
        }
    }
}
