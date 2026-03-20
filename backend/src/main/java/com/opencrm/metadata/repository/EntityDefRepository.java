package com.opencrm.metadata.repository;

import com.opencrm.metadata.model.EntityDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface EntityDefRepository extends JpaRepository<EntityDef, UUID> {
    Optional<EntityDef> findByApiName(String apiName);

    @Query("SELECT e FROM EntityDef e LEFT JOIN FETCH e.fields WHERE e.apiName = :apiName")
    Optional<EntityDef> findByApiNameWithFields(@Param("apiName") String apiName);

    boolean existsByApiName(String apiName);
}
