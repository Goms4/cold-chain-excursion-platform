package com.coldchain.platform.lot.model;

import com.coldchain.platform.shipment.model.Shipment;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "lots")
public class Lot {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    @Column(name = "batch_number", nullable = false, length = 64)
    private String batchNumber;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private LotRiskLevel riskLevel = LotRiskLevel.LOW;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition_status", nullable = false, length = 32)
    private LotDisposition dispositionStatus = LotDisposition.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "unloaded_at")
    private Instant unloadedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by", length = 128)
    private String deletedBy;

    @Column(name = "delete_reason", columnDefinition = "TEXT")
    private String deleteReason;

    public Lot() {}

    public Lot(String id, Shipment shipment, String productName, String batchNumber, Integer quantity) {
        this.id = id;
        this.shipment = shipment;
        this.productName = productName;
        this.batchNumber = batchNumber;
        this.quantity = quantity;
        this.riskLevel = LotRiskLevel.LOW;
        this.dispositionStatus = LotDisposition.PENDING;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Shipment getShipment() { return shipment; }
    public void setShipment(Shipment shipment) { this.shipment = shipment; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public LotRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(LotRiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public LotDisposition getDispositionStatus() { return dispositionStatus; }
    public void setDispositionStatus(LotDisposition dispositionStatus) { this.dispositionStatus = dispositionStatus; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Instant getUnloadedAt() { return unloadedAt; }
    public void setUnloadedAt(Instant unloadedAt) { this.unloadedAt = unloadedAt; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }
    public String getDeleteReason() { return deleteReason; }
    public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
    public boolean isDeleted() { return deletedAt != null; }
}
