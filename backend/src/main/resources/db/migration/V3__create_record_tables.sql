CREATE TABLE records (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_def_id   UUID NOT NULL REFERENCES entity_defs(id),
    record_type_id  UUID REFERENCES record_types(id),
    name            VARCHAR(500),
    data            JSONB NOT NULL DEFAULT '{}',
    owner_id        UUID REFERENCES users(id),
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now(),
    is_deleted      BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_records_entity ON records(entity_def_id);
CREATE INDEX idx_records_name ON records(name);
CREATE INDEX idx_records_data ON records USING GIN (data jsonb_path_ops);
CREATE INDEX idx_records_entity_name ON records(entity_def_id, name);
CREATE INDEX idx_records_not_deleted ON records(entity_def_id) WHERE is_deleted = FALSE;

CREATE TABLE auto_number_seq (
    field_def_id    UUID PRIMARY KEY REFERENCES field_defs(id),
    current_value   BIGINT DEFAULT 0
);
