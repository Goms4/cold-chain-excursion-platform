package com.coldchain.platform.audit.repository;

import com.coldchain.platform.audit.model.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {
    List<AuditRecord> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);
    Page<AuditRecord> findAllByOrderByTimestampDesc(Pageable pageable);
}
