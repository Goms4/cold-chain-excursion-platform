package com.coldchain.platform.reconciliation.dto;

public class ReconciliationSummary {
    private long totalRawEvents;
    private long totalProcessedEvents;
    private long totalDuplicateEvents;
    private long totalQuarantinedEvents;
    private long totalMissingIntervals;
    private long totalShipments;
    private long totalLots;
    private long totalSensors;
    private long totalExcursions;
    private long totalInvestigations;
    private long totalDecisions;
    private long totalAuditRecords;
    private boolean isReconciled;
    private String explanation;

    public ReconciliationSummary() {}

    public long getTotalRawEvents() { return totalRawEvents; }
    public void setTotalRawEvents(long totalRawEvents) { this.totalRawEvents = totalRawEvents; }

    public long getTotalProcessedEvents() { return totalProcessedEvents; }
    public void setTotalProcessedEvents(long totalProcessedEvents) { this.totalProcessedEvents = totalProcessedEvents; }

    public long getTotalDuplicateEvents() { return totalDuplicateEvents; }
    public void setTotalDuplicateEvents(long totalDuplicateEvents) { this.totalDuplicateEvents = totalDuplicateEvents; }

    public long getTotalQuarantinedEvents() { return totalQuarantinedEvents; }
    public void setTotalQuarantinedEvents(long totalQuarantinedEvents) { this.totalQuarantinedEvents = totalQuarantinedEvents; }

    public long getTotalMissingIntervals() { return totalMissingIntervals; }
    public void setTotalMissingIntervals(long totalMissingIntervals) { this.totalMissingIntervals = totalMissingIntervals; }

    public long getTotalShipments() { return totalShipments; }
    public void setTotalShipments(long totalShipments) { this.totalShipments = totalShipments; }

    public long getTotalLots() { return totalLots; }
    public void setTotalLots(long totalLots) { this.totalLots = totalLots; }

    public long getTotalSensors() { return totalSensors; }
    public void setTotalSensors(long totalSensors) { this.totalSensors = totalSensors; }

    public long getTotalExcursions() { return totalExcursions; }
    public void setTotalExcursions(long totalExcursions) { this.totalExcursions = totalExcursions; }

    public long getTotalInvestigations() { return totalInvestigations; }
    public void setTotalInvestigations(long totalInvestigations) { this.totalInvestigations = totalInvestigations; }

    public long getTotalDecisions() { return totalDecisions; }
    public void setTotalDecisions(long totalDecisions) { this.totalDecisions = totalDecisions; }

    public long getTotalAuditRecords() { return totalAuditRecords; }
    public void setTotalAuditRecords(long totalAuditRecords) { this.totalAuditRecords = totalAuditRecords; }

    public boolean isReconciled() { return isReconciled; }
    public void setReconciled(boolean reconciled) { isReconciled = reconciled; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
}
