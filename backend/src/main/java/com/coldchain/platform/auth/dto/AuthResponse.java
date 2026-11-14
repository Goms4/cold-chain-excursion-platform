package com.coldchain.platform.auth.dto;

import com.coldchain.platform.common.security.Role;

public record AuthResponse(Role role, String displayName) {}
