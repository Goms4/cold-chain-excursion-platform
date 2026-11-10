package com.coldchain.platform.event.model;

import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.shipment.model.Shipment;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "missing_intervals")
public class MissingInterval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id", nullable = false)
    private Sensor sensor;

    @Column(name = "gap_start_time", nullable = false)
    private Instant gapStartTime;

    @Column(name = "gap_end_time", nullable = false)
    private Instant gapEndTime;

    @Column(name = "expected_cadence_minutes", nullable = false)
    private Integer expectedCadenceMinutes;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "UNKNOWN";

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt = Instant.now();

    public MissingInterval() {}

    public MissingInterval(Shipment shipment, Sensor sensor, Instant gapStartTime, Instant gapEndTime, Integer expectedCadenceMinutes) {
        this.shipment = shipment;
        this.sensor = sensor;
        this.gapStartTime = gapStartTime;
        this.gapEndTime = gapEndTime;
        this.expectedCadenceMinutes = expectedCadenceMinutes;
        this.status = "UNKNOWN";
        this.detectedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public Sensor getSensor() { return sensor; }
    public void setSensor(Sensor sensor) { this.sensor = sensor; }

    public Instant getGapStartTime() { return gapStartTime; }
    public void setGapStartTime(Instant gapStartTime) { this.gapStartTime = gapStartTime; }

    public Instant getGapEndTime() { return gapEndTime; }
    public void setGapEndTime(Instant gapEndTime) { this.gapEndTime = gapEndTime; }

    public Integer getExpectedCadenceMinutes() { return expectedCadenceMinutes; }
    public void setExpectedCadenceMinutes(Integer expectedCadenceMinutes) { this.expectedCadenceMinutes = expectedCadenceMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
}
