package com.bank.gradle.aiattribution.extension;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Extension for configuring the AI Attribution plugin.
 * 
 * Usage in build.gradle:
 * <pre>
 * aiAttribution {
 *     maxCommits = 500
 *     sinceCommit = 'v1.0.0'
 *     outputFormats = ['json', 'html', 'xml']
 *     outputDirectory = file("$buildDir/reports/ai-attribution")
 * }
 * </pre>
 */
public abstract class AiAttributionExtension {

    /**
     * Maximum number of commits to analyze.
     * Default: 100
     */
    public abstract Property<Integer> getMaxCommits();

    /**
     * Start commit (exclusive) for analysis range.
     * Can be a commit hash, tag, or branch name.
     * Default: null (analyze from beginning)
     */
    public abstract Property<String> getSinceCommit();

    /**
     * End commit (inclusive) for analysis range.
     * Can be a commit hash, tag, or branch name.
     * Default: HEAD
     */
    public abstract Property<String> getUntilCommit();

    /**
     * Output formats to generate.
     * Supported: json, xml, html
     * Default: all formats
     */
    public abstract SetProperty<String> getOutputFormats();

    /**
     * Directory for output reports.
     * Default: $buildDir/reports/ai-attribution
     */
    public abstract DirectoryProperty getOutputDirectory();

    /**
     * Whether to fail the build if no AI attribution data is found.
     * Useful for enforcing attribution practices.
     * Default: false
     */
    public abstract Property<Boolean> getFailOnNoAttribution();

    /**
     * Minimum percentage of AI-attributed commits required.
     * Only enforced if failOnNoAttribution is true.
     * Default: 0 (no minimum)
     */
    public abstract Property<Double> getMinAttributionPercentage();

    /**
     * Whether to include file-level details in the report.
     * Default: true
     */
    public abstract Property<Boolean> getIncludeFileDetails();

    /**
     * Custom trailer names to recognize for AI-Tool.
     * Merged with defaults (AI-Tool, AI-Assisted).
     */
    public abstract SetProperty<String> getCustomAiToolTrailers();

    /**
     * File patterns to exclude from analysis.
     * E.g., '*.md', 'docs/**'
     */
    public abstract SetProperty<String> getExcludePatterns();

    @Inject
    protected AiAttributionExtension(ObjectFactory objects) {
        // Set defaults
        getMaxCommits().convention(100);
        getUntilCommit().convention("HEAD");
        getOutputFormats().convention(
            new HashSet<>(Arrays.asList("json", "html", "xml", "pr-comment"))
        );
        getFailOnNoAttribution().convention(false);
        getMinAttributionPercentage().convention(0.0);
        getIncludeFileDetails().convention(true);
        getCustomAiToolTrailers().convention(new HashSet<>());
        getExcludePatterns().convention(new HashSet<>());
    }

    /**
     * Convenience method to set a single output format.
     */
    public void outputFormat(String format) {
        getOutputFormats().add(format);
    }

    /**
     * Convenience method to add an exclude pattern.
     */
    public void exclude(String pattern) {
        getExcludePatterns().add(pattern);
    }
}
