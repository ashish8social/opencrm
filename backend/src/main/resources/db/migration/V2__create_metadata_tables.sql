CREATE TABLE entity_defs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_name        VARCHAR(100) UNIQUE NOT NULL,
    label           VARCHAR(255) NOT NULL,
    plural_label    VARCHAR(255) NOT NULL,
    description     TEXT,
    is_custom       BOOLEAN DEFAULT FALSE,
    icon            VARCHAR(50),
    name_field      VARCHAR(100) DEFAULT 'Name',
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_entity_defs_api_name ON entity_defs(api_name);

CREATE TABLE field_defs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_def_id     UUID NOT NULL REFERENCES entity_defs(id) ON DELETE CASCADE,
    api_name          VARCHAR(100) NOT NULL,
    label             VARCHAR(255) NOT NULL,
    field_type        VARCHAR(30) NOT NULL,
    required          BOOLEAN DEFAULT FALSE,
    unique_field      BOOLEAN DEFAULT FALSE,
    default_value     TEXT,
    sort_order        INT DEFAULT 0,
    ref_entity_id     UUID REFERENCES entity_defs(id),
    relation_type     VARCHAR(20),
    cascade_delete    BOOLEAN DEFAULT FALSE,
    picklist_values   JSONB,
    formula           TEXT,
    auto_number_fmt   TEXT,
    precision_val     INT,
    scale_val         INT,
    max_length        INT,
    created_at        TIMESTAMPTZ DEFAULT now(),
    updated_at        TIMESTAMPTZ DEFAULT now(),
    UNIQUE(entity_def_id, api_name)
);

CREATE INDEX idx_field_defs_entity ON field_defs(entity_def_id);

CREATE TABLE validation_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_def_id   UUID NOT NULL REFERENCES entity_defs(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    active          BOOLEAN DEFAULT TRUE,
    condition_expr  TEXT NOT NULL,
    error_message   TEXT NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE record_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_def_id   UUID NOT NULL REFERENCES entity_defs(id) ON DELETE CASCADE,
    api_name        VARCHAR(100) NOT NULL,
    label           VARCHAR(255) NOT NULL,
    active          BOOLEAN DEFAULT TRUE,
    UNIQUE(entity_def_id, api_name)
);

CREATE TABLE page_layouts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_def_id   UUID NOT NULL REFERENCES entity_defs(id) ON DELETE CASCADE,
    record_type_id  UUID REFERENCES record_types(id),
    name            VARCHAR(255) NOT NULL,
    layout          JSONB NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT now()
);
