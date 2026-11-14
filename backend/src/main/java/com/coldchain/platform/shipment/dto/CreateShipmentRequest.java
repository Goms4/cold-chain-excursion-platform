package com.coldchain.platform.shipment.dto;

import com.coldchain.platform.shipment.model.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateShipmentRequest {

    private String id;

    @NotBlank(message = "tracking number is required")
    private String trackingNumber;

    @NotBlank(message = "shipment reference is required")
    private String shipmentReference;

    @NotBlank(message = "origin is required")
    private String origin;

    @NotBlank(message = "destination is required")
    private String destination;

    @NotBlank(message = "storage_profile_id is required")
    private String storageProfileId;

    private ShipmentStatus status = ShipmentStatus.PLANNED;
    private Integer expectedCadenceMinutes = 5;
    private Integer allowedLatenessMinutes = 120;
    private java.time.Instant expectedDeliveryAt;

    public CreateShipmentRequest() {}

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

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public Integer getExpectedCadenceMinutes() { return expectedCadenceMinutes; }
    public void setExpectedCadenceMinutes(Integer expectedCadenceMinutes) { this.expectedCadenceMinutes = expectedCadenceMinutes; }

    public Integer getAllowedLatenessMinutes() { return allowedLatenessMinutes; }
    public void setAllowedLatenessMinutes(Integer allowedLatenessMinutes) { this.allowedLatenessMinutes = allowedLatenessMinutes; }

    public java.time.Instant getExpectedDeliveryAt() { return expectedDeliveryAt; }
    public void setExpectedDeliveryAt(java.time.Instant expectedDeliveryAt) { this.expectedDeliveryAt = expectedDeliveryAt; }
}
