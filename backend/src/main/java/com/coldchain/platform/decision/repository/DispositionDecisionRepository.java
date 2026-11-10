package com.coldchain.platform.decision.repository;

import com.coldchain.platform.decision.model.DispositionDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispositionDecisionRepository extends JpaRepository<DispositionDecision, Long> {
    List<DispositionDecision> findByInvestigationId(String investigationId);
    List<DispositionDecision> findByExcursionId(String excursionId);
}
