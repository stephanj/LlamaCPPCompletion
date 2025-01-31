package com.devoxx.llamacpp.core;

public interface CompletionListener {
    void onNewCompletion(LlamaResponse response);
}
