package com.coldchain.platform.decision.dto;

import com.coldchain.platform.decision.model.DecisionType;
import com.coldchain.platform.decision.model.DispositionDecision;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class DecisionResponse {
    private Long id;
    private String investigationId;
    private String excursionId;
    private DecisionType decision;
    private String investigatorName;
    private String reason;
    private String ruleVersion;
    private List<String> affectedLotIds;
    private Instant decidedAt;

    public DecisionResponse() {}

    public static DecisionResponse fromEntity(DispositionDecision entity) {
        DecisionResponse dto = new DecisionResponse();
        dto.setId(entity.getId());
        dto.setInvestigationId(entity.getInvestigation().getId());
        dto.setExcursionId(entity.getExcursion().getId());
        dto.setDecision(entity.getDecision());
        dto.setInvestigatorName(entity.getInvestigatorName());
        dto.setReason(entity.getReason());
        dto.setRuleVersion(entity.getRuleVersion());
        if (entity.getAffectedLotIds() != null) {
            dto.setAffectedLotIds(Arrays.asList(entity.getAffectedLotIds().split(",")));
        }
        dto.setDecidedAt(entity.getDecidedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getInvestigationId() { return investigationId; }
    public void setInvestigationId(String investigationId) { this.investigationId = investigationId; }

    public String getExcursionId() { return excursionId; }
    public void setExcursionId(String excursionId) { this.excursionId = excursionId; }

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

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
}
