package dev.personal.livelog;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class LogLensToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Panel panel = new Panel(project);
        Content content = ContentFactory.getInstance().createContent(panel.root, "", false);
        content.setDisposer(panel::dispose);
        toolWindow.getContentManager().addContent(content);
    }

    private static final class Panel {
        private static final String[] COLUMNS = {"Time", "Level", "Logger", "Message"};
        private final LogEventStore store;
        private final List<LogEvent> shown = new ArrayList<>();
        private final JBTable table;
        private final JBTextArea detail = new JBTextArea();
        private final JPanel root = new JPanel(new BorderLayout());
        private final Runnable listener = this::refresh;
        private final AbstractTableModel model = new AbstractTableModel() {
            @Override public int getRowCount() { return shown.size(); }
            @Override public int getColumnCount() { return COLUMNS.length; }
            @Override public String getColumnName(int c) { return COLUMNS[c]; }
            @Override public Object getValueAt(int r, int c) {
                LogEvent e = shown.get(r);
                switch (c) {
                    case 0: return e.timestamp;
                    case 1: return e.level == LogLevel.UNKNOWN ? "" : e.level.name();
                    case 2: return e.logger;
                    default: return e.message + (e.hasStackTrace() ? "  [+" + e.continuation().size() + " lines]" : "");
                }
            }
        };

        Panel(Project project) {
            store = LogEventStore.getInstance(project);
            table = new JBTable(model);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.getColumnModel().getColumn(0).setPreferredWidth(170);
            table.getColumnModel().getColumn(1).setPreferredWidth(60);
            table.getColumnModel().getColumn(2).setPreferredWidth(200);
            table.getColumnModel().getColumn(3).setPreferredWidth(700);
            table.getColumnModel().getColumn(1).setCellRenderer(new LevelRenderer());
            table.getSelectionModel().addListSelectionListener(e -> {
                int row = table.getSelectedRow();
                detail.setText(row >= 0 && row < shown.size() ? shown.get(row).detailText() : "");
                detail.setCaretPosition(0);
            });
            detail.setEditable(false);
            detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, detail.getFont().getSize()));

            ComboBox<LogLevel> levelBox = new ComboBox<>(LogLevel.values());
            levelBox.setSelectedItem(LogLensSettings.getInstance().minLevel());
            levelBox.addActionListener(e -> {
                LogLensSettings.getInstance().setMinLevel((LogLevel) levelBox.getSelectedItem());
                refresh();
            });
            JButton clear = new JButton("Clear");
            clear.addActionListener(e -> store.clear());
            JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            bar.add(new JLabel("Min level:"));
            bar.add(levelBox);
            bar.add(clear);

            JBSplitter splitter = new JBSplitter(true, 0.6f);
            splitter.setFirstComponent(new JBScrollPane(table));
            splitter.setSecondComponent(new JBScrollPane(detail));
            root.add(bar, BorderLayout.NORTH);
            root.add(splitter, BorderLayout.CENTER);

            store.addListener(listener);
            refresh();
        }

        void dispose() { store.removeListener(listener); }

        private void refresh() {
            LogEvent selected = table.getSelectedRow() >= 0 && table.getSelectedRow() < shown.size()
                ? shown.get(table.getSelectedRow()) : null;
            LogLevel min = LogLensSettings.getInstance().minLevel();
            shown.clear();
            for (LogEvent e : store.snapshot()) {
                if (e.level == LogLevel.UNKNOWN || e.level.compareTo(min) >= 0) shown.add(e);
            }
            boolean atBottom = selected == null;
            model.fireTableDataChanged();
            int sel = selected == null ? -1 : shown.indexOf(selected);
            if (sel >= 0) table.setRowSelectionInterval(sel, sel);
            else if (atBottom && !shown.isEmpty()) table.scrollRectToVisible(table.getCellRect(shown.size() - 1, 0, true));
        }
    }

    private static final class LevelRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            Component comp = super.getTableCellRendererComponent(t, v, sel, focus, r, c);
            if (!sel) {
                if ("ERROR".equals(v)) comp.setForeground(new JBColor(new Color(0xB00020), new Color(0xFF6B68)));
                else if ("WARN".equals(v)) comp.setForeground(new JBColor(new Color(0x9A6700), new Color(0xE5B567)));
                else comp.setForeground(t.getForeground());
            }
            return comp;
        }
    }
}
