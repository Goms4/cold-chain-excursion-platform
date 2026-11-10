package com.coldchain.platform.lot.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.excursion.model.ExcursionSeverity;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.lot.repository.LotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LotTracingService {

    private final LotRepository lotRepository;
    private final AuditService auditService;

    public LotTracingService(LotRepository lotRepository, AuditService auditService) {
        this.lotRepository = lotRepository;
        this.auditService = auditService;
    }

    @Transactional
    public List<Lot> traceAndFlagLotsForExcursion(String shipmentId, String excursionId, ExcursionSeverity severity) {
        List<Lot> lots = lotRepository.findByShipmentId(shipmentId);
        LotRiskLevel newRisk = (severity == ExcursionSeverity.CRITICAL) ? LotRiskLevel.CRITICAL : LotRiskLevel.HIGH;

        for (Lot lot : lots) {
            LotRiskLevel oldRisk = lot.getRiskLevel();
            lot.setRiskLevel(newRisk);
            lotRepository.save(lot);

            auditService.recordAudit("LOT", lot.getId(), "RISK_ELEVATED", "EXCURSION_ENGINE",
                    "risk: " + oldRisk.name(), "risk: " + newRisk.name(),
                    "Linked to excursion " + excursionId + " with severity " + severity.name());
        }

        return lots;
    }

    @Transactional(readOnly = true)
    public List<Lot> findLotsByShipment(String shipmentId) {
        return lotRepository.findByShipmentId(shipmentId);
    }
}
