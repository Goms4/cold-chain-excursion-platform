package com.coldchain.platform.investigation.dto;

import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.model.ManualInspectionRecord;

import java.time.Instant;
import java.util.List;

public class InvestigationResponse {
    private String id;
    private String excursionId;
    private String shipmentId;
    private InvestigationStatus status;
    private String assignedInvestigator;
    private Instant slaDueAt;
    private Instant openedAt;
    private Instant closedAt;
    private String notes;
    private List<ManualInspectionRecord> manualInspections;
    private Instant createdAt;

    public InvestigationResponse() {}

    public static InvestigationResponse fromEntity(Investigation inv, List<ManualInspectionRecord> manualInspections) {
        InvestigationResponse dto = new InvestigationResponse();
        dto.setId(inv.getId());
        dto.setExcursionId(inv.getExcursion().getId());
        dto.setShipmentId(inv.getExcursion().getShipment().getId());
        dto.setStatus(inv.getStatus());
        dto.setAssignedInvestigator(inv.getAssignedInvestigator());
        dto.setSlaDueAt(inv.getSlaDueAt());
        dto.setOpenedAt(inv.getOpenedAt());
        dto.setClosedAt(inv.getClosedAt());
        dto.setNotes(inv.getNotes());
        dto.setManualInspections(manualInspections);
        dto.setCreatedAt(inv.getCreatedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExcursionId() { return excursionId; }
    public void setExcursionId(String excursionId) { this.excursionId = excursionId; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public InvestigationStatus getStatus() { return status; }
    public void setStatus(InvestigationStatus status) { this.status = status; }

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

    public List<ManualInspectionRecord> getManualInspections() { return manualInspections; }
    public void setManualInspections(List<ManualInspectionRecord> manualInspections) { this.manualInspections = manualInspections; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
