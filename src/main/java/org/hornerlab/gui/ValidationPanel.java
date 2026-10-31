package org.hornerlab.gui;

import org.hornerlab.reporting.TestReportService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * « Tests et validation » : campagne intégrée réellement exécutée, et lecture des derniers
 * rapports Maven (Surefire, JaCoCo) s'ils existent dans {@code target/}.
 */
final class ValidationPanel extends JPanel {

    private final GuiController controller;
    private final JTextField seed = new JTextField(Long.toString(TestReportService.DEFAULT_SEED), 10);
    private final JTextField count = new JTextField("500", 6);
    final JButton run = UiTheme.primaryButton("Lancer la campagne");
    private final JButton export = UiTheme.secondaryButton("Exporter (Markdown)");
    private final DefaultTableModel model = new DefaultTableModel(new String[] {"Catégorie", "Contrôle", "Résultat", "Détails"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JLabel summary = UiTheme.title(" ", 18);
    private final JLabel maven = UiTheme.text(" ", 14);
    private TestReportService.SelfTestReport last;

    ValidationPanel(GuiController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 10));
        setBackground(UiTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);
        top.add(UiTheme.title("Tests et validation", 26));
        top.add(UiTheme.text("   Graine :", 15));
        top.add(seed);
        top.add(UiTheme.text("Cas aléatoires :", 15));
        top.add(count);
        top.add(run);
        top.add(export);
        export.setEnabled(false);

        JTable table = new JTable(model);
        UiTheme.styleTable(table, 14);
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(summary, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(maven, BorderLayout.SOUTH);

        run.addActionListener(e -> launch());
        export.addActionListener(e -> exportReport());
        refreshMavenInfo();
    }

    private void launch() {
        long s;
        int n;
        try {
            s = controller.validator().parseLong(seed.getText(), "graine", Long.MIN_VALUE, Long.MAX_VALUE);
            n = controller.validator().parseInteger(count.getText(), "cas aléatoires", 0, 20_000);
        } catch (IllegalArgumentException ex) {
            summary.setForeground(UiTheme.ERROR);
            summary.setText(ex.getMessage());
            return;
        }
        run.setEnabled(false);
        summary.setForeground(UiTheme.MUTED);
        summary.setText("Campagne en cours…");
        new SwingWorker<TestReportService.SelfTestReport, Void>() {
            @Override
            protected TestReportService.SelfTestReport doInBackground() {
                return new TestReportService().runSelfTest(s, n);
            }

            @Override
            protected void done() {
                run.setEnabled(true);
                try {
                    show(get());
                } catch (Exception ex) {
                    summary.setForeground(UiTheme.ERROR);
                    summary.setText("Erreur pendant la campagne : " + ex.getMessage());
                }
            }
        }.execute();
    }

    void show(TestReportService.SelfTestReport report) {
        last = report;
        model.setRowCount(0);
        for (TestReportService.CheckResult r : report.results()) {
            model.addRow(new Object[] {r.category(), r.name(), r.passed() ? "OK" : "ÉCHEC", r.details()});
        }
        summary.setForeground(report.allPassed() ? UiTheme.SUCCESS : UiTheme.ERROR);
        summary.setText(report.results().size() + " contrôles : " + report.passedCount() + " réussis, "
                + report.failedCount() + " échoués - graine " + report.seed() + ", " + report.duration().toMillis() + " ms");
        export.setEnabled(true);
    }

    private void exportReport() {
        if (last == null) {
            return;
        }
        try {
            Path file = TestReportService.writeMarkdown(last, Path.of("reports"));
            summary.setText("Rapport exporté : " + file.toAbsolutePath());
        } catch (RuntimeException ex) {
            summary.setForeground(UiTheme.ERROR);
            summary.setText("Export impossible : " + ex.getMessage());
        }
    }

    private void refreshMavenInfo() {
        Optional<TestReportService.MavenTestSummary> tests = TestReportService.readSurefireReports(Path.of("target", "surefire-reports"));
        Optional<List<TestReportService.PackageCoverage>> coverage = TestReportService.readJacocoCsv(Path.of("target", "site", "jacoco", "jacoco.csv"));
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(tests.map(t -> "Dernière exécution Maven (target/surefire-reports) : " + t.tests() + " tests, "
                + t.passed() + " réussis, " + t.failures() + " échecs, " + t.errors() + " erreurs, " + t.skipped() + " ignorés.")
                .orElse("Aucun rapport Maven trouvé dans target/ : lancez « mvn verify » depuis le dossier du projet."));
        coverage.ifPresent(list -> {
            sb.append("<br>Couverture JaCoCo : ");
            list.forEach(pc -> sb.append(String.format(Locale.ROOT, "%s %.1f%% instr. / %.1f%% branches ; ",
                    pc.packageName().replace('/', '.'), pc.instructionRatio() * 100, pc.branchRatio() * 100)));
        });
        maven.setText(sb.append("</html>").toString());
    }
}
