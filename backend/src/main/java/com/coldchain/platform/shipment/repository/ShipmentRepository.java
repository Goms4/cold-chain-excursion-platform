package com.coldchain.platform.shipment.repository;

import com.coldchain.platform.shipment.model.Shipment;
import com.coldchain.platform.shipment.model.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    List<Shipment> findByStatus(ShipmentStatus status);
    List<Shipment> findByArchivedAtIsNull();
    List<Shipment> findByArchivedAtIsNotNull();
    List<Shipment> findByArchivedAtIsNullAndDeletedAtIsNull();
    List<Shipment> findByArchivedAtIsNotNullAndDeletedAtIsNull();
    boolean existsByTrackingNumber(String trackingNumber);
}
