package com.coldchain.platform.event.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "raw_sensor_events")
public class RawSensorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "sensor_id", nullable = false, length = 64)
    private String sensorId;

    @Column(name = "shipment_id", nullable = false, length = 64)
    private String shipmentId;

    @Column(name = "sequence_no")
    private Long sequenceNo;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "ingestion_time", nullable = false)
    private Instant ingestionTime = Instant.now();

    @Column(name = "raw_payload", nullable = false, columnDefinition = "TEXT")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 32)
    private EventProcessingStatus processingStatus = EventProcessingStatus.RECEIVED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RawSensorEvent() {}

    public RawSensorEvent(String eventId, String sensorId, String shipmentId, Long sequenceNo,
                          Instant eventTime, Instant ingestionTime, String rawPayload,
                          EventProcessingStatus processingStatus) {
        this.eventId = eventId;
        this.sensorId = sensorId;
        this.shipmentId = shipmentId;
        this.sequenceNo = sequenceNo;
        this.eventTime = eventTime;
        this.ingestionTime = ingestionTime != null ? ingestionTime : Instant.now();
        this.rawPayload = rawPayload;
        this.processingStatus = processingStatus != null ? processingStatus : EventProcessingStatus.RECEIVED;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getRawPayload() { return rawPayload; }
    public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }

    public EventProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(EventProcessingStatus processingStatus) { this.processingStatus = processingStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
