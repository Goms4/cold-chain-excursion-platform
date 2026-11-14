package com.coldchain.platform.shipment.service;

import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.event.model.MissingInterval;
import com.coldchain.platform.event.model.ProcessedSensorEvent;
import com.coldchain.platform.event.model.TemperatureEvaluationStatus;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentConditionServiceTest {
    @Mock ProcessedSensorEventRepository events;
    @Mock ExcursionIncidentRepository incidents;
    @Mock MissingIntervalRepository gaps;
    @Mock InvestigationRepository investigations;

    private ShipmentConditionService service;
    private Shipment shipment;

    @BeforeEach
    void setUp() {
        service = new ShipmentConditionService(events, incidents, gaps, investigations);
        StorageProfile profile = new StorageProfile("PROFILE-REF", "Refrigerated",
                bd("2"), bd("8"), bd("8.1"), bd("12"), bd("12.1"), bd("20"), bd("20.1"), bd("99.9"), 120, "v1.0.0");
        shipment = new Shipment("SHP-10001", "TRK-1", "Origin", "Destination", profile,
                ShipmentStatus.IN_TRANSIT, 5, 120);
    }

    @Test
    void unknownOnlyWhenNoReadingExists() {
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId())).thenReturn(List.of());
        assertEquals("UNKNOWN", service.condition(shipment));
    }

    @Test
    void normalReadingUsesShipmentProfile() {
        when(gaps.findByShipmentId(shipment.getId())).thenReturn(List.of());
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId())).thenReturn(List.of(reading("5.4", Instant.now(), EventQualityFlag.VALID, false)));
        assertEquals("NORMAL", service.condition(shipment));
    }

    @Test
    void twentyDegreesIsNormalForControlledRoomAndAtRiskForRefrigerated() {
        Instant now = Instant.now();
        when(gaps.findByShipmentId(shipment.getId())).thenReturn(List.of());
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId()))
                .thenReturn(List.of(reading("20", now, EventQualityFlag.VALID, false)));
        assertEquals("AT_RISK", service.condition(shipment));

        StorageProfile ambient = new StorageProfile("PROFILE-CRT", "Controlled Room", bd("15"), bd("25"),
                bd("25.1"), bd("30"), bd("30.1"), bd("40"), bd("40.1"), bd("99.9"), 240, "v1.0.0");
        Shipment ambientShipment = new Shipment("SHP-10006", "TRK-6", "Origin", "Destination", ambient,
                ShipmentStatus.IN_TRANSIT, 5, 120);
        when(events.findByShipmentIdOrderByEventTimeAsc(ambientShipment.getId()))
                .thenReturn(List.of(new ProcessedSensorEvent("event", null, ambientShipment, 1L, now, now,
                        bd("20"), TemperatureEvaluationStatus.NORMAL, EventQualityFlag.VALID, false, null)));
        when(gaps.findByShipmentId(ambientShipment.getId())).thenReturn(List.of());
        assertEquals("NORMAL", service.condition(ambientShipment));
    }

    @Test
    void resolvedIncidentFallsThroughToLatestReadingAndNewReadingChangesCondition() {
        ExcursionIncident issue = mock(ExcursionIncident.class);
        when(issue.getStatus()).thenReturn(IncidentStatus.DISPOSITIONED);
        when(incidents.findByShipmentId(shipment.getId())).thenReturn(List.of(issue));
        when(gaps.findByShipmentId(shipment.getId())).thenReturn(List.of());
        Instant now = Instant.now();
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId())).thenReturn(List.of(
                reading("9.1", now.minus(5, ChronoUnit.MINUTES), EventQualityFlag.VALID, false)));
        assertEquals("AT_RISK", service.condition(shipment));
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId())).thenReturn(List.of(
                reading("9.1", now.minus(5, ChronoUnit.MINUTES), EventQualityFlag.VALID, false),
                reading("5.4", now, EventQualityFlag.VALID, false)));
        assertEquals("NORMAL", service.condition(shipment));
    }

    @Test
    void outOfRangeOrOverdueTelemetryIsAtRisk() {
        when(gaps.findByShipmentId(shipment.getId())).thenReturn(List.of());
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId())).thenReturn(List.of(reading("9.1", Instant.now(), EventQualityFlag.VALID, false)));
        assertEquals("AT_RISK", service.condition(shipment));
        when(events.findByShipmentIdOrderByEventTimeAsc(shipment.getId())).thenReturn(List.of(reading("5.4", Instant.now().minus(10, ChronoUnit.MINUTES), EventQualityFlag.VALID, false)));
        assertEquals("AT_RISK", service.condition(shipment));
    }

    @Test
    void openInvestigationHasPriorityOverUnresolvedIssueAndTelemetry() {
        ExcursionIncident issue = mock(ExcursionIncident.class);
        when(issue.getStatus()).thenReturn(IncidentStatus.OPEN);
        when(issue.getId()).thenReturn("ISS-10001");
        when(incidents.findByShipmentId(shipment.getId())).thenReturn(List.of(issue));
        Investigation investigation = mock(Investigation.class);
        when(investigation.getStatus()).thenReturn(InvestigationStatus.OPEN);
        when(investigations.findByExcursionId(issue.getId())).thenReturn(Optional.of(investigation));
        assertEquals("UNDER_REVIEW", service.condition(shipment));
    }

    @Test
    void unresolvedIssueWithoutInvestigationHasPriorityOverUnknownTelemetry() {
        ExcursionIncident issue = mock(ExcursionIncident.class);
        when(issue.getStatus()).thenReturn(IncidentStatus.OPEN);
        when(issue.getId()).thenReturn("ISS-10001");
        when(incidents.findByShipmentId(shipment.getId())).thenReturn(List.of(issue));
        when(investigations.findByExcursionId(issue.getId())).thenReturn(Optional.empty());
        assertEquals("TEMPERATURE_ISSUE", service.condition(shipment));
    }

    private ProcessedSensorEvent reading(String temperature, Instant time, EventQualityFlag quality, boolean duplicate) {
        return new ProcessedSensorEvent("event", null, shipment, 1L, time, Instant.now(), bd(temperature),
                TemperatureEvaluationStatus.NORMAL, quality, duplicate, null);
    }

    private static BigDecimal bd(String value) { return new BigDecimal(value); }
}
