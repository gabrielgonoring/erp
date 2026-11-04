package company.aura.erp.controller;

import company.aura.erp.dto.partner.PartnerSummaryResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.PartnerService;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerControllerTest {

    @Mock
    private PartnerService partnerService;

    @InjectMocks
    private PartnerController partnerController;

    @Test
    void shouldDeletePartnerAndReturnNoContent() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();

        ResponseEntity<Void> response = partnerController.delete(5L, userPrincipal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(partnerService).delete(5L, 10L);
    }

    @Test
    void shouldListPartnersWithFilters() {
        UserPrincipal userPrincipal = UserPrincipal.builder()
                .id(1L)
                .companyId(10L)
                .email("user@company.com")
                .authorities(Collections.emptyList())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<PartnerSummaryResponse> page = new PageImpl<>(List.of(
                new PartnerSummaryResponse(1L, "Partner Test", "12345678000199", "São Paulo", "SP")
        ));

        when(partnerService.listSummarized("test", true, true, false, 10L, pageable))
                .thenReturn(page);

        ResponseEntity<Page<PartnerSummaryResponse>> response = partnerController.list(
                "test", true, true, false, null, pageable, userPrincipal
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        verify(partnerService).listSummarized("test", true, true, false, 10L, pageable);
    }
}
