package org.hornerlab.gui;

import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.PolynomialFormatter;
import org.hornerlab.examples.ExampleLibrary;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Saisie du polynôme : exemples, degré, grille dynamique de coefficients, valeur de x et mode de calcul.
 * La formule mathématique est reconstruite en direct à partir des coefficients.
 */
final class InputPanel extends JPanel {

    static final int MAX_DEGREE = 30;

    private final GuiController controller;
    private final JSpinner degree = new JSpinner(new SpinnerNumberModel(3, 0, MAX_DEGREE, 1));
    private final JPanel grid = new JPanel();
    private final List<JTextField> fields = new ArrayList<>();
    private final JTextField xField = new JTextField("3", 8);
    private final JComboBox<String> mode = new JComboBox<>(new String[] {"Exact", "Précision contrôlée"});
    private final JSpinner precision = new JSpinner(new SpinnerNumberModel(34, 1, NumericSettings.MAX_PRECISION, 1));
    private final JLabel formula = UiTheme.title("", 22);
    private final JLabel message = UiTheme.text(" ", 15);
    private final JButton compute = UiTheme.primaryButton("Calculer");

    InputPanel(GuiController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 8));
        setBackground(UiTheme.SURFACE);
        setBorder(UiTheme.card());

        JComboBox<ExampleLibrary.Example> examples = new JComboBox<>(ExampleLibrary.load().toArray(ExampleLibrary.Example[]::new));
        examples.setFont(UiTheme.font(Font.PLAIN, 15));
        JButton load = UiTheme.secondaryButton("Charger l'exemple");
        load.addActionListener(e -> loadExample((ExampleLibrary.Example) examples.getSelectedItem()));

        JPanel top = row();
        top.add(UiTheme.text("Exemples :", 15));
        top.add(examples);
        top.add(load);
        top.add(Box.createHorizontalStrut(20));
        top.add(UiTheme.text("Degré :", 15));
        degree.setFont(UiTheme.font(Font.PLAIN, 16));
        top.add(degree);
        degree.addChangeListener(e -> rebuildGrid((Integer) degree.getValue(), currentTexts()));

        grid.setBackground(UiTheme.SURFACE);
        JScrollPane gridScroll = new JScrollPane(grid, JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        gridScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel bottom = row();
        bottom.add(UiTheme.text("x =", 18));
        xField.setFont(UiTheme.mono(18));
        bottom.add(xField);
        bottom.add(Box.createHorizontalStrut(16));
        bottom.add(UiTheme.text("Mode :", 15));
        mode.setFont(UiTheme.font(Font.PLAIN, 15));
        bottom.add(mode);
        bottom.add(UiTheme.text("chiffres :", 15));
        precision.setEnabled(false);
        bottom.add(precision);
        mode.addActionListener(e -> precision.setEnabled(mode.getSelectedIndex() == 1));
        bottom.add(Box.createHorizontalStrut(16));
        bottom.add(compute);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(gridScroll);
        center.add(bottom);
        JPanel status = new JPanel(new GridLayout(2, 1));
        status.setOpaque(false);
        status.add(formula);
        status.add(message);
        center.add(status);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        rebuildGrid(3, List.of("2", "-6", "2", "-1"));
        xField.getDocument().addDocumentListener(onChange());
    }

    JButton computeButton() {
        return compute;
    }

    /** Coefficients saisis, du dominant au terme constant. */
    List<String> currentTexts() {
        return fields.stream().map(JTextField::getText).toList();
    }

    String xText() {
        return xField.getText();
    }

    NumericSettings settings() {
        return mode.getSelectedIndex() == 0 ? NumericSettings.exact()
                : NumericSettings.controlled((Integer) precision.getValue());
    }

    void showMessage(String text, boolean error) {
        message.setForeground(error ? UiTheme.ERROR : UiTheme.SUCCESS);
        message.setText(text);
    }

    void loadExample(ExampleLibrary.Example example) {
        if (example == null) {
            return;
        }
        List<String> texts = example.polynomial().coefficients().stream().map(PolynomialFormatter::formatNumber).toList();
        degree.setValue(Math.min(MAX_DEGREE, texts.size() - 1));
        rebuildGrid(texts.size() - 1, texts);
        xField.setText(PolynomialFormatter.formatNumber(example.x()));
        showMessage(example.description(), false);
    }

    void setCoefficients(List<String> texts, BigDecimal x) {
        degree.setValue(texts.size() - 1);
        rebuildGrid(texts.size() - 1, texts);
        xField.setText(PolynomialFormatter.formatNumber(x));
    }

    private void rebuildGrid(int deg, List<String> previous) {
        grid.removeAll();
        fields.clear();
        grid.setLayout(new GridLayout(2, deg + 1, 8, 4));
        for (int i = 0; i <= deg; i++) {
            grid.add(UiTheme.text(GuiController.fieldLabel(deg - i), 14));
        }
        for (int i = 0; i <= deg; i++) {
            // Conserve les valeurs alignées sur le terme constant quand le degré change.
            int fromEnd = deg - i;
            int index = previous.size() - 1 - fromEnd;
            JTextField f = new JTextField(index >= 0 ? previous.get(index) : "0", 6);
            f.setFont(UiTheme.mono(18));
            f.getDocument().addDocumentListener(onChange());
            fields.add(f);
            grid.add(f);
        }
        grid.revalidate();
        grid.repaint();
        refreshFormula();
    }

    private void refreshFormula() {
        controller.formulaPreview(currentTexts()).ifPresentOrElse(text -> {
            formula.setForeground(UiTheme.TEXT);
            formula.setText(text);
        }, () -> {
            formula.setForeground(UiTheme.ERROR);
            formula.setText("Saisie incomplète ou invalide (le point est le séparateur décimal)");
        });
    }

    private DocumentListener onChange() {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshFormula();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshFormula();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshFormula();
            }
        };
    }

    private static JPanel row() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        return p;
    }
}
