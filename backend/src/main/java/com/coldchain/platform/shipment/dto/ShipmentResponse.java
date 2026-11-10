package com.coldchain.platform.shipment.dto;

import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;

import java.time.Instant;

public class ShipmentResponse {
    private String id;
    private String trackingNumber;
    private String origin;
    private String destination;
    private String storageProfileId;
    private String storageProfileName;
    private ShipmentStatus status;
    private Integer expectedCadenceMinutes;
    private Integer allowedLatenessMinutes;
    private Instant createdAt;
    private Instant updatedAt;

    public ShipmentResponse() {}

    public static ShipmentResponse fromEntity(Shipment shipment) {
        ShipmentResponse dto = new ShipmentResponse();
        dto.setId(shipment.getId());
        dto.setTrackingNumber(shipment.getTrackingNumber());
        dto.setOrigin(shipment.getOrigin());
        dto.setDestination(shipment.getDestination());
        if (shipment.getStorageProfile() != null) {
            dto.setStorageProfileId(shipment.getStorageProfile().getId());
            dto.setStorageProfileName(shipment.getStorageProfile().getName());
        }
        dto.setStatus(shipment.getStatus());
        dto.setExpectedCadenceMinutes(shipment.getExpectedCadenceMinutes());
        dto.setAllowedLatenessMinutes(shipment.getAllowedLatenessMinutes());
        dto.setCreatedAt(shipment.getCreatedAt());
        dto.setUpdatedAt(shipment.getUpdatedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

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
}
