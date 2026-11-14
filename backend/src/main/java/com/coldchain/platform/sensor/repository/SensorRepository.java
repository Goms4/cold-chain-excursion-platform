package com.coldchain.platform.sensor.repository;

import com.coldchain.platform.sensor.model.Sensor;
import com.coldchain.platform.sensor.model.SensorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findByShipmentId(String shipmentId);
    java.util.Optional<Sensor> findFirstByShipmentIdAndStatusOrderByRegisteredAtAsc(String shipmentId, SensorStatus status);
}
