package com.coldchain.platform.sensor.web;

import com.coldchain.platform.sensor.dto.AssignSensorRequest;
import com.coldchain.platform.sensor.dto.RegisterSensorRequest;
import com.coldchain.platform.sensor.dto.SensorResponse;
import com.coldchain.platform.sensor.service.SensorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SensorController {

    private final SensorService sensorService;

    public SensorController(SensorService sensorService) {
        this.sensorService = sensorService;
    }

    @PostMapping("/api/sensors")
    public ResponseEntity<SensorResponse> registerSensor(@Valid @RequestBody RegisterSensorRequest request,
                                                         Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        SensorResponse response = sensorService.registerSensor(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/api/shipments/{shipmentId}/sensors")
    public ResponseEntity<SensorResponse> assignSensor(@PathVariable String shipmentId,
                                                       @Valid @RequestBody AssignSensorRequest request,
                                                       Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        SensorResponse response = sensorService.assignSensorToShipment(shipmentId, request, actor);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/sensors/{id}")
    public ResponseEntity<SensorResponse> getSensorById(@PathVariable String id) {
        return ResponseEntity.ok(sensorService.getSensor(id));
    }

    @GetMapping("/api/sensors")
    public ResponseEntity<List<SensorResponse>> getAllSensors() {
        return ResponseEntity.ok(sensorService.getAllSensors());
    }

    @GetMapping("/api/shipments/{shipmentId}/sensors")
    public ResponseEntity<List<SensorResponse>> getSensorsForShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(sensorService.getSensorsForShipment(shipmentId));
    }
}
