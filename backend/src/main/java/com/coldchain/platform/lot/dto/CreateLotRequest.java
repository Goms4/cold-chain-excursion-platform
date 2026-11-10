package com.coldchain.platform.lot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateLotRequest {

    @NotBlank(message = "id is required")
    private String id;

    @NotBlank(message = "shipment_id is required")
    private String shipmentId;

    @NotBlank(message = "product_name is required")
    private String productName;

    @NotBlank(message = "batch_number is required")
    private String batchNumber;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    public CreateLotRequest() {}

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
}
