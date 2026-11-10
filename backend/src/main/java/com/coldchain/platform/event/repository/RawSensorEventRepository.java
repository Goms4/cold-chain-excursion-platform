package com.coldchain.platform.event.repository;

import com.coldchain.platform.event.model.RawSensorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawSensorEventRepository extends JpaRepository<RawSensorEvent, Long> {
    List<RawSensorEvent> findByShipmentIdOrderByEventTimeAsc(String shipmentId);
    boolean existsByEventId(String eventId);
    long countByShipmentId(String shipmentId);
}
