package com.opencrm.metadata.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "entity_defs")
public class EntityDef {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "api_name", unique = true, nullable = false, length = 100)
    private String apiName;

    @Column(nullable = false)
    private String label;

    @Column(name = "plural_label", nullable = false)
    private String pluralLabel;

    private String description;

    @Column(name = "is_custom")
    private Boolean isCustom = false;

    @Column(length = 50)
    private String icon;

    @Column(name = "name_field", length = 100)
    private String nameField = "Name";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "entityDef", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<FieldDef> fields = new ArrayList<>();

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
    public String getApiName() { return apiName; }
    public void setApiName(String apiName) { this.apiName = apiName; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getPluralLabel() { return pluralLabel; }
    public void setPluralLabel(String pluralLabel) { this.pluralLabel = pluralLabel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getIsCustom() { return isCustom; }
    public void setIsCustom(Boolean isCustom) { this.isCustom = isCustom; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getNameField() { return nameField; }
    public void setNameField(String nameField) { this.nameField = nameField; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<FieldDef> getFields() { return fields; }
    public void setFields(List<FieldDef> fields) { this.fields = fields; }
}
