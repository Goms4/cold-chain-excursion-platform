package com.coldchain.platform.reconciliation.web;

import com.coldchain.platform.audit.repository.AuditRecordRepository;
import com.coldchain.platform.decision.repository.DispositionDecisionRepository;
import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.event.repository.RawSensorEventRepository;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.reconciliation.dto.ReconciliationSummary;
import com.coldchain.platform.sensor.repository.SensorRepository;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationController {

    private final RawSensorEventRepository rawRepository;
    private final ProcessedSensorEventRepository processedRepository;
    private final MissingIntervalRepository missingRepository;
    private final ShipmentRepository shipmentRepository;
    private final LotRepository lotRepository;
    private final SensorRepository sensorRepository;
    private final ExcursionIncidentRepository excursionRepository;
    private final InvestigationRepository investigationRepository;
    private final DispositionDecisionRepository decisionRepository;
    private final AuditRecordRepository auditRepository;

    public ReconciliationController(RawSensorEventRepository rawRepository,
                                    ProcessedSensorEventRepository processedRepository,
                                    MissingIntervalRepository missingRepository,
                                    ShipmentRepository shipmentRepository,
                                    LotRepository lotRepository,
                                    SensorRepository sensorRepository,
                                    ExcursionIncidentRepository excursionRepository,
                                    InvestigationRepository investigationRepository,
                                    DispositionDecisionRepository decisionRepository,
                                    AuditRecordRepository auditRepository) {
        this.rawRepository = rawRepository;
        this.processedRepository = processedRepository;
        this.missingRepository = missingRepository;
        this.shipmentRepository = shipmentRepository;
        this.lotRepository = lotRepository;
        this.sensorRepository = sensorRepository;
        this.excursionRepository = excursionRepository;
        this.investigationRepository = investigationRepository;
        this.decisionRepository = decisionRepository;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<ReconciliationSummary> getSummary() {
        long rawCount = rawRepository.count();
        long processedCount = processedRepository.count();
        long dupCount = processedRepository.findAll().stream().filter(p -> Boolean.TRUE.equals(p.getIsDuplicate())).count();
        long quarCount = processedRepository.findAll().stream().filter(p -> p.getQualityFlag() == EventQualityFlag.QUARANTINED).count();
        long missingCount = missingRepository.count();
        long shipmentsCount = shipmentRepository.count();
        long lotsCount = lotRepository.count();
        long sensorsCount = sensorRepository.count();
        long excursionsCount = excursionRepository.count();
        long investigationsCount = investigationRepository.count();
        long decisionsCount = decisionRepository.count();
        long auditCount = auditRepository.count();

        // Mathematical verification: Raw events count equals processed events (valid + duplicates + quarantined)
        boolean reconciled = (rawCount == processedCount);
        String explanation = reconciled
                ? "Perfect reconciliation: Total Raw Events (" + rawCount + ") matches Processed Events (" + processedCount + ")."
                : "Discrepancy detected between raw (" + rawCount + ") and processed (" + processedCount + ") events.";

        ReconciliationSummary summary = new ReconciliationSummary();
        summary.setTotalRawEvents(rawCount);
        summary.setTotalProcessedEvents(processedCount);
        summary.setTotalDuplicateEvents(dupCount);
        summary.setTotalQuarantinedEvents(quarCount);
        summary.setTotalMissingIntervals(missingCount);
        summary.setTotalShipments(shipmentsCount);
        summary.setTotalLots(lotsCount);
        summary.setTotalSensors(sensorsCount);
        summary.setTotalExcursions(excursionsCount);
        summary.setTotalInvestigations(investigationsCount);
        summary.setTotalDecisions(decisionsCount);
        summary.setTotalAuditRecords(auditCount);
        summary.setReconciled(reconciled);
        summary.setExplanation(explanation);

        return ResponseEntity.ok(summary);
    }
}
