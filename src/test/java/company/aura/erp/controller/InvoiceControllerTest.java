package company.aura.erp.controller;

import company.aura.erp.dto.invoice.InvoiceCreateRequest;
import company.aura.erp.dto.invoice.InvoiceResponse;
import company.aura.erp.enums.DocumentType;
import company.aura.erp.enums.EnvironmentType;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.InvoiceService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Deve criar nota fiscal via controller retornando 201 CREATED")
    void createInvoiceSuccess() {
        InvoiceCreateRequest request = new InvoiceCreateRequest(
                5L,
                new BigDecimal("1000.00"),
                DocumentType.NFE,
                EnvironmentType.PRD,
                "AUTHORIZED",
                1, 101, 1,
                "35261100000000000000550010000001011000000001",
                "135260000000001",
                "PIX",
                null, 9,
                BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("1000.00"), BigDecimal.ZERO,
                "Obs", "EXT-1", LocalDateTime.now()
        );

        MockMultipartFile xmlFile = new MockMultipartFile("xmlFile", "nfe.xml", "application/xml", "<xml/>".getBytes());
        MockMultipartFile danfePdf = new MockMultipartFile("danfePdf", "danfe.pdf", "application/pdf", "%PDF".getBytes());

        InvoiceResponse responseDto = new InvoiceResponse(
                1L, 10L, 5L, "EXT-1", 1, DocumentType.NFE, EnvironmentType.PRD,
                "AUTHORIZED", 101, 1, "35261100000000000000550010000001011000000001",
                "135260000000001", "PIX", null, 9, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                "Obs", "https://r2/danfe.pdf", "https://r2/nfe.xml",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(invoiceService.createExternalInvoice(eq(request), eq(xmlFile), eq(danfePdf), eq(userPrincipal)))
                .thenReturn(responseDto);

        ResponseEntity<InvoiceResponse> response = invoiceController.createInvoice(
                request, xmlFile, danfePdf, userPrincipal
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("https://r2/danfe.pdf", response.getBody().getPdfUrl());
        assertEquals("https://r2/nfe.xml", response.getBody().getXmlUrl());

        verify(invoiceService, times(1)).createExternalInvoice(eq(request), eq(xmlFile), eq(danfePdf), eq(userPrincipal));
    }

    @Test
    @DisplayName("Deve buscar nota fiscal por ID via controller")
    void findByIdSuccess() {
        InvoiceResponse responseDto = new InvoiceResponse(
                1L, 10L, 5L, null, 1, DocumentType.NFE, EnvironmentType.STG,
                "ISSUED", 10, 1, null, null, null, null, 9,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("500.00"),
                null, "https://r2/danfe.pdf", "https://r2/nfe.xml",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        when(invoiceService.findById(1L, 10L)).thenReturn(responseDto);

        ResponseEntity<InvoiceResponse> response = invoiceController.findById(1L, userPrincipal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        verify(invoiceService, times(1)).findById(1L, 10L);
    }

    @Test
    @DisplayName("Deve listar notas fiscais via controller")
    void listSuccess() {
        InvoiceResponse responseDto = new InvoiceResponse(
                1L, 10L, 5L, "EXT-1", 1, DocumentType.NFE, EnvironmentType.PRD,
                "AUTHORIZED", 101, 1, "35261100000000000000550010000001011000000001",
                "135260000000001", "PIX", null, 9, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                "Obs", "https://r2/danfe.pdf", "https://r2/nfe.xml",
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );

        Pageable pageable = PageRequest.of(0, 20);
        Page<InvoiceResponse> pageResult = new PageImpl<>(List.of(responseDto));

        when(invoiceService.list(10L, List.of(5L), "EXT-1", 1, DocumentType.NFE, "AUTHORIZED", pageable))
                .thenReturn(pageResult);

        ResponseEntity<Page<InvoiceResponse>> response = invoiceController.list(
                List.of(5L),
                "EXT-1",
                1,
                DocumentType.NFE,
                "AUTHORIZED",
                pageable,
                userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(1L, response.getBody().getContent().get(0).getId());
        verify(invoiceService, times(1)).list(10L, List.of(5L), "EXT-1", 1, DocumentType.NFE, "AUTHORIZED", pageable);
    }

    @Test
    @DisplayName("Deve redirecionar 302 FOUND para URL temporária do PDF")
    void getPdfSuccess() {
        String temporaryUrl = "https://r2.aura.company/signed/danfe.pdf?token=xyz";
        when(invoiceService.getPdfUrl(1L, userPrincipal)).thenReturn(temporaryUrl);

        ResponseEntity<Void> response = invoiceController.getPdf(1L, userPrincipal);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create(temporaryUrl), response.getHeaders().getLocation());
        verify(invoiceService, times(1)).getPdfUrl(1L, userPrincipal);
    }

    @Test
    @DisplayName("Deve redirecionar 302 FOUND para URL temporária do XML")
    void getXmlSuccess() {
        String temporaryUrl = "https://r2.aura.company/signed/nota.xml?token=xyz";
        when(invoiceService.getXmlUrl(1L, userPrincipal)).thenReturn(temporaryUrl);

        ResponseEntity<Void> response = invoiceController.getXml(1L, userPrincipal);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals(URI.create(temporaryUrl), response.getHeaders().getLocation());
        verify(invoiceService, times(1)).getXmlUrl(1L, userPrincipal);
    }
}
