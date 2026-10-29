package dev.personal.livelog;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Highlights log levels and links bare {@code File.java:123} locations in Run/Debug consoles. */
public final class LogLensConsoleFilterProvider implements ConsoleFilterProvider {
    @Override
    public Filter @NotNull [] getDefaultFilters(@NotNull Project project) {
        return new Filter[]{new LogLensFilter(project)};
    }

    static final class LogLensFilter implements Filter {
        private static final TextAttributes ERROR = new TextAttributes(
            new JBColor(new Color(0xB00020), new Color(0xFF6B68)), null, null, EffectType.BOXED, Font.BOLD);
        private static final TextAttributes WARN = new TextAttributes(
            new JBColor(new Color(0x9A6700), new Color(0xE5B567)), null, null, EffectType.BOXED, Font.BOLD);

        private final Project project;

        LogLensFilter(Project project) { this.project = project; }

        @Override
        public @Nullable Result applyFilter(@NotNull String line, int entireLength) {
            if (!LogLensSettings.getInstance().enabled()) return null;
            int base = entireLength - line.length();
            List<ResultItem> items = new ArrayList<>();

            int[] span = LogParser.findLevelSpan(line);
            if (span != null) {
                LogLevel level = LogLevel.parse(line.substring(span[0], span[1]));
                TextAttributes attrs = level == LogLevel.ERROR ? ERROR : level == LogLevel.WARN ? WARN : null;
                if (attrs != null) items.add(new ResultItem(base + span[0], base + span[1], null, attrs));
            }
            for (LogParser.SourceLocation loc : LogParser.findSourceLocations(line)) {
                items.add(new ResultItem(base + loc.start(), base + loc.end(), new LazySourceLink(loc)));
            }
            return items.isEmpty() ? null : new Result(items);
        }
    }

    /** Resolves the file only on click, so the filter never touches indices off-thread. */
    private record LazySourceLink(LogParser.SourceLocation loc) implements HyperlinkInfo {
        @Override
        public void navigate(@NotNull Project project) {
            Collection<VirtualFile> files = com.intellij.openapi.application.ReadAction.computeBlocking(() ->
                FilenameIndex.getVirtualFilesByName(loc.fileName(), GlobalSearchScope.projectScope(project)));
            if (files.isEmpty()) return;
            VirtualFile file = files.iterator().next();
            new OpenFileHyperlinkInfo(project, file, Math.max(0, loc.line() - 1)).navigate(project);
        }
    }
}
