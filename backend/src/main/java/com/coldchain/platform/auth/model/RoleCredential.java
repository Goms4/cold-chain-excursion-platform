package com.coldchain.platform.auth.model;

import com.coldchain.platform.common.security.Role;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "role_credential")
public class RoleCredential {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected RoleCredential() {}
    public RoleCredential(Role role, String passwordHash) { this.role = role; this.passwordHash = passwordHash; this.updatedAt = Instant.now(); }
    public Role getRole() { return role; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; this.updatedAt = Instant.now(); }
}
