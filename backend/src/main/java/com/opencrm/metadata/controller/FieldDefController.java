package com.opencrm.metadata.controller;

import com.opencrm.common.dto.ApiResponse;
import com.opencrm.metadata.model.FieldDef;
import com.opencrm.metadata.service.FieldDefService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata/entities/{entityApiName}/fields")
public class FieldDefController {

    private final FieldDefService fieldDefService;

    public FieldDefController(FieldDefService fieldDefService) {
        this.fieldDefService = fieldDefService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(@PathVariable String entityApiName) {
        List<Map<String, Object>> fields = fieldDefService.findByEntity(entityApiName).stream()
                .map(this::toMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(fields));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@PathVariable String entityApiName,
                                                                    @RequestBody FieldDef fieldDef) {
        FieldDef created = fieldDefService.create(entityApiName, fieldDef);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toMap(created)));
    }

    @PutMapping("/{fieldApiName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable String entityApiName,
                                                                    @PathVariable String fieldApiName,
                                                                    @RequestBody FieldDef fieldDef) {
        FieldDef updated = fieldDefService.update(entityApiName, fieldApiName, fieldDef);
        return ResponseEntity.ok(ApiResponse.success(toMap(updated)));
    }

    @DeleteMapping("/{fieldApiName}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String entityApiName,
                                                     @PathVariable String fieldApiName) {
        fieldDefService.delete(entityApiName, fieldApiName);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Map<String, Object> toMap(FieldDef f) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", f.getId());
        m.put("apiName", f.getApiName());
        m.put("label", f.getLabel());
        m.put("fieldType", f.getFieldType().name());
        m.put("required", Boolean.TRUE.equals(f.getRequired()));
        m.put("uniqueField", Boolean.TRUE.equals(f.getUniqueField()));
        m.put("sortOrder", f.getSortOrder());
        if (f.getRefEntityId() != null) m.put("refEntityId", f.getRefEntityId());
        if (f.getRelationType() != null) m.put("relationType", f.getRelationType());
        if (f.getPicklistValues() != null) m.put("picklistValues", f.getPicklistValues());
        if (f.getDefaultValue() != null) m.put("defaultValue", f.getDefaultValue());
        if (f.getMaxLength() != null) m.put("maxLength", f.getMaxLength());
        if (f.getFormula() != null) m.put("formula", f.getFormula());
        if (f.getAutoNumberFmt() != null) m.put("autoNumberFmt", f.getAutoNumberFmt());
        return m;
    }
}
