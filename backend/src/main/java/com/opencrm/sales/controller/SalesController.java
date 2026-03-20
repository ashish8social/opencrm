package com.opencrm.sales.controller;

import com.opencrm.common.dto.ApiResponse;
import com.opencrm.data.dto.RecordDTO;
import com.opencrm.sales.service.OrderConversionService;
import com.opencrm.sales.service.QuotePdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    private final QuotePdfService quotePdfService;
    private final OrderConversionService orderConversionService;

    public SalesController(QuotePdfService quotePdfService, OrderConversionService orderConversionService) {
        this.quotePdfService = quotePdfService;
        this.orderConversionService = orderConversionService;
    }

    @GetMapping("/quotes/{id}/pdf")
    public ResponseEntity<byte[]> generateQuotePdf(@PathVariable UUID id) {
        byte[] pdf = quotePdfService.generateQuotePdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quote-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/quotes/{id}/convert-to-order")
    public ResponseEntity<ApiResponse<RecordDTO>> convertToOrder(
            @PathVariable UUID id, Authentication auth) {
        UUID userId = UUID.fromString(auth.getPrincipal().toString());
        RecordDTO order = orderConversionService.convertQuoteToOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }
}
