package com.opencrm.metadata.service;

import com.opencrm.common.exception.EntityNotFoundException;
import com.opencrm.common.exception.ValidationException;
import com.opencrm.metadata.model.EntityDef;
import com.opencrm.metadata.repository.EntityDefRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EntityDefService {

    private final EntityDefRepository entityDefRepository;

    public EntityDefService(EntityDefRepository entityDefRepository) {
        this.entityDefRepository = entityDefRepository;
    }

    public List<EntityDef> findAll() {
        return entityDefRepository.findAll();
    }

    public EntityDef findByApiName(String apiName) {
        return entityDefRepository.findByApiName(apiName)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + apiName));
    }

    public EntityDef findByApiNameWithFields(String apiName) {
        return entityDefRepository.findByApiNameWithFields(apiName)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + apiName));
    }

    public EntityDef findById(UUID id) {
        return entityDefRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Entity not found: " + id));
    }

    public EntityDef create(EntityDef entityDef) {
        if (entityDefRepository.existsByApiName(entityDef.getApiName())) {
            throw new ValidationException("Entity already exists: " + entityDef.getApiName());
        }
        entityDef.setIsCustom(true);
        return entityDefRepository.save(entityDef);
    }

    public EntityDef update(String apiName, EntityDef updates) {
        EntityDef existing = findByApiName(apiName);
        if (updates.getLabel() != null) existing.setLabel(updates.getLabel());
        if (updates.getPluralLabel() != null) existing.setPluralLabel(updates.getPluralLabel());
        if (updates.getDescription() != null) existing.setDescription(updates.getDescription());
        if (updates.getIcon() != null) existing.setIcon(updates.getIcon());
        if (updates.getNameField() != null) existing.setNameField(updates.getNameField());
        return entityDefRepository.save(existing);
    }

    public void delete(String apiName) {
        EntityDef existing = findByApiName(apiName);
        if (!Boolean.TRUE.equals(existing.getIsCustom())) {
            throw new ValidationException("Cannot delete standard entity: " + apiName);
        }
        entityDefRepository.delete(existing);
    }
}
