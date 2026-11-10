package com.coldchain.platform.lot.dto;

import com.coldchain.platform.lot.model.Lot;
import com.coldchain.platform.lot.model.LotDisposition;
import com.coldchain.platform.lot.model.LotRiskLevel;

import java.time.Instant;

public class LotResponse {
    private String id;
    private String shipmentId;
    private String productName;
    private String batchNumber;
    private Integer quantity;
    private LotRiskLevel riskLevel;
    private LotDisposition dispositionStatus;
    private Instant createdAt;

    public LotResponse() {}

    public static LotResponse fromEntity(Lot lot) {
        LotResponse dto = new LotResponse();
        dto.setId(lot.getId());
        dto.setShipmentId(lot.getShipment().getId());
        dto.setProductName(lot.getProductName());
        dto.setBatchNumber(lot.getBatchNumber());
        dto.setQuantity(lot.getQuantity());
        dto.setRiskLevel(lot.getRiskLevel());
        dto.setDispositionStatus(lot.getDispositionStatus());
        dto.setCreatedAt(lot.getCreatedAt());
        return dto;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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
}
