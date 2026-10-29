package dev.personal.livelog;

import java.util.ArrayList;
import java.util.List;

/** One logical log event: a header line plus any stack-trace / continuation lines. */
public final class LogEvent {
    public enum Format { OLYMPUS, SPRING, JUL, RAW_JSON, TEXT }

    public final Format format;
    public final String timestamp;
    public final String thread;
    public final LogLevel level;
    public final String logger;
    public final String context;
    public final String message;
    public final String rawHeader;
    private final List<String> continuation = new ArrayList<>();

    public LogEvent(Format format, String timestamp, String thread, LogLevel level, String logger,
                    String context, String message, String rawHeader) {
        this.format = format;
        this.timestamp = timestamp;
        this.thread = thread;
        this.level = level;
        this.logger = logger;
        this.context = context;
        this.message = message;
        this.rawHeader = rawHeader;
    }

    public void addContinuation(String line) { continuation.add(line); }

    public List<String> continuation() { return continuation; }

    public boolean hasStackTrace() { return !continuation.isEmpty(); }

    /** The JSON payload pretty-printed, or null when the message holds no valid JSON object/array. */
    public String prettyJson() {
        String json = JsonSupport.extractJson(message);
        return json == null ? null : JsonSupport.pretty(json);
    }

    /** Full text for the detail pane. */
    public String detailText() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawHeader);
        String pretty = prettyJson();
        if (pretty != null) sb.append("\n\n--- JSON payload ---\n").append(pretty);
        for (String c : continuation) sb.append('\n').append(c);
        return sb.toString();
    }
}
