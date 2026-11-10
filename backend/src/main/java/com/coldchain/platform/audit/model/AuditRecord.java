package com.coldchain.platform.audit.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_history")
public class AuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "actor", nullable = false, length = 128)
    private String actor;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "payload_before", columnDefinition = "TEXT")
    private String payloadBefore;

    @Column(name = "payload_after", columnDefinition = "TEXT")
    private String payloadAfter;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AuditRecord() {}

    public AuditRecord(String entityType, String entityId, String action, String actor,
                       String payloadBefore, String payloadAfter, String rationale) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.actor = actor;
        this.timestamp = Instant.now();
        this.payloadBefore = payloadBefore;
        this.payloadAfter = payloadAfter;
        this.rationale = rationale;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getPayloadBefore() { return payloadBefore; }
    public void setPayloadBefore(String payloadBefore) { this.payloadBefore = payloadBefore; }

    public String getPayloadAfter() { return payloadAfter; }
    public void setPayloadAfter(String payloadAfter) { this.payloadAfter = payloadAfter; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
