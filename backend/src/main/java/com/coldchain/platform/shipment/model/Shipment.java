package com.coldchain.platform.shipment.model;

import com.coldchain.platform.excursion.model.StorageProfile;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "tracking_number", nullable = false, unique = true, length = 128)
    private String trackingNumber;

    @Column(name = "origin", nullable = false, length = 128)
    private String origin;

    @Column(name = "destination", nullable = false, length = 128)
    private String destination;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_profile_id", nullable = false)
    private StorageProfile storageProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ShipmentStatus status = ShipmentStatus.PLANNED;

    @Column(name = "expected_cadence_minutes", nullable = false)
    private Integer expectedCadenceMinutes = 5;

    @Column(name = "allowed_lateness_minutes", nullable = false)
    private Integer allowedLatenessMinutes = 120;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Shipment() {}

    public Shipment(String id, String trackingNumber, String origin, String destination,
                    StorageProfile storageProfile, ShipmentStatus status,
                    Integer expectedCadenceMinutes, Integer allowedLatenessMinutes) {
        this.id = id;
        this.trackingNumber = trackingNumber;
        this.origin = origin;
        this.destination = destination;
        this.storageProfile = storageProfile;
        this.status = status != null ? status : ShipmentStatus.PLANNED;
        this.expectedCadenceMinutes = expectedCadenceMinutes != null ? expectedCadenceMinutes : 5;
        this.allowedLatenessMinutes = allowedLatenessMinutes != null ? allowedLatenessMinutes : 120;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isBusinessFinalized() {
        return this.status == ShipmentStatus.DELIVERED || this.status == ShipmentStatus.CLOSED;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public StorageProfile getStorageProfile() { return storageProfile; }
    public void setStorageProfile(StorageProfile storageProfile) { this.storageProfile = storageProfile; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Integer getExpectedCadenceMinutes() { return expectedCadenceMinutes; }
    public void setExpectedCadenceMinutes(Integer expectedCadenceMinutes) { this.expectedCadenceMinutes = expectedCadenceMinutes; }

    public Integer getAllowedLatenessMinutes() { return allowedLatenessMinutes; }
    public void setAllowedLatenessMinutes(Integer allowedLatenessMinutes) { this.allowedLatenessMinutes = allowedLatenessMinutes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
