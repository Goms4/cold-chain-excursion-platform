package com.coldchain.platform.auth.web;

import com.coldchain.platform.auth.dto.AuthRequest;
import com.coldchain.platform.auth.dto.AuthResponse;
import com.coldchain.platform.common.security.JwtTokenProvider;
import com.coldchain.platform.common.security.Role;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtTokenProvider tokenProvider;

    public AuthController(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String username = (request.getUsername() == null || request.getUsername().isBlank())
                ? "demo-user"
                : request.getUsername();
        Role role = (request.getRole() == null) ? Role.OPERATIONS : request.getRole();

        String token = tokenProvider.generateToken(username, role);
        return ResponseEntity.ok(new AuthResponse(token, username, role));
    }

    @GetMapping("/token/{role}")
    public ResponseEntity<AuthResponse> getDemoToken(@PathVariable String role) {
        Role parsedRole;
        try {
            parsedRole = Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            parsedRole = Role.OPERATIONS;
        }
        String username = parsedRole.name().toLowerCase() + "-officer";
        String token = tokenProvider.generateToken(username, parsedRole);
        return ResponseEntity.ok(new AuthResponse(token, username, parsedRole));
    }
}
