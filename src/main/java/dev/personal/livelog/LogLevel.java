package dev.personal.livelog;

import org.jetbrains.annotations.Nullable;

/** Ordered severity. UNKNOWN is used for lines without a recognisable level and is never filtered out. */
public enum LogLevel {
    UNKNOWN, TRACE, DEBUG, INFO, WARN, ERROR;

    /** Parses Log4j/Logback/Spring/JUL names; returns null when the text is not a level. */
    public static @Nullable LogLevel parse(@Nullable String text) {
        if (text == null) return null;
        switch (text.trim().toUpperCase()) {
            case "TRACE": case "FINEST": case "FINER": return TRACE;
            case "DEBUG": case "FINE": case "CONFIG": return DEBUG;
            case "INFO": return INFO;
            case "WARN": case "WARNING": return WARN;
            case "ERROR": case "SEVERE": case "FATAL": return ERROR;
            default: return null;
        }
    }
}
