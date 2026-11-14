package com.coldchain.platform.lot.web;

import com.coldchain.platform.lot.dto.CreateLotRequest;
import com.coldchain.platform.lot.dto.LotResponse;
import com.coldchain.platform.lot.dto.UpdateLotRequest;
import com.coldchain.platform.common.dto.DeleteReasonRequest;
import jakarta.validation.Valid;
import com.coldchain.platform.lot.service.LotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<List<LotResponse>> getLotsForShipment(@PathVariable String shipmentId,
                                                                @RequestParam(defaultValue = "false") boolean includeArchived) {
        return ResponseEntity.ok(lotService.getLotsForShipment(shipmentId, includeArchived));
    }

    @DeleteMapping("/api/lots/{id}")
    public ResponseEntity<Void> deleteLot(@PathVariable String id, @Valid @RequestBody DeleteReasonRequest request,
                                          Authentication authentication) {
        lotService.deleteLot(id, authentication == null ? "Operations officer" : authentication.getName(), request.getReason());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/lots/{id}")
    public ResponseEntity<LotResponse> updateLot(@PathVariable String id, @Valid @RequestBody UpdateLotRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.ok(lotService.updateLot(id, request,
                authentication == null ? "Operations officer" : authentication.getName()));
    }

    @PostMapping("/api/lots/{id}/archive")
    public ResponseEntity<LotResponse> archiveLot(@PathVariable String id, Authentication authentication) {
        return ResponseEntity.ok(lotService.archiveLot(id, authentication == null ? "OPERATIONS" : authentication.getName()));
    }
}
