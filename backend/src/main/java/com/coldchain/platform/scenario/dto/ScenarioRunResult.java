package com.coldchain.platform.scenario.dto;

import java.util.List;
import java.util.Map;

public class ScenarioRunResult {
    private String scenarioName;
    private String description;
    private boolean success;
    private String shipmentId;
    private int eventsIngested;
    private int incidentsCreated;
    private String excursionSeverity;
    private boolean hasDuplicate;
    private boolean hasMissingInterval;
    private boolean hasContradictoryEvidence;
    private List<String> verificationNotes;

    public ScenarioRunResult() {}

    public String getScenarioName() { return scenarioName; }
    public void setScenarioName(String scenarioName) { this.scenarioName = scenarioName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public int getEventsIngested() { return eventsIngested; }
    public void setEventsIngested(int eventsIngested) { this.eventsIngested = eventsIngested; }

    public int getIncidentsCreated() { return incidentsCreated; }
    public void setIncidentsCreated(int incidentsCreated) { this.incidentsCreated = incidentsCreated; }

    public String getExcursionSeverity() { return excursionSeverity; }
    public void setExcursionSeverity(String excursionSeverity) { this.excursionSeverity = excursionSeverity; }

    public boolean isHasDuplicate() { return hasDuplicate; }
    public void setHasDuplicate(boolean hasDuplicate) { this.hasDuplicate = hasDuplicate; }

    public boolean isHasMissingInterval() { return hasMissingInterval; }
    public void setHasMissingInterval(boolean hasMissingInterval) { this.hasMissingInterval = hasMissingInterval; }

    public boolean isHasContradictoryEvidence() { return hasContradictoryEvidence; }
    public void setHasContradictoryEvidence(boolean hasContradictoryEvidence) { this.hasContradictoryEvidence = hasContradictoryEvidence; }

    public List<String> getVerificationNotes() { return verificationNotes; }
    public void setVerificationNotes(List<String> verificationNotes) { this.verificationNotes = verificationNotes; }
}
