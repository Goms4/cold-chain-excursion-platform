package com.coldchain.platform.audit.dto;

import java.time.Instant;
import java.util.List;

public record AuditActivityResponse(Long id, Long seq, String rowHash, String prevHash, Instant timestamp, String action, String summary,
                                    String actor, String entityType, String entityId,
                                    String shipmentId, String investigationId, List<String> lotIds,
                                    String rationale) {}
