package com.coldchain.platform.lot.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.lot.dto.CreateLotRequest;
import com.coldchain.platform.lot.dto.LotResponse;
import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LotService {

    private final LotRepository lotRepository;
    private final ShipmentRepository shipmentRepository;
    private final AuditService auditService;

    public LotService(LotRepository lotRepository,
                      ShipmentRepository shipmentRepository,
                      AuditService auditService) {
        this.lotRepository = lotRepository;
        this.shipmentRepository = shipmentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public LotResponse createLot(CreateLotRequest request, String actor) {
        if (lotRepository.existsById(request.getId())) {
            throw new ValidationException("Lot with ID '" + request.getId() + "' already exists.");
        }

        Shipment shipment = shipmentRepository.findById(request.getShipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", request.getShipmentId()));

        Lot lot = new Lot(request.getId(), shipment, request.getProductName(), request.getBatchNumber(), request.getQuantity());
        Lot saved = lotRepository.save(lot);

        auditService.recordAudit("LOT", saved.getId(), "CREATED", actor, null,
                "Registered lot " + saved.getId() + " on shipment " + shipment.getId(), "Lot registration");

        return LotResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<LotResponse> getLotsForShipment(String shipmentId) {
        return lotRepository.findByShipmentId(shipmentId).stream()
                .map(LotResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Lot getLotEntity(String id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot", id));
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
}
