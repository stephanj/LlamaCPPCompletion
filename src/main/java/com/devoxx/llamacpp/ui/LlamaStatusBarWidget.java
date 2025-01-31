package com.devoxx.llamacpp.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import com.devoxx.llamacpp.core.LlamaResponse;
import com.devoxx.llamacpp.settings.LlamaSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseEvent;

public class LlamaStatusBarWidget implements StatusBarWidget,
        StatusBarWidget.TextPresentation,
        StatusBarWidget.Multiframe {

    private final Project project;
    private String currentText = "Llama Ready";
    private StatusBar statusBar;
    private long completionStartTime;

    public LlamaStatusBarWidget(Project project) {
        this.project = project;
    }

    @Override
    public @NotNull String ID() {
        return "LlamaCompletions";
    }

    @Override
    public @NotNull WidgetPresentation getPresentation() {
        return this;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;
    }

    @Override
    public @NotNull String getText() {
        return currentText;
    }

    @Override
    public float getAlignment() {
        return Component.LEFT_ALIGNMENT;
    }

    @Override
    public @NotNull StatusBarWidget copy() {
        return new LlamaStatusBarWidget(project);
    }

    public void showInfo(LlamaResponse data) {
        LlamaSettings settings = LlamaSettings.getInstance();
        long elapsed = System.currentTimeMillis() - completionStartTime;

        if (data == null || data.content() == null || data.content().trim().isEmpty()) {
            if (settings.isShowInfo()) {
                currentText = String.format("llama-idea | %s | t: %d ms",
                        settings.getUiText("no suggestion"), elapsed);
            } else {
                currentText = String.format("llama-idea | t: %d ms", elapsed);
            }
        } else {
            if (settings.isShowInfo()) {
                currentText = String.format("llama-idea | c: %d/%d | p: %d (%.2f ms, %.2f t/s) | " +
                                "g: %d (%.2f ms, %.2f t/s) | t: %d ms",
                        data.tokens_cached(),
                        Integer.parseInt((String)data.generation_settings().get("n_ctx")),
                        data.timings().prompt_n(),
                        data.timings().prompt_ms(),
                        data.timings().prompt_per_second(),
                        data.timings().predicted_n(),
                        data.timings().predicted_ms(),
                        data.timings().predicted_per_second(),
                        elapsed);
            } else {
                currentText = String.format("llama-idea | t: %d ms", elapsed);
            }
        }

        if (statusBar != null) {
            statusBar.updateWidget(ID());
        }
    }

//    public void showCachedInfo() {
//        long elapsed = System.currentTimeMillis() - completionStartTime;
//
//        currentText = String.format("llama-idea | t: %d ms", elapsed);
//        if (statusBar != null) {
//            statusBar.updateWidget(ID());
//        }
//    }

    public void showThinking() {
        LlamaSettings settings = LlamaSettings.getInstance();
        currentText = "llama-idea | " + settings.getUiText("thinking...");
        completionStartTime = System.currentTimeMillis();

        if (statusBar != null) {
            statusBar.updateWidget(ID());
        }
    }

    @Override
    public @Nullable String getTooltipText() {
        return "";
    }

    @Override
    public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return TextPresentation.super.getClickConsumer();
    }

    @Override
    public @Nullable String getShortcutText() {
        return TextPresentation.super.getShortcutText();
    }

    @Override
    public @Nullable WidgetPresentation getPresentation(@NotNull StatusBarWidget.PlatformType type) {
        return this;
    }
}
