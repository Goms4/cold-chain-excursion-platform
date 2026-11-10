package com.coldchain.platform.investigation.model;

import com.coldchain.platform.shipment.model.Shipment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "manual_inspections")
public class ManualInspectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investigation_id", nullable = false)
    private Investigation investigation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "inspector_name", nullable = false, length = 128)
    private String inspectorName;

    @Column(name = "inspection_time", nullable = false)
    private Instant inspectionTime;

    @Column(name = "observed_temperature_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal observedTemperatureC;

    @Column(name = "package_integrity_status", nullable = false, length = 64)
    private String packageIntegrityStatus;

    @Column(name = "visual_condition", columnDefinition = "TEXT")
    private String visualCondition;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "contradicts_sensor", nullable = false)
    private Boolean contradictsSensor = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public ManualInspectionRecord() {}

    public ManualInspectionRecord(Investigation investigation, Shipment shipment, String inspectorName,
                                  Instant inspectionTime, BigDecimal observedTemperatureC,
                                  String packageIntegrityStatus, String visualCondition,
                                  String notes, Boolean contradictsSensor) {
        this.investigation = investigation;
        this.shipment = shipment;
        this.inspectorName = inspectorName;
        this.inspectionTime = inspectionTime;
        this.observedTemperatureC = observedTemperatureC;
        this.packageIntegrityStatus = packageIntegrityStatus;
        this.visualCondition = visualCondition;
        this.notes = notes;
        this.contradictsSensor = contradictsSensor != null ? contradictsSensor : false;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Investigation getInvestigation() { return investigation; }
    public void setInvestigation(Investigation investigation) { this.investigation = investigation; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }

    public Instant getInspectionTime() { return inspectionTime; }
    public void setInspectionTime(Instant inspectionTime) { this.inspectionTime = inspectionTime; }

    public BigDecimal getObservedTemperatureC() { return observedTemperatureC; }
    public void setObservedTemperatureC(BigDecimal observedTemperatureC) { this.observedTemperatureC = observedTemperatureC; }

    public String getPackageIntegrityStatus() { return packageIntegrityStatus; }
    public void setPackageIntegrityStatus(String packageIntegrityStatus) { this.packageIntegrityStatus = packageIntegrityStatus; }

    public String getVisualCondition() { return visualCondition; }
    public void setVisualCondition(String visualCondition) { this.visualCondition = visualCondition; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getContradictsSensor() { return contradictsSensor; }
    public void setContradictsSensor(Boolean contradictsSensor) { this.contradictsSensor = contradictsSensor; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
