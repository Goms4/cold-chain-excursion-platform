package com.coldchain.platform.common;

import com.coldchain.platform.audit.repository.AuditRecordRepository;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.event.repository.RawSensorEventRepository;
import com.coldchain.platform.excursion.repository.ExcursionAffectedLotRepository;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.repository.ManualInspectionRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.lot.model.Lot;
import org.springframework.stereotype.Service;

@Service
public class OperationalHistoryService {
    private final ProcessedSensorEventRepository events;
    private final RawSensorEventRepository rawEvents;
    private final MissingIntervalRepository intervals;
    private final ExcursionIncidentRepository issues;
    private final ExcursionAffectedLotRepository affectedLots;
    private final ManualInspectionRepository inspections;
    private final AuditRecordRepository audits;

    public OperationalHistoryService(ProcessedSensorEventRepository events, RawSensorEventRepository rawEvents,
                                     MissingIntervalRepository intervals, ExcursionIncidentRepository issues,
                                     ExcursionAffectedLotRepository affectedLots, ManualInspectionRepository inspections,
                                     AuditRecordRepository audits) {
        this.events = events;
        this.rawEvents = rawEvents;
        this.intervals = intervals;
        this.issues = issues;
        this.affectedLots = affectedLots;
        this.inspections = inspections;
        this.audits = audits;
    }

    public boolean shipmentHasHistory(Shipment shipment) {
        String id = shipment.getId();
        return events.countByShipmentId(id) > 0 || rawEvents.countByShipmentId(id) > 0
                || intervals.countByShipmentId(id) > 0 || !issues.findByShipmentId(id).isEmpty()
                || !inspections.findByShipmentId(id).isEmpty()
                || audits.countByEntityTypeAndEntityIdAndActionNot("SHIPMENT", id, "CREATED") > 0;
    }

    public boolean lotHasHistory(Lot lot) {
        String id = lot.getId();
        return events.existsByLot_Id(id) || affectedLots.existsByLot_Id(id)
                || audits.countByEntityTypeAndEntityIdAndActionNot("LOT", id, "CREATED") > 0;
    }
}
