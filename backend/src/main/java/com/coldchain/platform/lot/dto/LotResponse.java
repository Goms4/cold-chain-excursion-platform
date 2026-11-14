package com.coldchain.platform.lot.dto;

import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;
import com.coldchain.platform.common.BusinessIds;

import java.time.Instant;
import java.time.LocalDate;

public class LotResponse {
    private String id;
    private String lotId;
    private String shipmentId;
    private String productName;
    private String batchNumber;
    private Integer quantity;
    private LotRiskLevel riskLevel;
    private LotDisposition dispositionStatus;
    private Instant createdAt;
    private LocalDate expiryDate;
    private Instant unloadedAt;
    private Instant archivedAt;
    private boolean hasOperationalHistory;
    private Instant deletedAt;

    public LotResponse() {}

    public static LotResponse fromEntity(Lot lot) {
        LotResponse dto = new LotResponse();
        dto.setId(BusinessIds.lot(lot.getId()));
        dto.setLotId(BusinessIds.lot(lot.getId()));
        dto.setShipmentId(BusinessIds.shipment(lot.getShipment().getId()));
        dto.setProductName(lot.getProductName());
        dto.setBatchNumber(lot.getBatchNumber());
        dto.setQuantity(lot.getQuantity());
        dto.setRiskLevel(lot.getRiskLevel());
        dto.setDispositionStatus(lot.getDispositionStatus());
        dto.setCreatedAt(lot.getCreatedAt());
        dto.setExpiryDate(lot.getExpiryDate());
        dto.setUnloadedAt(lot.getUnloadedAt());
        dto.setArchivedAt(lot.getArchivedAt());
        dto.setDeletedAt(lot.getDeletedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLotId() { return lotId; }
    public void setLotId(String lotId) { this.lotId = lotId; }

    public String getShipmentId() { return shipmentId; }
    public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

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
    public boolean isHasOperationalHistory() { return hasOperationalHistory; }
    public void setHasOperationalHistory(boolean hasOperationalHistory) { this.hasOperationalHistory = hasOperationalHistory; }
}
