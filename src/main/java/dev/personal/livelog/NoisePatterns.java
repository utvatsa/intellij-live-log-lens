package dev.personal.livelog;

import org.jetbrains.annotations.NotNull;

/** Known, harmless bootstrap chatter. Errors and stack traces are never classified as noise. */
public final class NoisePatterns {
    private NoisePatterns() {}

    private static final String[] MARKERS = {
        "SLF4J: ", "Picked up JAVA_TOOL_OPTIONS", "Picked up _JAVA_OPTIONS", "StatusLogger ",
        "Initializing ProtocolHandler", "Starting ProtocolHandler", "Starting service [Tomcat]",
        "Starting Servlet engine", "Initializing Spring embedded WebApplicationContext",
        "Root WebApplicationContext: initialization completed", "Tomcat initialized with port",
    };

    public static boolean isNoise(@NotNull String line) {
        if (LogParser.isContinuation(line)) return false;
        LogEvent e = LogParser.parse(line);
        if (e.level == LogLevel.ERROR || e.level == LogLevel.WARN) return false;
        for (String marker : MARKERS) {
            if (line.contains(marker)) return true;
        }
        return false;
    }
}
