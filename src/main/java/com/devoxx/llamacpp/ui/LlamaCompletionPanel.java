package com.devoxx.llamacpp.ui;

import com.devoxx.llamacpp.core.CompletionListener;
import com.devoxx.llamacpp.core.LlamaResponse;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.devoxx.llamacpp.core.LlamaCore;
import com.devoxx.llamacpp.settings.LlamaSettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LlamaCompletionPanel extends JPanel {
    private final JBList<CompletionItem> completionList;
    private final DefaultListModel<CompletionItem> listModel;
    private final Project project;
    private final LlamaCore llamaCore;
    private final LlamaSettings settings;
    private final JCheckBox enabledCheckbox;

    public LlamaCompletionPanel(Project project) {
        this.project = project;
        this.llamaCore = ApplicationManager.getApplication().getService(LlamaCore.class);
        this.settings = LlamaSettings.getInstance();

        listModel = new DefaultListModel<>();
        completionList = new JBList<>(listModel);

        // Register as completion listener
        llamaCore.addCompletionListener(new CompletionListener() {
            @Override
            public void onNewCompletion(LlamaResponse response) {
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    if (response != null && response.content() != null) {
                        String[] lines = response.content().split("\n");
                        for (String line : lines) {
                            if (!line.trim().isEmpty()) {
                                listModel.addElement(new CompletionItem(line, response.timings()));
                            }
                        }
                        if (!listModel.isEmpty()) {
                            completionList.setSelectedIndex(0);
                        }
                    }
                });
            }
        });

        setLayout(new BorderLayout());
        setBorder(JBUI.Borders.empty(5));

        // Create controls panel
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setBorder(JBUI.Borders.emptyBottom(5));

        // Add enable/disable checkbox
        enabledCheckbox = new JCheckBox("Enable Completion", settings.isEnabled());
        enabledCheckbox.addActionListener(e -> {
            settings.setEnabled(enabledCheckbox.isSelected());
        });
        controlsPanel.add(enabledCheckbox, BorderLayout.NORTH);

        // Setup cell renderer
        completionList.setCellRenderer(new CompletionCellRenderer());
        completionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add double-click handler
        completionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = completionList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        insertCompletion(listModel.getElementAt(index));
                    }
                }
            }
        });

        // Add to scroll pane
        JBScrollPane scrollPane = new JBScrollPane(completionList);
        add(scrollPane, BorderLayout.CENTER);

        // Add toolbar with actions
        JToolBar toolbar = createToolbar();
        controlsPanel.add(toolbar, BorderLayout.SOUTH);

        add(controlsPanel, BorderLayout.NORTH);
    }

    private @NotNull JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);

        // Add refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshCompletions());
        toolbar.add(refreshButton);

        // Add clear button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clearCompletions());
        toolbar.add(clearButton);

        return toolbar;
    }

    private void refreshCompletions() {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        Document document = editor.getDocument();
        int offset = editor.getCaretModel().getOffset();

        // Get the text before and after cursor
        String prefix = document.getText().substring(0, offset);
        String suffix = document.getText().substring(offset);

        // Clear existing completions
        listModel.clear();

        // Request new completion
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            var completion = llamaCore.getCompletion(prefix, suffix);
            if (completion != null) {
                SwingUtilities.invokeLater(() -> {
                    String[] lines = completion.content().split("\n");
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) {
                            listModel.addElement(new CompletionItem(line, completion.timings()));
                        }
                    }
                });
            }
        });
    }

    private void clearCompletions() {
        listModel.clear();
    }

    private void insertCompletion(CompletionItem item) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            int offset = editor.getCaretModel().getOffset();
            document.insertString(offset, item.text());
            editor.getCaretModel().moveToOffset(offset + item.text().length());
        });
    }

    // Custom cell renderer for completions
    private static class CompletionCellRenderer extends DefaultListCellRenderer {
        @Override
        public @NotNull Component getListCellRendererComponent(JList<?> list, Object value,
                                                               int index, boolean isSelected, boolean cellHasFocus) {
            CompletionItem item = (CompletionItem)value;
            JLabel label = (JLabel)super.getListCellRendererComponent(list,
                    item.getDisplayText(), index, isSelected, cellHasFocus);
            label.setBorder(JBUI.Borders.empty(5));
            return label;
        }
    }

    record CompletionItem(String text, LlamaResponse.Timings timings) {
        public @NotNull String getDisplayText() {
            return String.format("%s (%.0f ms)",
                    text,
                    timings.prompt_ms());
        }
    }
}
