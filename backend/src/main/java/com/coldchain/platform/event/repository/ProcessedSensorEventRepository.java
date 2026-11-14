package com.coldchain.platform.event.repository;

import com.coldchain.platform.event.model.ProcessedSensorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessedSensorEventRepository extends JpaRepository<ProcessedSensorEvent, Long> {

    List<ProcessedSensorEvent> findByShipmentIdOrderByEventTimeAsc(String shipmentId);

    List<ProcessedSensorEvent> findByShipmentIdAndSensorIdOrderByEventTimeAsc(String shipmentId, String sensorId);

    Optional<ProcessedSensorEvent> findFirstByShipmentIdAndSensorIdOrderByEventTimeDesc(String shipmentId, String sensorId);

    boolean existsByEventId(String eventId);

    Optional<ProcessedSensorEvent> findByEventId(String eventId);

    @Query("SELECT e FROM ProcessedSensorEvent e WHERE e.shipment.id = :shipmentId AND e.eventTime >= :startTime AND e.isDuplicate = false ORDER BY e.eventTime ASC")
    List<ProcessedSensorEvent> findNonDuplicatesFromTime(
            @Param("shipmentId") String shipmentId,
            @Param("startTime") Instant startTime);

    long countByShipmentId(String shipmentId);
    long countByShipmentIdAndIsDuplicateTrue(String shipmentId);
    long countByExcursionIncidentIdAndIsDuplicateFalse(String excursionIncidentId);
    List<ProcessedSensorEvent> findByExcursionIncidentId(String excursionIncidentId);
    boolean existsByLot_Id(String lotId);
}
