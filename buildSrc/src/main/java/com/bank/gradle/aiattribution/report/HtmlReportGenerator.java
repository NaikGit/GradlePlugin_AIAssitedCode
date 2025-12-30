package com.bank.gradle.aiattribution.report;

import com.bank.gradle.aiattribution.model.AiTool;
import com.bank.gradle.aiattribution.model.AttributionReport;
import com.bank.gradle.aiattribution.model.CommitAttribution;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates HTML format attribution report with a dashboard-style layout.
 */
public class HtmlReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public void generate(AttributionReport report, File outputFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            writeHtml(report, writer);
        }
    }

    @Override
    public String getFileExtension() {
        return "html";
    }

    private void writeHtml(AttributionReport report, PrintWriter w) {
        w.println("<!DOCTYPE html>");
        w.println("<html lang=\"en\">");
        w.println("<head>");
        w.println("  <meta charset=\"UTF-8\">");
        w.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        w.println("  <title>AI Attribution Report - " + escapeHtml(report.getProjectName()) + "</title>");
        writeStyles(w);
        w.println("</head>");
        w.println("<body>");
        
        // Header
        w.println("  <header>");
        w.println("    <h1>AI Attribution Report</h1>");
        w.println("    <p class=\"subtitle\">" + escapeHtml(report.getProjectName()) + 
            " v" + escapeHtml(report.getProjectVersion()) + "</p>");
        w.println("    <p class=\"meta\">Generated: " + DATE_FORMAT.format(report.getGeneratedAt()) + 
            " | Branch: " + escapeHtml(report.getBranch()) + "</p>");
        w.println("  </header>");
        
        // Summary cards
        w.println("  <section class=\"summary-cards\">");
        writeSummaryCard(w, "Total Commits", String.valueOf(report.getTotalCommits()), "commits-icon");
        writeSummaryCard(w, "AI-Assisted", String.valueOf(report.getAiAssistedCommits()), "ai-icon");
        writeSummaryCard(w, "AI Percentage", String.format("%.1f%%", report.getAiAssistedPercentage()), "percent-icon");
        writeSummaryCard(w, "Files Changed", String.valueOf(report.getTotalFilesChanged()), "files-icon");
        w.println("  </section>");
        
        // Tool breakdown
        if (!report.getCommitsByTool().isEmpty()) {
            w.println("  <section class=\"tool-breakdown\">");
            w.println("    <h2>AI Tool Usage</h2>");
            w.println("    <div class=\"tool-chart\">");
            for (Map.Entry<AiTool, Integer> entry : report.getCommitsByTool().entrySet()) {
                double pct = (entry.getValue() * 100.0) / report.getAiAssistedCommits();
                w.println("      <div class=\"tool-bar\">");
                w.println("        <span class=\"tool-name\">" + entry.getKey().getDisplayName() + "</span>");
                w.println("        <div class=\"bar-container\">");
                w.println("          <div class=\"bar\" style=\"width: " + pct + "%\"></div>");
                w.println("        </div>");
                w.println("        <span class=\"tool-count\">" + entry.getValue() + " commits</span>");
                w.println("      </div>");
            }
            w.println("    </div>");
            w.println("  </section>");
        }
        
        // Module breakdown
        if (!report.getModuleBreakdown().isEmpty()) {
            w.println("  <section class=\"module-breakdown\">");
            w.println("    <h2>Module Breakdown</h2>");
            w.println("    <table>");
            w.println("      <thead>");
            w.println("        <tr><th>Module</th><th>Total Files</th><th>AI-Assisted</th><th>Percentage</th></tr>");
            w.println("      </thead>");
            w.println("      <tbody>");
            for (Map.Entry<String, AttributionReport.ModuleStats> entry : report.getModuleBreakdown().entrySet()) {
                AttributionReport.ModuleStats stats = entry.getValue();
                w.println("        <tr>");
                w.println("          <td>" + escapeHtml(entry.getKey()) + "</td>");
                w.println("          <td>" + stats.getTotalFiles() + "</td>");
                w.println("          <td>" + stats.getAiAssistedFiles() + "</td>");
                w.println("          <td>" + String.format("%.1f%%", stats.getAiAssistedPercentage()) + "</td>");
                w.println("        </tr>");
            }
            w.println("      </tbody>");
            w.println("    </table>");
            w.println("  </section>");
        }
        
        // Recent AI-assisted commits
        w.println("  <section class=\"commits-section\">");
        w.println("    <h2>AI-Assisted Commits</h2>");
        w.println("    <div class=\"commits-list\">");
        for (CommitAttribution commit : report.getCommits()) {
            if (commit.isAiAssisted()) {
                writeCommitCard(w, commit);
            }
        }
        if (report.getAiAssistedCommits() == 0) {
            w.println("    <p class=\"no-data\">No AI-attributed commits found in the analyzed range.</p>");
        }
        w.println("    </div>");
        w.println("  </section>");
        
        // Footer
        w.println("  <footer>");
        w.println("    <p>AI Attribution Plugin | Analyzed " + report.getTotalCommits() + 
            " commits | HEAD: " + (report.getHeadCommit() != null ? report.getHeadCommit().substring(0, 7) : "N/A") + "</p>");
        w.println("  </footer>");
        
        w.println("</body>");
        w.println("</html>");
    }

    private void writeSummaryCard(PrintWriter w, String title, String value, String iconClass) {
        w.println("    <div class=\"summary-card\">");
        w.println("      <div class=\"card-icon " + iconClass + "\"></div>");
        w.println("      <div class=\"card-content\">");
        w.println("        <h3>" + value + "</h3>");
        w.println("        <p>" + title + "</p>");
        w.println("      </div>");
        w.println("    </div>");
    }

    private void writeCommitCard(PrintWriter w, CommitAttribution commit) {
        w.println("      <div class=\"commit-card\">");
        w.println("        <div class=\"commit-header\">");
        w.println("          <span class=\"commit-hash\">" + commit.getShortHash() + "</span>");
        w.println("          <span class=\"commit-tool\">" + commit.getAiTool().getDisplayName() + "</span>");
        w.println("        </div>");
        w.println("        <p class=\"commit-message\">" + escapeHtml(commit.getMessage()) + "</p>");
        w.println("        <div class=\"commit-meta\">");
        w.println("          <span>" + escapeHtml(commit.getAuthor()) + "</span>");
        w.println("          <span>" + DATE_FORMAT.format(commit.getCommitTime()) + "</span>");
        w.println("          <span>" + commit.getFileCount() + " files</span>");
        w.println("        </div>");
        w.println("      </div>");
    }

    private void writeStyles(PrintWriter w) {
        w.println("  <style>");
        w.println("    :root {");
        w.println("      --primary: #2563eb;");
        w.println("      --primary-dark: #1d4ed8;");
        w.println("      --success: #059669;");
        w.println("      --bg: #f8fafc;");
        w.println("      --card-bg: #ffffff;");
        w.println("      --text: #1e293b;");
        w.println("      --text-muted: #64748b;");
        w.println("      --border: #e2e8f0;");
        w.println("    }");
        w.println("    * { box-sizing: border-box; margin: 0; padding: 0; }");
        w.println("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;");
        w.println("           background: var(--bg); color: var(--text); line-height: 1.6; }");
        w.println("    header { background: var(--primary); color: white; padding: 2rem; text-align: center; }");
        w.println("    header h1 { font-size: 1.75rem; margin-bottom: 0.5rem; }");
        w.println("    header .subtitle { font-size: 1.1rem; opacity: 0.9; }");
        w.println("    header .meta { font-size: 0.85rem; opacity: 0.8; margin-top: 0.5rem; }");
        w.println("    section { max-width: 1200px; margin: 2rem auto; padding: 0 1rem; }");
        w.println("    h2 { font-size: 1.25rem; margin-bottom: 1rem; color: var(--text); }");
        w.println("    .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; }");
        w.println("    .summary-card { background: var(--card-bg); border-radius: 8px; padding: 1.5rem;");
        w.println("                    box-shadow: 0 1px 3px rgba(0,0,0,0.1); display: flex; align-items: center; gap: 1rem; }");
        w.println("    .card-icon { width: 48px; height: 48px; border-radius: 8px; background: var(--primary); opacity: 0.1; }");
        w.println("    .card-content h3 { font-size: 1.5rem; color: var(--primary); }");
        w.println("    .card-content p { color: var(--text-muted); font-size: 0.9rem; }");
        w.println("    .tool-breakdown { background: var(--card-bg); border-radius: 8px; padding: 1.5rem;");
        w.println("                      box-shadow: 0 1px 3px rgba(0,0,0,0.1); }");
        w.println("    .tool-bar { display: flex; align-items: center; gap: 1rem; margin-bottom: 0.75rem; }");
        w.println("    .tool-name { width: 140px; font-weight: 500; }");
        w.println("    .bar-container { flex: 1; height: 24px; background: var(--border); border-radius: 4px; overflow: hidden; }");
        w.println("    .bar { height: 100%; background: var(--success); border-radius: 4px; }");
        w.println("    .tool-count { width: 100px; text-align: right; color: var(--text-muted); font-size: 0.9rem; }");
        w.println("    table { width: 100%; border-collapse: collapse; background: var(--card-bg);");
        w.println("            border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }");
        w.println("    th, td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid var(--border); }");
        w.println("    th { background: var(--bg); font-weight: 600; }");
        w.println("    tr:last-child td { border-bottom: none; }");
        w.println("    .commits-list { display: grid; gap: 1rem; }");
        w.println("    .commit-card { background: var(--card-bg); border-radius: 8px; padding: 1rem;");
        w.println("                   box-shadow: 0 1px 3px rgba(0,0,0,0.1); border-left: 4px solid var(--success); }");
        w.println("    .commit-header { display: flex; justify-content: space-between; margin-bottom: 0.5rem; }");
        w.println("    .commit-hash { font-family: monospace; font-weight: 600; color: var(--primary); }");
        w.println("    .commit-tool { background: var(--success); color: white; padding: 0.2rem 0.5rem;");
        w.println("                   border-radius: 4px; font-size: 0.75rem; }");
        w.println("    .commit-message { margin-bottom: 0.5rem; }");
        w.println("    .commit-meta { display: flex; gap: 1rem; font-size: 0.85rem; color: var(--text-muted); }");
        w.println("    .no-data { text-align: center; color: var(--text-muted); padding: 2rem; }");
        w.println("    footer { text-align: center; padding: 2rem; color: var(--text-muted); font-size: 0.85rem; }");
        w.println("  </style>");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
