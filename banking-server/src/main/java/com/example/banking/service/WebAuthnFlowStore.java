package com.example.banking.service;

import com.example.banking.exception.BankingException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stockage temporaire (en memoire) des options WebAuthn entre "start" et "finish".
 * Suffisant pour un deploiement mono-instance ; TTL court pour limiter la surface.
 */
@Service
public class WebAuthnFlowStore {

    private static final long TTL_MS = 5 * 60 * 1000;

    private record Entry(Object value, long expiresAt) {}

    private final Map<String, Entry> map = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String put(Object value) {
        purge();
        byte[] b = new byte[16];
        random.nextBytes(b);
        String id = HexFormat.of().formatHex(b);
        map.put(id, new Entry(value, System.currentTimeMillis() + TTL_MS));
        return id;
    }

    @SuppressWarnings("unchecked")
    public <T> T take(String id, Class<T> type) {
        Entry e = (id == null) ? null : map.remove(id);
        if (e == null || e.expiresAt() < System.currentTimeMillis() || !type.isInstance(e.value())) {
            throw new BankingException("WEBAUTHN_FAILED", "Session WebAuthn expirée. Réessaie.");
        }
        return (T) e.value();
    }

    private void purge() {
        long now = System.currentTimeMillis();
        map.entrySet().removeIf(en -> en.getValue().expiresAt() < now);
    }
}
