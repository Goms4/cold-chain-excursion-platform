package com.coldchain.platform.audit.dto;

import java.time.Instant;
import java.util.List;

public record AuditTechnicalGroupResponse(String id, String action, String shipmentId,
                                          long count, String summary, Instant latestAt,
                                          List<Entry> entries) {
    public record Entry(Long id, Instant timestamp, String actor, String entityId, String details) {}
}
