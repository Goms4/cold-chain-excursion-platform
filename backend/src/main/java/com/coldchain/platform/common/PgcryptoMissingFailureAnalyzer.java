package com.coldchain.platform.common;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

public final class PgcryptoMissingFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {
    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
        for (Throwable current = rootFailure; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && (message.contains("COLDLATCH_PGCRYPTO_MISSING")
                    || (message.toLowerCase().contains("permission denied")
                        && message.toLowerCase().contains("pgcrypto")))) {
                return new FailureAnalysis(
                        "PostgreSQL extension pgcrypto is missing from the configured database.",
                        "Run: scripts/fix-pgcrypto.sh " + databaseName() + " and then restart the backend.",
                        rootFailure);
            }
        }
        return null;
    }

    private String databaseName() {
        return System.getenv().getOrDefault("DB_NAME", "cold_chain");
    }
}
