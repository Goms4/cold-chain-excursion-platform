package com.coldchain.platform.auth.dto;

import com.coldchain.platform.common.security.Role;

public class AuthRequest {
    private Role role;
    private String companyName;
    private String password;
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
