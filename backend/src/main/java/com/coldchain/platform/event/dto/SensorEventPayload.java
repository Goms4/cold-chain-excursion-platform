package com.coldchain.platform.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class SensorEventPayload {

    @NotBlank(message = "event_id is required")
    @JsonProperty("event_id")
    private String eventId;

    @NotBlank(message = "sensor_id is required")
    @JsonProperty("sensor_id")
    private String sensorId;

    @NotBlank(message = "shipment_id is required")
    @JsonProperty("shipment_id")
    private String shipmentId;

    @JsonProperty("sequence_no")
    private Long sequenceNo;

    @NotNull(message = "event_time is required")
    @JsonProperty("event_time")
    private Instant eventTime;

    @JsonProperty("ingestion_time")
    private Instant ingestionTime;

    @NotNull(message = "temperature_c is required")
    @JsonProperty("temperature_c")
    private BigDecimal temperatureC;

    public SensorEventPayload() {}

    public SensorEventPayload(String eventId, String sensorId, String shipmentId,
                              Long sequenceNo, Instant eventTime, Instant ingestionTime,
                              BigDecimal temperatureC) {
        this.eventId = eventId;
        this.sensorId = sensorId;
        this.shipmentId = shipmentId;
        this.sequenceNo = sequenceNo;
        this.eventTime = eventTime;
        this.ingestionTime = ingestionTime;
        this.temperatureC = temperatureC;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public Long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Long sequenceNo) { this.sequenceNo = sequenceNo; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public Instant getIngestionTime() { return ingestionTime; }
    public void setIngestionTime(Instant ingestionTime) { this.ingestionTime = ingestionTime; }

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }
}
