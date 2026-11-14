package com.coldchain.platform.shipment.service;

import com.coldchain.platform.event.model.EventQualityFlag;
import com.coldchain.platform.event.model.ProcessedSensorEvent;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.shipment.model.Shipment;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class ShipmentConditionService {
    private final ProcessedSensorEventRepository eventRepository;
    private final ExcursionIncidentRepository incidentRepository;
    private final MissingIntervalRepository missingIntervalRepository;
    private final InvestigationRepository investigationRepository;

    public ShipmentConditionService(ProcessedSensorEventRepository eventRepository,
                                    ExcursionIncidentRepository incidentRepository,
                                    MissingIntervalRepository missingIntervalRepository,
                                    InvestigationRepository investigationRepository) {
        this.eventRepository = eventRepository;
        this.incidentRepository = incidentRepository;
        this.missingIntervalRepository = missingIntervalRepository;
        this.investigationRepository = investigationRepository;
    }

    /** Condition precedence: under review, unresolved issue, telemetry at risk, normal, then no readings. */
    public String condition(Shipment shipment) {
        var unresolvedIssues = incidentRepository.findByShipmentId(shipment.getId()).stream()
                .filter(issue -> issue.getStatus() == IncidentStatus.OPEN
                        || issue.getStatus() == IncidentStatus.UNDER_INVESTIGATION).toList();
        for (var issue : unresolvedIssues) {
            var investigation = investigationRepository.findByExcursionId(issue.getId()).orElse(null);
            if (investigation != null && investigation.getStatus() != InvestigationStatus.COMPLETED) {
                return "UNDER_REVIEW";
            }
        }
        if (!unresolvedIssues.isEmpty()) return "TEMPERATURE_ISSUE";

        List<ProcessedSensorEvent> readings = eventRepository.findByShipmentIdOrderByEventTimeAsc(shipment.getId());
        if (readings.isEmpty()) return "UNKNOWN";

        ProcessedSensorEvent latestReliable = readings.stream()
                .filter(event -> !Boolean.TRUE.equals(event.getIsDuplicate()))
                .filter(event -> event.getQualityFlag() != EventQualityFlag.QUARANTINED)
                .filter(event -> event.getTemperatureC() != null)
                .max(Comparator.comparing(ProcessedSensorEvent::getEventTime))
                .orElse(null);
        if (latestReliable == null) return "AT_RISK";

        int cadence = shipment.getExpectedCadenceMinutes() == null ? 5 : shipment.getExpectedCadenceMinutes();
        boolean telemetryOverdue = latestReliable.getEventTime()
                .plusSeconds((long) (cadence + 1) * 60L).isBefore(Instant.now());
        boolean unresolvedGap = missingIntervalRepository.findByShipmentId(shipment.getId()).stream()
                .filter(interval -> "UNKNOWN".equalsIgnoreCase(interval.getStatus()))
                .anyMatch(interval -> interval.getGapEndTime().isAfter(latestReliable.getEventTime()));
        if (telemetryOverdue || unresolvedGap) return "AT_RISK";

        var temperature = latestReliable.getTemperatureC();
        var profile = shipment.getStorageProfile();
        return temperature.compareTo(profile.getTargetMinC()) < 0
                || temperature.compareTo(profile.getTargetMaxC()) > 0 ? "AT_RISK" : "NORMAL";
    }
}
