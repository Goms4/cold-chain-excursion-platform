package com.coldchain.platform.excursion.dto;

import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.ExcursionSeverity;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.common.BusinessIds;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ExcursionResponse {
    private String id;
    private String shipmentId;
    private String ruleVersion;
    private ExcursionSeverity severity;
    private IncidentStatus status;
    private Instant startEventTime;
    private Instant endEventTime;
    private BigDecimal peakTemperatureC;
    private Integer durationMinutes;
    private Long readingCount;
    private String summary;
    private List<String> affectedLotIds;
    private Instant createdAt;
    private Instant updatedAt;

    public ExcursionResponse() {}

    public static ExcursionResponse fromEntity(ExcursionIncident incident, List<String> affectedLotIds, long readingCount) {
        ExcursionResponse dto = new ExcursionResponse();
        dto.setId(incident.getId());
        dto.setShipmentId(BusinessIds.shipment(incident.getShipment().getId()));
        dto.setRuleVersion(incident.getRuleVersion());
        dto.setSeverity(incident.getSeverity());
        dto.setStatus(incident.getStatus());
        dto.setStartEventTime(incident.getStartEventTime());
        dto.setEndEventTime(incident.getEndEventTime());
        dto.setPeakTemperatureC(incident.getPeakTemperatureC());
        dto.setDurationMinutes(incident.getDurationMinutes());
        dto.setReadingCount(readingCount);
        dto.setSummary(readingCount == 1
                ? String.format(java.util.Locale.ROOT, "One reading reached %.1f°C outside the storage target.", incident.getPeakTemperatureC())
                : String.format(java.util.Locale.ROOT, "%d readings reached %.1f°C outside the storage target over %d minutes.",
                readingCount, incident.getPeakTemperatureC(), incident.getDurationMinutes() == null ? 0 : incident.getDurationMinutes()));
        dto.setAffectedLotIds(affectedLotIds.stream().map(BusinessIds::lot).toList());
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public ExcursionSeverity getSeverity() { return severity; }
    public void setSeverity(ExcursionSeverity severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public Instant getStartEventTime() { return startEventTime; }
    public void setStartEventTime(Instant startEventTime) { this.startEventTime = startEventTime; }

    public Instant getEndEventTime() { return endEventTime; }
    public void setEndEventTime(Instant endEventTime) { this.endEventTime = endEventTime; }

    public BigDecimal getPeakTemperatureC() { return peakTemperatureC; }
    public void setPeakTemperatureC(BigDecimal peakTemperatureC) { this.peakTemperatureC = peakTemperatureC; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public Long getReadingCount() { return readingCount; }
    public void setReadingCount(Long readingCount) { this.readingCount = readingCount; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<String> getAffectedLotIds() { return affectedLotIds; }
    public void setAffectedLotIds(List<String> affectedLotIds) { this.affectedLotIds = affectedLotIds; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
