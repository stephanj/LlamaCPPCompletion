package com.devoxx.llamacpp.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.project.Project;
import com.devoxx.llamacpp.CompletionState;
import org.jetbrains.annotations.NotNull;

public class TriggerCompletionAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(TriggerCompletionAction.class);

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        // Enable the action only when we have an active editor
        e.getPresentation().setEnabled(project != null && editor != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            return;
        }

        LOG.info("TriggerCompletionAction invoked");

        // Set manual trigger flag and invoke completion
        CompletionState.setManualTrigger(true);
        try {
            com.intellij.codeInsight.completion.CompletionType type =
                    com.intellij.codeInsight.completion.CompletionType.BASIC;
            com.intellij.codeInsight.completion.CodeCompletionHandlerBase.createHandler(type)
                    .invokeCompletion(project, editor);
        } finally {
            CompletionState.setManualTrigger(false);
        }
    }
}
