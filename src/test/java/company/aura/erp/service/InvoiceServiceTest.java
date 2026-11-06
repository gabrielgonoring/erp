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
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.math.BigDecimal;
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

    private Long companyId = 10L;
    private Long partnerId = 5L;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(companyId)
                .email("test@aura.company")
                .name("Test User")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Deve criar nota fiscal, enviar XML e DANFE PDF para o R2 e persistir no banco")
    void createInvoiceSuccess() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(
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
                "BOLETO",
                LocalDate.now().plusDays(30),
                9,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("1500.00"),
                BigDecimal.ZERO,
                "Informações adicionais da nota fiscal",
                "ORD-999",
                LocalDateTime.now()
        );

        MockMultipartFile xmlFile = new MockMultipartFile(
                "xmlFile",
                "nfe-101.xml",
                "application/xml",
                "<nfeProc>xml content</nfeProc>".getBytes()
        );

        MockMultipartFile danfePdf = new MockMultipartFile(
                "danfePdf",
                "danfe-101.pdf",
                "application/pdf",
                "%PDF-1.4 pdf content".getBytes()
        );

        when(partnerRepository.existsByIdAndCompanyId(partnerId, companyId)).thenReturn(true);
        when(invoiceRepository.existsByCompanyIdAndAccessKey(eq(companyId), anyString())).thenReturn(false);

        when(r2Service.upload(anyString(), any(InputStream.class), anyLong(), eq("application/xml")))
                .thenReturn("key-xml");
        when(r2Service.upload(anyString(), any(InputStream.class), anyLong(), eq("application/pdf")))
                .thenReturn("key-pdf");

        when(r2Service.getFileUrl(contains("/xml/"))).thenReturn("https://r2.aura.company/invoices/10/xml/nfe.xml");
        when(r2Service.getFileUrl(contains("/pdf/"))).thenReturn("https://r2.aura.company/invoices/10/pdf/danfe.pdf");

        when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(invocation -> {
            InvoiceEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            return entity;
        });

        InvoiceResponse response = invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(companyId, response.companyId());
        assertEquals(partnerId, response.partnerId());
        assertEquals(new BigDecimal("1500.00"), response.totalAmount());
        assertEquals("https://r2.aura.company/invoices/10/xml/nfe.xml", response.xmlUrl());
        assertEquals("https://r2.aura.company/invoices/10/pdf/danfe.pdf", response.pdfUrl());

        verify(r2Service, times(2)).upload(anyString(), any(InputStream.class), anyLong(), anyString());
        verify(invoiceRepository, times(1)).save(any(InvoiceEntity.class));
    }

    @Test
    @DisplayName("Deve falhar com NOT_FOUND se parceiro não pertencer à empresa")
    void createInvoicePartnerNotFound() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(
                999L,
                new BigDecimal("500.00"),
                DocumentType.NFE,
                EnvironmentType.STG,
                null, 1, 10, 1, null, null, null, null, null, null, null, null, null, null, null, null
        );

        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile danfePdf = new MockMultipartFile("danfePdf", "danfe.pdf", "application/pdf", "%PDF".getBytes());

        when(partnerRepository.existsByIdAndCompanyId(999L, companyId)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(r2Service);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar com CONFLICT se chave de acesso já existir")
    void createInvoiceDuplicateAccessKey() {
        String accessKey = "35261100000000000000550010000001011000000001";
        InvoiceCreateRequest request = new InvoiceCreateRequest(
                partnerId,
                new BigDecimal("500.00"),
                DocumentType.NFE,
                EnvironmentType.STG,
                null, 1, 10, 1, accessKey, null, null, null, null, null, null, null, null, null, null, null
        );

        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile danfePdf = new MockMultipartFile("danfePdf", "danfe.pdf", "application/pdf", "%PDF".getBytes());

        when(partnerRepository.existsByIdAndCompanyId(partnerId, companyId)).thenReturn(true);
        when(invoiceRepository.existsByCompanyIdAndAccessKey(companyId, accessKey)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, xmlFile, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verifyNoInteractions(r2Service);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve falhar com BAD_REQUEST se arquivo XML estiver vazio ou nulo")
    void createInvoiceMissingXmlFile() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(
                partnerId,
                new BigDecimal("500.00"),
                DocumentType.NFE,
                EnvironmentType.STG,
                null, 1, 10, 1, null, null, null, null, null, null, null, null, null, null, null, null
        );

        MockMultipartFile emptyXml = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", new byte[0]);
        MockMultipartFile danfePdf = new MockMultipartFile("danfePdf", "danfe.pdf", "application/pdf", "%PDF".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                invoiceService.createExternalInvoice(request, emptyXml, danfePdf, userPrincipal)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    @DisplayName("Deve buscar nota fiscal por ID com sucesso")
    void findByIdSuccess() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .partnerId(partnerId)
                .totalAmount(new BigDecimal("250.00"))
                .documentType(DocumentType.NFE)
                .environment(EnvironmentType.STG)
                .status("ISSUED")
                .xmlUrl("https://r2/xml")
                .pdfUrl("https://r2/pdf")
                .build();

        when(invoiceRepository.findByIdAndCompanyId(1L, companyId)).thenReturn(Optional.of(invoice));

        InvoiceResponse response = invoiceService.findById(1L, companyId);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("https://r2/xml", response.xmlUrl());
    }

    @Test
    @DisplayName("Deve listar notas fiscais da empresa")
    void listInvoicesSuccess() {
        InvoiceEntity invoice = InvoiceEntity.builder()
                .id(1L)
                .companyId(companyId)
                .partnerId(partnerId)
                .totalAmount(new BigDecimal("100.00"))
                .documentType(DocumentType.NFE)
                .environment(EnvironmentType.STG)
                .status("ISSUED")
                .build();

        when(invoiceRepository.findByCompanyId(companyId)).thenReturn(List.of(invoice));

        List<InvoiceResponse> list = invoiceService.list(companyId);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).id());
    }
}
