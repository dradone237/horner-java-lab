package org.hornerlab.gui;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.AWTError;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;

/** Fenêtre principale « HORNER JAVA LAB ». */
public final class HornerSwingApp extends JFrame {

    static final String HOME = "home";
    static final String EVALUATE = "evaluate";
    static final String STEPS = "steps";
    static final String DIVISION = "division";
    static final String VERIFY = "verify";
    static final String DEMO = "demo";
    static final String TESTS = "tests";

    private final CardLayout cards = new CardLayout();
    private final JPanel center = new JPanel(cards);
    private final JTabbedPane tabs = new JTabbedPane();
    private final DemonstrationPanel demo;
    private boolean fullScreen;
    private Rectangle normalBounds;

    HornerSwingApp() {
        super("HORNER JAVA LAB — Évaluation et vérification des polynômes");
        GuiController controller = new GuiController();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        getContentPane().setBackground(UiTheme.BACKGROUND);
        setLayout(new BorderLayout());

        InputPanel input = new InputPanel(controller);
        input.computeButton().addActionListener(e -> {
            try {
                controller.compute(input.currentTexts(), input.xText(), input.settings());
                input.showMessage("Calcul effectué par le moteur Java (Horner).", false);
            } catch (RuntimeException ex) {
                input.showMessage(ex.getMessage(), true);
            }
        });
        tabs.setFont(UiTheme.font(Font.BOLD, 16));
        tabs.addTab("Résolution", new EvaluationPanel(controller));
        tabs.addTab("Étapes", new StepsPanel(controller));
        tabs.addTab("Division synthétique", new DivisionPanel(controller));
        tabs.addTab("Vérification", new VerificationPanel(controller));
        JPanel work = new JPanel(new BorderLayout(0, 10));
        work.setBackground(UiTheme.BACKGROUND);
        work.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        work.add(input, BorderLayout.NORTH);
        work.add(tabs, BorderLayout.CENTER);

        demo = new DemonstrationPanel(this::toggleFullScreen);
        center.add(new HomePanel(this::navigate), HOME);
        center.add(work, "work");
        center.add(demo, DEMO);
        center.add(new ValidationPanel(controller), TESTS);

        add(header(), BorderLayout.NORTH);
        add(navigation(), BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), "fullscreen");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "leave");
        getRootPane().getActionMap().put("fullscreen", action(this::toggleFullScreen));
        getRootPane().getActionMap().put("leave", action(() -> {
            if (fullScreen) {
                toggleFullScreen();
            }
        }));

        // Premier calcul avec l'exemple de référence pour que tous les onglets soient remplis.
        controller.compute(input.currentTexts(), input.xText(), input.settings());
        pack();
        setSize(1280, 820);
        setLocationRelativeTo(null);
    }

    /**
     * Ouvre la fenêtre si un affichage est disponible.
     *
     * @return {@code false} en environnement sans affichage (aucune exception graphique n'est propagée)
     */
    public static boolean launchIfPossible() {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                UiTheme.installLookAndFeel();
                new HornerSwingApp().setVisible(true);
            });
            return true;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof HeadlessException || e.getCause() instanceof AWTError) {
                return false;
            }
            throw new IllegalStateException("Échec du démarrage de l'interface graphique", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    void navigate(String destination) {
        switch (destination) {
            case EVALUATE -> showWork(0);
            case STEPS -> showWork(1);
            case DIVISION -> showWork(2);
            case VERIFY -> showWork(3);
            case DEMO -> {
                cards.show(center, DEMO);
                demo.requestFocusInWindow();
            }
            default -> cards.show(center, destination);
        }
    }

    private void showWork(int tab) {
        cards.show(center, "work");
        tabs.setSelectedIndex(tab);
    }

    private JPanel header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.PRIMARY_DARK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel title = new JLabel("HORNER JAVA LAB");
        title.setFont(UiTheme.font(Font.BOLD, 24));
        title.setForeground(java.awt.Color.WHITE);
        JLabel subtitle = new JLabel("   Évaluation et vérification des polynômes");
        subtitle.setFont(UiTheme.font(Font.PLAIN, 16));
        subtitle.setForeground(new java.awt.Color(0xC9D6F5));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(title);
        left.add(subtitle);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        JButton smaller = UiTheme.secondaryButton("A−");
        JButton bigger = UiTheme.secondaryButton("A+");
        JButton full = UiTheme.secondaryButton("Plein écran");
        smaller.addActionListener(e -> UiTheme.scaleFonts(getContentPane(), 1 / 1.12f));
        bigger.addActionListener(e -> UiTheme.scaleFonts(getContentPane(), 1.12f));
        full.addActionListener(e -> toggleFullScreen());
        right.add(smaller);
        right.add(bigger);
        right.add(full);
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel navigation() {
        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 8));
        nav.setBackground(UiTheme.SURFACE);
        nav.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));
        String[][] entries = {{"Accueil", HOME}, {"Évaluation", EVALUATE}, {"Étapes", STEPS},
                {"Division synthétique", DIVISION}, {"Vérification", VERIFY}, {"Tests et validation", TESTS},
                {"MODE SOUTENANCE", DEMO}};
        for (String[] entry : entries) {
            JButton b = entry[1].equals(DEMO) ? UiTheme.primaryButton(entry[0]) : UiTheme.secondaryButton(entry[0]);
            b.addActionListener(e -> navigate(entry[1]));
            nav.add(b);
        }
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(UiTheme.SURFACE);
        wrapper.add(nav, BorderLayout.NORTH);
        return wrapper;
    }

    /** Bascule plein écran (F11) / fenêtre (Échap), utile au vidéoprojecteur. */
    void toggleFullScreen() {
        GraphicsDevice device = getGraphicsConfiguration().getDevice();
        boolean target = !fullScreen;
        if (target) {
            normalBounds = getBounds();
        }
        dispose();
        setUndecorated(target);
        if (target && device.isFullScreenSupported()) {
            device.setFullScreenWindow(this);
        } else {
            device.setFullScreenWindow(null);
            if (target) {
                setExtendedState(MAXIMIZED_BOTH);
            } else if (normalBounds != null) {
                setExtendedState(NORMAL);
                setBounds(normalBounds);
            }
        }
        setVisible(true);
        fullScreen = target;
    }

    private static AbstractAction action(Runnable r) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        };
    }
}
