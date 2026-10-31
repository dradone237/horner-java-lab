package org.hornerlab.gui;

import org.hornerlab.core.DivisionResult;
import org.hornerlab.core.PolynomialFormatter;
import org.hornerlab.core.RootStatus;
import org.hornerlab.validation.ResultVerifier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import static org.hornerlab.core.PolynomialFormatter.formatNumber;

/** Division synthétique : schéma de Horner, quotient, reste, identité et statut de racine. */
final class DivisionPanel extends JPanel {

    private final GuiController controller;
    final JTextField rootField = new JTextField("3", 8);
    final JButton divide = UiTheme.primaryButton("Diviser par (x − r)");
    private final DefaultTableModel model = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable scheme = new JTable(model);
    private final JLabel quotient = UiTheme.title(" ", 20);
    private final JLabel remainder = UiTheme.title(" ", 20);
    private final JLabel identity = UiTheme.text(" ", 17);
    private final JLabel status = UiTheme.text(" ", 17);

    DivisionPanel(GuiController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UiTheme.text("r =", 18));
        rootField.setFont(UiTheme.mono(18));
        top.add(rootField);
        top.add(divide);
        add(top, BorderLayout.NORTH);

        UiTheme.styleTable(scheme, 17);
        scheme.setTableHeader(null);
        add(new JScrollPane(scheme), BorderLayout.CENTER);

        JPanel results = new JPanel(new GridLayout(4, 1, 0, 6));
        results.setOpaque(false);
        results.add(quotient);
        results.add(remainder);
        results.add(identity);
        results.add(status);
        add(results, BorderLayout.SOUTH);

        divide.addActionListener(e -> run());
        rootField.addActionListener(e -> run());
        controller.addListener(c -> {
            rootField.setText(formatNumber(c.x()));
            show(c.divisionAtX());
        });
        quotient.setText("Calculez d'abord un polynôme, puis choisissez r.");
    }

    void run() {
        try {
            show(controller.divide(rootField.getText()));
        } catch (RuntimeException ex) {
            quotient.setText(" ");
            remainder.setText(" ");
            identity.setText(" ");
            status.setForeground(UiTheme.ERROR);
            status.setText(ex.getMessage());
        }
    }

    void show(DivisionResult d) {
        List<String[]> rows = GuiController.divisionScheme(d);
        model.setColumnCount(0);
        model.setRowCount(0);
        for (int i = 0; i < rows.get(0).length; i++) {
            model.addColumn("");
        }
        rows.forEach(model::addRow);

        quotient.setText(PolynomialFormatter.abbreviate("Quotient Q(x) = " + PolynomialFormatter.toUnicode(d.quotient())
                + "   " + PolynomialFormatter.formatList(d.quotientCoefficients()), 150));
        remainder.setText("Reste R = " + formatNumber(d.remainder()) + " = P(" + formatNumber(d.root()) + ")");
        ResultVerifier.Check check = ResultVerifier.verifyIdentity(d);
        identity.setForeground(check.passed() ? UiTheme.SUCCESS : UiTheme.ERROR);
        identity.setText("Identité P(x) = " + PolynomialFormatter.formatDivisor(d.root(), true) + "·Q(x) + R : "
                + check.verdict().label() + " (vérifiée par multiplication polynomiale, écart max "
                + formatNumber(check.difference()) + ")");
        RootStatus s = ResultVerifier.classifyRoot(d);
        status.setForeground(s == RootStatus.EXACT_ROOT ? UiTheme.SUCCESS
                : s == RootStatus.APPROXIMATE_ROOT ? UiTheme.WARNING : UiTheme.TEXT);
        status.setText("r = " + formatNumber(d.root()) + " : " + s.label());
    }
}
