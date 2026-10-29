package dev.personal.livelog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LogParserTest {
    private static final String OLYMPUS = "2026-10-29 18:34:11,369 50454 [main] INFO  [i.z.g.ApplicationMonitor] "
        + "{appResourceId=utsav.v@dev, flowID=abc} - {\"flowId\":\"abc\",\"id\":\"9\",\"type\":\"Info\","
        + "\"title\":\"Application shutdown complete\",\"level\":\"INFO\",\"attributes\":{}}";

    @Test void parsesOlympusLine() {
        LogEvent e = LogParser.parse(OLYMPUS);
        assertEquals(LogEvent.Format.OLYMPUS, e.format);
        assertEquals("2026-10-29 18:34:11,369", e.timestamp);
        assertEquals("main", e.thread);
        assertEquals(LogLevel.INFO, e.level);
        assertEquals("i.z.g.ApplicationMonitor", e.logger);
        assertEquals("appResourceId=utsav.v@dev, flowID=abc", e.context);
        assertTrue(e.message.startsWith("{\"flowId\""));
    }

    @Test void prettyPrintsOlympusJson() {
        String pretty = LogParser.parse(OLYMPUS).prettyJson();
        assertNotNull(pretty);
        assertTrue(pretty.contains("\n  \"title\": \"Application shutdown complete\""));
        assertTrue(pretty.contains("\"attributes\": {}"));
    }

    @Test void olympusWithoutContext() {
        LogEvent e = LogParser.parse("2026-10-29 18:34:11,369 12 [t-1] WARN  [a.B] - plain text");
        assertEquals(LogLevel.WARN, e.level);
        assertNull(e.context);
        assertEquals("plain text", e.message);
        assertNull(e.prettyJson());
    }

    @Test void parsesSpringDefault() {
        LogEvent e = LogParser.parse("2026-10-29T18:34:11.369+05:30  ERROR 123 --- [app] [main] c.x.Y : boom");
        assertEquals(LogEvent.Format.SPRING, e.format);
        assertEquals(LogLevel.ERROR, e.level);
        assertEquals("main", e.thread);
        assertEquals("c.x.Y", e.logger);
        assertEquals("boom", e.message);
    }

    @Test void parsesTomcatJul() {
        LogEvent e = LogParser.parse("29-Oct-2026 18:34:11.369 WARNING [main] org.apache.Foo.bar Something odd");
        assertEquals(LogEvent.Format.JUL, e.format);
        assertEquals(LogLevel.WARN, e.level);
        assertEquals("Something odd", e.message);
    }

    @Test void rawJsonLine() {
        LogEvent e = LogParser.parse("  {\"level\":\"ERROR\",\"msg\":\"x\"}");
        assertEquals(LogEvent.Format.RAW_JSON, e.format);
        assertEquals(LogLevel.ERROR, e.level);
        assertNotNull(e.prettyJson());
    }

    @Test void bannerAndBootstrapAreText() {
        for (String s : new String[]{"  .   ____          _            __ _ _", ":: Spring Boot ::  (v3.3.0)",
            "ERROR StatusLogger Unrecognized format specifier [d]", "[INFO] Building native image"}) {
            LogEvent e = LogParser.parse(s);
            assertEquals(LogEvent.Format.TEXT, e.format, s);
            assertEquals(LogLevel.UNKNOWN, e.level, s);
        }
    }

    @Test void malformedInputDoesNotThrow() {
        for (String s : new String[]{"", " ", "2026-10-29 18:34:11,369 50454 [main INFO", "{\"a\":", "{{{{", "\\", "- - -"}) {
            assertDoesNotThrow(() -> LogParser.parse(s), s);
        }
        assertNull(LogParser.parse("2026-10-29 18:34:11,369 1 [m] INFO [x] - {\"a\":").prettyJson());
    }

    @Test void continuationDetection() {
        assertTrue(LogParser.isContinuation("\tat com.x.Y.run(Y.java:42)"));
        assertTrue(LogParser.isContinuation("Caused by: java.io.IOException: nope"));
        assertTrue(LogParser.isContinuation("\t... 12 more"));
        assertTrue(LogParser.isContinuation("java.lang.IllegalStateException: bad"));
        assertFalse(LogParser.isContinuation(OLYMPUS));
        assertFalse(LogParser.isContinuation(":: Spring Boot ::"));
    }

    @Test void levelSpanOnlyForRealHeaders() {
        int[] span = LogParser.findLevelSpan("2026-10-29 18:34:11,369 1 [m] ERROR [x] - hi");
        assertNotNull(span);
        assertEquals("ERROR", "2026-10-29 18:34:11,369 1 [m] ERROR [x] - hi".substring(span[0], span[1]));
        assertNull(LogParser.findLevelSpan("Building ERROR handler"));
    }

    @Test void sourceLocations() {
        var locs = LogParser.findSourceLocations("failed at Foo.java:42 and (Bar.java:7)");
        assertEquals(1, locs.size());
        assertEquals("Foo.java", locs.get(0).fileName());
        assertEquals(42, locs.get(0).line());
    }

    @Test void jsonStringsWithBracesAndEscapes() {
        String json = "{\"m\":\"a } b \\\" ]\",\"n\":[1,2]} trailing";
        assertEquals("{\"m\":\"a } b \\\" ]\",\"n\":[1,2]}", JsonSupport.extractJson(json));
        assertTrue(JsonSupport.pretty(JsonSupport.extractJson(json)).contains("\"n\": [\n    1,\n    2\n  ]"));
    }

    @Test void noiseNeverHidesErrors() {
        assertTrue(NoisePatterns.isNoise("SLF4J: Class path contains multiple bindings"));
        assertFalse(NoisePatterns.isNoise("2026-10-29 18:34:11,369 1 [m] ERROR [x] - SLF4J: failed"));
        assertFalse(NoisePatterns.isNoise("\tat com.x.Y.run(Y.java:1)"));
    }

    @Test void lineAssemblerJoinsChunks() {
        java.util.List<String> out = new java.util.ArrayList<>();
        LineAssembler a = new LineAssembler(out::add);
        a.accept("hel"); a.accept("lo\r\nwor"); a.accept("ld\n\u001B[31mred\u001B[0m");
        a.flush();
        assertEquals(java.util.List.of("hello", "world", "red"), out);
    }
}
