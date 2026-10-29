package dev.personal.livelog;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

/** Taps stdout/stderr of every process started from the IDE (Run and Debug) via the supported listener topic. */
public final class LogLensExecutionListener implements ExecutionListener {
    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        if (!LogLensSettings.getInstance().enabled()) return;
        LogEventStore store = LogEventStore.getInstance(env.getProject());
        EventBuilder builder = new EventBuilder(store);
        LineAssembler assembler = new LineAssembler(builder::line);
        handler.addProcessListener(new ProcessListener() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                if (outputType == ProcessOutputTypes.SYSTEM) return;
                assembler.accept(event.getText());
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                assembler.flush();
            }
        });
    }

    /** Groups continuation lines (stack traces) onto the preceding event. */
    static final class EventBuilder {
        private final LogEventStore store;
        private LogEvent last;

        EventBuilder(LogEventStore store) { this.store = store; }

        synchronized void line(String line) {
            if (line.isBlank()) return;
            if (last != null && LogParser.isContinuation(line)) {
                last.addContinuation(line);
                store.touched();
                return;
            }
            last = LogParser.parse(line);
            store.add(last);
        }
    }
}
