package com.coldchain.platform.shipment.web;

import com.coldchain.platform.shipment.dto.CreateShipmentRequest;
import com.coldchain.platform.shipment.dto.ShipmentResponse;
import com.coldchain.platform.shipment.dto.UpdateShipmentRequest;
import com.coldchain.platform.common.dto.DeleteReasonRequest;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.service.ShipmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<ShipmentResponse>> getAllShipments(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(shipmentService.getAllShipments(includeArchived));
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

    @PatchMapping("/{id}")
    public ResponseEntity<ShipmentResponse> updateShipment(@PathVariable String id,
                                                            @Valid @RequestBody UpdateShipmentRequest request,
                                                            Authentication authentication) {
        return ResponseEntity.ok(shipmentService.updateShipment(id, request,
                authentication == null ? "Operations officer" : authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id, @Valid @RequestBody DeleteReasonRequest request,
                                       Authentication authentication) {
        shipmentService.deleteShipment(id, authentication == null ? "Operations officer" : authentication.getName(), request.getReason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ShipmentResponse> archive(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(shipmentService.archiveShipment(id, authentication == null ? "OPERATIONS" : authentication.getName()));
    }
}
