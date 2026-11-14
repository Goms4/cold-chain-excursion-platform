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
import com.coldchain.platform.excursion.repository.ExcursionAffectedLotRepository;
import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
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
    private final ShipmentRepository shipmentRepository;
    private final ExcursionAffectedLotRepository affectedLotRepository;

    public DecisionService(DispositionDecisionRepository decisionRepository,
                           InvestigationRepository investigationRepository,
                           ExcursionIncidentRepository excursionRepository,
                           LotRepository lotRepository,
                           AuditService auditService,
                           ShipmentRepository shipmentRepository,
                           ExcursionAffectedLotRepository affectedLotRepository) {
        this.decisionRepository = decisionRepository;
        this.investigationRepository = investigationRepository;
        this.excursionRepository = excursionRepository;
        this.lotRepository = lotRepository;
        this.auditService = auditService;
        this.shipmentRepository = shipmentRepository;
        this.affectedLotRepository = affectedLotRepository;
    }

    @Transactional
    public DecisionResponse recordDecision(String investigationId, RecordDecisionRequest request, String actor) {
        Investigation investigation = resolveInvestigation(investigationId);

        if (investigation.getStatus() == InvestigationStatus.COMPLETED) {
            throw new InvalidStateTransitionException("Investigation " + investigationId + " is already COMPLETED. Decisions cannot be overwritten.");
        }

        if (request.getReason() == null || request.getReason().trim().length() < 5) {
            throw new ValidationException("A detailed disposition reason is mandatory.");
        }

        String decidedBy = actor == null || actor.isBlank() ? "Analyst" : actor;

        ExcursionIncident excursion = investigation.getExcursion();
        List<String> selectedLotIds = request.getAffectedLotIds();
        if (selectedLotIds == null || selectedLotIds.isEmpty()) {
            selectedLotIds = affectedLotRepository.findByExcursionId(excursion.getId()).stream()
                    .map(row -> row.getLot().getId()).toList();
        }
        if (selectedLotIds.isEmpty()) throw new ValidationException("No affected lots are linked to this investigation.");
        List<String> internalLotIds = selectedLotIds.stream().map(id -> {
            String resolved = lotRepository.existsById(id) ? id : com.coldchain.platform.common.BusinessIds.internalLot(id);
            Lot lot = lotRepository.findById(resolved)
                    .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
            if (!lot.getShipment().getId().equals(excursion.getShipment().getId())) {
                throw new ValidationException("Lot '" + id + "' is not part of this investigation shipment.");
            }
            return resolved;
        }).toList();
        String lotIdsJoined = String.join(",", internalLotIds);

        // 1. Create append-only disposition decision
        DispositionDecision decision = new DispositionDecision(
                investigation,
                excursion,
                request.getDecision(),
                decidedBy,
                request.getReason(),
                excursion.getRuleVersion(),
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

        for (String lotId : internalLotIds) {
            lotRepository.findById(lotId).ifPresent(lot -> {
                lot.setDispositionStatus(targetDisposition);
                lot.setRiskLevel(targetRisk);
                lotRepository.save(lot);

                auditService.recordAudit("LOT", lot.getId(), "DISPOSITION_DECIDED", actor,
                        null, "disposition: " + targetDisposition.name(),
                        "Decision " + request.getDecision().name() + " recorded by " + decidedBy);
            });
        }

        // 3. Mark investigation as COMPLETED and excursion as DISPOSITIONED
        investigation.setStatus(InvestigationStatus.COMPLETED);
        investigation.setClosedAt(Instant.now());
        investigationRepository.save(investigation);

        excursion.setStatus(IncidentStatus.DISPOSITIONED);
        excursionRepository.save(excursion);
        var shipment = excursion.getShipment();
        shipment.setStatus(switch (request.getDecision()) {
            case RELEASE -> ShipmentStatus.RELEASED;
            case HOLD -> ShipmentStatus.ON_HOLD;
            case REJECT -> ShipmentStatus.REJECTED;
        });
        shipmentRepository.save(shipment);

        // 4. Append audit record
        auditService.recordAudit("DECISION", String.valueOf(saved.getId()), "RECORDED", actor,
                null, "Decision: " + request.getDecision().name() + " for excursion " + excursion.getId(),
                request.getReason());

        return DecisionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<DecisionResponse> getDecisionsForInvestigation(String investigationId) {
        Investigation investigation = resolveInvestigation(investigationId);
        return decisionRepository.findByInvestigationId(investigation.getId()).stream()
                .map(DecisionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private Investigation resolveInvestigation(String id) {
        return investigationRepository.findById(id).orElseGet(() -> investigationRepository.findAll().stream()
                .filter(inv -> com.coldchain.platform.common.BusinessIds.investigation(inv.getId()).equals(id))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Investigation", id)));
    }
}
