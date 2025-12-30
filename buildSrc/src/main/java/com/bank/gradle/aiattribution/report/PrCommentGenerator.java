package com.bank.gradle.aiattribution.report;

import com.bank.gradle.aiattribution.model.AiTool;
import com.bank.gradle.aiattribution.model.AttributionReport;
import com.bank.gradle.aiattribution.model.CommitAttribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a Markdown-formatted PR comment summarizing AI attribution.
 * 
 * <p>Output can be used with:
 * <ul>
 *   <li>GitHub Actions (gh pr comment)</li>
 *   <li>Bitbucket Pipelines</li>
 *   <li>Jenkins (via GitHub/Bitbucket plugins)</li>
 *   <li>Azure DevOps</li>
 * </ul>
 */
public class PrCommentGenerator implements ReportGenerator {

    private static final String ROBOT_EMOJI = "🤖";
    private static final String HUMAN_EMOJI = "👨‍💻";
    private static final String CHART_EMOJI = "📊";
    private static final String CHECK_EMOJI = "✅";
    private static final String FILE_EMOJI = "📁";

    @Override
    public void generate(AttributionReport report, File outputFile) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(outputFile))) {
            writeComment(report, w);
        }
    }

    @Override
    public String getFileExtension() {
        return "md";
    }

    private void writeComment(AttributionReport report, PrintWriter w) {
        // Header
        w.println("## " + ROBOT_EMOJI + " AI Attribution Summary");
        w.println();
        
        // Quick stats badges (GitHub-flavored markdown)
        writeStatsBadges(report, w);
        w.println();
        
        // Summary table
        writeSummaryTable(report, w);
        w.println();
        
        // Tool breakdown (if AI was used)
        if (report.getAiAssistedCommits() > 0) {
            writeToolBreakdown(report, w);
            w.println();
            
            // AI-assisted commits list
            writeCommitsList(report, w);
            w.println();
        }
        
        // Module breakdown (collapsible)
        if (!report.getModuleBreakdown().isEmpty()) {
            writeModuleBreakdown(report, w);
            w.println();
        }
        
        // Footer
        writeFooter(report, w);
    }

    private void writeStatsBadges(AttributionReport report, PrintWriter w) {
        double pct = report.getAiAssistedPercentage();
        String color = pct > 50 ? "blue" : pct > 20 ? "green" : "lightgrey";
        
        // Using shields.io-style badges (works in GitHub)
        w.printf("![Commits](https://img.shields.io/badge/commits-%d-blue) ", report.getTotalCommits());
        w.printf("![AI Assisted](https://img.shields.io/badge/AI%%20assisted-%d%%20(%.0f%%25)-%s) ", 
            report.getAiAssistedCommits(), pct, color);
        w.printf("![Files](https://img.shields.io/badge/files%%20changed-%d-lightgrey)%n", 
            report.getTotalFilesChanged());
    }

    private void writeSummaryTable(AttributionReport report, PrintWriter w) {
        w.println("### " + CHART_EMOJI + " Summary");
        w.println();
        w.println("| Metric | Value |");
        w.println("|--------|-------|");
        w.printf("| Total Commits | %d |%n", report.getTotalCommits());
        w.printf("| AI-Assisted Commits | %d |%n", report.getAiAssistedCommits());
        w.printf("| Human-Only Commits | %d |%n", report.getTotalCommits() - report.getAiAssistedCommits());
        w.printf("| AI Percentage | **%.1f%%** |%n", report.getAiAssistedPercentage());
        w.printf("| Files Changed (Total) | %d |%n", report.getTotalFilesChanged());
        w.printf("| Files Changed (AI) | %d |%n", report.getAiAssistedFilesChanged());
    }

    private void writeToolBreakdown(AttributionReport report, PrintWriter w) {
        w.println("### " + ROBOT_EMOJI + " AI Tools Used");
        w.println();
        w.println("| Tool | Commits | Percentage |");
        w.println("|------|---------|------------|");
        
        for (Map.Entry<AiTool, Integer> entry : report.getCommitsByTool().entrySet()) {
            double pct = (entry.getValue() * 100.0) / report.getAiAssistedCommits();
            String bar = generateProgressBar(pct, 10);
            w.printf("| %s | %d | %s %.0f%% |%n", 
                entry.getKey().getDisplayName(), 
                entry.getValue(),
                bar,
                pct);
        }
    }

    private void writeCommitsList(AttributionReport report, PrintWriter w) {
        List<CommitAttribution> aiCommits = report.getAiAssistedCommitsList();
        
        w.println("### " + CHECK_EMOJI + " AI-Assisted Commits");
        w.println();
        w.println("<details>");
        w.println("<summary>Click to expand (" + aiCommits.size() + " commits)</summary>");
        w.println();
        w.println("| Commit | Message | Tool | Files |");
        w.println("|--------|---------|------|-------|");
        
        for (CommitAttribution commit : aiCommits) {
            String message = truncate(commit.getMessage(), 50);
            w.printf("| `%s` | %s | %s | %d |%n",
                commit.getShortHash(),
                escapeMarkdown(message),
                commit.getAiTool().getDisplayName(),
                commit.getFileCount());
        }
        
        w.println();
        w.println("</details>");
    }

    private void writeModuleBreakdown(AttributionReport report, PrintWriter w) {
        w.println("### " + FILE_EMOJI + " Module Breakdown");
        w.println();
        w.println("<details>");
        w.println("<summary>Click to expand (" + report.getModuleBreakdown().size() + " modules)</summary>");
        w.println();
        w.println("| Module | Total Files | AI-Assisted | Percentage |");
        w.println("|--------|-------------|-------------|------------|");
        
        // Sort by AI percentage descending
        List<Map.Entry<String, AttributionReport.ModuleStats>> sorted = 
            report.getModuleBreakdown().entrySet().stream()
                .sorted((a, b) -> Double.compare(
                    b.getValue().getAiAssistedPercentage(),
                    a.getValue().getAiAssistedPercentage()))
                .collect(Collectors.toList());
        
        for (Map.Entry<String, AttributionReport.ModuleStats> entry : sorted) {
            AttributionReport.ModuleStats stats = entry.getValue();
            String bar = generateProgressBar(stats.getAiAssistedPercentage(), 8);
            w.printf("| `%s` | %d | %d | %s %.0f%% |%n",
                truncate(entry.getKey(), 40),
                stats.getTotalFiles(),
                stats.getAiAssistedFiles(),
                bar,
                stats.getAiAssistedPercentage());
        }
        
        w.println();
        w.println("</details>");
    }

    private void writeFooter(AttributionReport report, PrintWriter w) {
        w.println("---");
        w.printf("*Generated by AI Attribution Plugin | Branch: `%s` | Analyzed: %s*%n",
            report.getBranch(),
            report.getAnalyzedRange());
    }

    private String generateProgressBar(double percentage, int length) {
        int filled = (int) Math.round((percentage / 100.0) * length);
        int empty = length - filled;
        return "█".repeat(Math.max(0, filled)) + "░".repeat(Math.max(0, empty));
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("|", "\\|")
                   .replace("\n", " ")
                   .replace("\r", "");
    }
}
