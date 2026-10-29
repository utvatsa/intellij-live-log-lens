package dev.personal.livelog;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/** Turns arbitrary text chunks into complete lines (process output arrives in arbitrary pieces). */
public final class LineAssembler {
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*[A-Za-z]");
    private final StringBuilder buffer = new StringBuilder();
    private final Consumer<String> sink;

    public LineAssembler(Consumer<String> sink) { this.sink = sink; }

    public synchronized void accept(String text) {
        buffer.append(text);
        int nl;
        while ((nl = buffer.indexOf("\n")) >= 0) {
            String line = buffer.substring(0, nl);
            buffer.delete(0, nl + 1);
            emit(line);
        }
    }

    /** Flushes a trailing partial line (process ended without newline). */
    public synchronized void flush() {
        if (buffer.length() > 0) {
            String line = buffer.toString();
            buffer.setLength(0);
            emit(line);
        }
    }

    private void emit(String line) {
        if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
        sink.accept(ANSI.matcher(line).replaceAll(""));
    }
}
