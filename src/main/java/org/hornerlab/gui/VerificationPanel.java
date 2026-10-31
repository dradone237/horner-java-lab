package org.hornerlab.gui;

import org.hornerlab.validation.ResultVerifier;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import static org.hornerlab.core.PolynomialFormatter.abbreviate;
import static org.hornerlab.core.PolynomialFormatter.formatNumber;

/** Comparaison Horner / oracle indépendant : identique, proche sous tolérance, incohérent. */
final class VerificationPanel extends JPanel {

    static final String[] COLUMNS = {"Contrôle", "Algorithme", "Référence indépendante", "Écart", "Tolérance", "Verdict"};

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel summary = UiTheme.title(" ", 18);

    VerificationPanel(GuiController controller) {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JTable table = new JTable(model);
        UiTheme.styleTable(table, 15);
        table.getColumnModel().getColumn(5).setCellRenderer(new VerdictRenderer());
        add(summary, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JLabel note = UiTheme.text("<html>Mode exact : égalité stricte exigée. Mode contrôlé : tolérance = borne d'erreur "
                + "a priori de Horner (2·γ(2n)·Σ|a_i||x|^i). La référence n'appelle jamais Horner.</html>", 14);
        note.setForeground(UiTheme.MUTED);
        add(note, BorderLayout.SOUTH);
        summary.setText("Les contrôles s'affichent après un calcul.");
        controller.addListener(c -> show(controller.verify()));
    }

    void show(List<ResultVerifier.Check> checks) {
        model.setRowCount(0);
        long mismatches = 0;
        for (ResultVerifier.Check c : checks) {
            model.addRow(new Object[] {c.name(), abbreviate(formatNumber(c.computed()), 40),
                    abbreviate(formatNumber(c.reference()), 40), formatNumber(c.difference()),
                    formatNumber(c.tolerance()), c.verdict()});
            if (!c.passed()) {
                mismatches++;
            }
        }
        summary.setForeground(mismatches == 0 ? UiTheme.SUCCESS : UiTheme.ERROR);
        summary.setText(mismatches == 0
                ? checks.size() + " contrôles : aucun résultat incohérent"
                : mismatches + " contrôle(s) INCOHÉRENT(S) sur " + checks.size());
    }

    private static final class VerdictRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            ResultVerifier.Verdict v = (ResultVerifier.Verdict) value;
            Component c = super.getTableCellRendererComponent(table, v.label(), selected, focus, row, column);
            Color color = switch (v) {
                case IDENTICAL -> UiTheme.SUCCESS;
                case WITHIN_TOLERANCE -> UiTheme.WARNING;
                case MISMATCH -> UiTheme.ERROR;
            };
            c.setForeground(color);
            c.setFont(c.getFont().deriveFont(Font.BOLD));
            return c;
        }
    }
}
