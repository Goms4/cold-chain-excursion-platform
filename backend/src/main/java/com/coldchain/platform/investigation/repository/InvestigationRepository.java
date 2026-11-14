package com.coldchain.platform.investigation.repository;

import com.coldchain.platform.investigation.model.Investigation;
import com.coldchain.platform.investigation.model.InvestigationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestigationRepository extends JpaRepository<Investigation, String> {
    Optional<Investigation> findByExcursionId(String excursionId);
    List<Investigation> findByStatus(InvestigationStatus status);
    List<Investigation> findByExcursion_Shipment_Id(String shipmentId);
    long countByStatus(InvestigationStatus status);
}
