package org.hornerlab.gui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * MODE SOUTENANCE : parcours de 8 diapositives pour une présentation de cinq minutes.
 * Navigation : boutons ou clavier (→/Espace suivant, ← précédent, Début recommencer, F11 plein écran).
 */
final class DemonstrationPanel extends JPanel {

    private final List<DemonstrationScript.Slide> slides = DemonstrationScript.build();
    private final JLabel counter = UiTheme.text(" ", 16);
    private final JLabel title = UiTheme.title(" ", 34);
    private final JLabel lead = UiTheme.title(" ", 28);
    private final JLabel body = UiTheme.text(" ", 22);
    final JButton previous = UiTheme.secondaryButton("← Précédent");
    final JButton next = UiTheme.primaryButton("Suivant →");
    final JButton restart = UiTheme.secondaryButton("Recommencer");
    final JButton fullScreen = UiTheme.secondaryButton("Plein écran (F11)");

    private int slide;
    private int revealed;

    DemonstrationPanel(Runnable toggleFullScreen) {
        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(28, 48, 24, 48));
        setFocusable(true);

        lead.setForeground(UiTheme.PRIMARY);
        body.setVerticalAlignment(JLabel.TOP);
        body.setFont(UiTheme.font(Font.PLAIN, 22));
        JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel badge = UiTheme.title("MODE SOUTENANCE", 16);
        badge.setForeground(UiTheme.PRIMARY);
        titleRow.add(badge, BorderLayout.WEST);
        titleRow.add(counter, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);
        header.add(title, BorderLayout.CENTER);
        header.add(lead, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttons.setOpaque(false);
        buttons.add(previous);
        buttons.add(next);
        buttons.add(restart);
        buttons.add(fullScreen);
        add(buttons, BorderLayout.SOUTH);

        previous.addActionListener(e -> previous());
        next.addActionListener(e -> next());
        restart.addActionListener(e -> restart());
        fullScreen.addActionListener(e -> toggleFullScreen.run());
        bind(KeyEvent.VK_RIGHT, "next", this::next);
        bind(KeyEvent.VK_SPACE, "next", this::next);
        bind(KeyEvent.VK_PAGE_DOWN, "next", this::next);
        bind(KeyEvent.VK_LEFT, "previous", this::previous);
        bind(KeyEvent.VK_PAGE_UP, "previous", this::previous);
        bind(KeyEvent.VK_HOME, "restart", this::restart);
        restart();
    }

    int slideIndex() {
        return slide;
    }

    int revealedItems() {
        return revealed;
    }

    int slideCount() {
        return slides.size();
    }

    void next() {
        DemonstrationScript.Slide s = slides.get(slide);
        if (s.progressive() && revealed < s.items().size()) {
            revealed++;
        } else if (slide < slides.size() - 1) {
            slide++;
            revealed = slides.get(slide).progressive() ? 0 : slides.get(slide).items().size();
        }
        render();
    }

    void previous() {
        if (slide > 0) {
            slide--;
            revealed = slides.get(slide).items().size();
        }
        render();
    }

    void restart() {
        slide = 0;
        revealed = slides.get(0).progressive() ? 0 : slides.get(0).items().size();
        render();
    }

    private void render() {
        DemonstrationScript.Slide s = slides.get(slide);
        counter.setText("Étape " + (slide + 1) + " / " + slides.size());
        title.setText(s.title());
        lead.setText(s.lead());
        StringBuilder html = new StringBuilder("<html><div style='line-height:1.5'>");
        for (int i = 0; i < revealed; i++) {
            html.append("• ").append(escape(s.items().get(i))).append("<br><br>");
        }
        if (s.progressive() && revealed < s.items().size()) {
            html.append("<span style='color:#8A93A5'>(Suivant pour continuer…)</span>");
        }
        body.setText(html.append("</div></html>").toString());
        previous.setEnabled(slide > 0);
        next.setEnabled(slide < slides.size() - 1 || revealed < s.items().size());
    }

    private void bind(int key, String name, Runnable action) {
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), name);
        getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
