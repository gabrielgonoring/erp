package company.aura.erp.service;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.entity.InvoiceEntity;
import company.aura.erp.entity.PartnerEntity;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.repository.InvoiceRepository;
import company.aura.erp.repository.PartnerRepository;
import company.aura.erp.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private CloudflareR2Service r2Service;

    @InjectMocks
    private InvoiceService invoiceService;

    private Long companyId;
    private Long partnerId;
    private UserPrincipal userPrincipal;
    private InvoiceCreateRequest request;
    private MockMultipartFile xmlFile;
    private MockMultipartFile danfePdf;

    @BeforeEach
    void setUp() {
        companyId = 10L;
        partnerId = 5L;

        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(companyId)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();

        request = new InvoiceCreateRequest(
                partnerId,
                new BigDecimal("1500.00"),
                DocumentType.NFE,
                EnvironmentType.PRD,
                "AUTHORIZED",
                1,
                101,
                1,
                "35261100000000000000550010000001011000000001",
                "135260000000001",
                "PIX",
                LocalDate.now().plusDays(30),
                9,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1500.00"),
                BigDecimal.ZERO,
                "Informações adicionais da nota",
                "EXT-9999",
                LocalDateTime.now()
        );

        xmlFile = new MockMultipartFile(
                "xmlFile",
                "nfe.xml",
                "application/xml",
                "<xml>teste</xml>".getBytes()
        );

        danfePdf = new MockMultipartFile(
                "danfePdf",
                "danfe.pdf",
                "application/pdf",
                "%PDF-1.4 teste".getBytes()
        );
    }

    @Test
    @DisplayName("Deve criar nota fiscal externa com sucesso")
    void createExternalInvoiceSuccess() {
        when(partnerRepository.existsByIdAndCompanyId(partnerId, companyId)).thenReturn(true);
        when(invoiceRepository.existsByCompanyIdAndAccessKey(eq(companyId), anyString())).thenReturn(false);

        when(r2Service.getFileUrl(anyString())).thenReturn("https://r2.storage/mock-url");

        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> {
            InvoiceEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        InvoiceResponse response = invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(companyId, response.getCompanyId());
        assertEquals(partnerId, response.getPartnerId());
        assertEquals("35261100000000000000550010000001011000000001", response.getAccessKey());
        assertEquals(new BigDecimal("1500.00"), response.getTotalAmount());
        assertNotNull(response.getXmlUrl());
        assertNotNull(response.getPdfUrl());

        verify(r2Service, times(2)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(invoiceRepository, times(1)).save(any(InvoiceEntity.class));
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST se o XML da nota estiver ausente ou vazio")
    void createExternalInvoiceMissingXml() {
        MockMultipartFile emptyXml = new MockMultipartFile("xmlFile", "", "application/xml", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, emptyXml, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O arquivo XML da nota fiscal é obrigatório"));
    }

    @Test
    @DisplayName("Deve lançar BAD_REQUEST se o PDF da DANFE estiver ausente ou vazio")
    void createExternalInvoiceMissingPdf() {
        MockMultipartFile emptyPdf = new MockMultipartFile("danfePdf", "", "application/pdf", new byte[0]);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, xmlFile, emptyPdf, userPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("O arquivo PDF da DANFE é obrigatório"));
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND se o parceiro não pertencer à empresa")
    void createExternalInvoicePartnerNotFound() {
        when(partnerRepository.existsByIdAndCompanyId(partnerId, companyId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Parceiro não foi encontrado para esta empresa"));
    }

    @Test
    @DisplayName("Deve lançar CONFLICT se já existir nota com a mesma chave de acesso na empresa")
    void createExternalInvoiceAlreadyExists() {
        when(partnerRepository.existsByIdAndCompanyId(partnerId, companyId)).thenReturn(true);
        when(invoiceRepository.existsByCompanyIdAndAccessKey(eq(companyId), anyString())).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Já existe uma nota fiscal cadastrada com a chave de acesso"));
    }

    @Test
    @DisplayName("Deve fazer rollback/limpeza dos arquivos no R2 quando falhar o upload do PDF")
    void createExternalInvoiceRollbackOnUploadFailure() throws Exception {
        when(partnerRepository.existsByIdAndCompanyId(partnerId, companyId)).thenReturn(true);
        when(invoiceRepository.existsByCompanyIdAndAccessKey(eq(companyId), anyString())).thenReturn(false);

        when(r2Service.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("mock-key")
                .thenThrow(new RuntimeException("Simulated S3 network failure"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Não foi possível fazer o upload do arquivo"));

        verify(r2Service, times(2)).delete(anyString());
        verify(invoiceRepository, never()).save(any(InvoiceEntity.class));
    }

    @Test
    @DisplayName("Deve buscar nota fiscal por ID pertencente à empresa com sucesso")
    void findByIdSuccess() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .partnerId(partnerId)
                .totalAmount(new BigDecimal("500.00"))
                .documentType(DocumentType.NFE)
                .environment(EnvironmentType.STG)
                .status("ISSUED")
                .build();

        when(invoiceRepository.findByIdAndCompanyId(1L, companyId)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.findById(1L, companyId);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(companyId, response.getCompanyId());
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND ao buscar por ID se a nota não existir")
    void findByIdNotFound() {
        when(invoiceRepository.findByIdAndCompanyId(999L, companyId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.findById(999L, companyId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve listar notas fiscais filtradas e paginadas com sucesso")
    void listWithFiltersAndPageableSuccess() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .partnerId(partnerId)
                .externalReference("EXT-123")
                .operationType(1)
                .documentType(DocumentType.NFE)
                .environment(EnvironmentType.STG)
                .status("ISSUED")
                .totalAmount(new BigDecimal("100.00"))
                .build();

        Page<InvoiceEntity> invoicePage = new PageImpl<>(List.of(invoice));
        Pageable pageable = PageRequest.of(0, 20);

        when(invoiceRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(invoicePage);

        Page<InvoiceResponse> page = invoiceService.list(companyId, List.of(partnerId), "EXT-123", 1, DocumentType.NFE, "ISSUED", pageable);

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals(1L, page.getContent().get(0).getId());
        assertEquals("EXT-123", page.getContent().get(0).getExternalReference());
        verify(invoiceRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("Deve gerar link temporário de 5 minutos para PDF com sucesso")
    void getPdfUrlSuccess() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .pdfUrl("https://r2.aura.company/invoices/10/pdf/danfe.pdf")
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(r2Service.generateTemporaryUrl("https://r2.aura.company/invoices/10/pdf/danfe.pdf", Duration.ofMinutes(5)))
                .thenReturn("https://signed-url/danfe.pdf?exp=300");

        String temporaryUrl = invoiceService.getPdfUrl(1L, userPrincipal);

        assertEquals("https://signed-url/danfe.pdf?exp=300", temporaryUrl);
        verify(r2Service, times(1)).generateTemporaryUrl("https://r2.aura.company/invoices/10/pdf/danfe.pdf", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Deve lançar FORBIDDEN ao obter URL do PDF se a nota pertencer a outra empresa")
    void getPdfUrlForbidden() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(999L)
                .pdfUrl("https://r2.aura.company/invoices/999/pdf/danfe.pdf")
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.getPdfUrl(1L, userPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND ao obter URL do PDF se o campo pdfUrl estiver vazio")
    void getPdfUrlNotFoundWhenFieldBlank() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .pdfUrl(null)
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.getPdfUrl(1L, userPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Arquivo PDF não encontrado"));
    }

    @Test
    @DisplayName("Deve gerar link temporário de 5 minutos para XML com sucesso")
    void getXmlUrlSuccess() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .xmlUrl("https://r2.aura.company/invoices/10/xml/nota.xml")
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(r2Service.generateTemporaryUrl("https://r2.aura.company/invoices/10/xml/nota.xml", Duration.ofMinutes(5)))
                .thenReturn("https://signed-url/nota.xml?exp=300");

        String temporaryUrl = invoiceService.getXmlUrl(1L, userPrincipal);

        assertEquals("https://signed-url/nota.xml?exp=300", temporaryUrl);
        verify(r2Service, times(1)).generateTemporaryUrl("https://r2.aura.company/invoices/10/xml/nota.xml", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Deve lançar FORBIDDEN ao obter URL do XML se a nota pertencer a outra empresa")
    void getXmlUrlForbidden() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(999L)
                .xmlUrl("https://r2.aura.company/invoices/999/xml/nota.xml")
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.getXmlUrl(1L, userPrincipal)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve lançar NOT_FOUND ao obter URL do XML se o campo xmlUrl estiver vazio")
    void getXmlUrlNotFoundWhenFieldBlank() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .xmlUrl("   ")
                .build();

        when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.getXmlUrl(1L, userPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Arquivo XML não encontrado"));
    }
}
