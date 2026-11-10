package com.coldchain.platform.investigation.web;

import com.coldchain.platform.investigation.dto.InvestigationResponse;
import com.coldchain.platform.investigation.dto.ManualInspectionRequest;
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
        ManualInspectionRecord record = investigationService.addManualInspection(id, request, actor);
        return new ResponseEntity<>(record, HttpStatus.CREATED);
    }
}
