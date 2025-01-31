package com.devoxx.llamacpp.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

public class LRUCache {

    private final int capacity;
    private final Map<String, String> cache;
    public static final float LOAD_FACTOR = 0.75f;
    public static final String SHA_256 = "SHA-256";

    public LRUCache(int capacity) {
        this.capacity = capacity;
        // Create a LinkedHashMap with access-order enabled for LRU functionality
        this.cache = new LinkedHashMap<>(capacity, LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    @Nullable
    public synchronized String get(String key) {
        return cache.get(key);
    }

    public synchronized void put(String key, String value) {
        cache.put(key, value);
    }

    public synchronized int size() {
        return cache.size();
    }

    @NotNull
    public String generateKey(String prefix, String suffix) {
        String combined = prefix + "|" + suffix;
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to a simple hash if SHA-256 is not available
            return String.valueOf(combined.hashCode());
        }
    }
}
