package com.coldchain.platform.lot.web;

import com.coldchain.platform.lot.dto.CreateLotRequest;
import com.coldchain.platform.lot.dto.LotResponse;
import com.coldchain.platform.lot.service.LotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LotController {

    private final LotService lotService;

    public LotController(LotService lotService) {
        this.lotService = lotService;
    }

    @PostMapping("/api/lots")
    public ResponseEntity<LotResponse> createLot(@Valid @RequestBody CreateLotRequest request,
                                                 Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        LotResponse response = lotService.createLot(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/api/shipments/{shipmentId}/lots")
    public ResponseEntity<List<LotResponse>> getLotsForShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(lotService.getLotsForShipment(shipmentId));
    }
}
