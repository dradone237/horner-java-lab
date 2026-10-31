package org.hornerlab.gui;

import org.hornerlab.core.PolynomialFormatter;
import org.hornerlab.core.RootStatus;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Locale;

import static org.hornerlab.core.PolynomialFormatter.formatNumber;

/** Résolution : polynôme, x, P(x), P'(x), nombre d'opérations, durée indicative. */
final class EvaluationPanel extends JPanel {

    private final JLabel polynomial = value(22);
    private final JLabel nested = value(18);
    private final JLabel x = value(20);
    private final JLabel result = value(34);
    private final JLabel derivative = value(22);
    private final JLabel operations = value(17);
    private final JLabel duration = value(17);
    private final JLabel mode = value(17);
    private final JLabel root = value(17);

    EvaluationPanel(GuiController controller) {
        setLayout(new GridBagLayout());
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        result.setForeground(UiTheme.PRIMARY);
        addRow(0, "Polynôme", polynomial);
        addRow(1, "Forme de Horner", nested);
        addRow(2, "Valeur de x", x);
        addRow(3, "Résultat", result);
        addRow(4, "Dérivée", derivative);
        addRow(5, "Opérations", operations);
        addRow(6, "Durée indicative", duration);
        addRow(7, "Mode de calcul", mode);
        addRow(8, "Racine ?", root);
        result.setText("Saisissez un polynôme puis cliquez sur « Calculer ».");
        controller.addListener(this::show);
    }

    private void show(GuiController.Computation c) {
        String xs = formatNumber(c.x());
        polynomial.setText(PolynomialFormatter.abbreviate("P(x) = " + PolynomialFormatter.toUnicode(c.polynomial()), 140));
        nested.setText(PolynomialFormatter.abbreviate("P(x) = " + PolynomialFormatter.nestedForm(c.polynomial(), true), 140));
        x.setText("x = " + xs);
        result.setText(PolynomialFormatter.abbreviate("P(" + xs + ") = " + formatNumber(c.horner().value()), 90));
        derivative.setText(PolynomialFormatter.abbreviate("P'(" + xs + ") = " + formatNumber(c.derivative().derivative()), 90));
        operations.setText(c.horner().multiplications() + " multiplications + " + c.horner().additions()
                + " additions (Horner, degré " + Math.max(0, c.polynomial().degree()) + ")");
        duration.setText(String.format(Locale.ROOT, "%.3f ms (System.nanoTime, une exécution : valeur indicative)",
                c.elapsedNanos() / 1e6));
        mode.setText(c.settings().describe());
        RootStatus status = c.rootStatus();
        root.setForeground(status == RootStatus.NOT_A_ROOT ? UiTheme.TEXT
                : status == RootStatus.EXACT_ROOT ? UiTheme.SUCCESS : UiTheme.WARNING);
        root.setText(xs + " : " + status.label());
    }

    private void addRow(int row, String label, JLabel value) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(6, 0, 6, 24);
        JLabel l = UiTheme.text(label, 16);
        l.setForeground(UiTheme.MUTED);
        add(l, g);
        g.gridx = 1;
        g.weightx = 1;
        add(value, g);
    }

    private static JLabel value(float size) {
        JLabel l = UiTheme.text(" ", size);
        l.setFont(UiTheme.font(Font.BOLD, size));
        return l;
    }
}
