package org.hornerlab.gui;

import org.hornerlab.core.NumericSettings;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Accès de test aux panneaux (package-private) sans afficher de fenêtre.
 * Les manipulations Swing sont faites sur l'EDT, en mode headless.
 */
public final class GuiTestAccess {

    private GuiTestAccess() {
    }

    /** Observations faites sur les panneaux. */
    public record Result(int stepsAfterLoad, int stepsAfterTwoNext, int stepsAfterPrevious, int stepsAfterRestart,
                         int demoSlides, int demoLastSlideIndex, int demoAfterRestart, boolean divisionTextOk,
                         int verificationRows) {
    }

    /** Texte complet (titre, accroche, éléments) de chaque diapositive du mode soutenance. */
    public static List<String> slideTexts() {
        return DemonstrationScript.build().stream()
                .map(s -> s.title() + "\n" + s.lead() + "\n" + String.join("\n", s.items()))
                .toList();
    }

    public static Result exercisePanels() throws Exception {
        AtomicReference<Result> result = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            GuiController controller = new GuiController();
            InputPanel input = new InputPanel(controller);
            new EvaluationPanel(controller);
            StepsPanel steps = new StepsPanel(controller);
            DivisionPanel division = new DivisionPanel(controller);
            VerificationPanel verification = new VerificationPanel(controller);
            new ValidationPanel(controller);
            new HomePanel(destination -> { });
            if (!input.currentTexts().equals(List.of("2", "-6", "2", "-1"))) {
                throw new AssertionError("Grille initiale inattendue : " + input.currentTexts());
            }
            controller.compute(input.currentTexts(), input.xText(), input.settings());

            int afterLoad = steps.visibleRows();
            steps.next.doClick(0);
            steps.next.doClick(0);
            int afterTwoNext = steps.visibleRows();
            steps.previous.doClick(0);
            int afterPrevious = steps.visibleRows();
            steps.restart.doClick(0);
            int afterRestart = steps.visibleRows();

            DemonstrationPanel demo = new DemonstrationPanel(() -> { });
            for (int i = 0; i < 100; i++) {
                demo.next();
            }
            int last = demo.slideIndex();
            demo.previous();
            demo.restart();
            int afterDemoRestart = demo.slideIndex();

            division.rootField.setText("3");
            division.run();
            boolean divisionOk = labels(division).stream().anyMatch(t -> t.contains("Reste R = 5"))
                    && labels(division).stream().anyMatch(t -> t.contains("[2, 0, 2]"));
            division.rootField.setText("abc");
            division.run();
            divisionOk &= labels(division).stream().anyMatch(t -> t.contains("abc"));

            int rows = tables(verification).get(0).getRowCount();
            result.set(new Result(afterLoad, afterTwoNext, afterPrevious, afterRestart, demo.slideCount(), last,
                    afterDemoRestart, divisionOk, rows));
        });
        return result.get();
    }

    /** Charge un exemple dans la grille de saisie et renvoie les textes des champs. */
    public static List<String> loadExampleTexts(int index) throws Exception {
        AtomicReference<List<String>> texts = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            InputPanel input = new InputPanel(new GuiController());
            input.loadExample(org.hornerlab.examples.ExampleLibrary.load().get(index));
            texts.set(input.currentTexts());
            input.setCoefficients(List.of("1", "0", "-1"), java.math.BigDecimal.ONE);
            if (!input.settings().isExact() || NumericSettings.exact().precision() != 0) {
                throw new AssertionError("mode par défaut inattendu");
            }
        });
        return texts.get();
    }

    private static List<String> labels(Container root) {
        List<String> texts = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l) {
                texts.add(l.getText());
            }
            if (c instanceof Container child) {
                texts.addAll(labels(child));
            }
        }
        return texts;
    }

    private static List<JTable> tables(Container root) {
        List<JTable> found = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JTable t) {
                found.add(t);
            }
            if (c instanceof Container child) {
                found.addAll(tables(child));
            }
        }
        return found;
    }
}
