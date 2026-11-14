package com.coldchain.platform.investigation.dto;

import com.coldchain.platform.investigation.model.ManualInspectionRecord;
import java.math.BigDecimal;
import java.time.Instant;

public record ManualInspectionResponse(String inspectorName, Instant inspectionTime, BigDecimal observedTemperatureC,
                                       String packageIntegrityStatus, String visualCondition, String notes,
                                       Boolean contradictsSensor) {
    public static ManualInspectionResponse from(ManualInspectionRecord r) {
        return new ManualInspectionResponse(r.getInspectorName(), r.getInspectionTime(), r.getObservedTemperatureC(),
                r.getPackageIntegrityStatus(), r.getVisualCondition(), r.getNotes(), r.getContradictsSensor());
    }
}
