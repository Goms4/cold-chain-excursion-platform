package com.coldchain.platform.sensor.model;

import com.coldchain.platform.shipment.model.Shipment;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @Column(name = "model", nullable = false, length = 64)
    private String model;

    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SensorStatus status = SensorStatus.ACTIVE;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt = Instant.now();

    public Sensor() {}

    public Sensor(String id, String model, Integer batteryLevel, Shipment shipment) {
        this.id = id;
        this.model = model;
        this.batteryLevel = batteryLevel;
        this.shipment = shipment;
        this.status = SensorStatus.ACTIVE;
        this.registeredAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus status) { this.status = status; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
}
