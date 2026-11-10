package com.coldchain.platform.sensor.repository;

import com.coldchain.platform.sensor.model.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {
    List<Sensor> findByShipmentId(String shipmentId);
}
