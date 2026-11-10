package com.coldchain.platform.audit.web;

import com.coldchain.platform.audit.model.AuditRecord;
import com.coldchain.platform.audit.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/{entityType}/{entityId}")
    public ResponseEntity<List<AuditRecord>> getEntityAuditHistory(@PathVariable String entityType,
                                                                   @PathVariable String entityId) {
        return ResponseEntity.ok(auditService.getAuditHistory(entityType.toUpperCase(), entityId));
    }

    @GetMapping
    public ResponseEntity<Page<AuditRecord>> getAllAudits(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAllAudits(PageRequest.of(page, size)));
    }
}
