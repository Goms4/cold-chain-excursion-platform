package com.coldchain.platform.shipment.web;

import com.coldchain.platform.shipment.dto.CreateShipmentRequest;
import com.coldchain.platform.shipment.dto.ShipmentResponse;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;

    public ShipmentController(ShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    @PostMapping
    public ResponseEntity<ShipmentResponse> createShipment(@Valid @RequestBody CreateShipmentRequest request,
                                                           Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        ShipmentResponse response = shipmentService.createShipment(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ShipmentResponse>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentResponse> getShipmentById(@PathVariable String id) {
        return ResponseEntity.ok(shipmentService.getShipment(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShipmentResponse> updateShipmentStatus(@PathVariable String id,
                                                                 @RequestBody Map<String, String> body,
                                                                 Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        ShipmentStatus status = ShipmentStatus.valueOf(body.get("status").toUpperCase());
        return ResponseEntity.ok(shipmentService.updateStatus(id, status, actor));
    }
}
