package com.opencrm.data.repository;

import com.opencrm.data.model.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecordRepository extends JpaRepository<Record, UUID> {

    Page<Record> findByEntityDefIdAndIsDeletedFalse(UUID entityDefId, Pageable pageable);

    Optional<Record> findByIdAndIsDeletedFalse(UUID id);

    @Query(value = "SELECT * FROM records WHERE entity_def_id = :entityDefId AND is_deleted = false " +
            "AND data->>:fieldApiName = :value", nativeQuery = true)
    List<Record> findByEntityAndFieldValue(@Param("entityDefId") UUID entityDefId,
                                            @Param("fieldApiName") String fieldApiName,
                                            @Param("value") String value);

    @Query(value = "SELECT * FROM records WHERE entity_def_id IN :entityDefIds AND is_deleted = false " +
            "AND (name ILIKE '%' || :term || '%' OR data::text ILIKE '%' || :term || '%') " +
            "ORDER BY updated_at DESC LIMIT 20", nativeQuery = true)
    List<Record> search(@Param("entityDefIds") List<UUID> entityDefIds, @Param("term") String term);

    @Query(value = "SELECT * FROM records WHERE entity_def_id = :entityDefId AND is_deleted = false " +
            "AND name ILIKE '%' || :term || '%' ORDER BY name LIMIT 10", nativeQuery = true)
    List<Record> searchByName(@Param("entityDefId") UUID entityDefId, @Param("term") String term);
}
