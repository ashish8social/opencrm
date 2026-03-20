package com.opencrm.data.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencrm.common.dto.PagedResult;
import com.opencrm.common.exception.EntityNotFoundException;
import com.opencrm.common.exception.ValidationException;
import com.opencrm.data.dto.RecordDTO;
import com.opencrm.data.model.Record;
import com.opencrm.data.repository.RecordRepository;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.model.FieldDef;
import com.opencrm.metadata.model.FieldType;
import com.opencrm.metadata.repository.FieldDefRepository;
import com.opencrm.metadata.service.EntityDefService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class RecordService {

    private final RecordRepository recordRepository;
    private final EntityDefService entityDefService;
    private final FieldDefRepository fieldDefRepository;
    private final ObjectMapper objectMapper;

    public RecordService(RecordRepository recordRepository, EntityDefService entityDefService,
                         FieldDefRepository fieldDefRepository, ObjectMapper objectMapper) {
        this.recordRepository = recordRepository;
        this.entityDefService = entityDefService;
        this.fieldDefRepository = fieldDefRepository;
        this.objectMapper = objectMapper;
    }

    public PagedResult<RecordDTO> list(String entityApiName, int page, int size, String sort) {
        EntityDef entity = entityDefService.findByApiName(entityApiName);
        Sort sortObj = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortObj);

        Page<Record> records = recordRepository.findByEntityDefIdAndIsDeletedFalse(entity.getId(), pageable);

        List<RecordDTO> dtos = records.getContent().stream().map(this::toDTO).toList();
        return new PagedResult<>(dtos, records.getNumber(), records.getSize(),
                records.getTotalElements(), records.getTotalPages());
    }

    public RecordDTO get(String entityApiName, UUID id) {
        entityDefService.findByApiName(entityApiName);
        Record record = recordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Record not found: " + id));
        return toDTO(record);
    }

    public RecordDTO create(String entityApiName, Map<String, Object> data, UUID userId) {
        EntityDef entity = entityDefService.findByApiName(entityApiName);
        List<FieldDef> fields = fieldDefRepository.findByEntityDefIdOrderBySortOrderAsc(entity.getId());

        validateData(fields, data);

        Record record = new Record();
        record.setEntityDefId(entity.getId());
        record.setOwnerId(userId);
        record.setCreatedBy(userId);

        String nameField = entity.getNameField() != null ? entity.getNameField() : "Name";
        Object nameValue = data.get(nameField);
        record.setName(nameValue != null ? nameValue.toString() : null);

        try {
            record.setData(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid data format");
        }

        record = recordRepository.save(record);
        return toDTO(record);
    }

    public RecordDTO update(String entityApiName, UUID id, Map<String, Object> data, UUID userId) {
        EntityDef entity = entityDefService.findByApiName(entityApiName);
        Record record = recordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Record not found: " + id));

        List<FieldDef> fields = fieldDefRepository.findByEntityDefIdOrderBySortOrderAsc(entity.getId());

        Map<String, Object> existingData = parseData(record.getData());
        existingData.putAll(data);

        validateData(fields, existingData);

        String nameField = entity.getNameField() != null ? entity.getNameField() : "Name";
        Object nameValue = existingData.get(nameField);
        record.setName(nameValue != null ? nameValue.toString() : record.getName());

        try {
            record.setData(objectMapper.writeValueAsString(existingData));
        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid data format");
        }

        record = recordRepository.save(record);
        return toDTO(record);
    }

    public void delete(String entityApiName, UUID id) {
        EntityDef entity = entityDefService.findByApiName(entityApiName);
        Record record = recordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Record not found: " + id));

        record.setIsDeleted(true);
        recordRepository.save(record);

        cascadeDelete(entity.getId(), id);
    }

    public List<RecordDTO> getRelated(String entityApiName, UUID id, String relatedEntityApiName) {
        EntityDef parentEntity = entityDefService.findByApiName(entityApiName);
        EntityDef relatedEntity = entityDefService.findByApiName(relatedEntityApiName);

        recordRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Record not found: " + id));

        List<FieldDef> relatedFields = fieldDefRepository.findByEntityDefIdOrderBySortOrderAsc(relatedEntity.getId());
        FieldDef lookupField = relatedFields.stream()
                .filter(f -> parentEntity.getId().equals(f.getRefEntityId()))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "No relationship from " + relatedEntityApiName + " to " + entityApiName));

        List<Record> records = recordRepository.findByEntityAndFieldValue(
                relatedEntity.getId(), lookupField.getApiName(), id.toString());

        return records.stream().map(this::toDTO).toList();
    }

    public List<RecordDTO> search(String term, List<String> entityApiNames) {
        List<UUID> entityIds;
        if (entityApiNames != null && !entityApiNames.isEmpty()) {
            entityIds = entityApiNames.stream()
                    .map(name -> entityDefService.findByApiName(name).getId())
                    .toList();
        } else {
            entityIds = entityDefService.findAll().stream().map(EntityDef::getId).toList();
        }

        if (entityIds.isEmpty()) return List.of();

        return recordRepository.search(entityIds, term).stream().map(this::toDTO).toList();
    }

    public List<RecordDTO> searchByName(String entityApiName, String term) {
        EntityDef entity = entityDefService.findByApiName(entityApiName);
        return recordRepository.searchByName(entity.getId(), term).stream().map(this::toDTO).toList();
    }

    private void cascadeDelete(UUID parentEntityId, UUID parentRecordId) {
        List<FieldDef> childFields = fieldDefRepository.findByRefEntityId(parentEntityId);
        for (FieldDef childField : childFields) {
            if (Boolean.TRUE.equals(childField.getCascadeDelete())) {
                List<Record> children = recordRepository.findByEntityAndFieldValue(
                        childField.getEntityDef().getId(), childField.getApiName(), parentRecordId.toString());
                for (Record child : children) {
                    child.setIsDeleted(true);
                    recordRepository.save(child);
                    cascadeDelete(childField.getEntityDef().getId(), child.getId());
                }
            }
        }
    }

    private void validateData(List<FieldDef> fields, Map<String, Object> data) {
        for (FieldDef field : fields) {
            Object value = data.get(field.getApiName());

            if (Boolean.TRUE.equals(field.getRequired()) && (value == null || value.toString().isBlank())) {
                throw new ValidationException("Required field missing: " + field.getLabel());
            }

            if (value != null && !value.toString().isBlank()) {
                validateFieldType(field, value);
            }
        }
    }

    private void validateFieldType(FieldDef field, Object value) {
        switch (field.getFieldType()) {
            case NUMBER:
                try { Long.parseLong(value.toString()); }
                catch (NumberFormatException e) { throw new ValidationException(field.getLabel() + " must be a whole number"); }
                break;
            case DECIMAL, CURRENCY:
                try { Double.parseDouble(value.toString()); }
                catch (NumberFormatException e) { throw new ValidationException(field.getLabel() + " must be a number"); }
                break;
            case BOOLEAN:
                if (!(value instanceof Boolean) && !"true".equalsIgnoreCase(value.toString()) && !"false".equalsIgnoreCase(value.toString())) {
                    throw new ValidationException(field.getLabel() + " must be true or false");
                }
                break;
            case PICKLIST:
                if (field.getPicklistValues() != null) {
                    try {
                        List<Map<String, Object>> options = objectMapper.readValue(field.getPicklistValues(), new TypeReference<>() {});
                        boolean valid = options.stream().anyMatch(o -> value.toString().equals(o.get("value").toString()));
                        if (!valid) throw new ValidationException(field.getLabel() + ": invalid picklist value: " + value);
                    } catch (JsonProcessingException ignored) {}
                }
                break;
            default:
                break;
        }
    }

    private RecordDTO toDTO(Record record) {
        Map<String, Object> data = parseData(record.getData());
        return new RecordDTO(record.getId(), record.getEntityDefId(), record.getName(),
                data, record.getOwnerId(), record.getCreatedBy(),
                record.getCreatedAt(), record.getUpdatedAt());
    }

    private Map<String, Object> parseData(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.DESC, "updatedAt");
        String[] parts = sort.split(",");
        if (parts.length == 2) {
            Sort.Direction dir = "desc".equalsIgnoreCase(parts[1]) ? Sort.Direction.DESC : Sort.Direction.ASC;
            return Sort.by(dir, "name".equalsIgnoreCase(parts[0]) ? "name" : "updatedAt");
        }
        return Sort.by(Sort.Direction.DESC, "updatedAt");
    }
}
