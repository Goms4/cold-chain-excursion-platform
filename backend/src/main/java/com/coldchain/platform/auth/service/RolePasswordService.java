package com.coldchain.platform.auth.service;

import com.coldchain.platform.auth.model.RoleCredential;
import com.coldchain.platform.auth.repository.RoleCredentialRepository;
import com.coldchain.platform.common.security.Role;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.HexFormat;
import java.util.Map;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class RolePasswordService {
    private static final byte[] DEMO_OPERATIONS_PASSWORD_SHA256 = HexFormat.of().parseHex("b4a85e923bb9b18f524550178e9a0a26a90bbdf8047a732b337054e027bc9ea1");
    private static final byte[] DEMO_ANALYST_PASSWORD_SHA256 = HexFormat.of().parseHex("e9970134e315e0627846ae5c4aa9f378511c55f23f5d9a920a745e9a1a43689c");
    private final PasswordEncoder encoder;
    private final RoleCredentialRepository credentials;
    private final Environment environment;
    private final String operationsPassword;
    private final String analystPassword;
    private final Map<Role, String> hashes = new EnumMap<>(Role.class);

    public RolePasswordService(PasswordEncoder encoder, RoleCredentialRepository credentials, Environment environment,
            @Value("${app.auth.operations-password:}") String operationsPassword,
            @Value("${app.auth.analyst-password:}") String analystPassword) {
        this.encoder = encoder;
        this.credentials = credentials;
        this.environment = environment;
        this.operationsPassword = operationsPassword;
        this.analystPassword = analystPassword;
    }

    @PostConstruct
    public void initializeCredentials() {
        boolean demoProfile = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("local", "demo"));
        if (!demoProfile && (operationsPassword == null || operationsPassword.isBlank()
                || analystPassword == null || analystPassword.isBlank()
                || isDemoDefault(operationsPassword)
                || isDemoDefault(analystPassword))) {
            throw new IllegalStateException("Outside local/demo profiles, set distinct COLDLATCH_OPERATIONS_PASSWORD and COLDLATCH_ANALYST_PASSWORD values that are not demo defaults.");
        }
        if (operationsPassword == null || operationsPassword.isBlank() || analystPassword == null || analystPassword.isBlank()) {
            throw new IllegalStateException("Demo credentials are unavailable. Activate local/demo or set both COLDLATCH_*_PASSWORD variables.");
        }
        store(Role.OPERATIONS, operationsPassword);
        store(Role.ANALYST, analystPassword);
    }

    private boolean isDemoDefault(String candidate) {
        if (candidate == null) return false;
        try {
            byte[] candidateHash = MessageDigest.getInstance("SHA-256").digest(candidate.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return MessageDigest.isEqual(candidateHash, DEMO_OPERATIONS_PASSWORD_SHA256)
                    || MessageDigest.isEqual(candidateHash, DEMO_ANALYST_PASSWORD_SHA256);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    private void store(Role role, String password) {
        RoleCredential row = credentials.findById(role).orElseGet(() -> new RoleCredential(role, encoder.encode(password)));
        if (!encoder.matches(password, row.getPasswordHash())) row.setPasswordHash(encoder.encode(password));
        credentials.save(row);
        hashes.put(role, row.getPasswordHash());
    }

    public boolean matches(Role requestedRole, String candidate) {
        if (requestedRole == null || candidate == null) return false;
        String hash = hashes.get(requestedRole);
        return hash != null && encoder.matches(candidate, hash);
    }
}
