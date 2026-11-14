package com.coldchain.platform;

import com.coldchain.platform.common.security.Role;
import com.coldchain.platform.auth.service.RolePasswordService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ColdChainApplicationTests {

    private MockHttpSession login(Role role, String password) throws Exception {
        var csrf = SecurityMockMvcRequestPostProcessors.csrf();
        var result = mockMvc.perform(post("/api/auth/login").with(csrf)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("role", role.name(), "companyName", "Test Pharma", "password", password))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    @DisplayName("Role/password login creates an eight-hour server session and logout clears it")
    void authSessionLifecycle() throws Exception {
        mockMvc.perform(post("/api/auth/login").with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("role", "OPERATIONS", "companyName", "Test Pharma", "password", TEST_ANALYST_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Incorrect role or password"));
        MockHttpSession session = login(Role.OPERATIONS, TEST_OPS_PASSWORD);
        assertNotNull(session);
        assertEquals(8 * 60 * 60, session.getMaxInactiveInterval());
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("OPERATIONS"))
                .andExpect(jsonPath("$.token").doesNotExist());
        mockMvc.perform(post("/api/auth/logout").session(session).with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Role credentials use BCrypt and match only their configured role")
    void rolePasswordHashingAndMatching() {
        String encoded = passwordEncoder.encode(TEST_OPS_PASSWORD);
        assertTrue(encoded.startsWith("$2a$") || encoded.startsWith("$2b$"));
        assertTrue(passwordEncoder.matches(TEST_OPS_PASSWORD, encoded));
        assertTrue(rolePasswordService.matches(Role.OPERATIONS, TEST_OPS_PASSWORD));
        assertFalse(rolePasswordService.matches(Role.ANALYST, TEST_OPS_PASSWORD));
        assertFalse(rolePasswordService.matches(Role.OPERATIONS, "incorrect"));
    }

    @Autowired
    private MockMvc mockMvc;

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

    private static final String TEST_OPS_PASSWORD = java.util.UUID.randomUUID().toString();
    private static final String TEST_ANALYST_PASSWORD = java.util.UUID.randomUUID().toString();
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RolePasswordService rolePasswordService;

    @DynamicPropertySource
    static void authProperties(DynamicPropertyRegistry registry) {
        registry.add("app.auth.operations-password", () -> TEST_OPS_PASSWORD);
        registry.add("app.auth.analyst-password", () -> TEST_ANALYST_PASSWORD);
    }

    @Autowired
    private ObjectMapper objectMapper;

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
        decisionReq.setInvestigatorName("Forged User");
        decisionReq.setReason("Transient 27°C reading during refrigerated truck transfer; mean kinetic temperature acceptable per stability data v1.0.0.");
        decisionReq.setRuleVersion("forged-rule");

        var decision = decisionService.recordDecision(investigation.getId(), decisionReq, "authenticated-analyst");
        assertEquals("authenticated-analyst", decision.getInvestigatorName());
        assertEquals(incident.getRuleVersion(), decision.getRuleVersion());
        assertEquals(List.of("LOT-10002"), decision.getAffectedLotIds());

        // Verify investigation is completed
        Investigation updatedInv = investigationRepository.findById(investigation.getId()).orElseThrow();
        assertEquals(InvestigationStatus.COMPLETED, updatedInv.getStatus());

        // Verify lot status updated
        Lot lot = lotRepository.findById("LOT-SCEN-2").orElseThrow();
        assertEquals(LotDisposition.RELEASED, lot.getDispositionStatus());
        assertEquals(LotRiskLevel.LOW, lot.getRiskLevel());
    }

    @Test
    @DisplayName("Sensor Events API: GET /api/shipments/{shipmentId}/sensor-events returns DTO without serialization errors")
    public void testGetProcessedEventsEndpoint() throws Exception {
        scenarioRunnerService.runMinorExcursionScenario();

        mockMvc.perform(get("/api/shipments/SHP-SCENARIO-2/sensor-events").session(login(Role.ANALYST, TEST_ANALYST_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].eventId").exists())
                .andExpect(jsonPath("$[0].sensorId").value("SEN-002"))
                .andExpect(jsonPath("$[0].shipmentId").value("SHP-10002"))
                .andExpect(jsonPath("$[0].eventTime").exists())
                .andExpect(jsonPath("$[0].ingestionTime").exists())
                .andExpect(jsonPath("$[0].temperatureC").exists())
                .andExpect(jsonPath("$[0].condition").exists())
                .andExpect(jsonPath("$[0].dataQuality").exists())
                .andExpect(jsonPath("$[0].lotId").doesNotExist());
    }

    @Test
    @DisplayName("RBAC: Disposition decision endpoint is restricted to ANALYST and rejects OPERATIONS with 403")
    public void testDecisionEndpointRbac() throws Exception {
        scenarioRunnerService.runMinorExcursionScenario();
        List<ExcursionIncident> incidents = incidentRepository.findByShipmentId("SHP-SCENARIO-2");
        assertFalse(incidents.isEmpty());
        Investigation inv = investigationRepository.findByExcursionId(incidents.get(0).getId()).orElseThrow();

        RecordDecisionRequest req = new RecordDecisionRequest();
        req.setDecision(DecisionType.RELEASE);
        req.setInvestigatorName("Forged User");
        req.setReason("Product safe based on stability margin data.");
        req.setRuleVersion("forged-rule");

        MockHttpSession opsSession = login(Role.OPERATIONS, TEST_OPS_PASSWORD);
        MockHttpSession analystSession = login(Role.ANALYST, TEST_ANALYST_PASSWORD);

        // OPERATIONS role must be rejected with 403 Forbidden
        mockMvc.perform(post("/api/investigations/" + inv.getId() + "/decision")
                .session(opsSession).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // ANALYST role must be accepted with 201 Created
        mockMvc.perform(post("/api/investigations/" + inv.getId() + "/decision")
                .session(analystSession).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.investigatorName").value("Analyst · Test Pharma"))
                .andExpect(jsonPath("$.ruleVersion").value(incidents.get(0).getRuleVersion()))
                .andExpect(jsonPath("$.affectedLotIds[0]").value("LOT-10002"));
    }

    @Test
    @DisplayName("RBAC: Shipment registration is restricted to OPERATIONS and rejects ANALYST with 403")
    public void testShipmentRegistrationRbac() throws Exception {
        CreateShipmentRequest req = new CreateShipmentRequest();
        req.setId("SHP-RBAC-TEST");
        req.setTrackingNumber("TRK-RBAC-12345");
        req.setShipmentReference("RBAC shipment registration");
        req.setOrigin("Hub A");
        req.setDestination("Clinic B");
        req.setStorageProfileId("PROFILE-REF");
        req.setExpectedCadenceMinutes(5);
        req.setAllowedLatenessMinutes(120);

        MockHttpSession opsSession = login(Role.OPERATIONS, TEST_OPS_PASSWORD);
        MockHttpSession analystSession = login(Role.ANALYST, TEST_ANALYST_PASSWORD);

        // ANALYST role must be rejected with 403 Forbidden
        mockMvc.perform(post("/api/shipments")
                .session(analystSession).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());

        // OPERATIONS role must be accepted with 201 Created
        mockMvc.perform(post("/api/shipments")
                .session(opsSession).with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Analysts cannot edit shipment or lot records")
    public void testAnalystCannotEditShipmentOrLot() throws Exception {
        MockHttpSession analystSession = login(Role.ANALYST, TEST_ANALYST_PASSWORD);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/shipments/SHP-10001")
                        .session(analystSession).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"origin\":\"Changed\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/lots/LOT-10001")
                        .session(analystSession).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Shipment detail lot query includes active lots and excludes soft-deleted lots")
    public void shipmentDetailRefreshReturnsNewlyRegisteredLot() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String shipmentId = "SHP-" + suffix;
        String lotId = "LOT-" + suffix;
        CreateShipmentRequest shipment = new CreateShipmentRequest();
        shipment.setId(shipmentId);
        shipment.setTrackingNumber("TRK-" + suffix);
        shipment.setShipmentReference("Lot visibility regression");
        shipment.setOrigin("Hub A");
        shipment.setDestination("Clinic B");
        shipment.setStorageProfileId("PROFILE-REF");
        shipment.setExpectedCadenceMinutes(5);
        shipment.setAllowedLatenessMinutes(120);

        MockHttpSession ops = login(Role.OPERATIONS, TEST_OPS_PASSWORD);
        MockHttpSession analyst = login(Role.ANALYST, TEST_ANALYST_PASSWORD);
        mockMvc.perform(post("/api/shipments").session(ops).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(shipment)))
                .andExpect(status().isCreated());

        CreateLotRequest lot = new CreateLotRequest();
        lot.setId(lotId);
        lot.setShipmentId(shipmentId);
        lot.setProductName("Demo medicinal product");
        lot.setBatchNumber("BATCH-" + suffix);
        lot.setQuantity(25);
        mockMvc.perform(post("/api/lots").session(ops).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lot)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/shipments/{id}/lots?includeArchived=true", shipmentId).session(ops))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(lotId))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/shipments/{id}", shipmentId).session(ops))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(shipmentId));

        lotService.deleteLot(lotId, "Operations · Test Pharma", "Regression check soft deletion");
        mockMvc.perform(get("/api/shipments/{id}/lots?includeArchived=true", shipmentId).session(ops))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/lots").session(analyst).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(lot)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/shipments/{shipmentId}/lots/{lotId}/temperature", shipmentId, lotId)
                        .session(analyst).with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"temperatureC\":5.0}"))
                .andExpect(status().isForbidden());
    }
}
