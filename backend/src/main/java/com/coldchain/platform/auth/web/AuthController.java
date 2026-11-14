package com.coldchain.platform.auth.web;

import com.coldchain.platform.audit.service.AuditService;
import com.coldchain.platform.auth.dto.AuthRequest;
import com.coldchain.platform.auth.dto.AuthResponse;
import com.coldchain.platform.auth.service.LoginAttemptLimiter;
import com.coldchain.platform.auth.service.RolePasswordService;
import com.coldchain.platform.common.security.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String INCORRECT = "Incorrect role or password";
    private final RolePasswordService passwords;
    private final LoginAttemptLimiter attempts;
    private final AuditService audit;
    private final HttpSessionSecurityContextRepository contexts;

    public AuthController(RolePasswordService passwords, LoginAttemptLimiter attempts, AuditService audit,
                          HttpSessionSecurityContextRepository contexts) {
        this.passwords = passwords; this.attempts = attempts; this.audit = audit; this.contexts = contexts;
    }

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) { token.getToken(); return ResponseEntity.noContent().build(); }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest servletRequest, HttpServletResponse response) {
        String ip = clientIp(servletRequest);
        if (attempts.isBlocked(ip)) {
            failedAudit(ip, request.getRole());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("message", INCORRECT));
        }
        if (!passwords.matches(request.getRole(), request.getPassword())) {
            attempts.failed(ip);
            failedAudit(ip, request.getRole());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", INCORRECT));
        }
        attempts.succeeded(ip);
        Role role = request.getRole();
        if (request.getCompanyName() == null || request.getCompanyName().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Company label is required."));
        }
        String company = request.getCompanyName().trim();
        if (company.length() > 80 || company.chars().anyMatch(Character::isISOControl)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Company label must be 80 characters or fewer."));
        }
        String roleLabel = role == Role.ANALYST ? "Analyst" : "Operations officer";
        String displayName = roleLabel + " · " + company;
        if (servletRequest.getSession(false) != null) servletRequest.changeSessionId();
        HttpSession session = servletRequest.getSession(true);
        session.setMaxInactiveInterval(8 * 60 * 60);
        session.setAttribute("role", role.name());
        session.setAttribute("displayName", displayName);
        Authentication authentication = new UsernamePasswordAuthenticationToken(displayName, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, servletRequest, response);
        return ResponseEntity.ok(new AuthResponse(role, displayName));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated() || session == null || session.getAttribute("role") == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Sign in to continue"));
        return ResponseEntity.ok(new AuthResponse(Role.valueOf((String) session.getAttribute("role")),
                (String) session.getAttribute("displayName")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    private void failedAudit(String ip, Role role) {
        audit.recordAudit("AUTH", "LOGIN", "LOGIN_FAILED", "System", null,
                "Failed role login attempt", "TECHNICAL login failure from " + ip + " for role " + (role == null ? "UNKNOWN" : role.name()));
    }
    private String clientIp(HttpServletRequest request) { return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr(); }
}
