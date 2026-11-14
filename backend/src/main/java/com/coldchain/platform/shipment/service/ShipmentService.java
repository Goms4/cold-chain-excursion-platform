package com.coldchain.platform.shipment.service;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.common.BusinessIdGenerator;
import com.coldchain.platform.common.exception.ResourceNotFoundException;
import com.coldchain.platform.common.exception.ValidationException;
import com.coldchain.platform.excursion.model.StorageProfile;
import com.coldchain.platform.excursion.repository.StorageProfileRepository;
import com.coldchain.platform.sensor.service.SensorService;
import com.coldchain.platform.shipment.dto.CreateShipmentRequest;
import com.coldchain.platform.shipment.dto.ShipmentResponse;
import com.coldchain.platform.shipment.dto.UpdateShipmentRequest;
import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ShipmentService {
    private final ShipmentRepository shipmentRepository;
    private final StorageProfileRepository storageProfileRepository;
    private final AuditService auditService;
    private final ShipmentConditionService conditionService;
    private final ShipmentLifecycleService lifecycleService;
    private final BusinessIdGenerator idGenerator;
    private final SensorService sensorService;

    public ShipmentService(ShipmentRepository shipmentRepository, StorageProfileRepository storageProfileRepository,
                           AuditService auditService, ShipmentConditionService conditionService,
                           ShipmentLifecycleService lifecycleService, BusinessIdGenerator idGenerator,
                           SensorService sensorService) {
        this.shipmentRepository = shipmentRepository;
        this.storageProfileRepository = storageProfileRepository;
        this.auditService = auditService;
        this.conditionService = conditionService;
        this.lifecycleService = lifecycleService;
        this.idGenerator = idGenerator;
        this.sensorService = sensorService;
    }

    @Transactional
    public ShipmentResponse createShipment(CreateShipmentRequest request, String actor) {
        long businessNumber = idGenerator.nextShipmentNumber();
        String id = request.getId() == null || request.getId().isBlank() ? idGenerator.shipmentId(businessNumber) : request.getId().trim();
        String tracking = request.getTrackingNumber() == null || request.getTrackingNumber().isBlank()
                ? idGenerator.trackingNumber(businessNumber) : request.getTrackingNumber().trim();
        if (shipmentRepository.existsById(id)) throw new ValidationException("Shipment with ID '" + id + "' already exists.");
        if (shipmentRepository.existsByTrackingNumber(tracking)) throw new ValidationException("That tracking number is already in use.");

        StorageProfile profile = storageProfileRepository.findById(request.getStorageProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("StorageProfile", request.getStorageProfileId()));
        Shipment shipment = new Shipment(id, tracking, request.getOrigin(), request.getDestination(), profile,
                request.getStatus() == null ? ShipmentStatus.PLANNED : request.getStatus(),
                request.getExpectedCadenceMinutes(), request.getAllowedLatenessMinutes());
        shipment.setShipmentReference(request.getShipmentReference() == null || request.getShipmentReference().isBlank()
                ? id : request.getShipmentReference().trim());
        shipment.setExpectedDeliveryAt(request.getExpectedDeliveryAt());
        Shipment saved = shipmentRepository.save(shipment);
        sensorService.provisionForShipment(saved, actor);
        auditService.recordAudit("SHIPMENT", saved.getId(), "CREATED", actor, null,
                "Shipment " + saved.getId() + " registered from " + saved.getOrigin() + " to " + saved.getDestination(), "Shipment registration");
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Shipment getShipmentEntity(String id) {
        String resolvedId = resolveId(id);
        Shipment shipment = shipmentRepository.findById(resolvedId).orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (shipment.getArchivedAt() != null) throw new ResourceNotFoundException("Active Shipment", id);
        if (shipment.isDeleted()) throw new ResourceNotFoundException("Active Shipment", id);
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            throw new ValidationException("This shipment has been delivered and can no longer accept readings.");
        }
        return shipment;
    }

    @Transactional(readOnly = true)
    public ShipmentResponse getShipment(String id) {
        String resolvedId = resolveId(id);
        Shipment shipment = shipmentRepository.findById(resolvedId).orElseThrow(() -> new ResourceNotFoundException("Shipment", id));
        if (shipment.isDeleted()) throw new ResourceNotFoundException("Shipment", id);
        return response(shipment);
    }

    @Transactional(readOnly = true)
    public List<ShipmentResponse> getAllShipments(boolean includeArchived) {
        return (includeArchived ? shipmentRepository.findByArchivedAtIsNotNullAndDeletedAtIsNull()
                : shipmentRepository.findByArchivedAtIsNullAndDeletedAtIsNull())
                .stream().map(this::response).toList();
    }

    @Transactional
    public ShipmentResponse updateShipment(String id, UpdateShipmentRequest request, String actor) {
        Shipment shipment = getShipmentEntity(id);
        String before = "reference=" + shipment.getShipmentReference() + "; origin=" + shipment.getOrigin()
                + "; destination=" + shipment.getDestination() + "; storageProfile=" + shipment.getStorageProfile().getId();
        if (request.getShipmentReference() != null && !request.getShipmentReference().isBlank()) shipment.setShipmentReference(request.getShipmentReference().trim());
        if (request.getOrigin() != null && !request.getOrigin().isBlank()) shipment.setOrigin(request.getOrigin().trim());
        if (request.getDestination() != null && !request.getDestination().isBlank()) shipment.setDestination(request.getDestination().trim());
        if (request.getStorageProfileId() != null && !request.getStorageProfileId().isBlank()) {
            shipment.setStorageProfile(storageProfileRepository.findById(request.getStorageProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("StorageProfile", request.getStorageProfileId())));
        }
        if (request.getExpectedDeliveryAt() != null) shipment.setExpectedDeliveryAt(request.getExpectedDeliveryAt());
        Shipment saved = shipmentRepository.save(shipment);
        String after = "reference=" + saved.getShipmentReference() + "; origin=" + saved.getOrigin()
                + "; destination=" + saved.getDestination() + "; storageProfile=" + saved.getStorageProfile().getId();
        auditService.recordAudit("SHIPMENT", saved.getId(), "UPDATED", actor, before, after, "Shipment details corrected");
        return response(saved);
    }

    @Transactional
    public ShipmentResponse updateStatus(String id, ShipmentStatus newStatus, String actor) {
        Shipment shipment = getShipmentEntity(id);
        String oldStatus = shipment.getStatus().name();
        shipment.setStatus(newStatus);
        Shipment saved = shipmentRepository.save(shipment);
        auditService.recordAudit("SHIPMENT", saved.getId(), "UPDATED", actor,
                "status: " + oldStatus, "status: " + newStatus.name(), "Shipment status update");
        return response(saved);
    }

    @Transactional
    public void deleteShipment(String id, String actor, String reason) { lifecycleService.softDelete(id, actor, reason); }

    @Transactional
    public ShipmentResponse archiveShipment(String id, String actor) {
        return response(lifecycleService.archive(id, actor));
    }

    public String resolveId(String id) {
        if (shipmentRepository.existsById(id)) return id;
        String internalId = com.coldchain.platform.common.BusinessIds.internalShipment(id);
        return shipmentRepository.existsById(internalId) ? internalId : id;
    }

    private ShipmentResponse response(Shipment shipment) {
        ShipmentResponse dto = ShipmentResponse.fromEntity(shipment);
        dto.setCondition(conditionService.condition(shipment));
        dto.setHasOperationalHistory(lifecycleService.hasHistory(shipment));
        return dto;
    }
}
