package com.coldchain.platform.excursion.repository;

import com.coldchain.platform.excursion.model.StorageProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StorageProfileRepository extends JpaRepository<StorageProfile, String> {
}
