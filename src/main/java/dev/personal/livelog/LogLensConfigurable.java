package dev.personal.livelog;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public final class LogLensConfigurable implements Configurable {
    private JBCheckBox enabled;
    private JBCheckBox foldNoise;
    private ComboBox<LogLevel> minLevel;
    private JBTextField maxEvents;

    @Override public @Nls String getDisplayName() { return "Live Log Lens"; }

    @Override public @Nullable JComponent createComponent() {
        enabled = new JBCheckBox("Enable Live Log Lens");
        foldNoise = new JBCheckBox("Fold known bootstrap noise in the console (never errors or stack traces)");
        minLevel = new ComboBox<>(LogLevel.values());
        maxEvents = new JBTextField();
        return FormBuilder.createFormBuilder()
            .addComponent(enabled)
            .addComponent(foldNoise)
            .addLabeledComponent("Minimum level shown in tool window:", minLevel)
            .addLabeledComponent("Maximum events kept in memory:", maxEvents)
            .addComponentFillVertically(new JPanel(), 0)
            .getPanel();
    }

    @Override public boolean isModified() {
        LogLensSettings.State s = LogLensSettings.getInstance().getState();
        return enabled.isSelected() != s.enabled || foldNoise.isSelected() != s.foldNoise
            || minLevel.getSelectedItem() != LogLensSettings.getInstance().minLevel()
            || parsedMax(s.maxEvents) != s.maxEvents;
    }

    @Override public void apply() {
        LogLensSettings settings = LogLensSettings.getInstance();
        LogLensSettings.State s = settings.getState();
        s.enabled = enabled.isSelected();
        s.foldNoise = foldNoise.isSelected();
        settings.setMinLevel((LogLevel) minLevel.getSelectedItem());
        s.maxEvents = parsedMax(s.maxEvents);
    }

    @Override public void reset() {
        LogLensSettings settings = LogLensSettings.getInstance();
        LogLensSettings.State s = settings.getState();
        enabled.setSelected(s.enabled);
        foldNoise.setSelected(s.foldNoise);
        minLevel.setSelectedItem(settings.minLevel());
        maxEvents.setText(String.valueOf(s.maxEvents));
    }

    private int parsedMax(int fallback) {
        try { return Math.max(100, Integer.parseInt(maxEvents.getText().trim())); } catch (NumberFormatException e) { return fallback; }
    }
}
