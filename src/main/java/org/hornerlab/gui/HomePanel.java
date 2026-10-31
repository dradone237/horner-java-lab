package org.hornerlab.gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.function.Consumer;

/** Accueil : présentation courte et accès direct aux cinq parcours. */
final class HomePanel extends JPanel {

    HomePanel(Consumer<String> navigate) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JLabel title = UiTheme.title("HORNER JAVA LAB", 44);
        title.setForeground(UiTheme.PRIMARY_DARK);
        JLabel subtitle = UiTheme.title("Évaluation et vérification des polynômes", 24);
        subtitle.setForeground(UiTheme.MUTED);
        JLabel intro = UiTheme.text("<html><div style='width:640px'>La méthode de Horner évalue un polynôme de degré n "
                + "avec seulement n multiplications et n additions, en réécrivant "
                + "2x³ − 6x² + 2x − 1 sous la forme ((2x − 6)x + 2)x − 1. Cette application montre chaque étape du calcul, "
                + "la dérivée, la division synthétique, et vérifie chaque résultat avec une méthode indépendante.</div></html>", 18);
        for (JLabel l : new JLabel[] {title, subtitle, intro}) {
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        add(title);
        add(Box.createVerticalStrut(6));
        add(subtitle);
        add(Box.createVerticalStrut(24));
        add(intro);
        add(Box.createVerticalStrut(36));

        JPanel buttons = new JPanel(new GridLayout(2, 3, 14, 14));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(900, 130));
        buttons.add(button("Évaluer", () -> navigate.accept(HornerSwingApp.EVALUATE)));
        buttons.add(button("Voir les étapes", () -> navigate.accept(HornerSwingApp.STEPS)));
        buttons.add(button("Division synthétique", () -> navigate.accept(HornerSwingApp.DIVISION)));
        buttons.add(button("Démonstration", () -> navigate.accept(HornerSwingApp.DEMO)));
        buttons.add(button("Tests et validation", () -> navigate.accept(HornerSwingApp.TESTS)));
        add(buttons);
        add(Box.createVerticalGlue());
    }

    private static JButton button(String text, Runnable action) {
        JButton b = UiTheme.primaryButton(text);
        b.setFont(UiTheme.font(java.awt.Font.BOLD, 17));
        b.addActionListener(e -> action.run());
        return b;
    }
}
