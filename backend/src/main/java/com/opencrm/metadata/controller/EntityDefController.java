package com.opencrm.metadata.controller;

import com.opencrm.common.dto.ApiResponse;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.service.EntityDefService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata/entities")
public class EntityDefController {

    private final EntityDefService entityDefService;

    public EntityDefController(EntityDefService entityDefService) {
        this.entityDefService = entityDefService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list() {
        List<Map<String, Object>> entities = entityDefService.findAll().stream()
                .map(this::toSummary)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(entities));
    }

    @GetMapping("/{apiName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable String apiName) {
        EntityDef entity = entityDefService.findByApiNameWithFields(apiName);
        return ResponseEntity.ok(ApiResponse.success(toDetail(entity)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody EntityDef entityDef) {
        EntityDef created = entityDefService.create(entityDef);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(toSummary(created)));
    }

    @PutMapping("/{apiName}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable String apiName,
                                                                    @RequestBody EntityDef entityDef) {
        EntityDef updated = entityDefService.update(apiName, entityDef);
        return ResponseEntity.ok(ApiResponse.success(toSummary(updated)));
    }

    @DeleteMapping("/{apiName}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String apiName) {
        entityDefService.delete(apiName);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Map<String, Object> toSummary(EntityDef e) {
        return Map.of(
                "id", e.getId(),
                "apiName", e.getApiName(),
                "label", e.getLabel(),
                "pluralLabel", e.getPluralLabel(),
                "isCustom", Boolean.TRUE.equals(e.getIsCustom()),
                "icon", e.getIcon() != null ? e.getIcon() : ""
        );
    }

    private Map<String, Object> toDetail(EntityDef e) {
        var fields = e.getFields().stream().map(f -> {
            var m = new java.util.HashMap<String, Object>();
            m.put("id", f.getId());
            m.put("apiName", f.getApiName());
            m.put("label", f.getLabel());
            m.put("fieldType", f.getFieldType().name());
            m.put("required", Boolean.TRUE.equals(f.getRequired()));
            m.put("sortOrder", f.getSortOrder());
            if (f.getRefEntityId() != null) m.put("refEntityId", f.getRefEntityId());
            if (f.getRelationType() != null) m.put("relationType", f.getRelationType());
            if (f.getPicklistValues() != null) m.put("picklistValues", f.getPicklistValues());
            if (f.getDefaultValue() != null) m.put("defaultValue", f.getDefaultValue());
            if (f.getMaxLength() != null) m.put("maxLength", f.getMaxLength());
            if (f.getFormula() != null) m.put("formula", f.getFormula());
            if (f.getAutoNumberFmt() != null) m.put("autoNumberFmt", f.getAutoNumberFmt());
            return (Map<String, Object>) m;
        }).toList();

        var result = new java.util.HashMap<String, Object>();
        result.put("id", e.getId());
        result.put("apiName", e.getApiName());
        result.put("label", e.getLabel());
        result.put("pluralLabel", e.getPluralLabel());
        result.put("description", e.getDescription() != null ? e.getDescription() : "");
        result.put("isCustom", Boolean.TRUE.equals(e.getIsCustom()));
        result.put("icon", e.getIcon() != null ? e.getIcon() : "");
        result.put("nameField", e.getNameField() != null ? e.getNameField() : "Name");
        result.put("fields", fields);
        return result;
    }
}
