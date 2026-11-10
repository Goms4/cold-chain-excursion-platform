package com.coldchain.platform.investigation.model;

import com.coldchain.platform.excursion.model.ExcursionIncident;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "investigations")
public class Investigation {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "excursion_id", nullable = false, unique = true)
    private ExcursionIncident excursion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private InvestigationStatus status = InvestigationStatus.OPEN;

    @Column(name = "assigned_investigator", length = 128)
    private String assignedInvestigator;

    @Column(name = "sla_due_at", nullable = false)
    private Instant slaDueAt;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt = Instant.now();

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Investigation() {}

    public Investigation(String id, ExcursionIncident excursion, String assignedInvestigator, Instant slaDueAt) {
        this.id = id;
        this.excursion = excursion;
        this.assignedInvestigator = assignedInvestigator;
        this.slaDueAt = slaDueAt;
        this.status = InvestigationStatus.OPEN;
        this.openedAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isBusinessFinalized() {
        return this.status == InvestigationStatus.COMPLETED;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ExcursionIncident getExcursion() { return excursion; }
    public void setExcursion(ExcursionIncident excursion) { this.excursion = excursion; }

    public InvestigationStatus getStatus() { return status; }
    public void setStatus(InvestigationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getAssignedInvestigator() { return assignedInvestigator; }
    public void setAssignedInvestigator(String assignedInvestigator) { this.assignedInvestigator = assignedInvestigator; }

    public Instant getSlaDueAt() { return slaDueAt; }
    public void setSlaDueAt(Instant slaDueAt) { this.slaDueAt = slaDueAt; }

    public Instant getOpenedAt() { return openedAt; }
    public void setOpenedAt(Instant openedAt) { this.openedAt = openedAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
