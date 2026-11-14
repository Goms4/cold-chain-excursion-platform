package com.coldchain.platform.dashboard.web;

import com.coldchain.platform.dashboard.dto.DashboardSummaryResponse;
import com.coldchain.platform.dashboard.service.DashboardSummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardSummaryController {
    private final DashboardSummaryService summaryService;

    public DashboardSummaryController(DashboardSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary() {
        return ResponseEntity.ok(summaryService.getSummary());
    }
}
