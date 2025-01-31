package com.devoxx.llamacpp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompletionState {

    private static volatile boolean manualTrigger = false;
    private static volatile boolean hasActiveCompletion = false;
    private static volatile int nIndent = 0;

    public CompletionState() {}

    public static boolean isManuallyTriggered() {
        return manualTrigger;
    }

    public static synchronized void setActiveCompletion(boolean active) {
        hasActiveCompletion = active;
    }

    public static synchronized boolean hasActiveCompletion() {
        return hasActiveCompletion;
    }

    public static void setManualTrigger(boolean manualTrigger) {
        CompletionState.manualTrigger = manualTrigger;
    }

    public static int getNIndent() {
        return nIndent;
    }
}
