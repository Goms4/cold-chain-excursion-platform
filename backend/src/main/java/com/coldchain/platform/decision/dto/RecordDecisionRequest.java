package com.coldchain.platform.decision.dto;

import com.coldchain.platform.decision.model.DecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class RecordDecisionRequest {

    @NotNull(message = "decision is required (RELEASE, HOLD, REJECT)")
    private DecisionType decision;

    @NotBlank(message = "investigator_name is required")
    private String investigatorName;

    @NotBlank(message = "reason is required")
    private String reason;

    @NotBlank(message = "rule_version is required")
    private String ruleVersion;

    @NotEmpty(message = "affected_lot_ids list cannot be empty")
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
