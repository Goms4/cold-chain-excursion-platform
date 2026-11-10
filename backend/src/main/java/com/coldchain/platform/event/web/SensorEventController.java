package com.coldchain.platform.event.web;

import com.coldchain.platform.event.dto.BatchSensorEventPayload;
import com.coldchain.platform.event.dto.EventIngestResult;
import com.coldchain.platform.event.dto.SensorEventPayload;
import com.coldchain.platform.event.model.ProcessedSensorEvent;
import com.coldchain.platform.event.model.RawSensorEvent;
import com.coldchain.platform.event.service.EventIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SensorEventController {

    private final EventIngestionService ingestionService;

    public SensorEventController(EventIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/api/sensor-events")
    public ResponseEntity<EventIngestResult> ingestSingleEvent(@Valid @RequestBody SensorEventPayload payload,
                                                               Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        EventIngestResult result = ingestionService.ingestEvent(payload, actor);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PostMapping("/api/sensor-events/batch")
    public ResponseEntity<List<EventIngestResult>> ingestBatchEvents(@Valid @RequestBody BatchSensorEventPayload payload,
                                                                     Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        List<EventIngestResult> results = ingestionService.ingestBatch(payload.getEvents(), actor);
        return new ResponseEntity<>(results, HttpStatus.CREATED);
    }

    @GetMapping("/api/shipments/{shipmentId}/sensor-events")
    public ResponseEntity<List<ProcessedSensorEvent>> getProcessedEvents(@PathVariable String shipmentId) {
        return ResponseEntity.ok(ingestionService.getProcessedEvents(shipmentId));
    }

    @GetMapping("/api/shipments/{shipmentId}/raw-events")
    public ResponseEntity<List<RawSensorEvent>> getRawEvents(@PathVariable String shipmentId) {
        return ResponseEntity.ok(ingestionService.getRawEvents(shipmentId));
    }
}
