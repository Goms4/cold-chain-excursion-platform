package com.coldchain.platform.audit.service;

import com.coldchain.platform.audit.dto.AuditActivityResponse;
import com.coldchain.platform.audit.dto.AuditTechnicalGroupResponse;
import com.coldchain.platform.audit.model.AuditRecord;
import com.coldchain.platform.audit.repository.AuditRecordRepository;
import com.coldchain.platform.common.BusinessIds;
import com.coldchain.platform.decision.repository.DispositionDecisionRepository;
import com.coldchain.platform.event.repository.MissingIntervalRepository;
import com.coldchain.platform.event.repository.ProcessedSensorEventRepository;
import com.coldchain.platform.excursion.repository.ExcursionAffectedLotRepository;
import com.coldchain.platform.excursion.repository.ExcursionIncidentRepository;
import com.coldchain.platform.investigation.repository.InvestigationRepository;
import com.coldchain.platform.lot.repository.LotRepository;
import com.coldchain.platform.shipment.repository.ShipmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

@Service
public class AuditActivityService {
    private final AuditRecordRepository audits;
    private final ShipmentRepository shipments;
    private final LotRepository lots;
    private final ProcessedSensorEventRepository events;
    private final ExcursionIncidentRepository issues;
    private final ExcursionAffectedLotRepository affectedLots;
    private final InvestigationRepository investigations;
    private final DispositionDecisionRepository decisions;
    private final MissingIntervalRepository intervals;

    public AuditActivityService(AuditRecordRepository audits, ShipmentRepository shipments, LotRepository lots,
                                ProcessedSensorEventRepository events, ExcursionIncidentRepository issues,
                                ExcursionAffectedLotRepository affectedLots, InvestigationRepository investigations,
                                DispositionDecisionRepository decisions, MissingIntervalRepository intervals) {
        this.audits = audits;
        this.shipments = shipments;
        this.lots = lots;
        this.events = events;
        this.issues = issues;
        this.affectedLots = affectedLots;
        this.investigations = investigations;
        this.decisions = decisions;
        this.intervals = intervals;
    }

    @Transactional(readOnly = true)
    public Page<AuditActivityResponse> getActivity(Pageable pageable) {
        return audits.findBusinessActivity(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditActivityResponse> getEntityStory(String entityType, String entityId) {
        String type = entityType.toUpperCase(java.util.Locale.ROOT);
        List<AuditRecord> story = new ArrayList<>();
        if ("SHIPMENT".equals(type)) {
            String shipmentId = shipments.existsById(entityId) ? entityId : BusinessIds.internalShipment(entityId);
            addRecords(story, "SHIPMENT", shipmentId);
            lots.findByShipmentId(shipmentId).forEach(lot -> addRecords(story, "LOT", lot.getId()));
            events.findByShipmentIdOrderByEventTimeAsc(shipmentId).forEach(event -> addRecords(story, "SENSOR_EVENT", event.getEventId()));
            issues.findByShipmentId(shipmentId).forEach(issue -> addIssueStory(story, issue.getId()));
        } else if ("EXCURSION".equals(type)) {
            addIssueStory(story, entityId);
        } else if ("INVESTIGATION".equals(type)) {
            String investigationId = entityId;
            addRecords(story, "INVESTIGATION", investigationId);
            decisions.findByInvestigationId(investigationId).forEach(decision -> {
                addRecords(story, "DECISION", String.valueOf(decision.getId()));
                for (String lotId : decision.getAffectedLotIds() == null ? List.<String>of() : java.util.Arrays.asList(decision.getAffectedLotIds().split(","))) {
                    addRecords(story, "LOT", lotId);
                }
            });
        } else {
            addRecords(story, type, entityId);
        }
        return story.stream().collect(java.util.stream.Collectors.toMap(AuditRecord::getId, record -> record,
                        (first, ignored) -> first, LinkedHashMap::new)).values().stream()
                .filter(record -> "BUSINESS".equals(record.getCategory()))
                .sorted(java.util.Comparator.comparing(AuditRecord::getTimestamp).reversed())
                .map(this::toResponse).toList();
    }

    private void addIssueStory(List<AuditRecord> story, String issueId) {
        addRecords(story, "EXCURSION", issueId);
        issues.findById(issueId).ifPresent(issue -> {
            events.findByExcursionIncidentId(issueId).forEach(event -> addRecords(story, "SENSOR_EVENT", event.getEventId()));
            affectedLots.findByExcursionId(issueId).forEach(affected -> addRecords(story, "LOT", affected.getLot().getId()));
        });
        investigations.findByExcursionId(issueId).ifPresent(investigation -> {
            addRecords(story, "INVESTIGATION", investigation.getId());
            decisions.findByInvestigationId(investigation.getId()).forEach(decision -> {
                addRecords(story, "DECISION", String.valueOf(decision.getId()));
                for (String lotId : decision.getAffectedLotIds() == null ? List.<String>of() : java.util.Arrays.asList(decision.getAffectedLotIds().split(","))) {
                    addRecords(story, "LOT", lotId);
                }
            });
        });
    }

    private void addRecords(List<AuditRecord> target, String type, String id) {
        target.addAll(audits.findByEntityTypeAndEntityIdOrderByTimestampDesc(type, id));
    }

    @Transactional(readOnly = true)
    public List<AuditTechnicalGroupResponse> getTechnicalGroups(Pageable pageable) {
        Map<String, List<AuditRecord>> groups = new LinkedHashMap<>();
        Map<String, String> shipmentByKey = new LinkedHashMap<>();
        for (AuditRecord record : audits.findByCategoryOrderByTimestampDesc("TECHNICAL", pageable)) {
            String shipmentId = events.findByEventId(record.getEntityId())
                    .map(event -> BusinessIds.shipment(event.getShipment().getId())).orElse(null);
            boolean duplicate = "DUPLICATE_DETECTED".equals(record.getAction());
            String key = duplicate && shipmentId != null
                    ? record.getAction() + "|" + shipmentId
                    : record.getAction() + "|" + record.getEntityType() + "|" + record.getEntityId();
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
            shipmentByKey.putIfAbsent(key, shipmentId);
        }
        return groups.entrySet().stream().map(entry -> {
            List<AuditRecord> records = entry.getValue();
            AuditRecord first = records.get(0);
            String shipmentId = shipmentByKey.get(entry.getKey());
            String summary = "DUPLICATE_DETECTED".equals(first.getAction())
                    ? records.size() + " duplicate readings suppressed" + (shipmentId == null ? "" : " for " + shipmentId)
                    : first.getAction().replace('_', ' ').toLowerCase(java.util.Locale.ROOT)
                    + (shipmentId == null ? "" : " for " + shipmentId);
            List<AuditTechnicalGroupResponse.Entry> details = records.stream().map(record ->
                    new AuditTechnicalGroupResponse.Entry(record.getId(), record.getTimestamp(), actor(record.getActor()),
                            record.getEntityId(), record.getRationale() == null ? record.getPayloadAfter() : record.getRationale())).toList();
            return new AuditTechnicalGroupResponse(entry.getKey(), first.getAction(), shipmentId,
                    records.size(), summary, first.getTimestamp(), details);
        }).toList();
    }

    private AuditActivityResponse toResponse(AuditRecord record) {
        String entityId = record.getEntityId();
        String shipmentId = null;
        String investigationId = null;
        List<String> lotIds = List.of();
        String businessEntityId = entityId;
        String action = title(record);
        String summary;
        String type = record.getEntityType();

        switch (type) {
            case "SHIPMENT" -> {
                var shipment = shipments.findById(entityId).orElse(null);
                shipmentId = shipment == null ? BusinessIds.shipment(entityId) : BusinessIds.shipment(shipment.getId());
                businessEntityId = shipmentId;
                summary = switch (record.getAction()) {
                    case "CREATED" -> "Shipment " + shipmentId + " registered from " + (shipment == null ? "" : shipment.getOrigin() + " to " + shipment.getDestination());
                    case "ARCHIVED" -> "Shipment " + shipmentId + " archived";
                    case "DELETED" -> "Shipment " + shipmentId + " permanently deleted";
                    default -> "Shipment " + shipmentId + " updated: " + clean(record.getPayloadAfter());
                };
            }
            case "LOT" -> {
                var lot = lots.findById(entityId).orElse(null);
                businessEntityId = lot == null ? BusinessIds.lot(entityId) : BusinessIds.lot(lot.getId());
                if (lot != null) shipmentId = BusinessIds.shipment(lot.getShipment().getId());
                summary = switch (record.getAction()) {
                    case "CREATED" -> "Lot " + businessEntityId + (shipmentId == null ? "" : " added to " + shipmentId);
                    case "ARCHIVED" -> "Lot " + businessEntityId + " archived";
                    case "DELETED" -> "Lot " + businessEntityId + " permanently deleted";
                    default -> "Lot " + businessEntityId + " disposition updated: " + clean(record.getPayloadAfter());
                };
            }
            case "SENSOR_EVENT" -> {
                var event = events.findByEventId(entityId).orElse(null);
                if (event != null) {
                    businessEntityId = BusinessIds.event(event.getId());
                    shipmentId = BusinessIds.shipment(event.getShipment().getId());
                    if (event.getLot() != null) lotIds = List.of(BusinessIds.lot(event.getLot().getId()));
                    summary = "Temperature reading " + event.getTemperatureC() + "°C recorded for " + shipmentId;
                } else summary = "Temperature reading recorded";
            }
            case "EXCURSION" -> {
                var issue = issues.findById(entityId).orElse(null);
                businessEntityId = entityId;
                if (issue != null) {
                    shipmentId = BusinessIds.shipment(issue.getShipment().getId());
                    lotIds = affectedLots.findByExcursionId(issue.getId()).stream().map(a -> BusinessIds.lot(a.getLot().getId())).toList();
                    summary = shipmentId + " exceeded its storage target; peak " + issue.getPeakTemperatureC() + "°C";
                } else summary = "Temperature issue detected";
            }
            case "INVESTIGATION" -> {
                var investigation = investigations.findById(entityId).orElse(null);
                businessEntityId = BusinessIds.investigation(entityId);
                if (investigation != null) {
                    investigationId = BusinessIds.investigation(investigation.getId());
                    shipmentId = BusinessIds.shipment(investigation.getExcursion().getShipment().getId());
                    lotIds = affectedLots.findByExcursionId(investigation.getExcursion().getId()).stream().map(a -> BusinessIds.lot(a.getLot().getId())).toList();
                }
                summary = investigationSummary(record, shipmentId, investigationId);
            }
            case "DECISION" -> {
                var decision = parseLong(entityId) == null ? null : decisions.findById(parseLong(entityId)).orElse(null);
                if (decision != null) {
                    businessEntityId = BusinessIds.decision(decision.getId());
                    investigationId = BusinessIds.investigation(decision.getInvestigation().getId());
                    shipmentId = BusinessIds.shipment(decision.getExcursion().getShipment().getId());
                    lotIds = decision.getAffectedLotIds() == null ? List.of() : java.util.Arrays.stream(decision.getAffectedLotIds().split(","))
                            .map(BusinessIds::lot).toList();
                    summary = switch (decision.getDecision()) {
                        case RELEASE -> shipmentId + " released after investigation";
                        case HOLD -> shipmentId + " placed on hold after investigation";
                        case REJECT -> shipmentId + " rejected after investigation";
                    };
                } else summary = "Shipment disposition recorded";
            }
            default -> {
                var interval = parseLong(entityId) == null ? null : intervals.findById(parseLong(entityId)).orElse(null);
                if (interval != null) shipmentId = BusinessIds.shipment(interval.getShipment().getId());
                summary = shipmentId == null ? "Temperature monitoring gap detected" : "Monitoring gap detected for " + shipmentId;
            }
        }

        return new AuditActivityResponse(record.getId(), record.getSeq(), record.getRowHash(), record.getPrevHash(), record.getTimestamp(), action, summary,
                actor(record.getActor()), type, businessEntityId, shipmentId, investigationId, lotIds, record.getRationale());
    }

    private String title(AuditRecord record) {
        if ("DECISION".equals(record.getEntityType())) {
            Long id = parseLong(record.getEntityId());
            var decision = id == null ? null : decisions.findById(id).orElse(null);
            if (decision != null) return switch (decision.getDecision()) {
                case RELEASE -> "Shipment released";
                case HOLD -> "Shipment placed on hold";
                case REJECT -> "Shipment rejected";
            };
        }
        return switch (record.getAction()) {
            case "CREATED" -> "EXCURSION".equals(record.getEntityType()) ? "Temperature issue detected" : "SHIPMENT".equals(record.getEntityType()) ? "Shipment registered" : "Lot registered";
            case "UPDATED" -> "Shipment updated";
            case "ARCHIVED" -> "SHIPMENT".equals(record.getEntityType()) ? "Shipment archived" : "Lot archived";
            case "DELETED" -> "SHIPMENT".equals(record.getEntityType()) ? "Shipment removed" : "Lot removed";
            case "TEMPERATURE_RECORDED" -> "Temperature reading recorded";
            case "OPENED" -> "Investigation opened";
            case "ASSIGNED", "ASSIGNED_BY_OPERATIONS" -> "Investigator assigned";
            case "INSPECTION_RECORDED" -> "Evidence added";
            case "CONTRADICTION_FLAGGED" -> "Contradictory evidence recorded";
            case "DISPOSITION_DECIDED" -> "Lot disposition updated";
            case "RISK_ELEVATED" -> "Lot risk updated";
            default -> "Monitoring gap detected";
        };
    }

    private String investigationSummary(AuditRecord record, String shipmentId, String investigationId) {
        String shipment = shipmentId == null ? "the shipment" : shipmentId;
        return switch (record.getAction()) {
            case "OPENED" -> "Investigation " + investigationId + " opened for " + shipment;
            case "ASSIGNED", "ASSIGNED_BY_OPERATIONS" -> "Investigator assigned to review " + shipment;
            case "INSPECTION_RECORDED" -> "Manual inspection evidence added to " + shipment;
            case "CONTRADICTION_FLAGGED" -> "Evidence from " + shipment + " conflicts with sensor readings";
            default -> "Investigation updated for " + shipment;
        };
    }

    private String actor(String actor) {
        if (actor == null || actor.isBlank()) return "System";
        if (actor.equals("EXCURSION_ENGINE") || actor.equals("EVENT_PIPELINE") || actor.equals("demo-user")) return "System";
        return actor;
    }

    private String clean(String value) { return value == null || value.isBlank() ? "details updated" : value.replaceAll("(?i)SHP-SCENARIO-(\\d+)", "SHP-1000$1"); }
    private Long parseLong(String value) { try { return Long.parseLong(value); } catch (Exception ignored) { return null; } }
}
