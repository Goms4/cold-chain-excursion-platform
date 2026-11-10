package com.coldchain.platform.sensor.dto;

import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.sensor.model.SensorStatus;

import java.time.Instant;

public class SensorResponse {
    private String id;
    private String shipmentId;
    private String model;
    private Integer batteryLevel;
    private SensorStatus status;
    private Instant registeredAt;

    public SensorResponse() {}

    public static SensorResponse fromEntity(Sensor sensor) {
        SensorResponse dto = new SensorResponse();
        dto.setId(sensor.getId());
        if (sensor.getShipment() != null) {
            dto.setShipmentId(sensor.getShipment().getId());
        }
        dto.setModel(sensor.getModel());
        dto.setBatteryLevel(sensor.getBatteryLevel());
        dto.setStatus(sensor.getStatus());
        dto.setRegisteredAt(sensor.getRegisteredAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public SensorStatus getStatus() { return status; }
    public void setStatus(SensorStatus status) { this.status = status; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant registeredAt) { this.registeredAt = registeredAt; }
}
