package com.opencrm.metadata.service;

import com.opencrm.common.exception.EntityNotFoundException;
import com.opencrm.common.exception.ValidationException;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.model.FieldDef;
import com.opencrm.metadata.model.FieldType;
import com.opencrm.metadata.repository.EntityDefRepository;
import com.opencrm.metadata.repository.FieldDefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FieldDefService {

    private final FieldDefRepository fieldDefRepository;
    private final EntityDefRepository entityDefRepository;

    public FieldDefService(FieldDefRepository fieldDefRepository, EntityDefRepository entityDefRepository) {
        this.fieldDefRepository = fieldDefRepository;
        this.entityDefRepository = entityDefRepository;
    }

    public List<FieldDef> findByEntity(String entityApiName) {
        EntityDef entity = entityDefRepository.findByApiName(entityApiName)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityApiName));
        return fieldDefRepository.findByEntityDefIdOrderBySortOrderAsc(entity.getId());
    }

    public FieldDef create(String entityApiName, FieldDef fieldDef) {
        EntityDef entity = entityDefRepository.findByApiName(entityApiName)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityApiName));

        if (fieldDefRepository.findByEntityDefIdAndApiName(entity.getId(), fieldDef.getApiName()).isPresent()) {
            throw new ValidationException("Field already exists: " + fieldDef.getApiName());
        }

        validateFieldDef(fieldDef);
        fieldDef.setEntityDef(entity);
        return fieldDefRepository.save(fieldDef);
    }

    public FieldDef update(String entityApiName, String fieldApiName, FieldDef updates) {
        EntityDef entity = entityDefRepository.findByApiName(entityApiName)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityApiName));
        FieldDef existing = fieldDefRepository.findByEntityDefIdAndApiName(entity.getId(), fieldApiName)
                .orElseThrow(() -> new EntityNotFoundException("Field not found: " + fieldApiName));

        if (updates.getLabel() != null) existing.setLabel(updates.getLabel());
        if (updates.getRequired() != null) existing.setRequired(updates.getRequired());
        if (updates.getDefaultValue() != null) existing.setDefaultValue(updates.getDefaultValue());
        if (updates.getSortOrder() != null) existing.setSortOrder(updates.getSortOrder());
        if (updates.getPicklistValues() != null) existing.setPicklistValues(updates.getPicklistValues());
        if (updates.getMaxLength() != null) existing.setMaxLength(updates.getMaxLength());

        return fieldDefRepository.save(existing);
    }

    public void delete(String entityApiName, String fieldApiName) {
        EntityDef entity = entityDefRepository.findByApiName(entityApiName)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + entityApiName));
        FieldDef field = fieldDefRepository.findByEntityDefIdAndApiName(entity.getId(), fieldApiName)
                .orElseThrow(() -> new EntityNotFoundException("Field not found: " + fieldApiName));
        fieldDefRepository.delete(field);
    }

    private void validateFieldDef(FieldDef fieldDef) {
        if (fieldDef.getFieldType() == FieldType.LOOKUP || fieldDef.getFieldType() == FieldType.MASTER_DETAIL) {
            if (fieldDef.getRefEntityId() == null) {
                throw new ValidationException("Relationship fields must specify refEntityId");
            }
            if (!entityDefRepository.existsById(fieldDef.getRefEntityId())) {
                throw new ValidationException("Referenced entity not found: " + fieldDef.getRefEntityId());
            }
            if (fieldDef.getFieldType() == FieldType.MASTER_DETAIL) {
                fieldDef.setCascadeDelete(true);
                fieldDef.setRequired(true);
                fieldDef.setRelationType("MASTER_DETAIL");
            } else {
                fieldDef.setRelationType("LOOKUP");
            }
        }

        if (fieldDef.getFieldType() == FieldType.PICKLIST || fieldDef.getFieldType() == FieldType.MULTI_PICKLIST) {
            if (fieldDef.getPicklistValues() == null || fieldDef.getPicklistValues().isBlank()) {
                throw new ValidationException("Picklist fields must specify picklistValues");
            }
        }
    }
}
