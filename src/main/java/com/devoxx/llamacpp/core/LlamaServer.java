package com.devoxx.llamacpp.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.intellij.openapi.diagnostic.Logger;
import com.devoxx.llamacpp.settings.LlamaSettings;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Llama.cpp server class doing the handling the completion request/response.
 */
public class LlamaServer {

    private static final Logger LOG = Logger.getInstance(LlamaServer.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public static final String INPUT_PREFIX = "input_prefix";
    public static final String INPUT_SUFFIX = "input_suffix";
    public static final String INPUT_EXTRA = "input_extra";
    public static final String N_PREDICT = "n_predict";
    public static final String TOP_K = "top_k";
    public static final String TOP_P = "top_p";
    public static final String STREAM = "stream";
    public static final String N_INDENT = "n_indent";
    public static final String SAMPLERS = "samplers";
    public static final String CACHE_PROMPT = "cache_prompt";
    public static final String T_MAX_PROMPT_MS = "t_max_prompt_ms";
    public static final String T_MAX_PREDICT_MS = "t_max_predict_ms";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE1 = "application/json";
    public static final String INFILL = "infill";
    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER = "Bearer ";
    public static final int STATUS_OK = 200;

    private final HttpClient httpClient;

    public LlamaServer() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
                .build();
    }

    @Nullable
    public LlamaResponse getCompletion(String inputPrefix, String inputSuffix,
                                       List<ContextChunk> extraContext, int nIndent) {
        try {
            LlamaSettings settings = LlamaSettings.getInstance();

            ObjectMapper mapper = JsonMapper.builder()
                    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                    .build();

            // Configure to ignore unknown properties globally
            mapper.configOverride(Record.class)
                    .setIgnorals(JsonIgnoreProperties.Value.empty().withIgnoreUnknown());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put(INPUT_PREFIX, inputPrefix);
            requestBody.put(INPUT_SUFFIX, inputSuffix);
            requestBody.put(INPUT_EXTRA, extraContext);
            requestBody.put(N_PREDICT, settings.getMaxPredictTokens());
            requestBody.put(TOP_K, 40);
            requestBody.put(TOP_P, 0.99);
            requestBody.put(STREAM, false);
            requestBody.put(N_INDENT, nIndent);
            requestBody.put(SAMPLERS, List.of(TOP_K, TOP_P, INFILL));
            requestBody.put(CACHE_PROMPT, true);
            requestBody.put(T_MAX_PROMPT_MS, settings.getMaxPromptMs());
            requestBody.put(T_MAX_PREDICT_MS, settings.getMaxPredictMs());
            String jsonBody = MAPPER.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(settings.getEndpoint() + "/" + INFILL))
                    .header(CONTENT_TYPE, CONTENT_TYPE1)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofMillis(settings.getMaxPromptMs() + settings.getMaxPredictMs() + 1000))
                    .build();

            if (!settings.getApiKey().isEmpty()) {
                request = HttpRequest.newBuilder()
                        .header(AUTHORIZATION, BEARER + settings.getApiKey())
                        .build();
            }

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == STATUS_OK) {
                return MAPPER.readValue(response.body(), LlamaResponse.class);
            } else {
                LOG.warn("Server returned status code: " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            LOG.error("Error getting completion from server", e);
            return null;
        }
    }

//    public void prepareFutureCompletion(List<ContextChunk> extraContext) {
//        try {
//            LlamaSettings settings = LlamaSettings.getInstance();
//
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put(INPUT_PREFIX, "");
//            requestBody.put(INPUT_SUFFIX, "");
//            requestBody.put(INPUT_EXTRA, extraContext);
//            requestBody.put(N_PREDICT, 1);
//            requestBody.put(TOP_K, 40);
//            requestBody.put(TOP_P, 0.99);
//            requestBody.put(STREAM, false);
//            requestBody.put(SAMPLERS, List.of(TEMPERATURE));
//            requestBody.put(CACHE_PROMPT, true);
//            requestBody.put(T_MAX_PROMPT_MS, 1);
//            requestBody.put(T_MAX_PREDICT_MS, 1);
//
//            String jsonBody = MAPPER.writeValueAsString(requestBody);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create(settings.getEndpoint() + INFILL))
//                    .header(CONTENT_TYPE, CONTENT_TYPE1)
//                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
//                    .build();
//
//            // Send request asynchronously
//            httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
//        } catch (Exception e) {
//            LOG.error("Error preparing future completion", e);
//        }
//    }
}
