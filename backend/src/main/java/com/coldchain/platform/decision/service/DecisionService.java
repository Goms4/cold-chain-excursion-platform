package com.coldchain.platform.decision.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.exception.InvalidStateTransitionException;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.decision.dto.DecisionResponse;
import com.coldchain.platform.decision.dto.RecordDecisionRequest;
import com.coldchain.platform.decision.model.DecisionType;
import com.coldchain.platform.decision.model.DispositionDecision;
import com.coldchain.platform.decision.repository.DispositionDecisionRepository;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.lot.repository.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DecisionService {

    private final DispositionDecisionRepository decisionRepository;
    private final InvestigationRepository investigationRepository;
    private final ExcursionIncidentRepository excursionRepository;
    private final LotRepository lotRepository;
    private final AuditService auditService;

    public DecisionService(DispositionDecisionRepository decisionRepository,
                           InvestigationRepository investigationRepository,
                           ExcursionIncidentRepository excursionRepository,
                           LotRepository lotRepository,
                           AuditService auditService) {
        this.decisionRepository = decisionRepository;
        this.investigationRepository = investigationRepository;
        this.excursionRepository = excursionRepository;
        this.lotRepository = lotRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DecisionResponse recordDecision(String investigationId, RecordDecisionRequest request, String actor) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation", investigationId));

        if (investigation.getStatus() == InvestigationStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Investigation " + investigationId + " is already COMPLETED. Decisions cannot be overwritten.");
        }

        if (request.getReason() == null || request.getReason().trim().length() < 5) {
            throw new ValidationException("A detailed disposition reason is mandatory.");
        }

        if (request.getInvestigatorName() == null || request.getInvestigatorName().isBlank()) {
            throw new ValidationException("investigator_name is required.");
        }

        ExcursionIncident excursion = investigation.getExcursion();
        String lotIdsJoined = String.join(",", request.getAffectedLotIds());

        // 1. Create append-only disposition decision
        DispositionDecision decision = new DispositionDecision(
                investigation,
                excursion,
                request.getDecision(),
                request.getInvestigatorName(),
                request.getReason(),
                request.getRuleVersion(),
                lotIdsJoined,
                Instant.now()
        );

        DispositionDecision saved = decisionRepository.save(decision);

        // 2. Update affected lots
        LotDisposition targetDisposition;
        LotRiskLevel targetRisk;
        if (request.getDecision() == DecisionType.RELEASE) {
            targetDisposition = LotDisposition.RELEASED;
            targetRisk = LotRiskLevel.LOW;
        } else if (request.getDecision() == DecisionType.HOLD) {
            targetDisposition = LotDisposition.QUARANTINED;
            targetRisk = LotRiskLevel.HIGH;
        } else {
            targetDisposition = LotDisposition.REJECTED;
            targetRisk = LotRiskLevel.CRITICAL;
        }

        for (String lotId : request.getAffectedLotIds()) {
            lotRepository.findById(lotId).ifPresent(lot -> {
                lot.setDispositionStatus(targetDisposition);
                lot.setRiskLevel(targetRisk);
                lotRepository.save(lot);

                auditService.recordAudit("LOT", lot.getId(), "DISPOSITION_DECIDED", actor,
                        null, "disposition: " + targetDisposition.name(),
                        "Decision " + request.getDecision().name() + " recorded by " + request.getInvestigatorName());
            });
        }

        // 3. Mark investigation as COMPLETED and excursion as DISPOSITIONED
        investigation.setStatus(InvestigationStatus.COMPLETED);
        investigation.setClosedAt(Instant.now());
        investigationRepository.save(investigation);

        excursion.setStatus(IncidentStatus.DISPOSITIONED);
        excursionRepository.save(excursion);

        // 4. Append audit record
        auditService.recordAudit("DECISION", String.valueOf(saved.getId()), "RECORDED", actor,
                null, "Decision: " + request.getDecision().name() + " for excursion " + excursion.getId(),
                request.getReason());

        return DecisionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DecisionResponse> getDecisionsForInvestigation(String investigationId) {
        return decisionRepository.findByInvestigationId(investigationId).stream()
                .map(DecisionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
