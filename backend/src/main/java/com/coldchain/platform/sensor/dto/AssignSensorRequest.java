package com.coldchain.platform.sensor.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignSensorRequest {

    @NotBlank(message = "sensor_id is required")
    private String sensorId;

    public AssignSensorRequest() {}

    public AssignSensorRequest(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
}
