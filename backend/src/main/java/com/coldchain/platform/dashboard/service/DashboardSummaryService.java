package com.coldchain.platform.dashboard.service;

import com.coldchain.platform.dashboard.dto.DashboardSummaryResponse;
import com.coldchain.platform.excursion.model.IncidentStatus;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.shipment.service.ShipmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardSummaryService {
    private final ShipmentService shipments;
    private final ExcursionIncidentRepository issues;
    private final InvestigationRepository investigations;

    public DashboardSummaryService(ShipmentService shipments, ExcursionIncidentRepository issues,
                                   InvestigationRepository investigations) {
        this.shipments = shipments;
        this.issues = issues;
        this.investigations = investigations;
    }

    /** Need attention = active shipments with condition in {TEMPERATURE_ISSUE, AT_RISK}, excluding UNDER_REVIEW. */
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        var activeShipments = shipments.getAllShipments(false);
        long needAttention = activeShipments.stream()
                .filter(shipment -> "TEMPERATURE_ISSUE".equals(shipment.getCondition())
                        || "AT_RISK".equals(shipment.getCondition()))
                .count();
        long activeIssues = issues.countByStatus(IncidentStatus.OPEN)
                + issues.countByStatus(IncidentStatus.UNDER_INVESTIGATION);
        long openInvestigations = investigations.count() - investigations.countByStatus(InvestigationStatus.COMPLETED);
        return new DashboardSummaryResponse(activeShipments.size(), needAttention, activeIssues, openInvestigations);
    }
}
