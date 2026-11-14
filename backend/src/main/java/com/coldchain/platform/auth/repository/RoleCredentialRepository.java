package com.coldchain.platform.auth.repository;

import com.coldchain.platform.auth.model.RoleCredential;
import com.coldchain.platform.common.security.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleCredentialRepository extends JpaRepository<RoleCredential, Role> {}
