package com.coldchain.platform.investigation.repository;

import com.coldchain.platform.investigation.model.ManualInspectionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManualInspectionRepository extends JpaRepository<ManualInspectionRecord, Long> {
    List<ManualInspectionRecord> findByInvestigationId(String investigationId);
    List<ManualInspectionRecord> findByShipmentId(String shipmentId);
}
