package com.coldchain.platform.investigation.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.BusinessIdGenerator;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.investigation.dto.InvestigationResponse;
import com.coldchain.platform.investigation.dto.ManualInspectionRequest;
import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.model.ManualInspectionRecord;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.investigation.repository.ManualInspectionRepository;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.excursion.repository.ExcursionAffectedLotRepository;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.common.BusinessIds;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class InvestigationService {

    private final InvestigationRepository investigationRepository;
    private final ManualInspectionRepository manualInspectionRepository;
    private final AuditService auditService;
    private final ExcursionIncidentRepository excursionRepository;
    private final ExcursionAffectedLotRepository affectedLotRepository;
    private final LotRepository lotRepository;
    private final ShipmentRepository shipmentRepository;
    private final BusinessIdGenerator idGenerator;

    public InvestigationService(InvestigationRepository investigationRepository,
                                ManualInspectionRepository manualInspectionRepository,
                                AuditService auditService,
                                ExcursionIncidentRepository excursionRepository,
                                ExcursionAffectedLotRepository affectedLotRepository,
                                LotRepository lotRepository,
                                ShipmentRepository shipmentRepository,
                                BusinessIdGenerator idGenerator) {
        this.investigationRepository = investigationRepository;
        this.manualInspectionRepository = manualInspectionRepository;
        this.auditService = auditService;
        this.excursionRepository = excursionRepository;
        this.affectedLotRepository = affectedLotRepository;
        this.lotRepository = lotRepository;
        this.shipmentRepository = shipmentRepository;
        this.idGenerator = idGenerator;
    }

    @Transactional
    public Investigation openInvestigation(ExcursionIncident excursion, String assignedInvestigator) {
        return openInvestigation(excursion, assignedInvestigator, "System");
    }

    public Investigation openInvestigation(ExcursionIncident excursion, String assignedInvestigator, String actor) {
        var existing = investigationRepository.findByExcursionId(excursion.getId());
        if (existing.isPresent()) return existing.get();
        String invId = idGenerator.nextInvestigationId();

        Instant slaDeadline = Instant.now().plus(Duration.ofHours(24));
        Investigation investigation = new Investigation(invId, excursion, assignedInvestigator, slaDeadline);
        Investigation saved = investigationRepository.save(investigation);

        auditService.recordAudit("INVESTIGATION", saved.getId(), "OPENED", actor, null,
                "Opened investigation for excursion " + excursion.getId() + ", SLA due " + slaDeadline,
                "Excursion incident triggered investigation");

        return saved;
    }

    @Transactional
    public InvestigationResponse assignInvestigator(String id, String investigator, String actor) {
        if (investigator == null || investigator.isBlank()) throw new ValidationException("Investigator name is required.");
        Investigation investigation = getInvestigationEntity(id);
        if (investigation.getStatus() == InvestigationStatus.COMPLETED) throw new ValidationException("Completed investigations cannot be reassigned.");
        String prior = investigation.getAssignedInvestigator();
        investigation.setAssignedInvestigator(investigator.trim());
        if (investigation.getStatus() == InvestigationStatus.OPEN) investigation.setStatus(InvestigationStatus.UNDER_REVIEW);
        Investigation saved = investigationRepository.save(investigation);
        auditService.recordAudit("INVESTIGATION", saved.getId(), "ASSIGNED", actor, "investigator: " + prior,
                "investigator: " + saved.getAssignedInvestigator(), "Investigator assignment");
        return InvestigationResponse.fromEntity(saved, manualInspectionRepository.findByInvestigationId(saved.getId()));
    }

    @Transactional
    public InvestigationResponse registerForExcursion(String shipmentId, String lotId, String excursionId,
                                                       String investigator, String actor) {
        String resolvedShipmentId = shipmentRepository.existsById(shipmentId) ? shipmentId : BusinessIds.internalShipment(shipmentId);
        var shipment = shipmentRepository.findById(resolvedShipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        String resolvedLotId = lotRepository.existsById(lotId) ? lotId : BusinessIds.internalLot(lotId);
        var lot = lotRepository.findById(resolvedLotId).orElseThrow(() -> new ResourceNotFoundException("Lot", lotId));
        if (!lot.getShipment().getId().equals(shipment.getId())) throw new ValidationException("The lot does not belong to the selected shipment.");
        var excursion = excursionRepository.findById(excursionId)
                .orElseThrow(() -> new ResourceNotFoundException("Excursion", excursionId));
        if (!excursion.getShipment().getId().equals(shipment.getId())) throw new ValidationException("The excursion does not belong to the selected shipment.");
        boolean affected = affectedLotRepository.findByExcursionId(excursionId).stream()
                .anyMatch(row -> row.getLot().getId().equals(lot.getId()));
        if (!affected) throw new ValidationException("The selected lot is not affected by this excursion.");
        Investigation investigation = openInvestigation(excursion,
                investigator == null || investigator.isBlank() ? actor : investigator, actor);
        if (investigator != null && !investigator.isBlank()
                && !investigator.equals(investigation.getAssignedInvestigator())) {
            String priorAssignee = investigation.getAssignedInvestigator();
            investigation.setAssignedInvestigator(investigator);
            investigation = investigationRepository.save(investigation);
            auditService.recordAudit("INVESTIGATION", investigation.getId(), "ASSIGNED_BY_OPERATIONS", actor,
                    "investigator: " + priorAssignee, "investigator: " + investigator,
                    "Operations registered the existing excursion investigation");
        }
        List<ManualInspectionRecord> inspections = manualInspectionRepository.findByInvestigationId(investigation.getId());
        return InvestigationResponse.fromEntity(investigation, inspections);
    }

    @Transactional
    public ManualInspectionRecord addManualInspection(String investigationId, ManualInspectionRequest request, String actor) {
        Investigation investigation = investigationRepository.findById(investigationId)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation", investigationId));

        StorageProfile profile = investigation.getExcursion().getShipment().getStorageProfile();
        BigDecimal obsTemp = request.getObservedTemperatureC();

        // Check contradiction: does manual inspection fall within compliant range while sensor triggered an excursion?
        boolean manualWithinCompliant = obsTemp.compareTo(profile.getTargetMinC()) >= 0 && obsTemp.compareTo(profile.getTargetMaxC()) <= 0;
        boolean sensorWasExcursion = true; // Excursion incident exists

        boolean contradicts = manualWithinCompliant && sensorWasExcursion;

        ManualInspectionRecord record = new ManualInspectionRecord(
                investigation,
                investigation.getExcursion().getShipment(),
                request.getInspectorName(),
                request.getInspectionTime(),
                request.getObservedTemperatureC(),
                request.getPackageIntegrityStatus(),
                request.getVisualCondition(),
                request.getNotes(),
                contradicts
        );

        ManualInspectionRecord saved = manualInspectionRepository.save(record);

        if (contradicts) {
            investigation.setStatus(InvestigationStatus.CONTRADICTORY_EVIDENCE);
            investigationRepository.save(investigation);

            auditService.recordAudit("INVESTIGATION", investigation.getId(), "CONTRADICTION_FLAGGED", actor,
                    "status: OPEN", "status: CONTRADICTORY_EVIDENCE",
                    "Manual inspection observed " + obsTemp + "°C (compliant) contradicting sensor excursion");
        } else {
            auditService.recordAudit("INVESTIGATION", investigation.getId(), "INSPECTION_RECORDED", actor,
                    null, "Recorded manual inspection by " + request.getInspectorName(), "Manual inspection evidence added");
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public InvestigationResponse getInvestigationById(String id) {
        Investigation inv = investigationRepository.findById(id).orElseGet(() -> investigationRepository.findAll().stream()
                .filter(x -> BusinessIds.investigation(x.getId()).equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Investigation", id)));
        List<ManualInspectionRecord> inspections = manualInspectionRepository.findByInvestigationId(inv.getId());
        return InvestigationResponse.fromEntity(inv, inspections);
    }

    @Transactional(readOnly = true)
    public InvestigationResponse getInvestigationByExcursionId(String excursionId) {
        Investigation inv = investigationRepository.findByExcursionId(excursionId)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation for excursion", excursionId));
        List<ManualInspectionRecord> inspections = manualInspectionRepository.findByInvestigationId(inv.getId());
        return InvestigationResponse.fromEntity(inv, inspections);
    }

    @Transactional(readOnly = true)
    public Investigation getInvestigationEntity(String id) {
        return investigationRepository.findById(id).orElseGet(() -> investigationRepository.findAll().stream()
                .filter(x -> BusinessIds.investigation(x.getId()).equals(id)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Investigation", id)));
    }
}
