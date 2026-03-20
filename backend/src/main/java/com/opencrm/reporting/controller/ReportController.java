package com.opencrm.reporting.controller;

import com.opencrm.common.dto.ApiResponse;
import com.opencrm.reporting.dto.PipelineSummary;
import com.opencrm.reporting.dto.RevenueByMonth;
import com.opencrm.reporting.dto.TopAccount;
import com.opencrm.reporting.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/pipeline-summary")
    public ResponseEntity<ApiResponse<List<PipelineSummary>>> pipelineSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getPipelineSummary()));
    }

    @GetMapping("/revenue-by-month")
    public ResponseEntity<ApiResponse<List<RevenueByMonth>>> revenueByMonth() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getRevenueByMonth()));
    }

    @GetMapping("/top-accounts")
    public ResponseEntity<ApiResponse<List<TopAccount>>> topAccounts() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getTopAccounts()));
    }

    @GetMapping("/record-counts")
    public ResponseEntity<ApiResponse<Map<String, Long>>> recordCounts() {
        return ResponseEntity.ok(ApiResponse.success(reportService.getRecordCounts()));
    }
}
