package company.aura.erp.service;

import company.aura.erp.config.CloudflareR2Properties;
import company.aura.erp.dto.storage.R2FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudflareR2ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private CloudflareR2Properties properties;

    @InjectMocks
    private CloudflareR2Service r2Service;

    private static final String BUCKET_NAME = "test-erp-bucket";

    @BeforeEach
    void setUp() {
        lenient().when(properties.getBucketName()).thenReturn(BUCKET_NAME);
    }

    @Test
    @DisplayName("Deve fazer upload de MultipartFile com sucesso na raiz")
    void uploadMultipartFileSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "documento.pdf",
                "application/pdf",
                "conteudo".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = r2Service.upload(file);

        assertNotNull(key);
        assertTrue(key.endsWith("-documento.pdf"));
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Deve fazer upload de MultipartFile com pasta específica")
    void uploadMultipartFileWithFolderSuccess() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "imagem.png",
                "image/png",
                "pixels".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = r2Service.upload(file, "empresas/1/logos");

        assertNotNull(key);
        assertTrue(key.startsWith("empresas/1/logos/"));
        assertTrue(key.endsWith("-imagem.png"));
    }

    @Test
    @DisplayName("Deve falhar no upload se MultipartFile estiver vazio")
    void uploadMultipartFileEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", new byte[0]);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> r2Service.upload(emptyFile)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("Deve fazer upload de bytes diretamente")
    void uploadBytesSuccess() {
        String key = "backups/dump.sql";
        byte[] content = "SELECT 1;".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String resultKey = r2Service.upload(key, content, "application/sql");

        assertEquals(key, resultKey);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Deve fazer upload de InputStream diretamente")
    void uploadInputStreamSuccess() {
        String key = "documentos/relatorio.csv";
        byte[] bytes = "id;nome\n1;Item".getBytes();
        ByteArrayInputStream is = new ByteArrayInputStream(bytes);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String resultKey = r2Service.upload(key, is, bytes.length, "text/csv");

        assertEquals(key, resultKey);
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Deve fazer download de arquivo retornando bytes")
    void downloadSuccess() {
        String key = "fotos/perfil.jpg";
        byte[] expectedContent = "dados-da-imagem".getBytes();

        ResponseBytes<GetObjectResponse> responseBytes = mock(ResponseBytes.class);
        when(responseBytes.asByteArray()).thenReturn(expectedContent);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(responseBytes);

        byte[] result = r2Service.download(key);

        assertArrayEquals(expectedContent, result);
        verify(s3Client, times(1)).getObjectAsBytes(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Deve falhar no download com 404 se arquivo não existir")
    void downloadNotFound() {
        String key = "nao-existe.txt";

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> r2Service.download(key)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    @DisplayName("Deve obter stream do arquivo com sucesso")
    void getFileInputStreamSuccess() {
        String key = "video/demo.mp4";
        ResponseInputStream<GetObjectResponse> expectedStream = mock(ResponseInputStream.class);

        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(expectedStream);

        ResponseInputStream<GetObjectResponse> resultStream = r2Service.getFileInputStream(key);

        assertNotNull(resultStream);
        assertEquals(expectedStream, resultStream);
        verify(s3Client, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    @DisplayName("Deve listar arquivos pelo prefixo")
    void searchFilesSuccess() {
        String prefix = "orders/";

        S3Object obj1 = S3Object.builder().key("orders/1.pdf").size(1024L).lastModified(Instant.now()).eTag("etag1").build();
        S3Object obj2 = S3Object.builder().key("orders/2.pdf").size(2048L).lastModified(Instant.now()).eTag("etag2").build();

        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(obj1, obj2)
                .build();

        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        List<R2FileInfo> results = r2Service.searchFiles(prefix);

        assertEquals(2, results.size());
        assertEquals("orders/1.pdf", results.get(0).key());
        assertEquals(1024L, results.get(0).size());
        assertEquals("orders/2.pdf", results.get(1).key());
    }

    @Test
    @DisplayName("Deve verificar se arquivo existe")
    void existsFile() {
        String key = "foto.jpg";

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());

        assertTrue(r2Service.exists(key));

        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertFalse(r2Service.exists("inexistente.jpg"));
    }

    @Test
    @DisplayName("Deve retornar URL pública quando configurada")
    void getFileUrlWithPublicUrl() {
        when(properties.getPublicUrl()).thenReturn("https://pub-cdn.aura.company");

        String url = r2Service.getFileUrl("invoices/doc.pdf");

        assertEquals("https://pub-cdn.aura.company/invoices/doc.pdf", url);
    }

    @Test
    @DisplayName("Deve gerar URL pré-assinada com sucesso")
    void generatePresignedUrlSuccess() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URI("https://r2.storage/signed-url").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = r2Service.generatePresignedUrl("relatorio.xlsx", Duration.ofMinutes(30));

        assertEquals("https://r2.storage/signed-url", url);
    }

    @Test
    @DisplayName("Deve extrair chave do bucket de diferentes formatos de URL ou chave")
    void extractKeyFromUrlSuccess() {
        when(properties.getPublicUrl()).thenReturn("https://pub-cdn.aura.company");

        assertEquals("invoices/10/pdf/danfe.pdf", r2Service.extractKeyFromUrl("invoices/10/pdf/danfe.pdf"));
        assertEquals("invoices/10/pdf/danfe.pdf", r2Service.extractKeyFromUrl("/invoices/10/pdf/danfe.pdf"));
        assertEquals("invoices/10/pdf/danfe.pdf", r2Service.extractKeyFromUrl("https://pub-cdn.aura.company/invoices/10/pdf/danfe.pdf"));
        assertEquals("invoices/10/pdf/danfe.pdf", r2Service.extractKeyFromUrl("https://storage.r2.cloudflarestorage.com/test-erp-bucket/invoices/10/pdf/danfe.pdf?X-Amz=123"));
    }

    @Test
    @DisplayName("Deve gerar URL temporária a partir de URL ou chave")
    void generateTemporaryUrlSuccess() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URI("https://r2.storage/temp-url").toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

        String url = r2Service.generateTemporaryUrl("invoices/10/pdf/danfe.pdf", Duration.ofMinutes(5));

        assertEquals("https://r2.storage/temp-url", url);
    }

    @Test
    @DisplayName("Deve excluir arquivo com sucesso")
    void deleteFileSuccess() {
        String key = "arquivo-antigo.txt";
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

        assertDoesNotThrow(() -> r2Service.delete(key));

        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    @DisplayName("Deve excluir múltiplos arquivos com sucesso")
    void deleteManyFilesSuccess() {
        List<String> keys = List.of("arquivo1.txt", "arquivo2.txt");
        when(s3Client.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(DeleteObjectsResponse.builder().build());

        assertDoesNotThrow(() -> r2Service.deleteMany(keys));

        verify(s3Client, times(1)).deleteObjects(any(DeleteObjectsRequest.class));
    }
}
