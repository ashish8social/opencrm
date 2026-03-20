package com.opencrm.metadata.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "field_defs", uniqueConstraints = @UniqueConstraint(columnNames = {"entity_def_id", "api_name"}))
public class FieldDef {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entity_def_id", nullable = false)
    private EntityDef entityDef;

    @Column(name = "api_name", nullable = false, length = 100)
    private String apiName;

    @Column(nullable = false)
    private String label;

    @Column(name = "field_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private FieldType fieldType;

    private Boolean required = false;

    @Column(name = "unique_field")
    private Boolean uniqueField = false;

    @Column(name = "default_value")
    private String defaultValue;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "ref_entity_id")
    private UUID refEntityId;

    @Column(name = "relation_type", length = 20)
    private String relationType;

    @Column(name = "cascade_delete")
    private Boolean cascadeDelete = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "picklist_values", columnDefinition = "jsonb")
    private String picklistValues;

    private String formula;

    @Column(name = "auto_number_fmt")
    private String autoNumberFmt;

    @Column(name = "precision_val")
    private Integer precisionVal;

    @Column(name = "scale_val")
    private Integer scaleVal;

    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public EntityDef getEntityDef() { return entityDef; }
    public void setEntityDef(EntityDef entityDef) { this.entityDef = entityDef; }
    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public FieldType getFieldType() { return fieldType; }
    public void setFieldType(FieldType fieldType) { this.fieldType = fieldType; }
    public Boolean getRequired() { return required; }
    public void setRequired(Boolean required) { this.required = required; }
    public Boolean getUniqueField() { return uniqueField; }
    public void setUniqueField(Boolean uniqueField) { this.uniqueField = uniqueField; }
    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public UUID getRefEntityId() { return refEntityId; }
    public void setRefEntityId(UUID refEntityId) { this.refEntityId = refEntityId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public Boolean getCascadeDelete() { return cascadeDelete; }
    public void setCascadeDelete(Boolean cascadeDelete) { this.cascadeDelete = cascadeDelete; }
    public String getPicklistValues() { return picklistValues; }
    public void setPicklistValues(String picklistValues) { this.picklistValues = picklistValues; }
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }
    public String getAutoNumberFmt() { return autoNumberFmt; }
    public void setAutoNumberFmt(String autoNumberFmt) { this.autoNumberFmt = autoNumberFmt; }
    public Integer getPrecisionVal() { return precisionVal; }
    public void setPrecisionVal(Integer precisionVal) { this.precisionVal = precisionVal; }
    public Integer getScaleVal() { return scaleVal; }
    public void setScaleVal(Integer scaleVal) { this.scaleVal = scaleVal; }
    public Integer getMaxLength() { return maxLength; }
    public void setMaxLength(Integer maxLength) { this.maxLength = maxLength; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
