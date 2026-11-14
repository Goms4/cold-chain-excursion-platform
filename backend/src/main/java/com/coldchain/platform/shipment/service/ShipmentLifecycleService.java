package com.coldchain.platform.shipment.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.OperationalHistoryService;
import com.coldchain.platform.common.exception.InvalidStateTransitionException;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.decision.repository.DispositionDecisionRepository;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShipmentLifecycleService {
    private final ShipmentRepository shipments;
    private final LotRepository lots;
    private final OperationalHistoryService history;
    private final AuditService audit;
    private final InvestigationRepository investigations;
    private final DispositionDecisionRepository decisions;

    public ShipmentLifecycleService(ShipmentRepository shipments, LotRepository lots,
                                    OperationalHistoryService history, AuditService audit,
                                    InvestigationRepository investigations, DispositionDecisionRepository decisions) {
        this.shipments = shipments;
        this.lots = lots;
        this.history = history;
        this.audit = audit;
        this.investigations = investigations;
        this.decisions = decisions;
    }

    public boolean hasHistory(Shipment shipment) {
        return history.shipmentHasHistory(shipment) || lots.findByShipmentId(shipment.getId()).stream().anyMatch(history::lotHasHistory);
    }

    @Transactional
    public void softDelete(String id, String actor, String reason) {
        Shipment shipment = shipments.findById(resolve(id)).orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (shipment.isDeleted()) throw new ResourceNotFoundException("Active Shipment", id);
        boolean openInvestigation = investigations.findByExcursion_Shipment_Id(shipment.getId()).stream()
                .anyMatch(investigation -> investigation.getStatus() != InvestigationStatus.COMPLETED);
        if (openInvestigation) throw new InvalidStateTransitionException("This shipment has an open investigation and cannot be removed.");
        if (decisions.existsByExcursion_Shipment_Id(shipment.getId())) {
            throw new InvalidStateTransitionException("This shipment has a recorded quality decision and cannot be removed.");
        }
        String prior = "active";
        shipment.setDeletedAt(Instant.now());
        shipment.setDeletedBy(actor == null || actor.isBlank() ? "System" : actor);
        shipment.setDeleteReason(reason.trim());
        shipments.save(shipment);
        audit.recordAudit("SHIPMENT", shipment.getId(), "DELETED", shipment.getDeletedBy(), prior,
                "Shipment removed from active operations", reason.trim());
    }

    @Transactional
    public Shipment archive(String id, String actor) {
        Shipment shipment = shipments.findById(resolve(id)).orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (shipment.getArchivedAt() == null) {
            shipment.setArchivedAt(Instant.now());
            shipments.save(shipment);
            audit.recordAudit("SHIPMENT", shipment.getId(), "ARCHIVED", actor, null,
                    "Archived shipment " + shipment.getId(), "Shipment retained with operational history");
        }
        return shipment;
    }

    private String resolve(String id) { return shipments.existsById(id) ? id : com.coldchain.platform.common.BusinessIds.internalShipment(id); }
}
