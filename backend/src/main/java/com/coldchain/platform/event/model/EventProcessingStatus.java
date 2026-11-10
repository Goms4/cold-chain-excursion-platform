package com.coldchain.platform.event.model;

public enum EventProcessingStatus {
    RECEIVED,
    VALIDATED,
    DUPLICATE,
    QUARANTINED,
    REJECTED
}
