package company.aura.erp.controller;

import company.aura.erp.dto.payment.CardPaymentSummaryResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.ReportPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportPaymentService reportPaymentService;

    @GetMapping("/card-payments")
    public ResponseEntity<CardPaymentSummaryResponse> getCardPaymentsReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        CardPaymentSummaryResponse response = reportPaymentService.getCardPaymentSummary(
                startDate,
                endDate,
                userPrincipal.getCompanyId()
        );
        return ResponseEntity.ok(response);
    }
}
