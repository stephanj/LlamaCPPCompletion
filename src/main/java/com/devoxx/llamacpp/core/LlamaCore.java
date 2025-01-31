package com.devoxx.llamacpp.core;

import com.devoxx.llamacpp.CompletionState;
import com.intellij.openapi.diagnostic.Logger;
import com.devoxx.llamacpp.settings.LlamaSettings;
import com.devoxx.llamacpp.ui.LlamaStatusBarFactory;
import com.devoxx.llamacpp.ui.LlamaStatusBarWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

public class LlamaCore {
    private static final Logger LOG = Logger.getInstance(LlamaCore.class);
    private final List<CompletionListener> listeners = new ArrayList<>();
    private static final long REQUEST_TIMEOUT_MS = 5000;

    private final LlamaServer llamaServer;
    private final LRUCache completionCache;
    private final ExtraContext extraContext;
    private final AtomicBoolean isRequestInProgress;
    private volatile CompletionDetails lastCompletion;

    public LlamaCore() {
        LlamaSettings settings = LlamaSettings.getInstance();
        this.llamaServer = new LlamaServer();
        this.completionCache = new LRUCache(settings.getMaxCacheKeys());
        this.extraContext = new ExtraContext();
        this.isRequestInProgress = new AtomicBoolean(false);
    }

    @Nullable
    public LlamaResponse getCompletion(@NotNull String prefix, @NotNull String suffix) {
        if (!tryAcquireRequestLock()) {
            LOG.debug("Another completion request is in progress");
            return null;
        }

        try {
            // First, check if we have a cached completion that matches
            String cacheKey = completionCache.generateKey(prefix, suffix);
            String cachedCompletion = completionCache.get(cacheKey);

            LlamaStatusBarWidget widget = LlamaStatusBarFactory.getCurrentWidget();

            if (cachedCompletion != null) {
                LOG.debug("Found cached completion for key: " + cacheKey);
                updateLastCompletion(cachedCompletion, prefix, suffix);

                // Create a response object for the cached completion
                LlamaResponse cachedResponse = new LlamaResponse(
                        cachedCompletion,
                        Map.of("n_ctx", "2048"),  // Default context size
                        false,
                        completionCache.size(),
                        new LlamaResponse.Timings(0.0, 0, 0, 0.0, 0.0, 0.0, 0.0)
                );

                if (widget != null) {
                    widget.showInfo(cachedResponse);
                }

                notifyListeners(cachedResponse);
                return cachedResponse;
            }

            // If no cache hit, get completion from server
            LlamaResponse response = llamaServer.getCompletion(
                    prefix,
                    suffix,
                    extraContext.getContextChunks(),
                    CompletionState.getNIndent()
            );

            if (widget != null) {
                widget.showInfo(response);
            }

            if (response != null && response.content() != null && !response.content().trim().isEmpty()) {
                String completion = processCompletion(response.content(), prefix, suffix);
                if (completion != null) {
                    completionCache.put(cacheKey, completion);
                    updateLastCompletion(completion, prefix, suffix);
                    notifyListeners(response);

                    // Cache potential future completions
                    cacheFutureCompletions(prefix, suffix, completion);
                }
                return response;
            }

            return null;
        } catch (Exception e) {
            LOG.error("Error getting completion", e);
            return null;
        } finally {
            isRequestInProgress.set(false);
        }
    }

    // Rest of the class remains the same...
    private boolean tryAcquireRequestLock() {
        LlamaSettings settings = LlamaSettings.getInstance();
        long startTime = System.currentTimeMillis();

        while (isRequestInProgress.get()) {
            if (System.currentTimeMillis() - startTime > REQUEST_TIMEOUT_MS) {
                LOG.warn("Timeout waiting for completion lock");
                return false;
            }

            try {
                TimeUnit.MILLISECONDS.sleep(settings.getMaxPromptMs() / 10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return isRequestInProgress.compareAndSet(false, true);
    }

    private @Nullable String processCompletion(@NotNull String rawCompletion, String prefix, String suffix) {
        // Split completion into lines for processing
        String[] completionLines = rawCompletion.split("\n");

        // Remove trailing empty lines
        while (completionLines.length > 0 && completionLines[completionLines.length - 1].trim().isEmpty()) {
            completionLines = Arrays.copyOf(completionLines, completionLines.length - 1);
        }

        if (completionLines.length == 0) {
            return null;
        }

        // Join the lines back together
        return String.join("\n", completionLines);
    }

    private void cacheFutureCompletions(String prefix, String suffix, @NotNull String currentCompletion) {
        // Split the current completion into lines
        String[] completionLines = currentCompletion.split("\n");

        if (completionLines.length > 1) {
            // Cache the completion that would happen after accepting the first line
            String futurePrefix = prefix + completionLines[0] + "\n";
            String futureCompletion = String.join("\n",
                    Arrays.copyOfRange(completionLines, 1, completionLines.length));

            String futureCacheKey = completionCache.generateKey(futurePrefix, suffix);
            completionCache.put(futureCacheKey, futureCompletion);

            // Also try to get the next completion after this one
            CompletableFuture.runAsync(() -> {
                try {
                    LlamaResponse futureResponse = llamaServer.getCompletion(
                            futurePrefix,
                            suffix,
                            extraContext.getContextChunks(),
                            getIndentationLevel(completionLines[0])
                    );

                    if (futureResponse != null && futureResponse.content() != null) {
                        String nextCompletion = processCompletion(futureResponse.content(), futurePrefix, suffix);
                        if (nextCompletion != null) {
                            String nextCacheKey = completionCache.generateKey(
                                    futurePrefix + futureCompletion, suffix);
                            completionCache.put(nextCacheKey, nextCompletion);
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("Error caching future completion", e);
                }
            });
        }
    }

    @NotNull
    public ExtraContext getExtraContext() {
        return extraContext;
    }

    private int getIndentationLevel(@NotNull String line) {
        int indent = 0;
        while (indent < line.length() && Character.isWhitespace(line.charAt(indent))) {
            indent++;
        }
        return indent;
    }

    private void updateLastCompletion(String completion, String prefix, String suffix) {
        lastCompletion = new CompletionDetails(completion, prefix, suffix);
    }

    @Nullable
    public String getFirstLineOfLastCompletion() {
        if (lastCompletion == null) {
            return null;
        }

        String[] lines = lastCompletion.completion().split("\n");
        if (lines.length == 0) {
            return null;
        }

        // If first line is empty but there are more lines, return the second line
        if (lines[0].trim().isEmpty() && lines.length > 1) {
            return "\n" + lines[1];
        }

        return lines[0];
    }

    @Nullable
    public String getFirstWordOfLastCompletion() {
        String firstLine = getFirstLineOfLastCompletion();
        if (firstLine == null) {
            return null;
        }

        // Preserve leading whitespace
        StringBuilder leadingSpace = new StringBuilder();
        int contentStart = 0;
        while (contentStart < firstLine.length() &&
                Character.isWhitespace(firstLine.charAt(contentStart))) {
            leadingSpace.append(firstLine.charAt(contentStart));
            contentStart++;
        }

        // Get first word
        String[] words = firstLine.substring(contentStart).split("\\s+", 2);
        if (words.length == 0 || words[0].isEmpty()) {
            return null;
        }

        return leadingSpace + words[0];
    }

    public void updateExtraContext(String text, String filename) {
        extraContext.addChunk(text, filename);
    }

    public record CompletionDetails(String completion, String prefix, String suffix) {
    }

    public void addCompletionListener(CompletionListener listener) {
        listeners.add(listener);
    }

    public void notifyListeners(LlamaResponse response) {
        for (CompletionListener listener : listeners) {
            listener.onNewCompletion(response);
        }
    }
}
