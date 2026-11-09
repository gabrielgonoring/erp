package company.aura.erp.controller;

import company.aura.erp.dto.dashboard.DashboardSummaryResponse;
import company.aura.erp.dto.dashboard.SystemAlertResponse;
import company.aura.erp.security.UserPrincipal;
import company.aura.erp.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;



    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        DashboardSummaryResponse response = dashboardService.getSummary(startDate, endDate, userPrincipal.getCompanyId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<SystemAlertResponse>> getAlerts(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        // (Importante: não coloque a implementação)
        List<SystemAlertResponse> response = dashboardService.getAlerts(userPrincipal);
        return ResponseEntity.ok(response);
    }
}
