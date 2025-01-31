package com.devoxx.llamacpp.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
@State(
        name = "LlamaSettings",
        storages = @Storage("llama-settings.xml")
)
public class LlamaSettings implements PersistentStateComponent<LlamaSettings> {
    private String endpoint = "http://127.0.0.1:8012";
    private boolean autoTrigger = true;
    private String apiKey = "";
    private int prefixLines = 256;
    private int suffixLines = 64;
    private int maxPredictTokens = 128;
    private int maxPromptMs = 500;
    private int maxPredictMs = 2500;
    private boolean showInfo = true;
    private int maxLineSuffix = 8;
    private int maxCacheKeys = 250;
    private int ringChunks = 16;
    private int ringChunkSize = 64;
    private int ringScope = 1024;
    private int ringUpdateMs = 1000;
    private String language = "en";
    private boolean enabled = true;
    private boolean insertEnabled = false;

    public static final Integer RING_UPDATE_MIN_TIME_LAST_COMPL = 3000;
    public static final Integer MAX_QUEUED_CHUNKS = 16;
    public static final Integer MAX_LAST_PICK_LINE_DISTANCE = 32;

    public static LlamaSettings getInstance() {
        return ApplicationManager.getApplication().getService(LlamaSettings.class);
    }

    @Override
    public @Nullable LlamaSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull LlamaSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public String getUiText(@NotNull String key) {
        return switch (key) {
            case "thinking..." -> language.equals("en") ? "thinking..." : "...";
            case "no suggestion" -> language.equals("en") ? "no suggestion" : "...";
            default -> "...";
        };
    }
}
