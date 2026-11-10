package com.coldchain.platform.event.repository;

import com.coldchain.platform.event.model.MissingInterval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissingIntervalRepository extends JpaRepository<MissingInterval, Long> {
    List<MissingInterval> findByShipmentId(String shipmentId);
    long countByShipmentId(String shipmentId);
}
