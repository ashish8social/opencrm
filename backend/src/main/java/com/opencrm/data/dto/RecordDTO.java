package com.opencrm.data.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RecordDTO(
    UUID id,
    UUID entityDefId,
    String name,
    Map<String, Object> data,
    UUID ownerId,
    UUID createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
