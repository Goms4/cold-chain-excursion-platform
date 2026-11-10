package com.coldchain.platform.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchSensorEventPayload {

    @NotEmpty(message = "events list cannot be empty")
    @Valid
    private List<SensorEventPayload> events;

    public BatchSensorEventPayload() {}

    public BatchSensorEventPayload(List<SensorEventPayload> events) {
        this.events = events;
    }

    public List<SensorEventPayload> getEvents() { return events; }
    public void setEvents(List<SensorEventPayload> events) { this.events = events; }
}
