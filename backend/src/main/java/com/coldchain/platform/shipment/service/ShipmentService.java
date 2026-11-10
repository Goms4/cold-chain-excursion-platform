package com.coldchain.platform.shipment.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.excursion.repository.StorageProfileRepository;
import com.coldchain.platform.shipment.dto.CreateShipmentRequest;
import com.coldchain.platform.shipment.dto.ShipmentResponse;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final StorageProfileRepository storageProfileRepository;
    private final AuditService auditService;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           StorageProfileRepository storageProfileRepository,
                           AuditService auditService) {
        this.shipmentRepository = shipmentRepository;
        this.storageProfileRepository = storageProfileRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request, String actor) {
        if (shipmentRepository.existsById(request.getId())) {
            throw new ValidationException("Shipment with ID '" + request.getId() + "' already exists.");
        }

        StorageProfile profile = storageProfileRepository.findById(request.getStorageProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("StorageProfile", request.getStorageProfileId()));

        Shipment shipment = new Shipment(
                request.getId(),
                request.getTrackingNumber(),
                request.getOrigin(),
                request.getDestination(),
                profile,
                request.getStatus() != null ? request.getStatus() : ShipmentStatus.PLANNED,
                request.getExpectedCadenceMinutes(),
                request.getAllowedLatenessMinutes()
        );

        Shipment saved = shipmentRepository.save(shipment);
        auditService.recordAudit("SHIPMENT", saved.getId(), "CREATED", actor, null,
                "Created shipment " + saved.getId() + " (" + saved.getTrackingNumber() + ")", "Shipment registration");

        return ShipmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentEntity(String id) {
        return shipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(String id) {
        return ShipmentResponse.fromEntity(getShipmentEntity(id));
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments() {
        return shipmentRepository.findAll().stream()
                .map(ShipmentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShipmentResponse updateStatus(String id, ShipmentStatus newStatus, String actor) {
        Shipment shipment = getShipmentEntity(id);
        String oldStatus = shipment.getStatus().name();
        shipment.setStatus(newStatus);
        Shipment saved = shipmentRepository.save(shipment);

        auditService.recordAudit("SHIPMENT", saved.getId(), "UPDATED", actor,
                "status: " + oldStatus, "status: " + newStatus.name(), "Shipment status update");

        return ShipmentResponse.fromEntity(saved);
    }
}
