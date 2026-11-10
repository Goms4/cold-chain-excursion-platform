package com.coldchain.platform.event.model;

import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.shipment.model.Shipment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sensor_events")
public class ProcessedSensorEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "sequence_no")
    private Long sequenceNo;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "ingestion_time", nullable = false)
    private Instant ingestionTime;

    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TemperatureEvaluationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_flag", nullable = false, length = 32)
    private EventQualityFlag qualityFlag;

    @Column(name = "is_duplicate", nullable = false)
    private Boolean isDuplicate = false;

    @Column(name = "duplicate_of_event_id", length = 64)
    private String duplicateOfEventId;

    @Column(name = "excursion_incident_id", length = 64)
    private String excursionIncidentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ProcessedSensorEvent() {}

    public ProcessedSensorEvent(String eventId, Sensor sensor, Shipment shipment, Long sequenceNo,
                                Instant eventTime, Instant ingestionTime, BigDecimal temperatureC,
                                TemperatureEvaluationStatus status, EventQualityFlag qualityFlag,
                                Boolean isDuplicate, String duplicateOfEventId) {
        this.eventId = eventId;
        this.sensor = sensor;
        this.shipment = shipment;
        this.sequenceNo = sequenceNo;
        this.eventTime = eventTime;
        this.ingestionTime = ingestionTime;
        this.temperatureC = temperatureC;
        this.status = status;
        this.qualityFlag = qualityFlag;
        this.isDuplicate = isDuplicate != null ? isDuplicate : false;
        this.duplicateOfEventId = duplicateOfEventId;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public Long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Long sequenceNo) { this.sequenceNo = sequenceNo; }

    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }

    public Instant getIngestionTime() { return ingestionTime; }
    public void setIngestionTime(Instant ingestionTime) { this.ingestionTime = ingestionTime; }

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }

    public TemperatureEvaluationStatus getStatus() { return status; }
    public void setStatus(TemperatureEvaluationStatus status) { this.status = status; }

    public EventQualityFlag getQualityFlag() { return qualityFlag; }
    public void setQualityFlag(EventQualityFlag qualityFlag) { this.qualityFlag = qualityFlag; }

    public Boolean getIsDuplicate() { return isDuplicate; }
    public void setIsDuplicate(Boolean duplicate) { isDuplicate = duplicate; }

    public String getDuplicateOfEventId() { return duplicateOfEventId; }
    public void setDuplicateOfEventId(String duplicateOfEventId) { this.duplicateOfEventId = duplicateOfEventId; }

    public String getExcursionIncidentId() { return excursionIncidentId; }
    public void setExcursionIncidentId(String excursionIncidentId) { this.excursionIncidentId = excursionIncidentId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
