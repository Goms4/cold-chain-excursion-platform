package com.coldchain.platform.lot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Instant;

public class UpdateLotRequest {
    @Size(max = 128, message = "Product name must be 128 characters or fewer.")
    private String productName;
    @Size(max = 64, message = "Lot number must be 64 characters or fewer.")
    private String batchNumber;
    @Min(value = 1, message = "Quantity must be at least one.")
    private Integer quantity;
    private LocalDate expiryDate;
    private Instant unloadedAt;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Instant getUnloadedAt() { return unloadedAt; }
    public void setUnloadedAt(Instant unloadedAt) { this.unloadedAt = unloadedAt; }
}
