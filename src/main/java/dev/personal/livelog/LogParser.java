package dev.personal.livelog;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure line parser; no IDE dependencies so it is unit-testable. */
public final class LogParser {
    private LogParser() {}

    // 2026-10-29 18:34:11,369 50454 [main] INFO  [i.z.g.ApplicationMonitor] {k=v, ...} - message
    private static final Pattern OLYMPUS = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[,.]\\d{1,9})?)\\s+(\\d+)\\s+\\[([^\\]]*)\\]\\s+"
        + "(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL)\\s+\\[([^\\]]*)\\]\\s*(?:\\{([^}]*)\\}\\s*)?-\\s?(.*)$");

    // 2026-10-29T18:34:11.369+05:30  INFO 123 --- [app] [main] c.x.Y : message
    private static final Pattern SPRING = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.,]\\d+(?:Z|[+-]\\d{2}:?\\d{2})?)\\s+"
        + "(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\\s+(\\d+)\\s+---\\s+(?:\\[([^\\]]*)\\]\\s+)*([^:\\s]+)\\s*:\\s?(.*)$");

    // 29-Oct-2026 18:34:11.369 INFO [main] org.apache.Foo.bar message
    private static final Pattern JUL = Pattern.compile(
        "^(\\d{2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(SEVERE|WARNING|INFO|CONFIG|FINE|FINER|FINEST)\\s+"
        + "\\[([^\\]]*)\\]\\s+(\\S+)\\s+(.*)$");

    private static final Pattern JSON_LEVEL = Pattern.compile("\"level\"\\s*:\\s*\"([A-Za-z]+)\"");

    private static final Pattern CONTINUATION = Pattern.compile(
        "^(?:\\s+at\\s+\\S.*|\\s*\\.\\.\\. \\d+ (?:more|common frames omitted).*|\\s*Caused by:.*|\\s*Suppressed:.*"
        + "|[\\w.$]+(?:Exception|Error|Throwable)(?::.*)?)$");

    private static final Pattern LEVEL_TOKEN = Pattern.compile(
        "(?<![\\w])(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)(?![\\w])");

    private static final Pattern SOURCE_LOCATION = Pattern.compile("([\\w$]+\\.(?:java|kt|groovy|scala)):(\\d+)");

    /** Parses one physical line (no trailing newline). Never returns null; unknown text becomes a TEXT event. */
    public static @NotNull LogEvent parse(@NotNull String line) {
        Matcher m = OLYMPUS.matcher(line);
        if (m.matches()) {
            return new LogEvent(LogEvent.Format.OLYMPUS, m.group(1), m.group(3), LogLevel.parse(m.group(4)),
                m.group(5), m.group(6), m.group(7), line);
        }
        m = SPRING.matcher(line);
        if (m.matches()) {
            return new LogEvent(LogEvent.Format.SPRING, m.group(1), m.group(4) == null ? "" : m.group(4),
                LogLevel.parse(m.group(2)), m.group(5), null, m.group(6), line);
        }
        m = JUL.matcher(line);
        if (m.matches()) {
            return new LogEvent(LogEvent.Format.JUL, m.group(1), m.group(3), LogLevel.parse(m.group(2)),
                m.group(4), null, m.group(5), line);
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("{") && JsonSupport.extractJson(trimmed) != null) {
            Matcher lm = JSON_LEVEL.matcher(trimmed);
            LogLevel level = lm.find() ? LogLevel.parse(lm.group(1)) : null;
            return new LogEvent(LogEvent.Format.RAW_JSON, "", "", level == null ? LogLevel.UNKNOWN : level,
                "", null, trimmed, line);
        }
        return new LogEvent(LogEvent.Format.TEXT, "", "", LogLevel.UNKNOWN, "", null, line, line);
    }

    /** True for stack-trace frames, "Caused by:", "... N more" and bare exception headers. */
    public static boolean isContinuation(@NotNull String line) {
        return CONTINUATION.matcher(line).matches();
    }

    /** Start/end offsets of the level token in a log line, or null. Only scans the line header area. */
    public static int @Nullable [] findLevelSpan(@NotNull String line) {
        Matcher header = LEVEL_TOKEN.matcher(line.length() > 160 ? line.substring(0, 160) : line);
        if (!header.find()) return null;
        // Require that the line looks like a log header: something timestamp-like comes first.
        if (!(OLYMPUS.matcher(line).matches() || SPRING.matcher(line).matches() || JUL.matcher(line).matches())) {
            return null;
        }
        return new int[]{header.start(1), header.end(1)};
    }

    /** A "File.java:123" location. */
    public record SourceLocation(String fileName, int line, int start, int end) {}

    /** Finds bare {@code File.java:123} occurrences (not the parenthesised form IntelliJ already links). */
    public static java.util.List<SourceLocation> findSourceLocations(@NotNull String line) {
        java.util.List<SourceLocation> out = new java.util.ArrayList<>();
        Matcher m = SOURCE_LOCATION.matcher(line);
        while (m.find()) {
            if (m.start() > 0 && line.charAt(m.start() - 1) == '(') continue;
            out.add(new SourceLocation(m.group(1), Integer.parseInt(m.group(2)), m.start(), m.end()));
        }
        return out;
    }
}
