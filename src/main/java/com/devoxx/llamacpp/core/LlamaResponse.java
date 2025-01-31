package com.devoxx.llamacpp.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LlamaResponse(
        String content,
        Map<String, Object> generation_settings,
        boolean truncated,
        int tokens_cached,
        Timings timings
) {
    // For nested records, we also need to handle unknown properties
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Timings(
            double prompt_ms,
            int prompt_n,
            int predicted_n,
            double predicted_ms,
            double prompt_per_second,
            double predicted_per_second,
            double prompt_per_token_ms
    ) {}
}
