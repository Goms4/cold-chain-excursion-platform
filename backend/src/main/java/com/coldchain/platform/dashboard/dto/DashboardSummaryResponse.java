package com.coldchain.platform.dashboard.dto;

public record DashboardSummaryResponse(long activeShipments, long needAttention,
                                       long activeIssues, long openInvestigations) {}
