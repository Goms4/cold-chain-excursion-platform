package com.coldchain.platform.decision.model;

import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.investigation.model.Investigation;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "disposition_decisions")
public class DispositionDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_id", nullable = false)
    private Investigation investigation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "excursion_id", nullable = false)
    private ExcursionIncident excursion;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private DecisionType decision;

    @Column(name = "investigator_name", nullable = false, length = 128)
    private String investigatorName;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "rule_version", nullable = false, length = 32)
    private String ruleVersion;

    @Column(name = "affected_lot_ids", nullable = false, columnDefinition = "TEXT")
    private String affectedLotIds;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DispositionDecision() {}

    public DispositionDecision(Investigation investigation, ExcursionIncident excursion,
                               DecisionType decision, String investigatorName, String reason,
                               String ruleVersion, String affectedLotIds, Instant decidedAt) {
        this.investigation = investigation;
        this.excursion = excursion;
        this.decision = decision;
        this.investigatorName = investigatorName;
        this.reason = reason;
        this.ruleVersion = ruleVersion;
        this.affectedLotIds = affectedLotIds;
        this.decidedAt = decidedAt != null ? decidedAt : Instant.now();
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Investigation getInvestigation() { return investigation; }
    public void setInvestigation(Investigation investigation) { this.investigation = investigation; }

    public ExcursionIncident getExcursion() { return excursion; }
    public void setExcursion(ExcursionIncident excursion) { this.excursion = excursion; }

    public DecisionType getDecision() { return decision; }
    public void setDecision(DecisionType decision) { this.decision = decision; }

    public String getInvestigatorName() { return investigatorName; }
    public void setInvestigatorName(String investigatorName) { this.investigatorName = investigatorName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public String getAffectedLotIds() { return affectedLotIds; }
    public void setAffectedLotIds(String affectedLotIds) { this.affectedLotIds = affectedLotIds; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
