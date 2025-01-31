package com.devoxx.llamacpp.core;

import com.devoxx.llamacpp.settings.LlamaSettings;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages extra context for code completion by maintaining a ring buffer of code chunks.
 * This implementation provides thread-safe operations for adding and retrieving context
 * chunks that can be used to improve completion quality.
 */
public class ExtraContext {

    private final Queue<ContextChunk> chunks;
    private final Queue<ContextChunk> queuedChunks;
    private final AtomicInteger ringEvictionCount;
    private final long lastCompletionStartTime;
    private volatile int lastPickLine;

    private static final double SIMILARITY_THRESHOLD = 0.9;
    private final LlamaSettings settings;
    private final Random random = new Random();

    public ExtraContext() {
        this.chunks = new ConcurrentLinkedQueue<>();
        this.queuedChunks = new ConcurrentLinkedQueue<>();
        this.ringEvictionCount = new AtomicInteger(0);
        this.lastCompletionStartTime = System.currentTimeMillis();
        this.lastPickLine = -9999;
        settings = LlamaSettings.getInstance();
    }

    /**
     * Adds a new chunk of code context. The chunk will be queued first and then
     * added to the main ring buffer during the next update cycle.
     *
     * @param text     The code content to add as context
     * @param filename The source file name
     */
    public void addChunk(@NotNull String text, @NotNull String filename) {
        if (text.trim().isEmpty()) {
            return;
        }

        if (settings.getRingChunks() <= 0) {
            return;
        }

        String[] lines = text.split("\n");
        if (lines.length < 3) {
            return;
        }

        // Process the chunk size according to settings
        String[] chunkLines;
        if (lines.length + 1 < settings.getRingChunkSize()) {
            chunkLines = lines;
        } else {
            int startLine = random.nextInt(Math.max(0, lines.length - settings.getRingChunkSize() / 2 + 1));
            int endLine = Math.min(startLine + settings.getRingChunkSize() / 2, lines.length);
            chunkLines = Arrays.copyOfRange(lines, startLine, endLine);
        }

        String chunkContent = String.join("\n", chunkLines) + "\n";

        // Check for duplicates
        if (isDuplicateChunk(chunkContent)) {
            return;
        }

        // Evict similar chunks
        evictSimilarChunks(chunkLines);

        if (queuedChunks.size() >= LlamaSettings.MAX_QUEUED_CHUNKS) {
            queuedChunks.poll(); // Remove oldest chunk if queue is full
        }

        ContextChunk newChunk = new ContextChunk(chunkContent, System.currentTimeMillis(), filename);
        queuedChunks.offer(newChunk);
    }

    /**
     * Updates the ring buffer by processing queued chunks. This should be called
     * periodically to maintain fresh context.
     */
    public void updateRingBuffer() {
        if (queuedChunks.isEmpty() ||
            System.currentTimeMillis() - lastCompletionStartTime < LlamaSettings.RING_UPDATE_MIN_TIME_LAST_COMPL) {
            return;
        }

        ContextChunk chunk = queuedChunks.poll();
        if (chunk != null) {
            chunks.offer(chunk);

            // Maintain ring buffer size
            while (chunks.size() > settings.getRingChunks()) {
                chunks.poll();
            }
        }
    }

    /**
     * Gets the current list of context chunks for use in completion requests.
     */
    @NotNull
    public List<ContextChunk> getContextChunks() {
        return new ArrayList<>(chunks);
    }

    /**
     * Updates the context based on cursor position in the editor.
     *
     * @param cursorLine Current line number of cursor
     * @param text       Text content around cursor
     * @param filename   Source file name
     */
    public void updateContextAroundCursor(int cursorLine, @NotNull String text, @NotNull String filename) {
        int deltaLines = Math.abs(cursorLine - lastPickLine);

        if (deltaLines > LlamaSettings.MAX_LAST_PICK_LINE_DISTANCE) {
            String[] lines = text.split("\n");

            // Add prefix context
            int prefixStart = Math.max(0, cursorLine - settings.getRingScope());
            int prefixEnd = Math.max(0, cursorLine - settings.getPrefixLines());
            if (prefixEnd > prefixStart) {
                String prefixText = String.join("\n",
                        Arrays.copyOfRange(lines, prefixStart, prefixEnd));
                addChunk(prefixText, filename);
            }

            // Add suffix context
            int suffixStart = Math.min(lines.length - 1, cursorLine + settings.getSuffixLines());
            int suffixEnd = Math.min(lines.length - 1,
                    cursorLine + settings.getSuffixLines() + settings.getRingChunkSize());
            if (suffixEnd > suffixStart) {
                String suffixText = String.join("\n",
                        Arrays.copyOfRange(lines, suffixStart, suffixEnd));
                addChunk(suffixText, filename);
            }

            lastPickLine = cursorLine;
        }
    }

    private boolean isDuplicateChunk(String content) {
        return chunks.stream().anyMatch(chunk -> chunk.text().equals(content)) ||
                queuedChunks.stream().anyMatch(chunk -> chunk.text().equals(content));
    }

    private void evictSimilarChunks(String[] newLines) {
        // Remove chunks that are very similar based on Jaccard similarity
        chunks.removeIf(chunk -> calculateJaccardSimilarity(
                chunk.text().split("\n"), newLines) > SIMILARITY_THRESHOLD);
        queuedChunks.removeIf(chunk -> calculateJaccardSimilarity(
                chunk.text().split("\n"), newLines) > SIMILARITY_THRESHOLD);
        ringEvictionCount.incrementAndGet();
    }

    private double calculateJaccardSimilarity(String[] lines1, String[] lines2) {
        Set<String> set1 = new HashSet<>(Arrays.asList(lines1));
        Set<String> set2 = new HashSet<>(Arrays.asList(lines2));

        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - lastCompletionStartTime;
    }
}
