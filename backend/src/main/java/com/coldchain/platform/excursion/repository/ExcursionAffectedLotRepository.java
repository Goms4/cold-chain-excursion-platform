package com.coldchain.platform.excursion.repository;

import com.coldchain.platform.excursion.model.ExcursionAffectedLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcursionAffectedLotRepository extends JpaRepository<ExcursionAffectedLot, Long> {
    List<ExcursionAffectedLot> findByExcursionId(String excursionId);
}
