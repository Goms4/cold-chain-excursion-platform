package com.coldchain.platform.sensor.dto;

import jakarta.validation.constraints.NotBlank;

public class RegisterSensorRequest {

    @NotBlank(message = "id is required")
    private String id;

    @NotBlank(message = "model is required")
    private String model;

    private Integer batteryLevel;
    private String shipmentId;

    public RegisterSensorRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getBatteryLevel() { return batteryLevel; }
    public void setBatteryLevel(Integer batteryLevel) { this.batteryLevel = batteryLevel; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
}
