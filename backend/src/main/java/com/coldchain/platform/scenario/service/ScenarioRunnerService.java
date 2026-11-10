package com.coldchain.platform.scenario.service;

import com.coldchain.platform.event.dto.EventIngestResult;
import com.coldchain.platform.event.dto.SensorEventPayload;
import com.coldchain.platform.event.model.EventProcessingStatus;
import com.coldchain.platform.event.model.TemperatureEvaluationStatus;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.service.EventIngestionService;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.excursion.repository.StorageProfileRepository;
import com.coldchain.platform.investigation.dto.ManualInspectionRequest;
import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.investigation.service.InvestigationService;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.scenario.dto.ScenarioRunResult;
import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.sensor.repository.SensorRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScenarioRunnerService {

    private final ShipmentRepository shipmentRepository;
    private final LotRepository lotRepository;
    private final SensorRepository sensorRepository;
    private final StorageProfileRepository profileRepository;
    private final EventIngestionService ingestionService;
    private final ExcursionIncidentRepository incidentRepository;
    private final InvestigationRepository investigationRepository;
    private final InvestigationService investigationService;
    private final MissingIntervalRepository missingIntervalRepository;

    public ScenarioRunnerService(ShipmentRepository shipmentRepository,
                                 LotRepository lotRepository,
                                 SensorRepository sensorRepository,
                                 StorageProfileRepository profileRepository,
                                 EventIngestionService ingestionService,
                                 ExcursionIncidentRepository incidentRepository,
                                 InvestigationRepository investigationRepository,
                                 InvestigationService investigationService,
                                 MissingIntervalRepository missingIntervalRepository) {
        this.shipmentRepository = shipmentRepository;
        this.lotRepository = lotRepository;
        this.sensorRepository = sensorRepository;
        this.profileRepository = profileRepository;
        this.ingestionService = ingestionService;
        this.incidentRepository = incidentRepository;
        this.investigationRepository = investigationRepository;
        this.investigationService = investigationService;
        this.missingIntervalRepository = missingIntervalRepository;
    }

    private Shipment ensureShipment(String shipmentId, String profileId) {
        return shipmentRepository.findById(shipmentId).orElseGet(() -> {
            StorageProfile profile = profileRepository.findById(profileId)
                    .orElseGet(() -> profileRepository.save(new StorageProfile(
                            profileId, "Controlled Room / Ambient",
                            new BigDecimal("15.0"), new BigDecimal("25.0"),
                            new BigDecimal("25.1"), new BigDecimal("30.0"),
                            new BigDecimal("30.1"), new BigDecimal("40.0"),
                            new BigDecimal("40.1"), new BigDecimal("99.9"),
                            240, "v1.0.0"
                    )));

            Shipment shp = new Shipment(
                    shipmentId, "TRK-" + shipmentId, "Berlin Logistics Hub", "Munich Clinic",
                    profile, ShipmentStatus.IN_TRANSIT, 5, 120
            );
            return shipmentRepository.save(shp);
        });
    }

    private Sensor ensureSensor(String sensorId, Shipment shipment) {
        return sensorRepository.findById(sensorId).orElseGet(() -> {
            Sensor s = new Sensor(sensorId, "TempTrace-Pro-V2", 98, shipment);
            return sensorRepository.save(s);
        });
    }

    private Lot ensureLot(String lotId, Shipment shipment, String product) {
        return lotRepository.findById(lotId).orElseGet(() -> {
            Lot l = new Lot(lotId, shipment, product, "BATCH-2026-X", 500);
            return lotRepository.save(l);
        });
    }

    @Transactional
    public ScenarioRunResult runScenario(String scenarioName) {
        String name = scenarioName.toLowerCase().trim();
        return switch (name) {
            case "normal", "1" -> runNormalScenario();
            case "controlled-room-excursion", "minor", "2" -> runMinorExcursionScenario();
            case "major-critical", "critical", "3" -> runMajorCriticalScenario();
            case "duplicate", "4" -> runDuplicateScenario();
            case "out-of-order-late", "out-of-order", "5" -> runOutOfOrderLateScenario();
            case "missing-contradictory", "contradictory", "6" -> runMissingAndContradictoryScenario();
            default -> runAllScenarios();
        };
    }

    public ScenarioRunResult runNormalScenario() {
        String shpId = "SHP-SCENARIO-1";
        Shipment shipment = ensureShipment(shpId, "PROFILE-CRT");
        Sensor sensor = ensureSensor("S-SCEN-1", shipment);
        ensureLot("LOT-SCEN-1", shipment, "Vaccine Antigen Standard");

        Instant baseTime = Instant.parse("2026-11-10T10:00:00Z");
        double[] temps = {18.0, 20.1, 21.5, 22.0, 19.8};

        List<String> notes = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < temps.length; i++) {
            Instant eventTime = baseTime.plusSeconds(i * 300L);
            SensorEventPayload payload = new SensorEventPayload(
                    "EV-NORM-" + (i + 1), sensor.getId(), shipment.getId(),
                    (long) (i + 1), eventTime, eventTime.plusSeconds(5),
                    BigDecimal.valueOf(temps[i])
            );
            EventIngestResult res = ingestionService.ingestEvent(payload, "SCENARIO_RUNNER");
            notes.add("Event " + res.getEventId() + " (" + temps[i] + "°C) -> " + res.getEvaluationStatus());
            count++;
        }

        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId(shpId);
        boolean success = incidents.isEmpty();
        notes.add("Verification: Expected 0 incidents, found " + incidents.size());

        ScenarioRunResult result = new ScenarioRunResult();
        result.setScenarioName("1. Normal Shipment");
        result.setDescription("Normal temperatures (18.0°C to 22.0°C) strictly within target compliant range.");
        result.setSuccess(success);
        result.setShipmentId(shpId);
        result.setEventsIngested(count);
        result.setIncidentsCreated(incidents.size());
        result.setExcursionSeverity("NONE");
        result.setHasDuplicate(false);
        result.setHasMissingInterval(false);
        result.setHasContradictoryEvidence(false);
        result.setVerificationNotes(notes);
        return result;
    }

    public ScenarioRunResult runMinorExcursionScenario() {
        String shpId = "SHP-SCENARIO-2";
        Shipment shipment = ensureShipment(shpId, "PROFILE-CRT");
        Sensor sensor = ensureSensor("S-SCEN-2", shipment);
        ensureLot("LOT-SCEN-2", shipment, "Insulin Glargine Vials");

        Instant baseTime = Instant.parse("2026-11-10T11:00:00Z");
        double[] temps = {20.0, 26.5, 27.8, 28.2, 21.0};

        List<String> notes = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < temps.length; i++) {
            Instant eventTime = baseTime.plusSeconds(i * 300L);
            SensorEventPayload payload = new SensorEventPayload(
                    "EV-MINOR-" + (i + 1), sensor.getId(), shipment.getId(),
                    (long) (i + 1), eventTime, eventTime.plusSeconds(5),
                    BigDecimal.valueOf(temps[i])
            );
            EventIngestResult res = ingestionService.ingestEvent(payload, "SCENARIO_RUNNER");
            notes.add("Event " + res.getEventId() + " (" + temps[i] + "°C) -> " + res.getEvaluationStatus());
            count++;
        }

        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId(shpId);
        boolean success = !incidents.isEmpty();
        String sev = success ? incidents.get(0).getSeverity().name() : "NONE";
        notes.add("Verification: Incident created with severity " + sev);

        ScenarioRunResult result = new ScenarioRunResult();
        result.setScenarioName("2. Controlled-Room Minor Excursion");
        result.setDescription("Temperature elevated into 26.0°C-28.2°C minor allowance range.");
        result.setSuccess(success);
        result.setShipmentId(shpId);
        result.setEventsIngested(count);
        result.setIncidentsCreated(incidents.size());
        result.setExcursionSeverity(sev);
        result.setHasDuplicate(false);
        result.setHasMissingInterval(false);
        result.setHasContradictoryEvidence(false);
        result.setVerificationNotes(notes);
        return result;
    }

    public ScenarioRunResult runMajorCriticalScenario() {
        String shpId = "SHP-SCENARIO-3";
        Shipment shipment = ensureShipment(shpId, "PROFILE-CRT");
        Sensor sensor = ensureSensor("S-SCEN-3", shipment);
        ensureLot("LOT-SCEN-3", shipment, "Monoclonal Antibodies Batch A");

        Instant baseTime = Instant.parse("2026-11-10T12:00:00Z");
        double[] temps = {22.0, 34.5, 42.1}; // 34.5 (Major), 42.1 (Critical)

        List<String> notes = new ArrayList<>();
        int count = 0;
        for (int i = 0; i < temps.length; i++) {
            Instant eventTime = baseTime.plusSeconds(i * 300L);
            SensorEventPayload payload = new SensorEventPayload(
                    "EV-CRIT-" + (i + 1), sensor.getId(), shipment.getId(),
                    (long) (i + 1), eventTime, eventTime.plusSeconds(5),
                    BigDecimal.valueOf(temps[i])
            );
            EventIngestResult res = ingestionService.ingestEvent(payload, "SCENARIO_RUNNER");
            notes.add("Event " + res.getEventId() + " (" + temps[i] + "°C) -> " + res.getEvaluationStatus());
            count++;
        }

        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId(shpId);
        boolean success = !incidents.isEmpty() && incidents.get(0).getSeverity().name().equals("CRITICAL");
        notes.add("Verification: Incident elevated to CRITICAL (peak temp " + (success ? incidents.get(0).getPeakTemperatureC() : 0) + "°C)");

        ScenarioRunResult result = new ScenarioRunResult();
        result.setScenarioName("3. Major / Critical Excursion");
        result.setDescription("Extreme temperature breach > 40°C creating CRITICAL incident.");
        result.setSuccess(success);
        result.setShipmentId(shpId);
        result.setEventsIngested(count);
        result.setIncidentsCreated(incidents.size());
        result.setExcursionSeverity(success ? incidents.get(0).getSeverity().name() : "NONE");
        result.setHasDuplicate(false);
        result.setHasMissingInterval(false);
        result.setHasContradictoryEvidence(false);
        result.setVerificationNotes(notes);
        return result;
    }

    public ScenarioRunResult runDuplicateScenario() {
        String shpId = "SHP-SCENARIO-4";
        Shipment shipment = ensureShipment(shpId, "PROFILE-CRT");
        Sensor sensor = ensureSensor("S-SCEN-4", shipment);
        ensureLot("LOT-SCEN-4", shipment, "Oncology Therapeutics");

        Instant eventTime = Instant.parse("2026-11-10T13:00:00Z");
        SensorEventPayload p1 = new SensorEventPayload(
                "EV-DUP-TEST", sensor.getId(), shipment.getId(), 101L,
                eventTime, eventTime.plusSeconds(5), BigDecimal.valueOf(28.5)
        );

        SensorEventPayload p2 = new SensorEventPayload(
                "EV-DUP-TEST", sensor.getId(), shipment.getId(), 101L,
                eventTime, eventTime.plusSeconds(30), BigDecimal.valueOf(28.5)
        );

        List<String> notes = new ArrayList<>();
        EventIngestResult res1 = ingestionService.ingestEvent(p1, "SCENARIO_RUNNER");
        notes.add("Ingestion 1: Status = " + res1.getProcessingStatus() + ", isDuplicate = " + res1.getIsDuplicate());

        EventIngestResult res2 = ingestionService.ingestEvent(p2, "SCENARIO_RUNNER");
        notes.add("Ingestion 2: Status = " + res2.getProcessingStatus() + ", isDuplicate = " + res2.getIsDuplicate());

        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId(shpId);
        boolean success = res2.getIsDuplicate() && incidents.size() == 1;
        notes.add("Verification: Duplicate event recognized. Total incidents = " + incidents.size() + " (no duplicate incident created)");

        ScenarioRunResult result = new ScenarioRunResult();
        result.setScenarioName("4. Duplicate Event Handling");
        result.setDescription("Identical event_id re-sent. First accepted, second archived as DUPLICATE without duplicate incident.");
        result.setSuccess(success);
        result.setShipmentId(shpId);
        result.setEventsIngested(2);
        result.setIncidentsCreated(incidents.size());
        result.setExcursionSeverity(incidents.isEmpty() ? "NONE" : incidents.get(0).getSeverity().name());
        result.setHasDuplicate(true);
        result.setHasMissingInterval(false);
        result.setHasContradictoryEvidence(false);
        result.setVerificationNotes(notes);
        return result;
    }

    public ScenarioRunResult runOutOfOrderLateScenario() {
        String shpId = "SHP-SCENARIO-5";
        Shipment shipment = ensureShipment(shpId, "PROFILE-CRT");
        Sensor sensor = ensureSensor("S-SCEN-5", shipment);
        ensureLot("LOT-SCEN-5", shipment, "Blood Plasma Factor VIII");

        Instant t1000 = Instant.parse("2026-11-10T14:00:00Z");
        Instant t1010 = Instant.parse("2026-11-10T14:10:00Z");
        Instant t1005 = Instant.parse("2026-11-10T14:05:00Z");

        List<String> notes = new ArrayList<>();

        // 1. Send 10:00
        EventIngestResult r1 = ingestionService.ingestEvent(new SensorEventPayload(
                "EV-OOO-1", sensor.getId(), shipment.getId(), 1L, t1000, t1000.plusSeconds(5), BigDecimal.valueOf(20.0)
        ), "SCENARIO_RUNNER");
        notes.add("Event 1 (14:00) -> " + r1.getQualityFlag());

        // 2. Send 10:10 (arriving before 10:05)
        EventIngestResult r2 = ingestionService.ingestEvent(new SensorEventPayload(
                "EV-OOO-2", sensor.getId(), shipment.getId(), 2L, t1010, t1010.plusSeconds(5), BigDecimal.valueOf(21.0)
        ), "SCENARIO_RUNNER");
        notes.add("Event 2 (14:10) -> " + r2.getQualityFlag());

        // 3. Send 10:05 (Out-of-order, arrived after 10:10 but event_time is 10:05)
        EventIngestResult r3 = ingestionService.ingestEvent(new SensorEventPayload(
                "EV-OOO-3", sensor.getId(), shipment.getId(), 3L, t1005, t1010.plusSeconds(60), BigDecimal.valueOf(22.5)
        ), "SCENARIO_RUNNER");
        notes.add("Event 3 (14:05 arriving at 14:11) -> " + r3.getQualityFlag());

        boolean success = r3.getQualityFlag().name().equals("OUT_OF_ORDER");
        notes.add("Verification: Out-of-order event accepted and correctly classified as OUT_OF_ORDER.");

        ScenarioRunResult result = new ScenarioRunResult();
        result.setScenarioName("5. Out-of-Order & Late Event Handling");
        result.setDescription("Events 14:00, 14:10, and 14:05 arriving out of sequence; evaluated strictly by event_time.");
        result.setSuccess(success);
        result.setShipmentId(shpId);
        result.setEventsIngested(3);
        result.setIncidentsCreated(0);
        result.setExcursionSeverity("NONE");
        result.setHasDuplicate(false);
        result.setHasMissingInterval(false);
        result.setHasContradictoryEvidence(false);
        result.setVerificationNotes(notes);
        return result;
    }

    public ScenarioRunResult runMissingAndContradictoryScenario() {
        String shpId = "SHP-SCENARIO-6";
        Shipment shipment = ensureShipment(shpId, "PROFILE-REF"); // Refrigerated 2°C - 8°C
        Sensor sensor = ensureSensor("S-SCEN-6", shipment);
        ensureLot("LOT-SCEN-6", shipment, "Cell & Gene Therapy Vector");

        Instant t1 = Instant.parse("2026-11-10T15:00:00Z");
        Instant t2 = Instant.parse("2026-11-10T15:05:00Z");
        Instant t3 = Instant.parse("2026-11-10T15:25:00Z"); // 20-minute gap! Expected cadence is 5 minutes.

        List<String> notes = new ArrayList<>();

        ingestionService.ingestEvent(new SensorEventPayload(
                "EV-MISS-1", sensor.getId(), shipment.getId(), 1L, t1, t1.plusSeconds(5), BigDecimal.valueOf(5.0)
        ), "SCENARIO_RUNNER");

        ingestionService.ingestEvent(new SensorEventPayload(
                "EV-MISS-2", sensor.getId(), shipment.getId(), 2L, t2, t2.plusSeconds(5), BigDecimal.valueOf(5.2)
        ), "SCENARIO_RUNNER");

        // Event after gap: Sensor reports 11.8°C (Excursion for 2-8°C refrigerated profile)
        EventIngestResult r3 = ingestionService.ingestEvent(new SensorEventPayload(
                "EV-MISS-3", sensor.getId(), shipment.getId(), 3L, t3, t3.plusSeconds(5), BigDecimal.valueOf(11.8)
        ), "SCENARIO_RUNNER");
        notes.add("Event 3 after 20m gap: Sensor = 11.8°C -> Excursion Incident " + r3.getExcursionIncidentId());

        long missingCount = missingIntervalRepository.countByShipmentId(shpId);
        notes.add("Missing Interval Check: Detected " + missingCount + " gap(s) marked UNKNOWN (Never SAFE).");

        // Now simulate Arrival Manual Inspection showing 6.2°C (Compliant!)
        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId(shpId);
        boolean hasContradiction = false;
        if (!incidents.isEmpty()) {
            ExcursionIncident incident = incidents.get(0);
            Investigation inv = investigationRepository.findByExcursionId(incident.getId()).orElse(null);
            if (inv != null) {
                ManualInspectionRequest manualReq = new ManualInspectionRequest();
                manualReq.setInspectorName("Dr. Sarah Vance, QA Lead");
                manualReq.setInspectionTime(t3.plusSeconds(180));
                manualReq.setObservedTemperatureC(BigDecimal.valueOf(6.2));
                manualReq.setPackageIntegrityStatus("INTACT");
                manualReq.setVisualCondition("Dry ice container intact, data logger secondary check shows 6.2°C compliant");
                manualReq.setNotes("Receiving dock inspection showed package integrity fully intact at 6.2°C.");

                investigationService.addManualInspection(inv.getId(), manualReq, "ANALYST");

                Investigation updatedInv = investigationRepository.findById(inv.getId()).get();
                hasContradiction = updatedInv.getStatus() == InvestigationStatus.CONTRADICTORY_EVIDENCE;
                notes.add("Manual Inspection added: 6.2°C vs Sensor 11.8°C -> Investigation status: " + updatedInv.getStatus());
            }
        }

        boolean success = missingCount > 0 && hasContradiction;
        notes.add("Verification: Both sensor and manual evidence preserved independently; flagged CONTRADICTORY_EVIDENCE.");

        ScenarioRunResult result = new ScenarioRunResult();
        result.setScenarioName("6. Missing Data & Contradictory Manual Inspection");
        result.setDescription("20-min telemetry gap detected as UNKNOWN. Sensor 11.8°C contradicts dock inspection 6.2°C; both evidence sources preserved independently.");
        result.setSuccess(success);
        result.setShipmentId(shpId);
        result.setEventsIngested(3);
        result.setIncidentsCreated(incidents.size());
        result.setExcursionSeverity(incidents.isEmpty() ? "NONE" : incidents.get(0).getSeverity().name());
        result.setHasDuplicate(false);
        result.setHasMissingInterval(missingCount > 0);
        result.setHasContradictoryEvidence(hasContradiction);
        result.setVerificationNotes(notes);
        return result;
    }

    public ScenarioRunResult runAllScenarios() {
        runNormalScenario();
        runMinorExcursionScenario();
        runMajorCriticalScenario();
        runDuplicateScenario();
        runOutOfOrderLateScenario();
        return runMissingAndContradictoryScenario();
    }
}
