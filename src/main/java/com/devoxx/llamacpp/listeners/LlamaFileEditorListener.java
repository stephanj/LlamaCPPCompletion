package com.devoxx.llamacpp.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.BulkAwareDocumentListener;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.devoxx.llamacpp.core.LlamaCore;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for file editor events to maintain context for the completion system.
 * This includes tracking file changes, cursor movements, and content modifications
 * to ensure the context remains relevant and up-to-date.
 */
public class LlamaFileEditorListener implements FileEditorManagerListener {
    private final LlamaCore llamaCore;

    public LlamaFileEditorListener() {
        this.llamaCore = new LlamaCore();
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        FileEditor[] editors = source.getEditors(file);
        for (FileEditor editor : editors) {
            if (editor instanceof TextEditor textEditor) {
                setupEditorListeners(textEditor, file);
            }
        }
    }

    private void setupEditorListeners(@NotNull TextEditor editor, VirtualFile file) {
        Editor textEditor = editor.getEditor();

        // Listen for cursor movements
        textEditor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent e) {
                EditorImpl editorImpl = (EditorImpl) e.getEditor();
                Document document = editorImpl.getDocument();
                int line = document.getLineNumber(e.getNewPosition().column); // .offset);

                String text = document.getText();
                llamaCore.updateExtraContext(text, file.getPath());
                updateContextAroundCursor(text, line, file.getPath());
            }
        });

        // Listen for document changes
        textEditor.getDocument().addDocumentListener(new BulkAwareDocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                if (!event.getDocument().isInBulkUpdate()) {
                    handleDocumentChange(event, file);
                }
            }
        });
    }

    private void updateContextAroundCursor(String text, int line, String filepath) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            llamaCore.updateExtraContext(text, filepath);
        });
    }

    private void handleDocumentChange(@NotNull DocumentEvent event, VirtualFile file) {
        Document document = event.getDocument();
        if (document.getText().isEmpty()) {
            return;
        }

        // Schedule context update
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            llamaCore.updateExtraContext(document.getText(), file.getPath());
        });
    }
}

