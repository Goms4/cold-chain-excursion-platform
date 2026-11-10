package com.coldchain.platform.investigation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public class ManualInspectionRequest {

    @NotBlank(message = "inspector_name is required")
    private String inspectorName;

    @NotNull(message = "inspection_time is required")
    private Instant inspectionTime;

    @NotNull(message = "observed_temperature_c is required")
    private BigDecimal observedTemperatureC;

    @NotBlank(message = "package_integrity_status is required")
    private String packageIntegrityStatus;

    private String visualCondition;
    private String notes;

    public ManualInspectionRequest() {}

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
}
