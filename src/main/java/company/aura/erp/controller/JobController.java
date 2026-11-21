package company.aura.erp.controller;

import company.aura.erp.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping("/orders/receivable/notify")
    public ResponseEntity<Void> notifyReceivables() {
        jobService.notifyUpcomingReceivables();
        return ResponseEntity.accepted().build();
    }
}
