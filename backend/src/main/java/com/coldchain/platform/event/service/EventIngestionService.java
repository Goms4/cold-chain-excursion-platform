package com.coldchain.platform.event.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.event.dto.EventIngestResult;
import com.coldchain.platform.event.dto.SensorEventPayload;
import com.coldchain.platform.event.model.*;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.event.repository.RawSensorEventRepository;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.ExcursionSeverity;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.excursion.service.ExcursionIncidentService;
import com.coldchain.platform.excursion.service.ExcursionRuleEngine;
import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.sensor.repository.SensorRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class EventIngestionService {

    private final RawSensorEventRepository rawRepository;
    private final ProcessedSensorEventRepository processedRepository;
    private final ShipmentRepository shipmentRepository;
    private final SensorRepository sensorRepository;
    private final ExcursionIncidentRepository incidentRepository;
    private final DeduplicationService deduplicationService;
    private final EventTimeEvaluationService eventTimeEvaluationService;
    private final MissingDataDetectorService missingDataDetectorService;
    private final ExcursionRuleEngine excursionRuleEngine;
    private final ExcursionIncidentService excursionIncidentService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public EventIngestionService(RawSensorEventRepository rawRepository,
                                 ProcessedSensorEventRepository processedRepository,
                                 ShipmentRepository shipmentRepository,
                                 SensorRepository sensorRepository,
                                 ExcursionIncidentRepository incidentRepository,
                                 DeduplicationService deduplicationService,
                                 EventTimeEvaluationService eventTimeEvaluationService,
                                 MissingDataDetectorService missingDataDetectorService,
                                 ExcursionRuleEngine excursionRuleEngine,
                                 ExcursionIncidentService excursionIncidentService,
                                 AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.rawRepository = rawRepository;
        this.processedRepository = processedRepository;
        this.shipmentRepository = shipmentRepository;
        this.sensorRepository = sensorRepository;
        this.incidentRepository = incidentRepository;
        this.deduplicationService = deduplicationService;
        this.eventTimeEvaluationService = eventTimeEvaluationService;
        this.missingDataDetectorService = missingDataDetectorService;
        this.excursionRuleEngine = excursionRuleEngine;
        this.excursionIncidentService = excursionIncidentService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventIngestResult ingestEvent(SensorEventPayload payload, String actor) {
        Instant effectiveIngestionTime = payload.getIngestionTime() != null ? payload.getIngestionTime() : Instant.now();

        // 1. Serialize and save raw sensor event (Append-Only payload retention)
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            rawJson = "{}";
        }

        // Validate entity existence
        Shipment shipment = shipmentRepository.findById(payload.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", payload.getShipmentId()));

        Sensor sensor = sensorRepository.findById(payload.getSensorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sensor", payload.getSensorId()));

        if (payload.getTemperatureC() == null) {
            throw new ValidationException("temperature_c is required");
        }

        // 2. Deduplication check by event_id
        boolean isDuplicate = deduplicationService.isDuplicateEvent(payload.getEventId());
        if (isDuplicate) {
            RawSensorEvent rawEvent = new RawSensorEvent(
                    payload.getEventId(), sensor.getId(), shipment.getId(), payload.getSequenceNo(),
                    payload.getEventTime(), effectiveIngestionTime, rawJson, EventProcessingStatus.DUPLICATE
            );
            rawRepository.save(rawEvent);

            ProcessedSensorEvent processedDuplicate = new ProcessedSensorEvent(
                    payload.getEventId() + "-dup-" + System.currentTimeMillis(),
                    sensor, shipment, payload.getSequenceNo(),
                    payload.getEventTime(), effectiveIngestionTime, payload.getTemperatureC(),
                    TemperatureEvaluationStatus.UNKNOWN, EventQualityFlag.VALID, true, payload.getEventId()
            );
            processedRepository.save(processedDuplicate);

            auditService.recordAudit("SENSOR_EVENT", payload.getEventId(), "DUPLICATE_DETECTED", actor,
                    null, "Duplicate event_id '" + payload.getEventId() + "' suppressed",
                    "Deduplication policy: duplicate event retained for traceability but excluded from incident creation");

            EventIngestResult result = new EventIngestResult();
            result.setEventId(payload.getEventId());
            result.setShipmentId(shipment.getId());
            result.setSensorId(sensor.getId());
            result.setEventTime(payload.getEventTime());
            result.setIngestionTime(effectiveIngestionTime);
            result.setTemperatureC(payload.getTemperatureC());
            result.setProcessingStatus(EventProcessingStatus.DUPLICATE);
            result.setQualityFlag(EventQualityFlag.VALID);
            result.setEvaluationStatus(TemperatureEvaluationStatus.UNKNOWN);
            result.setIsDuplicate(true);
            result.setMessage("Duplicate event_id detected. Event archived without triggering duplicate incident.");
            return result;
        }

        // 3. Temporal watermark and business finalization check
        Optional<ProcessedSensorEvent> lastEventOpt = processedRepository
                .findFirstByShipmentIdAndSensorIdOrderByEventTimeDesc(shipment.getId(), sensor.getId());
        Instant lastObservedEventTime = lastEventOpt.map(ProcessedSensorEvent::getEventTime).orElse(null);

        Optional<ExcursionIncident> activeIncidentOpt = incidentRepository
                .findFirstByShipmentIdAndStatusNot(shipment.getId(), IncidentStatus.CLOSED);
        boolean isIncidentFinalized = activeIncidentOpt.map(ExcursionIncident::isBusinessFinalized).orElse(false);

        EventTimeEvaluationService.EventTemporalDisposition temporalDisposition =
                eventTimeEvaluationService.evaluateTemporalDisposition(
                        shipment, payload.getEventTime(), effectiveIngestionTime,
                        lastObservedEventTime, isIncidentFinalized
                );

        EventQualityFlag qualityFlag = eventTimeEvaluationService.toQualityFlag(temporalDisposition);

        if (qualityFlag == EventQualityFlag.QUARANTINED) {
            EventProcessingStatus procStatus = EventProcessingStatus.QUARANTINED;
            RawSensorEvent rawEvent = new RawSensorEvent(
                    payload.getEventId(), sensor.getId(), shipment.getId(), payload.getSequenceNo(),
                    payload.getEventTime(), effectiveIngestionTime, rawJson, procStatus
            );
            rawRepository.save(rawEvent);

            ProcessedSensorEvent processedEvent = new ProcessedSensorEvent(
                    payload.getEventId(), sensor, shipment, payload.getSequenceNo(),
                    payload.getEventTime(), effectiveIngestionTime, payload.getTemperatureC(),
                    TemperatureEvaluationStatus.UNKNOWN, qualityFlag, false, null
            );
            processedRepository.save(processedEvent);

            String quarantineReason = temporalDisposition == EventTimeEvaluationService.EventTemporalDisposition.QUARANTINED_BUSINESS_FINALIZED
                    ? "Event arrived for a business-finalized shipment/excursion. Quarantined to prevent mutation of final disposition."
                    : "Event breached allowed-lateness watermark window. Quarantined for audit review.";

            auditService.recordAudit("SENSOR_EVENT", payload.getEventId(), "QUARANTINED", actor,
                    null, "Event quarantined: " + temporalDisposition.name(), quarantineReason);

            EventIngestResult result = new EventIngestResult();
            result.setEventId(payload.getEventId());
            result.setShipmentId(shipment.getId());
            result.setSensorId(sensor.getId());
            result.setEventTime(payload.getEventTime());
            result.setIngestionTime(effectiveIngestionTime);
            result.setTemperatureC(payload.getTemperatureC());
            result.setProcessingStatus(procStatus);
            result.setQualityFlag(qualityFlag);
            result.setEvaluationStatus(TemperatureEvaluationStatus.UNKNOWN);
            result.setIsDuplicate(false);
            result.setMessage(quarantineReason);
            return result;
        }

        // 4. Missing interval detection (gap between consecutive measurements)
        if (lastObservedEventTime != null && payload.getEventTime().isAfter(lastObservedEventTime)) {
            missingDataDetectorService.checkAndRecordMissingInterval(shipment, sensor, lastObservedEventTime, payload.getEventTime());
        }

        // 5. Excursion evaluation using authoritative event_time and storage profile
        int currentDuration = 0;
        if (activeIncidentOpt.isPresent() && activeIncidentOpt.get().getStartEventTime() != null) {
            currentDuration = activeIncidentOpt.get().getDurationMinutes();
        }

        ExcursionSeverity calculatedSeverity = excursionRuleEngine.evaluateTemperature(
                payload.getTemperatureC(),
                shipment.getStorageProfile(),
                currentDuration
        );

        TemperatureEvaluationStatus evalStatus;
        String linkedExcursionId = null;

        if (calculatedSeverity != ExcursionSeverity.NORMAL) {
            evalStatus = TemperatureEvaluationStatus.EXCURSION;
            ExcursionIncident incident = excursionIncidentService.handleExcursionObservation(
                    shipment,
                    payload.getTemperatureC(),
                    payload.getEventTime(),
                    calculatedSeverity,
                    shipment.getStorageProfile().getRuleVersion()
            );
            linkedExcursionId = incident.getId();
        } else {
            evalStatus = TemperatureEvaluationStatus.NORMAL;
        }

        // 6. Save raw event as VALIDATED
        RawSensorEvent rawEvent = new RawSensorEvent(
                payload.getEventId(), sensor.getId(), shipment.getId(), payload.getSequenceNo(),
                payload.getEventTime(), effectiveIngestionTime, rawJson, EventProcessingStatus.VALIDATED
        );
        rawRepository.save(rawEvent);

        // 7. Save normalized processed event
        ProcessedSensorEvent processedEvent = new ProcessedSensorEvent(
                payload.getEventId(), sensor, shipment, payload.getSequenceNo(),
                payload.getEventTime(), effectiveIngestionTime, payload.getTemperatureC(),
                evalStatus, qualityFlag, false, null
        );
        processedEvent.setExcursionIncidentId(linkedExcursionId);
        processedRepository.save(processedEvent);

        EventIngestResult result = new EventIngestResult();
        result.setEventId(payload.getEventId());
        result.setShipmentId(shipment.getId());
        result.setSensorId(sensor.getId());
        result.setEventTime(payload.getEventTime());
        result.setIngestionTime(effectiveIngestionTime);
        result.setTemperatureC(payload.getTemperatureC());
        result.setProcessingStatus(EventProcessingStatus.VALIDATED);
        result.setQualityFlag(qualityFlag);
        result.setEvaluationStatus(evalStatus);
        result.setIsDuplicate(false);
        result.setExcursionIncidentId(linkedExcursionId);
        result.setMessage("Event successfully processed and evaluated using event_time.");
        return result;
    }

    @Transactional
    public List<EventIngestResult> ingestBatch(List<SensorEventPayload> events, String actor) {
        // Sort incoming batch by event_time to respect chronological evaluation
        List<SensorEventPayload> sorted = new ArrayList<>(events);
        sorted.sort(Comparator.comparing(SensorEventPayload::getEventTime));

        List<EventIngestResult> results = new ArrayList<>();
        for (SensorEventPayload payload : sorted) {
            results.add(ingestEvent(payload, actor));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<ProcessedSensorEvent> getProcessedEvents(String shipmentId) {
        return processedRepository.findByShipmentIdOrderByEventTimeAsc(shipmentId);
    }

    @Transactional(readOnly = true)
    public List<RawSensorEvent> getRawEvents(String shipmentId) {
        return rawRepository.findByShipmentIdOrderByEventTimeAsc(shipmentId);
    }
}
