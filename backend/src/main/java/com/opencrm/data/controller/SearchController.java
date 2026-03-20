package com.opencrm.data.controller;

import com.opencrm.common.dto.ApiResponse;
import com.opencrm.data.dto.RecordDTO;
import com.opencrm.data.service.RecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final RecordService recordService;

    public SearchController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RecordDTO>>> search(
            @RequestParam String q,
            @RequestParam(required = false) String entities) {
        List<String> entityNames = entities != null ? Arrays.asList(entities.split(",")) : null;
        List<RecordDTO> results = recordService.search(q, entityNames);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/data/{entityApiName}/search")
    public ResponseEntity<ApiResponse<List<RecordDTO>>> searchByName(
            @PathVariable String entityApiName,
            @RequestParam String q) {
        List<RecordDTO> results = recordService.searchByName(entityApiName, q);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
