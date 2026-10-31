package org.hornerlab;

import org.hornerlab.core.NumericSettings;
import org.hornerlab.core.RootStatus;
import org.hornerlab.gui.GuiController;
import org.hornerlab.gui.GuiTestAccess;
import org.hornerlab.validation.InvalidInputException;
import org.hornerlab.validation.ResultVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de la logique de l'interface graphique, exécutés sans affichage (java.awt.headless=true).
 * Aucune fenêtre n'est ouverte : seuls le contrôleur et des panneaux Swing non affichés sont utilisés.
 */
class GuiControllerTest {

    private static final List<String> REF = List.of("2", "-6", "2", "-1");

    @Test
    @DisplayName("Les tests s'exécutent en mode sans affichage")
    void runsHeadless() {
        assertTrue(GraphicsEnvironment.isHeadless());
    }

    @Test
    @DisplayName("Calcul depuis la GUI : valeurs du moteur réel et notification des écrans")
    void computeNotifiesListeners() {
        GuiController controller = new GuiController();
        List<GuiController.Computation> received = new ArrayList<>();
        controller.addListener(received::add);
        GuiController.Computation c = controller.compute(REF, "3", NumericSettings.exact());
        assertEquals(1, received.size());
        assertEquals(0, c.horner().value().intValueExact() - 5);
        assertEquals(20, c.derivative().derivative().intValueExact());
        assertEquals(RootStatus.NOT_A_ROOT, c.rootStatus());
        assertTrue(c.elapsedNanos() >= 0);
        assertTrue(controller.current().isPresent());
        assertTrue(controller.verify().stream().allMatch(ResultVerifier.Check::passed));
        assertEquals("[2, 0, 2]", org.hornerlab.core.PolynomialFormatter.formatList(controller.divide("3").quotientCoefficients()));
    }

    @Test
    @DisplayName("Saisie invalide : message précis, aucun calcul effectué")
    void invalidInput() {
        GuiController controller = new GuiController();
        InvalidInputException e = assertThrows(InvalidInputException.class,
                () -> controller.compute(List.of("2", "abc"), "3", NumericSettings.exact()));
        assertEquals("coefficient a0", e.field());
        assertThrows(InvalidInputException.class, () -> controller.compute(REF, "1,5", NumericSettings.exact()));
        assertTrue(controller.current().isEmpty());
        assertThrows(IllegalStateException.class, controller::verify);
        assertThrows(IllegalStateException.class, () -> controller.divide("1"));
    }

    @Test
    @DisplayName("Formule en direct : affichée si valide, absente sinon")
    void formulaPreview() {
        GuiController controller = new GuiController();
        assertEquals("P(x) = 2x³ − 6x² + 2x − 1", controller.formulaPreview(REF).orElseThrow());
        assertTrue(controller.formulaPreview(List.of("2", "")).isEmpty());
    }

    @Test
    @DisplayName("Tableau des étapes : Étape | Coefficient | Acc. avant | Multiplication | Addition | Acc. après")
    void stepRows() {
        GuiController.Computation c = new GuiController().compute(REF, "3", NumericSettings.exact());
        List<String[]> rows = GuiController.stepRows(c.horner());
        assertEquals(4, rows.size());
        assertArrayEquals(new String[] {"1", "2", "—", "—", "—", "2"}, rows.get(0));
        assertArrayEquals(new String[] {"2", "-6", "2", "2 × 3 = 6", "6 + (-6) = 0", "0"}, rows.get(1));
        assertArrayEquals(new String[] {"4", "-1", "2", "2 × 3 = 6", "6 + (-1) = 5", "5"}, rows.get(3));
        assertTrue(GuiController.explainStep(c.horner(), 0).contains("coefficient dominant"));
        assertTrue(GuiController.explainStep(c.horner(), 3).contains("→ 5"));
    }

    @Test
    @DisplayName("Schéma de division : coefficients, produits, sommes (Q puis R)")
    void divisionScheme() {
        GuiController controller = new GuiController();
        controller.compute(REF, "3", NumericSettings.exact());
        List<String[]> scheme = GuiController.divisionScheme(controller.divide("3"));
        assertArrayEquals(new String[] {"Coefficients de P", "2", "-6", "2", "-1"}, scheme.get(0));
        assertArrayEquals(new String[] {"× r = 3", "", "6", "0", "6"}, scheme.get(1));
        assertArrayEquals(new String[] {"Sommes (Q puis R)", "2", "0", "2", "5  = R"}, scheme.get(2));
    }

    @Test
    @DisplayName("Libellés des champs de coefficients")
    void fieldLabels() {
        assertEquals("a₀ (constante)", GuiController.fieldLabel(0));
        assertEquals("a₁ (x)", GuiController.fieldLabel(1));
        assertEquals("a₃ (x³)", GuiController.fieldLabel(3));
        assertEquals("a₁₂ (x¹²)", GuiController.fieldLabel(12));
    }

    @Test
    @DisplayName("Mode soutenance : 8 étapes, valeurs issues du moteur (P(3) = 5, P'(3) = 20, R = 5)")
    void demonstrationScript() {
        List<String> slides = GuiTestAccess.slideTexts();
        assertEquals(8, slides.size());
        assertTrue(slides.get(2).contains("Étape 2 : 2 × 3 + (-6) = 0"), slides.get(2));
        assertTrue(slides.get(2).contains("Résultat : P(3) = 5"), slides.get(2));
        assertTrue(slides.get(3).contains("Identique"), slides.get(3));
        assertTrue(slides.get(4).contains("[2, 0, 2]") && slides.get(4).contains("Reste R = 5"), slides.get(4));
        assertTrue(slides.get(5).contains("P'(3) = 20"), slides.get(5));
        assertFalse(slides.get(6).contains("ÉCHEC"), slides.get(6));
        assertFalse(slides.get(6).contains("anomalie"), slides.get(6));
    }

    @Test
    @DisplayName("Panneaux Swing non affichés : navigation des étapes et du mode soutenance")
    void swingPanelsHeadless() throws Exception {
        GuiTestAccess.Result r = GuiTestAccess.exercisePanels();
        assertEquals(1, r.stepsAfterLoad());
        assertEquals(3, r.stepsAfterTwoNext());
        assertEquals(2, r.stepsAfterPrevious());
        assertEquals(1, r.stepsAfterRestart());
        assertEquals(8, r.demoSlides());
        assertEquals(7, r.demoLastSlideIndex());
        assertEquals(0, r.demoAfterRestart());
        assertTrue(r.divisionTextOk());
        assertTrue(r.verificationRows() == 7);
    }

    @Test
    @DisplayName("Chargement d'un exemple dans la grille : coefficients du fichier d'exemples")
    void loadExampleIntoGrid() throws Exception {
        assertEquals(List.of("1", "-6", "11", "-6"), GuiTestAccess.loadExampleTexts(1));
    }
}
