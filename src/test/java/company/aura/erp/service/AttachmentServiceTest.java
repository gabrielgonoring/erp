package company.aura.erp.service;

import company.aura.erp.modules.bucket.service.CloudflareR2Service;
import company.aura.erp.modules.email.domain.DownloadedAttachment;
import company.aura.erp.modules.email.domain.template.Attachment;
import company.aura.erp.modules.email.enumerate.AttachmentSource;
import company.aura.erp.modules.email.service.AttachmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private CloudflareR2Service cloudflareR2Service;

    @InjectMocks
    private AttachmentService attachmentService;

    @Test
    @DisplayName("Deve retornar lista vazia quando a lista de anexos for nula ou vazia")
    void shouldReturnEmptyListWhenAttachmentsNullOrEmpty() {
        assertTrue(attachmentService.downloadAttachments(null).isEmpty());
        assertTrue(attachmentService.downloadAttachments(Collections.emptyList()).isEmpty());
        verifyNoInteractions(cloudflareR2Service);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Deve ignorar anexos com URL nula, vazia ou em branco")
    void shouldIgnoreAttachmentWithInvalidUrl(String url) {
        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url(url)
                .build();

        List<DownloadedAttachment> result = attachmentService.downloadAttachments(List.of(attachment));

        assertTrue(result.isEmpty());
        verifyNoInteractions(cloudflareR2Service);
    }

    @Test
    @DisplayName("Deve baixar anexo com sucesso, removendo UUID do nome do arquivo e inferindo tipo MIME")
    void shouldDownloadAttachmentSuccessfullyAndCleanUuid() {
        String url = "https://pub-r2.com/orders/d83f4b82-7472-4d2c-8134-2e92cbbd42b1-boleto.pdf";
        String key = "orders/d83f4b82-7472-4d2c-8134-2e92cbbd42b1-boleto.pdf";
        byte[] content = "pdf-content".getBytes();

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url(url)
                .build();

        when(cloudflareR2Service.extractKeyFromUrl(url)).thenReturn(key);
        when(cloudflareR2Service.download(key)).thenReturn(content);

        List<DownloadedAttachment> result = attachmentService.downloadAttachments(List.of(attachment));

        assertEquals(1, result.size());
        DownloadedAttachment downloaded = result.get(0);
        assertEquals("boleto.pdf", downloaded.getFileName());
        assertEquals(MediaType.APPLICATION_PDF_VALUE, downloaded.getContentType());
        assertArrayEquals(content, downloaded.getData());
    }

    @ParameterizedTest
    @CsvSource({
            "relatorio.pdf, application/pdf",
            "nota.xml, application/xml",
            "imagem.png, image/png",
            "foto.jpg, image/jpeg",
            "foto.jpeg, image/jpeg",
            "dados.txt, text/plain",
            "pagina.html, text/html",
            "arquivo.xyz, application/octet-stream"
    })
    @DisplayName("Deve determinar o Content-Type corretamente baseado na extensão do arquivo")
    void shouldDetermineContentTypeCorrectly(String filename, String expectedContentType) {
        String url = "https://pub-r2.com/files/" + filename;
        String key = "files/" + filename;
        byte[] content = "test-content".getBytes();

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url(url)
                .build();

        when(cloudflareR2Service.extractKeyFromUrl(url)).thenReturn(key);
        when(cloudflareR2Service.download(key)).thenReturn(content);

        List<DownloadedAttachment> result = attachmentService.downloadAttachments(List.of(attachment));

        assertEquals(1, result.size());
        assertEquals(expectedContentType, result.get(0).getContentType());
        assertEquals(filename, result.get(0).getFileName());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o download retornar array nulo ou vazio")
    void shouldReturnEmptyListWhenDownloadReturnsEmpty() {
        String url = "https://pub-r2.com/orders/vazio.pdf";
        String key = "orders/vazio.pdf";

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url(url)
                .build();

        when(cloudflareR2Service.extractKeyFromUrl(url)).thenReturn(key);
        when(cloudflareR2Service.download(key)).thenReturn(new byte[0]);

        List<DownloadedAttachment> result = attachmentService.downloadAttachments(List.of(attachment));

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve ignorar anexo que lançar exceção durante o download sem propagar o erro")
    void shouldCatchExceptionAndReturnEmptyListWhenDownloadFails() {
        String url = "https://pub-r2.com/orders/falha.pdf";
        String key = "orders/falha.pdf";

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url(url)
                .build();

        when(cloudflareR2Service.extractKeyFromUrl(url)).thenReturn(key);
        when(cloudflareR2Service.download(key)).thenThrow(new RuntimeException("S3 connection timeout"));

        assertDoesNotThrow(() -> {
            List<DownloadedAttachment> result = attachmentService.downloadAttachments(List.of(attachment));
            assertTrue(result.isEmpty());
        });
    }

    @Test
    @DisplayName("Deve remover parâmetros de query do nome do arquivo")
    void shouldRemoveQueryParamsFromFileName() {
        String url = "https://pub-r2.com/orders/recibo.pdf?token=abc&expire=123";
        String key = "orders/recibo.pdf?token=abc&expire=123";
        byte[] content = "receipt-content".getBytes();

        Attachment attachment = Attachment.builder()
                .source(AttachmentSource.BUCKET)
                .url(url)
                .build();

        when(cloudflareR2Service.extractKeyFromUrl(url)).thenReturn(key);
        when(cloudflareR2Service.download(key)).thenReturn(content);

        List<DownloadedAttachment> result = attachmentService.downloadAttachments(List.of(attachment));

        assertEquals(1, result.size());
        assertEquals("recibo.pdf", result.get(0).getFileName());
    }
}
