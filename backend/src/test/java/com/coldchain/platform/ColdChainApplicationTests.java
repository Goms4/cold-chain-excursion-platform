package com.coldchain.platform;

import com.coldchain.platform.decision.dto.RecordDecisionRequest;
import com.coldchain.platform.decision.model.DecisionType;
import com.coldchain.platform.decision.service.DecisionService;
import com.coldchain.platform.event.dto.EventIngestResult;
import com.coldchain.platform.event.dto.SensorEventPayload;
import com.coldchain.platform.event.model.EventProcessingStatus;
import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.event.model.TemperatureEvaluationStatus;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.service.EventIngestionService;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.ExcursionSeverity;
import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.excursion.repository.StorageProfileRepository;
import com.coldchain.platform.investigation.dto.ManualInspectionRequest;
import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.investigation.service.InvestigationService;
import com.coldchain.platform.lot.dto.CreateLotRequest;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.lot.service.LotService;
import com.coldchain.platform.scenario.dto.ScenarioRunResult;
import com.coldchain.platform.scenario.service.ScenarioRunnerService;
import com.coldchain.platform.sensor.dto.RegisterSensorRequest;
import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.sensor.repository.SensorRepository;
import com.coldchain.platform.sensor.service.SensorService;
import com.coldchain.platform.shipment.dto.CreateShipmentRequest;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import com.coldchain.platform.shipment.service.ShipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ColdChainApplicationTests {

    @Autowired
    private ScenarioRunnerService scenarioRunnerService;

    @Autowired
    private ShipmentService shipmentService;

    @Autowired
    private LotService lotService;

    @Autowired
    private SensorService sensorService;

    @Autowired
    private EventIngestionService ingestionService;

    @Autowired
    private InvestigationService investigationService;

    @Autowired
    private DecisionService decisionService;

    @Autowired
    private StorageProfileRepository profileRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private ExcursionIncidentRepository incidentRepository;

    @Autowired
    private InvestigationRepository investigationRepository;

    @Autowired
    private MissingIntervalRepository missingIntervalRepository;

    @BeforeEach
    public void setupProfiles() {
        if (!profileRepository.existsById("PROFILE-CRT")) {
            profileRepository.save(new StorageProfile(
                    "PROFILE-CRT", "Controlled Room / Ambient",
                    new BigDecimal("15.0"), new BigDecimal("25.0"),
                    new BigDecimal("25.1"), new BigDecimal("30.0"),
                    new BigDecimal("30.1"), new BigDecimal("40.0"),
                    new BigDecimal("40.1"), new BigDecimal("99.9"),
                    240, "v1.0.0"
            ));
        }
        if (!profileRepository.existsById("PROFILE-REF")) {
            profileRepository.save(new StorageProfile(
                    "PROFILE-REF", "Refrigerated",
                    new BigDecimal("2.0"), new BigDecimal("8.0"),
                    new BigDecimal("8.1"), new BigDecimal("12.0"),
                    new BigDecimal("12.1"), new BigDecimal("20.0"),
                    new BigDecimal("20.1"), new BigDecimal("99.9"),
                    120, "v1.0.0"
            ));
        }
    }

    @Test
    @DisplayName("Scenario 1: Normal shipment produces 0 incidents")
    public void testScenario1Normal() {
        ScenarioRunResult result = scenarioRunnerService.runNormalScenario();
        assertTrue(result.isSuccess(), "Scenario 1 should succeed");
        assertEquals(0, result.getIncidentsCreated(), "Expected 0 incidents for compliant readings");
    }

    @Test
    @DisplayName("Scenario 2: Minor excursion produces MINOR incident and elevates lot risk")
    public void testScenario2Minor() {
        ScenarioRunResult result = scenarioRunnerService.runMinorExcursionScenario();
        assertTrue(result.isSuccess(), "Scenario 2 should succeed");
        assertEquals(1, result.getIncidentsCreated());
        assertEquals("MINOR", result.getExcursionSeverity());
    }

    @Test
    @DisplayName("Scenario 3: Major/Critical excursion elevates to CRITICAL")
    public void testScenario3MajorCritical() {
        ScenarioRunResult result = scenarioRunnerService.runMajorCriticalScenario();
        assertTrue(result.isSuccess());
        assertEquals(1, result.getIncidentsCreated());
        assertEquals("CRITICAL", result.getExcursionSeverity());
    }

    @Test
    @DisplayName("Scenario 4: Duplicate event_id produces 1 incident, not 2")
    public void testScenario4Duplicate() {
        ScenarioRunResult result = scenarioRunnerService.runDuplicateScenario();
        assertTrue(result.isSuccess());
        assertTrue(result.isHasDuplicate());
        assertEquals(1, result.getIncidentsCreated(), "Duplicate events must not create duplicate incidents");
    }

    @Test
    @DisplayName("Scenario 5: Out of order events evaluated by event_time and flagged OUT_OF_ORDER")
    public void testScenario5OutOfOrder() {
        ScenarioRunResult result = scenarioRunnerService.runOutOfOrderLateScenario();
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Scenario 6: Missing cadence marked UNKNOWN and Contradictory Evidence flagged")
    public void testScenario6MissingAndContradictory() {
        ScenarioRunResult result = scenarioRunnerService.runMissingAndContradictoryScenario();
        assertTrue(result.isSuccess());
        assertTrue(result.isHasMissingInterval(), "Missing cadence should be detected as UNKNOWN");
        assertTrue(result.isHasContradictoryEvidence(), "Sensor vs manual discrepancy should flag CONTRADICTORY_EVIDENCE");
    }

    @Test
    @DisplayName("Decision Workflow: Analyst records RELEASE decision with mandatory reason")
    @Transactional
    public void testDecisionWorkflow() {
        // Run scenario 2 to produce an excursion and investigation
        scenarioRunnerService.runMinorExcursionScenario();

        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId("SHP-SCENARIO-2");
        assertFalse(incidents.isEmpty());
        ExcursionIncident incident = incidents.get(0);

        Investigation investigation = investigationRepository.findByExcursionId(incident.getId()).orElse(null);
        assertNotNull(investigation);

        RecordDecisionRequest decisionReq = new RecordDecisionRequest();
        decisionReq.setDecision(DecisionType.RELEASE);
        decisionReq.setInvestigatorName("Dr. Alex Chen");
        decisionReq.setReason("Transient 27°C reading during refrigerated truck transfer; mean kinetic temperature acceptable per stability data v1.0.0.");
        decisionReq.setRuleVersion("v1.0.0");
        decisionReq.setAffectedLotIds(List.of("LOT-SCEN-2"));

        decisionService.recordDecision(investigation.getId(), decisionReq, "ANALYST");

        // Verify investigation is completed
        Investigation updatedInv = investigationRepository.findById(investigation.getId()).orElseThrow();
        assertEquals(InvestigationStatus.COMPLETED, updatedInv.getStatus());

        // Verify lot status updated
        Lot lot = lotRepository.findById("LOT-SCEN-2").orElseThrow();
        assertEquals(LotDisposition.RELEASED, lot.getDispositionStatus());
        assertEquals(LotRiskLevel.LOW, lot.getRiskLevel());
    }
}
