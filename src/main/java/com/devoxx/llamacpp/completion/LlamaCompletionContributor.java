package com.devoxx.llamacpp.completion;

import com.devoxx.llamacpp.core.LlamaCore;
import com.devoxx.llamacpp.core.LlamaResponse;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.devoxx.llamacpp.CompletionState;
import com.devoxx.llamacpp.settings.LlamaSettings;
import com.devoxx.llamacpp.ui.LlamaStatusBarFactory;
import com.devoxx.llamacpp.ui.LlamaStatusBarWidget;
import org.jetbrains.annotations.NotNull;

public class LlamaCompletionContributor extends CompletionContributor {

    private static final Logger LOG = Logger.getInstance(LlamaCompletionContributor.class);

    private final LlamaCore llamaCore;

    public LlamaCompletionContributor() {
        this.llamaCore = ApplicationManager.getApplication().getService(LlamaCore.class);

        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement(),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        if (!shouldProvideCompletion(parameters)) {
                            return;
                        }

                        Editor editor = parameters.getEditor();
                        Document document = editor.getDocument();

                        String prefix = getPrefix(document, parameters.getOffset());
                        String suffix = getSuffix(document, parameters.getOffset());

                        LlamaStatusBarWidget widget = LlamaStatusBarFactory.getCurrentWidget();
                        if (widget != null) {
                            widget.showThinking();
                        }

                        CompletionState.setActiveCompletion(true);

                        ApplicationManager.getApplication().invokeLater(() -> {
                            try {
                                LlamaResponse suggestion = llamaCore.getCompletion(prefix, suffix);
                                if (suggestion != null && !suggestion.content().isEmpty()) {
                                    // Create lookup elements for each line of the suggestion
                                    String[] lines = suggestion.content().split("\n");
                                    for (String line : lines) {
                                        if (!line.trim().isEmpty()) {
                                            LookupElement element = createLookupElement(line);
                                            result.addElement(
                                                    PrioritizedLookupElement.withPriority(element, Double.MAX_VALUE)
                                            );
                                        }
                                    }

                                    // Notify any completion listeners (including the panel)
                                    llamaCore.notifyListeners(suggestion);
                                }
                            } catch (Exception ex) {
                                LOG.error("Error during completion:", ex);
                            } finally {
                                CompletionState.setActiveCompletion(false);
                                if (widget != null) {
                                    widget.showInfo(null);
                                }
                            }
                        });
                    }
                });
    }

    private boolean shouldProvideCompletion(CompletionParameters parameters) {
        LlamaSettings settings = LlamaSettings.getInstance();
        if (!settings.isEnabled()) {
            return false;
        }
        // Check if auto-trigger is enabled for automatic completion
        return settings.isAutoTrigger() || CompletionState.isManuallyTriggered();
    }

    private @NotNull String getPrefix(@NotNull Document document, int offset) {
        LlamaSettings settings = LlamaSettings.getInstance();
        int startLine = Math.max(0, document.getLineNumber(offset) - settings.getPrefixLines());
        return document.getText(TextRange.create(
                document.getLineStartOffset(startLine),
                offset
        ));
    }

    private @NotNull String getSuffix(@NotNull Document document, int offset) {
        LlamaSettings settings = LlamaSettings.getInstance();
        int endLine = Math.min(document.getLineCount() - 1,
                document.getLineNumber(offset) + settings.getSuffixLines());
        return document.getText(TextRange.create(
                offset,
                document.getLineEndOffset(endLine)
        ));
    }

    private @NotNull LookupElement createLookupElement(String suggestion) {
        LOG.info(">>> Suggestion: " + suggestion);
        long elapsed = llamaCore.getExtraContext().getElapsedTime();

        return LookupElementBuilder.create(suggestion)
                .withPresentableText(suggestion)
                .withTypeText(String.format("Llama (%d ms)", elapsed), true)
                .withBoldness(true)
                // Add these properties to ensure our handler takes precedence
                .withInsertHandler((context, item) -> {
                    Editor editor = context.getEditor();
                    Document document = editor.getDocument();

                    // Ensure we're running in a write action
                    WriteCommandAction.runWriteCommandAction(context.getProject(), () -> {
                        // Calculate the exact range to replace
                        int startOffset = context.getStartOffset();
                        int endOffset = context.getTailOffset();

                        // Get the current document state
                        String currentText = document.getText(new TextRange(startOffset, endOffset));

                        // Only delete existing text if it differs from what we want to insert
                        if (!suggestion.equals(currentText)) {
                            document.deleteString(startOffset, endOffset);
                            document.insertString(startOffset, suggestion);
                        }

                        // Move caret to end of inserted text
                        editor.getCaretModel().moveToOffset(startOffset + suggestion.length());
                    });

                    // Mark completion as no longer active
                    CompletionState.setActiveCompletion(false);
                });
    }
}
