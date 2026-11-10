package com.coldchain.platform.excursion.repository;

import com.coldchain.platform.excursion.model.ExcursionIncident;
import com.coldchain.platform.excursion.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExcursionIncidentRepository extends JpaRepository<ExcursionIncident, String> {
    List<ExcursionIncident> findByShipmentId(String shipmentId);
    Optional<ExcursionIncident> findFirstByShipmentIdAndStatusNot(String shipmentId, IncidentStatus status);
    Optional<ExcursionIncident> findFirstByShipmentIdAndStatus(String shipmentId, IncidentStatus status);
    long countByStatus(IncidentStatus status);
}
