package com.coldchain.platform.auth.dto;

import com.coldchain.platform.common.security.Role;

public class AuthRequest {
    private String username;
    private Role role;

    public AuthRequest() {}

    public AuthRequest(String username, Role role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
