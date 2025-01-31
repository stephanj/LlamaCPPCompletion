package com.devoxx.llamacpp.core;

/**
 * Represents a chunk of code context with metadata about when it was captured
 * and where it came from.
 */
public record ContextChunk(
        String text,
        long timestamp,
        String filename
) {
}
