package com.coldchain.platform.shipment.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;

public class UpdateShipmentRequest {
    @Size(max = 128, message = "Shipment reference must be 128 characters or fewer.")
    private String shipmentReference;
    @Size(max = 128, message = "Origin must be 128 characters or fewer.")
    private String origin;
    @Size(max = 128, message = "Destination must be 128 characters or fewer.")
    private String destination;
    private String storageProfileId;
    private Instant expectedDeliveryAt;

    public String getShipmentReference() { return shipmentReference; }
    public void setShipmentReference(String shipmentReference) { this.shipmentReference = shipmentReference; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getStorageProfileId() { return storageProfileId; }
    public void setStorageProfileId(String storageProfileId) { this.storageProfileId = storageProfileId; }
    public Instant getExpectedDeliveryAt() { return expectedDeliveryAt; }
    public void setExpectedDeliveryAt(Instant expectedDeliveryAt) { this.expectedDeliveryAt = expectedDeliveryAt; }
}
