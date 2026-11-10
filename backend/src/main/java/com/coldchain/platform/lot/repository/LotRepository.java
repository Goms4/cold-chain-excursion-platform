package com.coldchain.platform.lot.repository;

import com.coldchain.platform.lot.model.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, String> {
    List<Lot> findByShipmentId(String shipmentId);
}
