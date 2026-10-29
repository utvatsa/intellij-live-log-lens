package dev.personal.livelog;

import com.intellij.execution.ConsoleFolding;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Optionally folds runs of known bootstrap noise (off by default). */
public final class LogLensConsoleFolding extends ConsoleFolding {
    @Override
    public boolean shouldFoldLine(@NotNull Project project, @NotNull String line) {
        LogLensSettings s = LogLensSettings.getInstance();
        return s.enabled() && s.foldNoise() && NoisePatterns.isNoise(line);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull Project project, @NotNull List<String> lines) {
        return "... " + lines.size() + " bootstrap line(s) folded by Live Log Lens ...";
    }
}
