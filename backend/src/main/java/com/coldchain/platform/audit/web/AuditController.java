package com.coldchain.platform.audit.web;

import com.coldchain.platform.audit.model.AuditRecord;
import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.audit.service.AuditActivityService;
import com.coldchain.platform.audit.dto.AuditActivityResponse;
import com.coldchain.platform.audit.dto.AuditTechnicalGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;
    private final AuditActivityService auditActivityService;
    private final JdbcTemplate jdbcTemplate;

    public AuditController(AuditService auditService, AuditActivityService auditActivityService, JdbcTemplate jdbcTemplate) {
        this.auditService = auditService;
        this.auditActivityService = auditActivityService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditRecord>> getEntityAuditHistory(@PathVariable String entityType,
                                                                   @PathVariable String entityId) {
        return ResponseEntity.ok(auditService.getAuditHistory(entityType.toUpperCase(), entityId));
    }

    @GetMapping("/entity/{entityType}/{entityId}/activity")
    public ResponseEntity<List<AuditActivityResponse>> getEntityActivity(@PathVariable String entityType,
                                                                         @PathVariable String entityId) {
        return ResponseEntity.ok(auditActivityService.getEntityStory(entityType, entityId));
    }

    @GetMapping
    public ResponseEntity<Page<AuditRecord>> getAllAudits(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAllAudits(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)) )));
    }

    @GetMapping("/activity")
    public ResponseEntity<Page<AuditActivityResponse>> getBusinessActivity(@RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(auditActivityService.getActivity(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))));
    }

    @GetMapping("/technical")
    public ResponseEntity<Page<AuditRecord>> getTechnicalAudits(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(auditService.getTechnicalAudits(PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "timestamp"))));
    }

    @GetMapping("/technical/groups")
    public ResponseEntity<List<AuditTechnicalGroupResponse>> getTechnicalGroups(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(auditActivityService.getTechnicalGroups(
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "timestamp"))));
    }

    @GetMapping("/verify")
    public ResponseEntity<AuditChainVerificationResponse> verifyAuditChain() {
        var result = jdbcTemplate.queryForMap("SELECT * FROM verify_audit_chain()");
        return ResponseEntity.ok(new AuditChainVerificationResponse(
                (Boolean) result.get("ok"), (Long) result.get("first_broken_seq"),
                (Long) result.get("rows_checked"), java.time.Instant.now()));
    }
}
