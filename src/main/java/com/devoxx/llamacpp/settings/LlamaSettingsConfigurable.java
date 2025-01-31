package com.devoxx.llamacpp.settings;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations .*;

import javax.swing .*;
import java.awt .*;

public class LlamaSettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JTextField endpointField;
    private JCheckBox autoTriggerCheckbox;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "LlamaCPP Completions";
    }

    @Override
    public @Nullable JComponent createComponent() {
        LlamaSettings settings = LlamaSettings.getInstance();

        mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // Add components
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;

        addLabelAndComponent("Endpoint:", endpointField = new JTextField(settings.getEndpoint()), c);
        addLabelAndComponent("Auto-trigger:", autoTriggerCheckbox = new JCheckBox("", settings.isAutoTrigger()), c);

        return mainPanel;
    }

    private void addLabelAndComponent(String labelText, JComponent component, @NotNull GridBagConstraints c) {
        JLabel label = new JLabel(labelText);
        c.gridx = 0;
        c.weightx = 0.2;
        mainPanel.add(label, c);

        c.gridx = 1;
        c.weightx = 0.8;
        mainPanel.add(component, c);

        c.gridy++;
    }

    @Override
    public boolean isModified() {
        LlamaSettings settings = LlamaSettings.getInstance();
        return !settings.getEndpoint().equals(endpointField.getText()) ||
                settings.isAutoTrigger() != autoTriggerCheckbox.isSelected();
    }

    @Override
    public void apply() {
        LlamaSettings settings = LlamaSettings.getInstance();
        settings.setEndpoint(endpointField.getText());
        settings.setAutoTrigger(autoTriggerCheckbox.isSelected());
    }
}
