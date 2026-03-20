package com.opencrm.data.controller;

import com.opencrm.common.dto.ApiResponse;
import com.opencrm.common.dto.PagedResult;
import com.opencrm.data.dto.RecordDTO;
import com.opencrm.data.service.RecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/data")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping("/{entityApiName}")
    public ResponseEntity<ApiResponse<PagedResult<RecordDTO>>> list(
            @PathVariable String entityApiName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            List<RecordDTO> results = recordService.searchByName(entityApiName, q);
            return ResponseEntity.ok(ApiResponse.success(
                    new PagedResult<>(results, 0, results.size(), results.size(), 1)));
        }
        PagedResult<RecordDTO> result = recordService.list(entityApiName, page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{entityApiName}/{id}")
    public ResponseEntity<ApiResponse<RecordDTO>> get(
            @PathVariable String entityApiName,
            @PathVariable UUID id) {
        RecordDTO record = recordService.get(entityApiName, id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PostMapping("/{entityApiName}")
    public ResponseEntity<ApiResponse<RecordDTO>> create(
            @PathVariable String entityApiName,
            @RequestBody Map<String, Object> data,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getPrincipal().toString());
        RecordDTO record = recordService.create(entityApiName, data, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(record));
    }

    @PutMapping("/{entityApiName}/{id}")
    public ResponseEntity<ApiResponse<RecordDTO>> update(
            @PathVariable String entityApiName,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> data,
            Authentication auth) {
        UUID userId = UUID.fromString(auth.getPrincipal().toString());
        RecordDTO record = recordService.update(entityApiName, id, data, userId);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @DeleteMapping("/{entityApiName}/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String entityApiName,
            @PathVariable UUID id) {
        recordService.delete(entityApiName, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{entityApiName}/{id}/related/{relatedEntityApiName}")
    public ResponseEntity<ApiResponse<List<RecordDTO>>> related(
            @PathVariable String entityApiName,
            @PathVariable UUID id,
            @PathVariable String relatedEntityApiName) {
        List<RecordDTO> records = recordService.getRelated(entityApiName, id, relatedEntityApiName);
        return ResponseEntity.ok(ApiResponse.success(records));
    }
}
