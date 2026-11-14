package com.coldchain.platform.event.dto;

import com.coldchain.platform.common.BusinessIds;
import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.event.model.ProcessedSensorEvent;
import com.coldchain.platform.event.model.TemperatureEvaluationStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Business-facing reading projection; processing metadata remains on the persisted entity. */
public class ProcessedSensorEventResponse {
    private String eventId;
    private String shipmentId;
    private String lotId;
    private String sensorId;
    private Instant eventTime;
    private Instant ingestionTime;
    private BigDecimal temperatureC;
    private String condition;
    private String dataQuality;
    private String source;

    public static ProcessedSensorEventResponse fromEntity(ProcessedSensorEvent event) {
        ProcessedSensorEventResponse dto = new ProcessedSensorEventResponse();
        dto.eventId = BusinessIds.event(event.getId());
        dto.shipmentId = event.getShipment() == null ? null : BusinessIds.shipment(event.getShipment().getId());
        dto.lotId = event.getLot() == null ? null : BusinessIds.lot(event.getLot().getId());
        dto.sensorId = event.getSensor() == null ? null : BusinessIds.sensor(event.getSensor().getId());
        dto.eventTime = event.getEventTime();
        dto.ingestionTime = event.getIngestionTime();
        dto.temperatureC = event.getTemperatureC();
        dto.condition = condition(event);
        dto.dataQuality = dataQuality(event);
        dto.source = event.getSource();
        return dto;
    }

    private static String condition(ProcessedSensorEvent e) {
        if (e.getTemperatureC() == null) return "UNKNOWN";
        var profile = e.getShipment() == null ? null : e.getShipment().getStorageProfile();
        if (profile == null) return e.getStatus() == TemperatureEvaluationStatus.NORMAL ? "NORMAL" : "UNKNOWN";
        if (e.getTemperatureC().compareTo(profile.getTargetMinC()) < 0) return "LOW";
        if (e.getTemperatureC().compareTo(profile.getTargetMaxC()) > 0) return "HIGH";
        return "NORMAL";
    }

    private static String dataQuality(ProcessedSensorEvent e) {
        if (Boolean.TRUE.equals(e.getIsDuplicate())) return "DUPLICATE";
        return switch (e.getQualityFlag()) {
            case VALID -> "VALID";
            case LATE, OUT_OF_ORDER -> "LATE";
            case QUARANTINED -> "QUARANTINED";
        };
    }

    public String getEventId() { return eventId; }
    public String getShipmentId() { return shipmentId; }
    public String getLotId() { return lotId; }
    public String getSensorId() { return sensorId; }
    public Instant getEventTime() { return eventTime; }
    public Instant getIngestionTime() { return ingestionTime; }
    public BigDecimal getTemperatureC() { return temperatureC; }
    public String getCondition() { return condition; }
    public String getDataQuality() { return dataQuality; }
    public String getSource() { return source; }
}
