package dev.personal.livelog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "LiveLogLensSettings", storages = @Storage("liveLogLens.xml"))
public final class LogLensSettings implements PersistentStateComponent<LogLensSettings.State> {
    public static final class State {
        public boolean enabled = true;
        public String minLevel = LogLevel.UNKNOWN.name();
        public boolean foldNoise = false;
        public int maxEvents = 5000;
    }

    private State state = new State();

    public static LogLensSettings getInstance() {
        return ApplicationManager.getApplication().getService(LogLensSettings.class);
    }

    @Override public @NotNull State getState() { return state; }

    @Override public void loadState(@NotNull State s) { state = s; }

    public boolean enabled() { return state.enabled; }
    public boolean foldNoise() { return state.foldNoise; }
    public int maxEvents() { return Math.max(100, state.maxEvents); }

    public LogLevel minLevel() {
        try { return LogLevel.valueOf(state.minLevel); } catch (IllegalArgumentException e) { return LogLevel.UNKNOWN; }
    }

    public void setMinLevel(LogLevel level) { state.minLevel = level.name(); }
}
