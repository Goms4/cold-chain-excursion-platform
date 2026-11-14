package com.coldchain.platform.audit.repository;

import com.coldchain.platform.audit.model.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    List<AuditRecord> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    Page<AuditRecord> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<AuditRecord> findByCategoryOrderByTimestampDesc(String category, Pageable pageable);
    long countByEntityTypeAndEntityIdAndActionNot(String entityType, String entityId, String action);

    @Query("SELECT a FROM AuditRecord a WHERE a.category = 'BUSINESS' AND a.actor <> 'SCENARIO_RUNNER' AND ((a.entityType = 'SHIPMENT' AND a.action IN ('CREATED','UPDATED','ARCHIVED','DELETED')) OR (a.entityType = 'LOT' AND a.action IN ('CREATED','UPDATED','ARCHIVED','DELETED','DISPOSITION_DECIDED','DISPOSITION_UPDATED','RISK_ELEVATED')) OR (a.entityType = 'SENSOR_EVENT' AND a.action = 'TEMPERATURE_RECORDED') OR (a.entityType = 'EXCURSION' AND a.action = 'CREATED') OR (a.entityType = 'INVESTIGATION' AND a.action IN ('OPENED','ASSIGNED','ASSIGNED_BY_OPERATIONS','INSPECTION_RECORDED','CONTRADICTION_FLAGGED')) OR (a.entityType = 'DECISION' AND a.action = 'RECORDED')) ORDER BY a.timestamp DESC")
    Page<AuditRecord> findBusinessActivity(Pageable pageable);
}
