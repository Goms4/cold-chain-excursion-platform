package com.coldchain.platform.decision.dto;

import com.coldchain.platform.decision.model.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RecordDecisionRequest {

    @NotNull(message = "Choose RELEASE, HOLD, or REJECT.")
    private DecisionType decision;

    private String investigatorName;

    @NotBlank(message = "Please provide a reason for this decision.")
    private String reason;

    private String ruleVersion;

    private List<String> affectedLotIds;

    public RecordDecisionRequest() {}

    public DecisionType getDecision() { return decision; }
    public void setDecision(DecisionType decision) { this.decision = decision; }

    public String getInvestigatorName() { return investigatorName; }
    public void setInvestigatorName(String investigatorName) { this.investigatorName = investigatorName; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public List<String> getAffectedLotIds() { return affectedLotIds; }
    public void setAffectedLotIds(List<String> affectedLotIds) { this.affectedLotIds = affectedLotIds; }
}
