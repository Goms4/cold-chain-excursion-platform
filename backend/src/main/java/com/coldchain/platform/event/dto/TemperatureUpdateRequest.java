package com.coldchain.platform.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import java.math.BigDecimal;
import java.time.Instant;

public class TemperatureUpdateRequest {
    @NotNull(message = "Enter a temperature reading in degrees Celsius.")
    @DecimalMin(value = "-100.00", message = "temperatureC must be at least -100.00°C")
    @DecimalMax(value = "100.00", message = "temperatureC must be at most 100.00°C")
    private BigDecimal temperatureC;
    private Instant eventTime;
    private String sensorId;

    public BigDecimal getTemperatureC() { return temperatureC; }
    public void setTemperatureC(BigDecimal temperatureC) { this.temperatureC = temperatureC; }
    public Instant getEventTime() { return eventTime; }
    public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }
}
