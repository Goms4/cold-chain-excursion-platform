package com.coldchain.platform.event.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TemperatureReadingResponse(String eventId, String shipmentId, String lotId, String sensorId,
                                         Instant eventTime, Instant ingestionTime, BigDecimal temperatureC,
                                         String condition, String dataQuality, String source, String message) {
    public static TemperatureReadingResponse from(EventIngestResult result) {
        return new TemperatureReadingResponse(result.getEventId(), result.getShipmentId(), result.getLotId(),
                result.getSensorId(), result.getEventTime(), result.getIngestionTime(), result.getTemperatureC(),
                result.getCondition(), result.getDataQuality(), result.getSource(),
                Boolean.TRUE.equals(result.getIsDuplicate()) ? "This reading was already recorded."
                        : "QUARANTINED".equals(result.getDataQuality())
                        ? "This reading was quarantined for review." : "Reading recorded.");
    }
}
