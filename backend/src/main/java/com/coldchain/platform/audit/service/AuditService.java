package com.coldchain.platform.audit.service;

import com.coldchain.platform.audit.model.AuditRecord;
import com.coldchain.platform.audit.repository.AuditRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuditService {

    private final AuditRecordRepository auditRecordRepository;

    public AuditService(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @Transactional
    public AuditRecord recordAudit(String entityType, String entityId, String action, String actor,
                                   String payloadBefore, String payloadAfter, String rationale) {
        String resolvedActor = (actor == null || actor.isBlank()) ? "SYSTEM" : actor;
        AuditRecord record = new AuditRecord(entityType, entityId, action, resolvedActor, payloadBefore, payloadAfter, rationale);
        return auditRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<AuditRecord> getAuditHistory(String entityType, String entityId) {
        return auditRecordRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditRecord> getAllAudits(Pageable pageable) {
        return auditRecordRepository.findAllByOrderByTimestampDesc(pageable);
    }
}
