package com.coldchain.platform.excursion.model;

import com.coldchain.platform.shipment.model.Shipment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "excursion_incidents")
public class ExcursionIncident {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "rule_version", nullable = false, length = 32)
    private String ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private ExcursionSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "start_event_time", nullable = false)
    private Instant startEventTime;

    @Column(name = "end_event_time")
    private Instant endEventTime;

    @Column(name = "peak_temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal peakTemperatureC;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ExcursionIncident() {}

    public ExcursionIncident(String id, Shipment shipment, String ruleVersion, ExcursionSeverity severity,
                             IncidentStatus status, Instant startEventTime, BigDecimal peakTemperatureC) {
        this.id = id;
        this.shipment = shipment;
        this.ruleVersion = ruleVersion;
        this.severity = severity;
        this.status = status != null ? status : IncidentStatus.OPEN;
        this.startEventTime = startEventTime;
        this.peakTemperatureC = peakTemperatureC;
        this.durationMinutes = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isBusinessFinalized() {
        return this.status == IncidentStatus.CLOSED || this.status == IncidentStatus.DISPOSITIONED;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getRuleVersion() { return ruleVersion; }
    public void setRuleVersion(String ruleVersion) { this.ruleVersion = ruleVersion; }

    public ExcursionSeverity getSeverity() { return severity; }
    public void setSeverity(ExcursionSeverity severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getStartEventTime() { return startEventTime; }
    public void setStartEventTime(Instant startEventTime) { this.startEventTime = startEventTime; }

    public Instant getEndEventTime() { return endEventTime; }
    public void setEndEventTime(Instant endEventTime) { this.endEventTime = endEventTime; }

    public BigDecimal getPeakTemperatureC() { return peakTemperatureC; }
    public void setPeakTemperatureC(BigDecimal peakTemperatureC) { this.peakTemperatureC = peakTemperatureC; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
