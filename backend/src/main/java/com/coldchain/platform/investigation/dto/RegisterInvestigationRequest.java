package com.coldchain.platform.investigation.dto;

import jakarta.validation.constraints.NotBlank;

public class RegisterInvestigationRequest {
    @NotBlank private String shipmentId;
    @NotBlank private String lotId;
    @NotBlank private String excursionId;
    private String investigator;
    private String issueType = "TEMPERATURE_EXCURSION";

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }
    public String getExcursionId() { return excursionId; }
    public void setExcursionId(String excursionId) { this.excursionId = excursionId; }
    public String getInvestigator() { return investigator; }
    public void setInvestigator(String investigator) { this.investigator = investigator; }
    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }
}
