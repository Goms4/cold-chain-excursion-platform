package com.coldchain.platform.auth.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final Duration BLOCK = Duration.ofMinutes(1);
    private final ConcurrentHashMap<String, Attempts> byIp = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Attempts state = byIp.get(ip);
        if (state == null) return false;
        synchronized (state) {
            if (state.blockedUntil != null && state.blockedUntil.isAfter(Instant.now())) return true;
            if (state.blockedUntil != null) { state.blockedUntil = null; state.failures.clear(); }
            trim(state);
            return false;
        }
    }

    public void failed(String ip) {
        Attempts state = byIp.computeIfAbsent(ip, ignored -> new Attempts());
        synchronized (state) {
            Instant now = Instant.now(); trim(state); state.failures.addLast(now);
            if (state.failures.size() >= MAX_FAILURES) state.blockedUntil = now.plus(BLOCK);
        }
    }

    public void succeeded(String ip) { byIp.remove(ip); }
    private void trim(Attempts state) {
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!state.failures.isEmpty() && state.failures.peekFirst().isBefore(cutoff)) state.failures.removeFirst();
    }
    private static final class Attempts {
        private final ArrayDeque<Instant> failures = new ArrayDeque<>();
        private Instant blockedUntil;
    }
}
