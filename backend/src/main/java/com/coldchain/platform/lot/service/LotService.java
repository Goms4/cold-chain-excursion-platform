package com.coldchain.platform.lot.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.BusinessIdGenerator;
import com.coldchain.platform.common.OperationalHistoryService;
import com.coldchain.platform.common.exception.InvalidStateTransitionException;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.lot.dto.CreateLotRequest;
import com.coldchain.platform.lot.dto.LotResponse;
import com.coldchain.platform.lot.dto.UpdateLotRequest;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuditService auditService;
    private final BusinessIdGenerator idGenerator;
    private final OperationalHistoryService historyService;

    public LotService(LotRepository lotRepository,
                      ShipmentRepository shipmentRepository,
                      AuditService auditService, BusinessIdGenerator idGenerator,
                      OperationalHistoryService historyService) {
        this.lotRepository = lotRepository;
        this.shipmentRepository = shipmentRepository;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
        this.historyService = historyService;
    }

    @Transactional
    public LotResponse createLot(CreateLotRequest request, String actor) {
        Shipment shipment = shipmentRepository.findById(resolveShipmentId(request.getShipmentId()))
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", request.getShipmentId()));
        if (shipment.getArchivedAt() != null) throw new ValidationException("Cannot add a lot to an archived shipment.");
        if (shipment.isDeleted()) throw new ResourceNotFoundException("Active Shipment", request.getShipmentId());
        String id = request.getId() == null || request.getId().isBlank() ? idGenerator.nextLotId() : request.getId().trim();
        if (lotRepository.existsById(id)) throw new ValidationException("Lot with ID '" + id + "' already exists.");

        Lot lot = new Lot(id, shipment, request.getProductName(), request.getBatchNumber(), request.getQuantity());
        lot.setExpiryDate(request.getExpiryDate());
        Lot saved = lotRepository.save(lot);

        auditService.recordAudit("LOT", saved.getId(), "CREATED", actor, null,
                "Registered lot " + saved.getId() + " on shipment " + shipment.getId(), "Lot registration");

        return response(saved);
    }

    @Transactional(readOnly = true)
    public List<LotResponse> getLotsForShipment(String shipmentId, boolean includeArchived) {
        String resolvedShipmentId = shipmentRepository.existsById(shipmentId) ? shipmentId : com.coldchain.platform.common.BusinessIds.internalShipment(shipmentId);
        Shipment shipment = shipmentRepository.findById(resolvedShipmentId).orElseThrow(() -> new ResourceNotFoundException("Shipment", shipmentId));
        if (shipment.isDeleted()) throw new ResourceNotFoundException("Shipment", shipmentId);
        return (includeArchived ? lotRepository.findByShipmentIdAndDeletedAtIsNull(resolvedShipmentId)
                : lotRepository.findByShipmentIdAndArchivedAtIsNullAndDeletedAtIsNull(resolvedShipmentId)).stream()
                .map(this::response)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Lot getLotEntity(String id) {
        String resolvedId = lotRepository.existsById(id) ? id : com.coldchain.platform.common.BusinessIds.internalLot(id);
        Lot lot = lotRepository.findById(resolvedId).orElseThrow(() -> new ResourceNotFoundException("Lot", id));
        if (lot.getArchivedAt() != null) throw new ResourceNotFoundException("Active Lot", id);
        if (lot.isDeleted()) throw new ResourceNotFoundException("Active Lot", id);
        return lot;
    }

    @Transactional
    public void updateLotDisposition(String lotId, LotDisposition disposition, String actor, String reason) {
        Lot lot = getLotEntity(lotId);
        String oldDisp = lot.getDispositionStatus().name();
        lot.setDispositionStatus(disposition);
        lotRepository.save(lot);

        auditService.recordAudit("LOT", lot.getId(), "DISPOSITION_UPDATED", actor,
                "disposition: " + oldDisp, "disposition: " + disposition.name(), reason);
    }

    @Transactional
    public void deleteLot(String lotId, String actor, String reason) {
        Lot lot = findLot(lotId);
        if (lot.isDeleted()) throw new ResourceNotFoundException("Active Lot", lotId);
        lot.setDeletedAt(Instant.now());
        lot.setDeletedBy(actor == null || actor.isBlank() ? "System" : actor);
        lot.setDeleteReason(reason.trim());
        lotRepository.save(lot);
        auditService.recordAudit("LOT", lot.getId(), "DELETED", lot.getDeletedBy(), "active",
                "Lot removed from active operations", reason.trim());
    }

    @Transactional
    public LotResponse updateLot(String lotId, UpdateLotRequest request, String actor) {
        Lot lot = getLotEntity(lotId);
        String before = "product=" + lot.getProductName() + "; lotNumber=" + lot.getBatchNumber() + "; quantity=" + lot.getQuantity();
        if (request.getProductName() != null && !request.getProductName().isBlank()) lot.setProductName(request.getProductName().trim());
        if (request.getBatchNumber() != null && !request.getBatchNumber().isBlank()) lot.setBatchNumber(request.getBatchNumber().trim());
        if (request.getQuantity() != null) lot.setQuantity(request.getQuantity());
        if (request.getExpiryDate() != null) lot.setExpiryDate(request.getExpiryDate());
        if (request.getUnloadedAt() != null) lot.setUnloadedAt(request.getUnloadedAt());
        Lot saved = lotRepository.save(lot);
        String after = "product=" + saved.getProductName() + "; lotNumber=" + saved.getBatchNumber() + "; quantity=" + saved.getQuantity();
        auditService.recordAudit("LOT", saved.getId(), "UPDATED", actor, before, after, "Lot details corrected");
        return response(saved);
    }

    @Transactional
    public LotResponse archiveLot(String lotId, String actor) {
        Lot lot = findLot(lotId);
        if (lot.isDeleted()) throw new ResourceNotFoundException("Active Lot", lotId);
        if (lot.getArchivedAt() == null) {
            lot.setArchivedAt(Instant.now());
            lotRepository.save(lot);
            auditService.recordAudit("LOT", lot.getId(), "ARCHIVED", actor, null, "Archived lot " + lot.getId(), "Lot retained with operational history");
        }
        return response(lot);
    }

    private Lot findLot(String id) {
        String resolvedId = lotRepository.existsById(id) ? id : com.coldchain.platform.common.BusinessIds.internalLot(id);
        return lotRepository.findById(resolvedId).orElseThrow(() -> new ResourceNotFoundException("Lot", id));
    }

    private String resolveShipmentId(String id) {
        return shipmentRepository.existsById(id) ? id : com.coldchain.platform.common.BusinessIds.internalShipment(id);
    }

    private LotResponse response(Lot lot) {
        LotResponse dto = LotResponse.fromEntity(lot);
        dto.setHasOperationalHistory(historyService.lotHasHistory(lot));
        return dto;
    }
}
