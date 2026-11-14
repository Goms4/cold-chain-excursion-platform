package com.coldchain.platform.event.web;

import com.coldchain.platform.event.dto.BatchSensorEventPayload;
import com.coldchain.platform.event.dto.EventIngestResult;
import com.coldchain.platform.event.dto.ProcessedSensorEventResponse;
import com.coldchain.platform.event.dto.SensorEventPayload;
import com.coldchain.platform.event.dto.TemperatureUpdateRequest;
import com.coldchain.platform.event.dto.TemperatureReadingResponse;
import com.coldchain.platform.event.dto.MissingIntervalResponse;
import com.coldchain.platform.event.model.RawSensorEvent;
import com.coldchain.platform.event.service.EventIngestionService;
import com.coldchain.platform.sensor.repository.SensorRepository;
import com.coldchain.platform.sensor.model.SensorStatus;
import com.coldchain.platform.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
public class SensorEventController {

    private final EventIngestionService ingestionService;
    private final SensorRepository sensorRepository;
    private final ShipmentService shipmentService;

    public SensorEventController(EventIngestionService ingestionService, SensorRepository sensorRepository, ShipmentService shipmentService) {
        this.ingestionService = ingestionService;
        this.sensorRepository = sensorRepository;
        this.shipmentService = shipmentService;
    }

    @PostMapping("/api/shipments/{shipmentId}/lots/{lotId}/temperature")
    public ResponseEntity<TemperatureReadingResponse> recordTemperature(@PathVariable String shipmentId,
                                                               @PathVariable String lotId,
                                                               @Valid @RequestBody TemperatureUpdateRequest request,
                                                               Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        var shipment = shipmentService.getShipmentEntity(shipmentId);
        var activeSensors = sensorRepository.findByShipmentId(shipment.getId()).stream()
                .filter(candidate -> candidate.getStatus() == SensorStatus.ACTIVE).toList();
        if (activeSensors.isEmpty()) {
            throw new com.coldchain.platform.common.exception.ValidationException("No active sensor is assigned to this shipment.");
        }
        if (activeSensors.size() > 1 && (request.getSensorId() == null || request.getSensorId().isBlank())) {
            throw new com.coldchain.platform.common.exception.ValidationException("Select a sensor for this shipment.");
        }
        var sensor = activeSensors.size() == 1 && (request.getSensorId() == null || request.getSensorId().isBlank())
                ? activeSensors.get(0)
                : activeSensors.stream().filter(candidate -> candidate.getId().equals(request.getSensorId())).findFirst()
                .orElseThrow(() -> new com.coldchain.platform.common.exception.ValidationException("The selected sensor is not active on this shipment."));
        Instant ingestionTime = Instant.now();
        Instant eventTime = request.getEventTime() != null ? request.getEventTime() : ingestionTime;
        String manualEventKey = sensor.getId() + "|" + eventTime + "|" + request.getTemperatureC().stripTrailingZeros().toPlainString();
        String eventId = "MAN-" + UUID.nameUUIDFromBytes(manualEventKey.getBytes(StandardCharsets.UTF_8));
        SensorEventPayload payload = new SensorEventPayload(eventId, sensor.getId(), shipment.getId(),
                null, eventTime, ingestionTime, request.getTemperatureC());
        payload.setLotId(lotId);
        payload.setSource("MANUAL");
        return new ResponseEntity<>(TemperatureReadingResponse.from(ingestionService.ingestEvent(payload, actor)), HttpStatus.CREATED);
    }

    @PostMapping("/api/sensor-events")
    public ResponseEntity<EventIngestResult> ingestSingleEvent(@Valid @RequestBody SensorEventPayload payload,
                                                               Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        payload.setIngestionTime(Instant.now());
        payload.setSource("SENSOR");
        EventIngestResult result = ingestionService.ingestEvent(payload, actor);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/api/sensor-events/batch")
    public ResponseEntity<List<EventIngestResult>> ingestBatchEvents(@Valid @RequestBody BatchSensorEventPayload payload,
                                                                     Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        payload.getEvents().forEach(event -> { event.setIngestionTime(Instant.now()); event.setSource("SENSOR"); });
        List<EventIngestResult> results = ingestionService.ingestBatch(payload.getEvents(), actor);
        return new ResponseEntity<>(results, HttpStatus.CREATED);
    }

    @GetMapping("/api/shipments/{shipmentId}/sensor-events")
    public ResponseEntity<List<ProcessedSensorEventResponse>> getProcessedEvents(@PathVariable String shipmentId) {
        return ResponseEntity.ok(ingestionService.getProcessedEvents(shipmentId));
    }

    @GetMapping("/api/shipments/{shipmentId}/missing-intervals")
    public ResponseEntity<List<MissingIntervalResponse>> getMissingIntervals(@PathVariable String shipmentId) {
        return ResponseEntity.ok(ingestionService.getMissingIntervals(shipmentId));
    }

    @GetMapping("/api/shipments/{shipmentId}/raw-events")
    public ResponseEntity<List<RawSensorEvent>> getRawEvents(@PathVariable String shipmentId) {
        return ResponseEntity.ok(ingestionService.getRawEvents(shipmentId));
    }
}
