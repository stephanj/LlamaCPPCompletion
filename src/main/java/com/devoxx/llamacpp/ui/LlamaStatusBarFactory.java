package com.devoxx.llamacpp.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class LlamaStatusBarFactory implements StatusBarWidgetFactory {

    @Getter
    private static LlamaStatusBarWidget currentWidget;

    @Override
    public @NotNull String getId() {
        return "LlamaCPP Completions";
    }

    @Override
    public @NotNull String getDisplayName() {
        return "LlamaCPP Completions Status";
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new LlamaStatusBarWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        // Clean up any resources if needed
    }
}

