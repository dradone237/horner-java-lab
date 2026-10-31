package org.hornerlab.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;

/** Couleurs, polices et composants stylés communs (lisibles au vidéoprojecteur). */
final class UiTheme {

    static final Color BACKGROUND = new Color(0xF4F6FB);
    static final Color SURFACE = Color.WHITE;
    static final Color PRIMARY = new Color(0x1F4FB8);
    static final Color PRIMARY_DARK = new Color(0x163A8A);
    static final Color TEXT = new Color(0x1D2433);
    static final Color MUTED = new Color(0x5B6475);
    static final Color SUCCESS = new Color(0x1E8E5A);
    static final Color WARNING = new Color(0xB86E00);
    static final Color ERROR = new Color(0xC0392B);
    static final Color BORDER = new Color(0xD8DEEA);
    static final Color HIGHLIGHT = new Color(0xE3ECFF);

    static final String FAMILY = "Segoe UI";
    static final String MONO = "Consolas";

    private UiTheme() {
    }

    static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Le look and feel par défaut de Swing reste utilisable.
        }
    }

    static Font font(int style, float size) {
        return new Font(FAMILY, style, Math.round(size));
    }

    static Font mono(float size) {
        return new Font(MONO, Font.PLAIN, Math.round(size));
    }

    static JLabel title(String text, float size) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.BOLD, size));
        label.setForeground(TEXT);
        return label;
    }

    static JLabel text(String text, float size) {
        JLabel label = new JLabel(text);
        label.setFont(font(Font.PLAIN, size));
        label.setForeground(TEXT);
        return label;
    }

    static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.BOLD, 16));
        b.setForeground(Color.WHITE);
        b.setBackground(PRIMARY);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(font(Font.PLAIN, 15));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    static Border card() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    static void styleTable(JTable table, float size) {
        table.setFont(mono(size));
        table.getTableHeader().setFont(font(Font.BOLD, size));
        table.setRowHeight(Math.round(size * 1.9f));
        table.setGridColor(BORDER);
        table.setSelectionBackground(HIGHLIGHT);
        table.setSelectionForeground(TEXT);
    }

    /** Agrandit ou réduit toutes les polices d'un arbre de composants (boutons A+ / A−). */
    static void scaleFonts(Component root, float factor) {
        Font f = root.getFont();
        if (f != null) {
            root.setFont(f.deriveFont(Math.max(9f, f.getSize2D() * factor)));
        }
        if (root instanceof JTable table) {
            table.setRowHeight(Math.max(16, Math.round(table.getRowHeight() * factor)));
            scaleFonts(table.getTableHeader(), factor);
        }
        if (root instanceof Container container) {
            for (Component child : container.getComponents()) {
                scaleFonts(child, factor);
            }
        }
        if (root instanceof JComponent jc) {
            jc.revalidate();
        }
    }
}
