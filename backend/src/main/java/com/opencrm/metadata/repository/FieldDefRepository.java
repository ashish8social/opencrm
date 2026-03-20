package com.opencrm.metadata.repository;

import com.opencrm.metadata.model.FieldDef;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FieldDefRepository extends JpaRepository<FieldDef, UUID> {
    List<FieldDef> findByEntityDefIdOrderBySortOrderAsc(UUID entityDefId);
    Optional<FieldDef> findByEntityDefIdAndApiName(UUID entityDefId, String apiName);
    List<FieldDef> findByRefEntityId(UUID refEntityId);
}
