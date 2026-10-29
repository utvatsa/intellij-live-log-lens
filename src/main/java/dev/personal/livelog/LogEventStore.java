package dev.personal.livelog;

import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** In-memory ring of recent events for one project. Thread-safe; listeners are notified on the EDT. */
public final class LogEventStore {
    private final ArrayDeque<LogEvent> events = new ArrayDeque<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static LogEventStore getInstance(@NotNull com.intellij.openapi.project.Project project) {
        return project.getService(LogEventStore.class);
    }

    public synchronized void add(LogEvent event) {
        events.addLast(event);
        int max = LogLensSettings.getInstance().maxEvents();
        while (events.size() > max) events.removeFirst();
        fire();
    }

    public synchronized void clear() {
        events.clear();
        fire();
    }

    public synchronized List<LogEvent> snapshot() { return new ArrayList<>(events); }

    public void addListener(Runnable r) { listeners.add(r); }

    public void removeListener(Runnable r) { listeners.remove(r); }

    /** Called after a continuation line mutated the newest event. */
    public void touched() { fire(); }

    private void fire() {
        ApplicationManager.getApplication().invokeLater(() -> listeners.forEach(Runnable::run));
    }
}
