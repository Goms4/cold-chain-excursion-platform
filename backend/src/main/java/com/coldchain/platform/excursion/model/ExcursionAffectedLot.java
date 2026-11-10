package com.coldchain.platform.excursion.model;

import com.coldchain.platform.lot.model.Lot;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "excursion_affected_lots")
public class ExcursionAffectedLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "excursion_id", nullable = false)
    private ExcursionIncident excursion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @Column(name = "initial_risk_level", nullable = false, length = 32)
    private String initialRiskLevel;

    @Column(name = "disposition_status", nullable = false, length = 32)
    private String dispositionStatus = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ExcursionAffectedLot() {}

    public ExcursionAffectedLot(ExcursionIncident excursion, Lot lot, String initialRiskLevel) {
        this.excursion = excursion;
        this.lot = lot;
        this.initialRiskLevel = initialRiskLevel;
        this.dispositionStatus = "PENDING";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ExcursionIncident getExcursion() { return excursion; }
    public void setExcursion(ExcursionIncident excursion) { this.excursion = excursion; }

    public Lot getLot() { return lot; }
    public void setLot(Lot lot) { this.lot = lot; }

    public String getInitialRiskLevel() { return initialRiskLevel; }
    public void setInitialRiskLevel(String initialRiskLevel) { this.initialRiskLevel = initialRiskLevel; }

    public String getDispositionStatus() { return dispositionStatus; }
    public void setDispositionStatus(String dispositionStatus) { this.dispositionStatus = dispositionStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
