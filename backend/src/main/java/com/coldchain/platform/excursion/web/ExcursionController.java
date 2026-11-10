package com.coldchain.platform.excursion.web;

import com.coldchain.platform.excursion.dto.ExcursionResponse;
import com.coldchain.platform.excursion.service.ExcursionIncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ExcursionController {

    private final ExcursionIncidentService excursionIncidentService;

    public ExcursionController(ExcursionIncidentService excursionIncidentService) {
        this.excursionIncidentService = excursionIncidentService;
    }

    @GetMapping("/api/excursions")
    public ResponseEntity<List<ExcursionResponse>> getAllExcursions() {
        return ResponseEntity.ok(excursionIncidentService.getAllExcursions());
    }

    @GetMapping("/api/excursions/{id}")
    public ResponseEntity<ExcursionResponse> getExcursionById(@PathVariable String id) {
        return ResponseEntity.ok(excursionIncidentService.getExcursionById(id));
    }

    @GetMapping("/api/shipments/{shipmentId}/excursions")
    public ResponseEntity<List<ExcursionResponse>> getExcursionsByShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(excursionIncidentService.getExcursionsByShipment(shipmentId));
    }
}
