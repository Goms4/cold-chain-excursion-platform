package com.coldchain.platform.audit.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(name = "audit_history")
public class AuditRecord {
    // H2 test profile has no PostgreSQL trigger; production values are always overwritten in the database.
    private static final AtomicLong TEST_CHAIN_SEQUENCE = new AtomicLong();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seq", nullable = false, updatable = false, unique = true)
    private Long seq;

    @Column(name = "prev_hash", nullable = false, updatable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String prevHash;

    @Column(name = "row_hash", nullable = false, updatable = false, length = 64, columnDefinition = "CHAR(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String rowHash;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "action", nullable = false, length = 32)
    private String action;

    @Column(name = "category", nullable = false, length = 16, updatable = false)
    private String category = "BUSINESS";

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

    @PrePersist
    private void provideTestProfileChainPlaceholders() {
        if (seq == null) {
            seq = TEST_CHAIN_SEQUENCE.incrementAndGet();
            prevHash = "0".repeat(64);
            rowHash = "0".repeat(64);
        }
    }

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

    public AuditRecord(String entityType, String entityId, String action, String category, String actor,
                       String payloadBefore, String payloadAfter, String rationale) {
        this(entityType, entityId, action, actor, payloadBefore, payloadAfter, rationale);
        this.category = category;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSeq() { return seq; }
    public void setSeq(Long seq) { this.seq = seq; }
    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }
    public String getRowHash() { return rowHash; }
    public void setRowHash(String rowHash) { this.rowHash = rowHash; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

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
