# Live Log Lens

A small, original, **local-only** IntelliJ IDEA plugin that makes live **Run/Debug console** logs of
Spring Boot / Java apps easier to read. Not a Marketplace product; no network, telemetry or files.

## What it does

| Feature | Where it works | How |
|---|---|---|
| `ERROR` / `WARN` level highlighting (bold, boxed, colour) | Run/Debug console | `ConsoleFilterProvider` |
| Clickable bare `File.java:123` locations (resolved on click, project scope) | Run/Debug console | same filter, lazy hyperlink |
| Optional folding of known bootstrap noise (SLF4J, `StatusLogger`, Tomcat init, ...) — **off by default**, never folds ERROR/WARN or stack traces | Run/Debug console | `ConsoleFolding` |
| *Live Log Lens* tool window: event table (time, level, logger, message), min-level filter, detail pane with the **pretty-printed JSON payload** after the Olympus ` - ` delimiter, stack traces attached to their event | Output of processes started from the IDE (Run and Debug) | `ExecutionListener` + `ProcessListener` |

Java stack-trace links `(File.java:42)` are already provided natively by IntelliJ; the plugin does not duplicate them.

### Recognised formats

- Olympus: `2026-10-29 18:34:11,369 50454 [main] INFO  [i.z.g.ApplicationMonitor] {ctx=...} - {"json":"..."}`
  (context braces optional)
- Spring Boot default: `2026-10-29T18:34:11.369+05:30  INFO 123 --- [app] [main] c.x.Y : message`
- Tomcat/JUL: `29-Oct-2026 18:34:11.369 INFO [main] org.apache.Foo.bar message`
- Whole-line JSON (`level` field used if present)
- Anything else (banner, Maven/native-image, malformed Log4j status lines) stays plain text, level unknown
  (always shown, never filtered).
- Continuation lines (`\tat ...`, `Caused by:`, `... N more`, bare `x.YException: msg`) attach to the previous event.

## Run/Debug console vs Terminal tabs

- **Run/Debug console:** everything above. Use the run configuration's console (not "Run in terminal").
- **Terminal tab:** **not supported.** The Terminal is a PTY emulator, not the Console API; filters, folding and
  process listeners do not apply to it, so nothing here can transform it. Run the app via a Run/Debug
  configuration (e.g. `--spring.output.ansi.enabled=never` helps keep text clean).
- The plugin never rewrites console output: the original live text stays untouched.

## Build / test / verify

Requires a local IntelliJ IDEA (the build compiles against it) and a **JDK** (not a JRE) as `JAVA_HOME`.
The 2026.2 platform jars are Java 25 class files, so building against 2026.2 needs JDK 25 (the IDE's bundled
JBR works); building against 2025.3 needs 21+. Output is always Java 21 bytecode, so one ZIP loads in both.

```
IDE="/Users/utsav.v/Desktop/IntelliJ IDEA.app"                      # IDEA 2026.2.3 (262.10968.63)
export JAVA_HOME="$IDE/Contents/jbr/Contents/Home"                    # JBR 25 (has javac)
./gradlew "-PideaPath=$IDE" test buildPlugin verifyPlugin runIde
# default (no -PideaPath) is /Applications/IntelliJ IDEA.app (2025.3.3); use a JDK 21+ there:
# export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
# individual tasks:
./gradlew test            # 14 unit tests (parser, JSON, levels, malformed input, stack traces, noise)
./gradlew buildPlugin     # -> build/distributions/intellij-live-log-lens-0.1.0.zip
./gradlew verifyPlugin    # IntelliJ Plugin Verifier against the local IDE (slow, downloads dependencies)
./gradlew runIde          # sandbox IDE with the plugin loaded
```

Verified on IDEA 2025.3.3 (253.31033.145) and 2026.2.3 (262.10968.63). Target: build `253+`, no upper bound set;
re-run `verifyPlugin` after IDE upgrades.
Gradle 9.1 wrapper + IntelliJ Platform Gradle Plugin 2.19.0.

## Install the ZIP

1. IDEA → Settings → Plugins → ⚙ → **Install Plugin from Disk...**
2. Choose `build/distributions/intellij-live-log-lens-0.1.0.zip`, restart when asked.
3. Settings → Tools → **Live Log Lens** (enable, min level, fold noise, max events); tool window *Live Log Lens* (bottom).

## Limitations

- Terminal tabs are out of scope (see above). Processes not started by the IDE are not seen.
- The tool window shows events from IDE-started processes since the IDE opened; in memory only, capped (default 5000).
- JSON validation is structural (balanced brackets/strings), not a full parser.
- Bare `File.java:N` links pick the first project file with that name.
- GUI behaviour (colours, tool window, folding) has been loaded in the sandbox IDE but not exercised by hand.
