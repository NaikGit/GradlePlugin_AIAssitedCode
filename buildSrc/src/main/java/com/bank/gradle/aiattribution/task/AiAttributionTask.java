package com.bank.gradle.aiattribution.task;

import com.bank.gradle.aiattribution.extension.AiAttributionExtension;
import com.bank.gradle.aiattribution.model.AttributionReport;
import com.bank.gradle.aiattribution.model.CommitAttribution;
import com.bank.gradle.aiattribution.parser.GitCommitParser;
import com.bank.gradle.aiattribution.report.HtmlReportGenerator;
import com.bank.gradle.aiattribution.report.JsonReportGenerator;
import com.bank.gradle.aiattribution.report.PrCommentGenerator;
import com.bank.gradle.aiattribution.report.ReportGenerator;
import com.bank.gradle.aiattribution.report.XmlReportGenerator;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gradle task that analyzes Git commits and generates AI attribution reports.
 * 
 * Usage: ./gradlew aiAttributionReport
 */
public abstract class AiAttributionTask extends DefaultTask {

    @Input
    public abstract Property<Integer> getMaxCommits();

    @Input
    @Optional
    public abstract Property<String> getSinceCommit();

    @Input
    public abstract Property<String> getUntilCommit();

    @Input
    public abstract SetProperty<String> getOutputFormats();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDirectory();

    @Input
    public abstract Property<Boolean> getFailOnNoAttribution();

    @Input
    public abstract Property<Double> getMinAttributionPercentage();

    @Input
    public abstract Property<Boolean> getIncludeFileDetails();

    private final Map<String, ReportGenerator> generators;

    public AiAttributionTask() {
        setGroup("reporting");
        setDescription("Analyzes Git commits and generates AI attribution reports");
        
        this.generators = new HashMap<>();
        generators.put("json", new JsonReportGenerator());
        generators.put("html", new HtmlReportGenerator());
        generators.put("xml", new XmlReportGenerator());
        generators.put("pr-comment", new PrCommentGenerator());
        generators.put("markdown", new PrCommentGenerator());
    }

    @TaskAction
    public void generateReport() throws IOException {
        getLogger().lifecycle("Starting AI Attribution analysis...");
        
        File projectDir = getProject().getProjectDir();
        File outputDir = getOutputDirectory().get().getAsFile();
        
        // Ensure output directory exists
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new GradleException("Failed to create output directory: " + outputDir);
        }
        
        // Parse Git commits
        GitCommitParser parser = new GitCommitParser(
            projectDir,
            getMaxCommits().get(),
            getSinceCommit().getOrNull(),
            getUntilCommit().get()
        );
        
        List<CommitAttribution> commits = parser.parseCommits();
        
        // Build the report
        AttributionReport report = AttributionReport.builder()
            .projectName(getProject().getName())
            .projectVersion(getProject().getVersion().toString())
            .branch(parser.getCurrentBranch())
            .headCommit(parser.getHeadCommit())
            .analyzedRange(buildRangeDescription())
            .commits(commits)
            .build();
        
        // Log summary
        logSummary(report);
        
        // Validate attribution requirements
        validateAttribution(report);
        
        // Generate reports in requested formats
        Set<String> formats = getOutputFormats().get();
        for (String format : formats) {
            generateReportFormat(report, format, outputDir);
        }
        
        getLogger().lifecycle("AI Attribution reports generated in: {}", outputDir.getAbsolutePath());
    }

    private void generateReportFormat(AttributionReport report, String format, File outputDir) {
        ReportGenerator generator = generators.get(format.toLowerCase());
        
        if (generator == null) {
            getLogger().warn("Unknown report format '{}', skipping", format);
            return;
        }
        
        String fileName = "ai-attribution-report." + generator.getFileExtension();
        File outputFile = new File(outputDir, fileName);
        
        try {
            generator.generate(report, outputFile);
            getLogger().lifecycle("Generated {} report: {}", format.toUpperCase(), outputFile.getName());
        } catch (IOException e) {
            throw new GradleException("Failed to generate " + format + " report", e);
        }
    }

    private void logSummary(AttributionReport report) {
        getLogger().lifecycle("");
        getLogger().lifecycle("┌────────────────────────────────────────────────┐");
        getLogger().lifecycle("│           AI ATTRIBUTION SUMMARY               │");
        getLogger().lifecycle("├────────────────────────────────────────────────┤");
        getLogger().lifecycle("│  Total Commits Analyzed:  {:>6}               │", report.getTotalCommits());
        getLogger().lifecycle("│  AI-Assisted Commits:     {:>6}               │", report.getAiAssistedCommits());
        getLogger().lifecycle("│  AI-Assisted Percentage:  {:>6.1f}%              │", report.getAiAssistedPercentage());
        getLogger().lifecycle("│  Total Files Changed:     {:>6}               │", report.getTotalFilesChanged());
        getLogger().lifecycle("│  AI-Assisted Files:       {:>6}               │", report.getAiAssistedFilesChanged());
        getLogger().lifecycle("└────────────────────────────────────────────────┘");
        getLogger().lifecycle("");
        
        if (!report.getCommitsByTool().isEmpty()) {
            getLogger().lifecycle("AI Tools Used:");
            report.getCommitsByTool().forEach((tool, count) -> 
                getLogger().lifecycle("  - {}: {} commits", tool.getDisplayName(), count)
            );
            getLogger().lifecycle("");
        }
    }

    private void validateAttribution(AttributionReport report) {
        if (!getFailOnNoAttribution().get()) {
            return;
        }
        
        double minPercentage = getMinAttributionPercentage().get();
        double actualPercentage = report.getAiAssistedPercentage();
        
        if (actualPercentage < minPercentage) {
            throw new GradleException(String.format(
                "AI attribution percentage (%.1f%%) is below required minimum (%.1f%%). " +
                "Ensure commits include AI-Tool or AI-Assisted trailers.",
                actualPercentage, minPercentage
            ));
        }
        
        if (report.getAiAssistedCommits() == 0 && report.getTotalCommits() > 0) {
            getLogger().warn("No AI-attributed commits found. Consider adding Git trailers to track AI assistance.");
        }
    }

    private String buildRangeDescription() {
        String since = getSinceCommit().getOrNull();
        String until = getUntilCommit().get();
        
        if (since != null && !since.isBlank()) {
            return since + ".." + until;
        }
        return "last " + getMaxCommits().get() + " commits up to " + until;
    }

    /**
     * Configure the task from the extension.
     */
    public void configureFrom(AiAttributionExtension extension) {
        getMaxCommits().set(extension.getMaxCommits());
        getSinceCommit().set(extension.getSinceCommit());
        getUntilCommit().set(extension.getUntilCommit());
        getOutputFormats().set(extension.getOutputFormats());
        getOutputDirectory().set(extension.getOutputDirectory());
        getFailOnNoAttribution().set(extension.getFailOnNoAttribution());
        getMinAttributionPercentage().set(extension.getMinAttributionPercentage());
        getIncludeFileDetails().set(extension.getIncludeFileDetails());
    }
}
