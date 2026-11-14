package com.coldchain.platform.event.dto;

import com.coldchain.platform.event.model.EventProcessingStatus;
import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.event.model.TemperatureEvaluationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.Instant;

public class EventIngestResult {
    private String eventId;
    private String shipmentId;
    private String lotId;
    private String sensorId;
    private Instant eventTime;
    private Instant ingestionTime;
    private BigDecimal temperatureC;
    private EventProcessingStatus processingStatus;
    private EventQualityFlag qualityFlag;
    private TemperatureEvaluationStatus evaluationStatus;
    private Boolean isDuplicate;
    private String excursionIncidentId;
    private String message;
    private String condition;
    private String dataQuality;
    private String source;

    public EventIngestResult() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }

    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getDataQuality() { return dataQuality; }
    public void setDataQuality(String dataQuality) { this.dataQuality = dataQuality; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public Instant getIngestionTime() { return ingestionTime; }
    public void setIngestionTime(Instant ingestionTime) { this.ingestionTime = ingestionTime; }

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }

    @JsonIgnore
    public EventProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(EventProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    @JsonIgnore
    public EventQualityFlag getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(EventQualityFlag qualityFlag) { this.qualityFlag = qualityFlag; }

    @JsonIgnore
    public TemperatureEvaluationStatus getEvaluationStatus() { return evaluationStatus; }
    public void setEvaluationStatus(TemperatureEvaluationStatus evaluationStatus) { this.evaluationStatus = evaluationStatus; }

    @JsonIgnore
    public Boolean getIsDuplicate() { return isDuplicate; }
    public void setIsDuplicate(Boolean duplicate) { isDuplicate = duplicate; }

    @JsonIgnore
    public String getExcursionIncidentId() { return excursionIncidentId; }
    public void setExcursionIncidentId(String excursionIncidentId) { this.excursionIncidentId = excursionIncidentId; }

    @JsonIgnore
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
