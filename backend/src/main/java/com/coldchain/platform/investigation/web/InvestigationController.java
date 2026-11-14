package com.coldchain.platform.investigation.web;

import com.coldchain.platform.investigation.dto.InvestigationResponse;
import com.coldchain.platform.investigation.dto.ManualInspectionRequest;
import com.coldchain.platform.investigation.dto.RegisterInvestigationRequest;
import com.coldchain.platform.investigation.dto.AssignInvestigatorRequest;
import com.coldchain.platform.investigation.model.ManualInspectionRecord;
import com.coldchain.platform.investigation.service.InvestigationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investigations")
public class InvestigationController {

    private final InvestigationService investigationService;

    public InvestigationController(InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @PostMapping
    public ResponseEntity<InvestigationResponse> registerInvestigation(@Valid @RequestBody RegisterInvestigationRequest request,
                                                                       Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : "OPERATIONS";
        return new ResponseEntity<>(investigationService.registerForExcursion(request.getShipmentId(), request.getLotId(),
                request.getExcursionId(), request.getInvestigator(), actor), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvestigationResponse> getInvestigationById(@PathVariable String id) {
        return ResponseEntity.ok(investigationService.getInvestigationById(id));
    }

    @GetMapping("/excursion/{excursionId}")
    public ResponseEntity<InvestigationResponse> getInvestigationByExcursionId(@PathVariable String excursionId) {
        return ResponseEntity.ok(investigationService.getInvestigationByExcursionId(excursionId));
    }

    @PostMapping("/{id}/manual-inspection")
    public ResponseEntity<ManualInspectionRecord> addManualInspection(@PathVariable String id,
                                                                      @Valid @RequestBody ManualInspectionRequest request,
                                                                      Authentication authentication) {
        String actor = authentication != null ? authentication.getName() : request.getInspectorName();
        if (authentication != null) request.setInspectorName(authentication.getName());
        ManualInspectionRecord record = investigationService.addManualInspection(id, request, actor);
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/assignee")
    public ResponseEntity<InvestigationResponse> assignInvestigator(@PathVariable String id,
                                                                    @Valid @RequestBody AssignInvestigatorRequest request,
                                                                    Authentication authentication) {
        String actor = authentication == null ? "OPERATIONS" : authentication.getName();
        return ResponseEntity.ok(investigationService.assignInvestigator(id, request.getInvestigator(), actor));
    }
}
