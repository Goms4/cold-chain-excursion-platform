package com.coldchain.platform.investigation.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignInvestigatorRequest {
    @NotBlank(message = "investigator is required")
    private String investigator;

    public String getInvestigator() { return investigator; }
    public void setInvestigator(String investigator) { this.investigator = investigator; }
}
