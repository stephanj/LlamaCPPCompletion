package com.devoxx.llamacpp.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.devoxx.llamacpp.CompletionState;
import com.devoxx.llamacpp.core.LlamaCore;
import org.jetbrains.annotations.NotNull;

public class AcceptFirstLineAction extends AnAction {
    private final LlamaCore llamaCore;

    public AcceptFirstLineAction() {
        this.llamaCore = ApplicationManager.getApplication().getService(LlamaCore.class);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        // Only enable when we have an editor and a completion is visible
        boolean enabled = project != null && editor != null &&
                CompletionState.hasActiveCompletion();
        e.getPresentation().setEnabled(enabled);
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

        String firstLine = llamaCore.getFirstLineOfLastCompletion();
        if (firstLine == null) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            CaretModel caretModel = editor.getCaretModel();
            Document document = editor.getDocument();
            int offset = caretModel.getOffset();

            document.insertString(offset, firstLine);
            caretModel.moveToOffset(offset + firstLine.length());
        });
    }
}
