package com.coldchain.platform.investigation.service;

import com.coldchain.platform.audit.service.AuditService;
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

    public InvestigationService(InvestigationRepository investigationRepository,
                                ManualInspectionRepository manualInspectionRepository,
                                AuditService auditService) {
        this.investigationRepository = investigationRepository;
        this.manualInspectionRepository = manualInspectionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Investigation openInvestigation(ExcursionIncident excursion, String assignedInvestigator) {
        String invId = "INV-" + excursion.getId();
        if (investigationRepository.existsById(invId)) {
            return investigationRepository.findById(invId).get();
        }

        Instant slaDeadline = Instant.now().plus(Duration.ofHours(24));
        Investigation investigation = new Investigation(invId, excursion, assignedInvestigator, slaDeadline);
        Investigation saved = investigationRepository.save(investigation);

        auditService.recordAudit("INVESTIGATION", saved.getId(), "OPENED", "EXCURSION_ENGINE", null,
                "Opened investigation for excursion " + excursion.getId() + ", SLA due " + slaDeadline,
                "Excursion incident triggered investigation");

        return saved;
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
        Investigation inv = investigationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation", id));
        List<ManualInspectionRecord> inspections = manualInspectionRepository.findByInvestigationId(id);
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
        return investigationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investigation", id));
    }
}
