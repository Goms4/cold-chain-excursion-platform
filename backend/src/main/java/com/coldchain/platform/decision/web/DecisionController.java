package com.coldchain.platform.decision.web;

import com.coldchain.platform.decision.dto.DecisionResponse;
import com.coldchain.platform.decision.dto.RecordDecisionRequest;
import com.coldchain.platform.decision.service.DecisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/api/investigations/{id}/decision")
    public ResponseEntity<DecisionResponse> recordDecision(@PathVariable String id,
                                                           @Valid @RequestBody RecordDecisionRequest request,
                                                           Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("An authenticated analyst is required to record a decision.");
        }
        String actor = authentication.getName();
        DecisionResponse response = decisionService.recordDecision(id, request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/api/investigations/{id}/decisions")
    public ResponseEntity<List<DecisionResponse>> getDecisions(@PathVariable String id) {
        return ResponseEntity.ok(decisionService.getDecisionsForInvestigation(id));
    }
}
