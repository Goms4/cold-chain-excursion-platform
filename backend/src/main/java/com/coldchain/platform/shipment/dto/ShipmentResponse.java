package com.coldchain.platform.shipment.dto;

import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import com.coldchain.platform.common.BusinessIds;

import java.time.Instant;
import java.math.BigDecimal;

public class ShipmentResponse {
    private String id;
    private String shipmentId;
    private String trackingNumber;
    private String shipmentReference;
    private String origin;
    private String destination;
    private String storageProfileId;
    private String storageProfileName;
    private String storageType;
    private BigDecimal targetMinC;
    private BigDecimal targetMaxC;
    private BigDecimal minorMaxC;
    private BigDecimal criticalMaxC;
    private String condition = "UNKNOWN";
    private ShipmentStatus status;
    private Integer expectedCadenceMinutes;
    private Integer allowedLatenessMinutes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expectedDeliveryAt;
    private Instant archivedAt;
    private boolean hasOperationalHistory;
    private Instant deletedAt;

    public ShipmentResponse() {}

    public static ShipmentResponse fromEntity(Shipment shipment) {
        ShipmentResponse dto = new ShipmentResponse();
        dto.setId(BusinessIds.shipment(shipment.getId()));
        dto.setShipmentId(BusinessIds.shipment(shipment.getId()));
        dto.setTrackingNumber(BusinessIds.tracking(shipment.getTrackingNumber()));
        dto.setShipmentReference(shipment.getShipmentReference());
        dto.setOrigin(shipment.getOrigin());
        dto.setDestination(shipment.getDestination());
        if (shipment.getStorageProfile() != null) {
            dto.setStorageProfileId(shipment.getStorageProfile().getId());
            dto.setStorageProfileName(shipment.getStorageProfile().getName());
            dto.setStorageType(shipment.getStorageProfile().getName());
            dto.setTargetMinC(shipment.getStorageProfile().getTargetMinC());
            dto.setTargetMaxC(shipment.getStorageProfile().getTargetMaxC());
            dto.setMinorMaxC(shipment.getStorageProfile().getMinorMaxC());
            dto.setCriticalMaxC(shipment.getStorageProfile().getCriticalMaxC());
        }
        dto.setStatus(shipment.getStatus());
        dto.setExpectedCadenceMinutes(shipment.getExpectedCadenceMinutes());
        dto.setAllowedLatenessMinutes(shipment.getAllowedLatenessMinutes());
        dto.setCreatedAt(shipment.getCreatedAt());
        dto.setUpdatedAt(shipment.getUpdatedAt());
        dto.setExpectedDeliveryAt(shipment.getExpectedDeliveryAt());
        dto.setArchivedAt(shipment.getArchivedAt());
        dto.setDeletedAt(shipment.getDeletedAt());
        return dto;
    }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public BigDecimal getTargetMinC() { return targetMinC; }
    public void setTargetMinC(BigDecimal targetMinC) { this.targetMinC = targetMinC; }
    public BigDecimal getTargetMaxC() { return targetMaxC; }
    public void setTargetMaxC(BigDecimal targetMaxC) { this.targetMaxC = targetMaxC; }
    public BigDecimal getMinorMaxC() { return minorMaxC; }
    public void setMinorMaxC(BigDecimal minorMaxC) { this.minorMaxC = minorMaxC; }
    public BigDecimal getCriticalMaxC() { return criticalMaxC; }
    public void setCriticalMaxC(BigDecimal criticalMaxC) { this.criticalMaxC = criticalMaxC; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getShipmentReference() { return shipmentReference; }
    public void setShipmentReference(String shipmentReference) { this.shipmentReference = shipmentReference; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getStorageProfileId() { return storageProfileId; }
    public void setStorageProfileId(String storageProfileId) { this.storageProfileId = storageProfileId; }

    public String getStorageProfileName() { return storageProfileName; }
    public void setStorageProfileName(String storageProfileName) { this.storageProfileName = storageProfileName; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public Integer getExpectedCadenceMinutes() { return expectedCadenceMinutes; }
    public void setExpectedCadenceMinutes(Integer expectedCadenceMinutes) { this.expectedCadenceMinutes = expectedCadenceMinutes; }

    public Integer getAllowedLatenessMinutes() { return allowedLatenessMinutes; }
    public void setAllowedLatenessMinutes(Integer allowedLatenessMinutes) { this.allowedLatenessMinutes = allowedLatenessMinutes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getExpectedDeliveryAt() { return expectedDeliveryAt; }
    public void setExpectedDeliveryAt(Instant expectedDeliveryAt) { this.expectedDeliveryAt = expectedDeliveryAt; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public boolean isHasOperationalHistory() { return hasOperationalHistory; }
    public void setHasOperationalHistory(boolean hasOperationalHistory) { this.hasOperationalHistory = hasOperationalHistory; }
}
