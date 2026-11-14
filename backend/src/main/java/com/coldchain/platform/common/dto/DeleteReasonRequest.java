package com.coldchain.platform.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DeleteReasonRequest {
    @NotBlank(message = "Please enter a reason for removing this record.")
    @Size(max = 500, message = "The removal reason must be 500 characters or fewer.")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
