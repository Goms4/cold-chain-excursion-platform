package com.coldchain.platform.audit.web;

import java.time.Instant;

public record AuditChainVerificationResponse(boolean ok, Long firstBrokenSeq, long rowsChecked, Instant checkedAt) {}
