package org.hornerlab.gui;

import org.hornerlab.core.HornerResult;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** Tableau des étapes de Horner, révélées une à une (Précédent, Suivant, Recommencer, Exécution automatique). */
final class StepsPanel extends JPanel {

    static final String[] COLUMNS = {"Étape", "Coefficient", "Accumulateur avant", "Multiplication", "Addition",
            "Accumulateur après"};

    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel explanation = UiTheme.text(" ", 18);
    final JButton previous = UiTheme.secondaryButton("← Précédent");
    final JButton next = UiTheme.secondaryButton("Suivant →");
    final JButton restart = UiTheme.secondaryButton("Recommencer");
    final JButton auto = UiTheme.primaryButton("Exécution automatique");
    private final Timer timer = new Timer(1000, e -> advance());

    private HornerResult result;
    private List<String[]> rows = List.of();
    private int visible;

    StepsPanel(GuiController controller) {
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        UiTheme.styleTable(table, 17);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.add(previous);
        buttons.add(next);
        buttons.add(restart);
        buttons.add(auto);
        JPanel south = new JPanel(new BorderLayout(0, 8));
        south.setOpaque(false);
        south.add(explanation, BorderLayout.NORTH);
        south.add(buttons, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        previous.addActionListener(e -> show(visible - 1));
        next.addActionListener(e -> advance());
        restart.addActionListener(e -> {
            timer.stop();
            auto.setText("Exécution automatique");
            show(1);
        });
        auto.addActionListener(e -> toggleAuto());
        controller.addListener(c -> load(c.horner()));
        explanation.setText("Les étapes apparaîtront ici après un calcul.");
        updateButtons();
    }

    void load(HornerResult r) {
        timer.stop();
        auto.setText("Exécution automatique");
        result = r;
        rows = GuiController.stepRows(r);
        show(1);
    }

    int visibleRows() {
        return visible;
    }

    private void advance() {
        if (visible >= rows.size()) {
            timer.stop();
            auto.setText("Exécution automatique");
            return;
        }
        show(visible + 1);
    }

    private void toggleAuto() {
        if (timer.isRunning()) {
            timer.stop();
            auto.setText("Exécution automatique");
        } else if (result != null) {
            if (visible >= rows.size()) {
                show(1);
            }
            auto.setText("Pause");
            timer.start();
        }
    }

    private void show(int count) {
        if (result == null) {
            return;
        }
        visible = Math.max(1, Math.min(rows.size(), count));
        model.setRowCount(0);
        for (int i = 0; i < visible; i++) {
            model.addRow(rows.get(i));
        }
        table.setRowSelectionInterval(visible - 1, visible - 1);
        String text = GuiController.explainStep(result, visible - 1);
        if (visible == rows.size()) {
            text += "  Résultat final : " + rows.get(visible - 1)[5] + ".";
        }
        explanation.setText(text);
        updateButtons();
    }

    private void updateButtons() {
        previous.setEnabled(result != null && visible > 1);
        next.setEnabled(result != null && visible < rows.size());
        restart.setEnabled(result != null);
        auto.setEnabled(result != null);
    }
}
