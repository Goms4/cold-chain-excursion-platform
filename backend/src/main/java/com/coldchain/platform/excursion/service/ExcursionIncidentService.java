package com.coldchain.platform.excursion.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.BusinessIdGenerator;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.excursion.dto.ExcursionResponse;
import com.coldchain.platform.excursion.model.ExcursionAffectedLot;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.ExcursionSeverity;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.excursion.repository.ExcursionAffectedLotRepository;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.investigation.service.InvestigationService;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.service.LotTracingService;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExcursionIncidentService {

    private final ExcursionIncidentRepository incidentRepository;
    private final ExcursionAffectedLotRepository affectedLotRepository;
    private final ShipmentRepository shipmentRepository;
    private final LotTracingService lotTracingService;
    private final InvestigationService investigationService;
    private final AuditService auditService;
    private final BusinessIdGenerator businessIdGenerator;
    private final ProcessedSensorEventRepository eventRepository;

    public ExcursionIncidentService(ExcursionIncidentRepository incidentRepository,
                                    ExcursionAffectedLotRepository affectedLotRepository,
                                    ShipmentRepository shipmentRepository,
                                    LotTracingService lotTracingService,
                                    InvestigationService investigationService,
                                    AuditService auditService,
                                    BusinessIdGenerator businessIdGenerator,
                                    ProcessedSensorEventRepository eventRepository) {
        this.incidentRepository = incidentRepository;
        this.affectedLotRepository = affectedLotRepository;
        this.shipmentRepository = shipmentRepository;
        this.lotTracingService = lotTracingService;
        this.investigationService = investigationService;
        this.auditService = auditService;
        this.businessIdGenerator = businessIdGenerator;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ExcursionIncident handleExcursionObservation(Shipment shipment, BigDecimal temperature,
                                                        Instant eventTime, ExcursionSeverity severity,
                                                        String ruleVersion) {
        // Find existing open or active excursion for this shipment
        Optional<ExcursionIncident> activeIncidentOpt = incidentRepository
                .findFirstByShipmentIdAndStatusNot(shipment.getId(), IncidentStatus.CLOSED);

        ExcursionIncident incident;
        if (activeIncidentOpt.isPresent()) {
            incident = activeIncidentOpt.get();
            // Don't modify if business finalized
            if (incident.isBusinessFinalized()) {
                return incident;
            }

            // Update peak temperature if current reading is higher (or more extreme)
            if (temperature.compareTo(incident.getPeakTemperatureC()) > 0) {
                incident.setPeakTemperatureC(temperature);
            }
            // Upgrade severity if current severity is higher
            if (severity.ordinal() > incident.getSeverity().ordinal()) {
                incident.setSeverity(severity);
            }

            // Update duration based on event time
            if (eventTime.isAfter(incident.getStartEventTime())) {
                incident.setEndEventTime(eventTime);
                long duration = Duration.between(incident.getStartEventTime(), eventTime).toMinutes();
                incident.setDurationMinutes((int) duration);
            }

            incidentRepository.save(incident);
        } else {
            // Create a new Excursion Incident
            String incidentId = businessIdGenerator.nextIssueId();
            incident = new ExcursionIncident(
                    incidentId,
                    shipment,
                    ruleVersion,
                    severity,
                    IncidentStatus.OPEN,
                    eventTime,
                    temperature
            );
            incident.setEndEventTime(eventTime);
            incidentRepository.save(incident);

            // Update shipment status to EXCURSION
            auditService.recordAudit("EXCURSION", incident.getId(), "CREATED", "System", null,
                    "Excursion detected: " + severity.name() + " at " + temperature + "°C",
                    "Storage profile excursion threshold breached");

            // Trace and link affected lots
            List<Lot> affectedLots = lotTracingService.traceAndFlagLotsForExcursion(shipment.getId(), incident.getId(), severity, eventTime);
            for (Lot lot : affectedLots) {
                ExcursionAffectedLot affectedLot = new ExcursionAffectedLot(incident, lot, lot.getRiskLevel().name());
                affectedLotRepository.save(affectedLot);
            }

            // Automatically open an Investigation
            investigationService.openInvestigation(incident, "unassigned");
        }

        return incident;
    }

    @Transactional(readOnly = true)
    public ExcursionResponse getExcursionById(String id) {
        ExcursionIncident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExcursionIncident", id));
        List<String> affectedLotIds = affectedLotRepository.findByExcursionId(id).stream()
                .map(a -> a.getLot().getId())
                .collect(Collectors.toList());
        return response(incident, affectedLotIds);
    }

    @Transactional(readOnly = true)
    public List<ExcursionResponse> getAllExcursions() {
        return incidentRepository.findAll().stream()
                .map(i -> {
                    List<String> lotIds = affectedLotRepository.findByExcursionId(i.getId()).stream()
                            .map(a -> a.getLot().getId())
                            .collect(Collectors.toList());
                    return response(i, lotIds);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExcursionResponse> getExcursionsByShipment(String shipmentId) {
        String resolvedShipmentId = shipmentRepository.existsById(shipmentId) ? shipmentId : com.coldchain.platform.common.BusinessIds.internalShipment(shipmentId);
        return incidentRepository.findByShipmentId(resolvedShipmentId).stream()
                .map(i -> {
                    List<String> lotIds = affectedLotRepository.findByExcursionId(i.getId()).stream()
                            .map(a -> a.getLot().getId())
                            .collect(Collectors.toList());
                    return response(i, lotIds);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExcursionIncident getExcursionEntity(String id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExcursionIncident", id));
    }

    private ExcursionResponse response(ExcursionIncident incident, List<String> lotIds) {
        return ExcursionResponse.fromEntity(incident, lotIds,
                eventRepository.countByExcursionIncidentIdAndIsDuplicateFalse(incident.getId()));
    }
}
