package com.coldchain.platform.investigation.dto;

import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.model.ManualInspectionRecord;
import com.coldchain.platform.common.BusinessIds;
import java.math.BigDecimal;

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
    private List<ManualInspectionResponse> manualInspections;
    private Instant createdAt;
    private String investigationId;
    private String issueType = "TEMPERATURE_EXCURSION";
    private String severity;
    private Instant startedAt;
    private Integer durationMinutes;
    private BigDecimal peakTemperatureC;
    private String investigator;

    public InvestigationResponse() {}

    public static InvestigationResponse fromEntity(Investigation inv, List<ManualInspectionRecord> manualInspections) {
        InvestigationResponse dto = new InvestigationResponse();
        dto.setId(BusinessIds.investigation(inv.getId()));
        dto.setInvestigationId(BusinessIds.investigation(inv.getId()));
        dto.setExcursionId(inv.getExcursion().getId());
        dto.setShipmentId(BusinessIds.shipment(inv.getExcursion().getShipment().getId()));
        dto.setStatus(inv.getStatus());
        dto.setAssignedInvestigator(inv.getAssignedInvestigator());
        dto.setSlaDueAt(inv.getSlaDueAt());
        dto.setOpenedAt(inv.getOpenedAt());
        dto.setClosedAt(inv.getClosedAt());
        dto.setNotes(inv.getNotes());
        dto.setManualInspections(manualInspections.stream().map(ManualInspectionResponse::from).toList());
        dto.setCreatedAt(inv.getCreatedAt());
        dto.setSeverity(inv.getExcursion().getSeverity().name());
        dto.setStartedAt(inv.getExcursion().getStartEventTime());
        dto.setDurationMinutes(inv.getExcursion().getDurationMinutes());
        dto.setPeakTemperatureC(inv.getExcursion().getPeakTemperatureC());
        dto.setInvestigator(inv.getAssignedInvestigator());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInvestigationId() { return investigationId; }
    public void setInvestigationId(String investigationId) { this.investigationId = investigationId; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public BigDecimal getPeakTemperatureC() { return peakTemperatureC; }
    public void setPeakTemperatureC(BigDecimal peakTemperatureC) { this.peakTemperatureC = peakTemperatureC; }
    public String getInvestigator() { return investigator; }
    public void setInvestigator(String investigator) { this.investigator = investigator; }

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

    public List<ManualInspectionResponse> getManualInspections() { return manualInspections; }
    public void setManualInspections(List<ManualInspectionResponse> manualInspections) { this.manualInspections = manualInspections; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
