package com.bank.gradle.aiattribution;

import com.bank.gradle.aiattribution.extension.AiAttributionExtension;
import com.bank.gradle.aiattribution.task.AiAttributionTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

/**
 * Gradle plugin for tracking and reporting AI-assisted code contributions.
 * 
 * <p>This plugin analyzes Git commit history to identify commits that were
 * created with AI assistance (GitHub Copilot, Devin, Claude, etc.) based on
 * Git trailer metadata.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * plugins {
 *     id 'com.bank.ai-attribution'
 * }
 * 
 * aiAttribution {
 *     maxCommits = 500
 *     sinceCommit = 'v1.0.0'  // Optional: analyze since this tag/commit
 *     outputFormats = ['json', 'html', 'xml']
 *     outputDirectory = file("$buildDir/reports/ai-attribution")
 * }
 * </pre>
 * 
 * <h2>Git Trailer Format</h2>
 * <p>Commits should include trailers like:</p>
 * <pre>
 * Fix payment validation edge case
 * 
 * AI-Tool: github-copilot
 * AI-Assisted: true
 * AI-Confidence: high
 * </pre>
 * 
 * <h2>Tasks</h2>
 * <ul>
 *   <li>{@code aiAttributionReport} - Generates AI attribution reports</li>
 * </ul>
 */
public class AiAttributionPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "aiAttribution";
    public static final String TASK_NAME = "aiAttributionReport";

    @Override
    public void apply(Project project) {
        // Create the extension for configuration DSL
        AiAttributionExtension extension = project.getExtensions()
            .create(EXTENSION_NAME, AiAttributionExtension.class, project.getObjects());
        
        // Set default output directory
        extension.getOutputDirectory().convention(
            project.getLayout().getBuildDirectory().dir("reports/ai-attribution")
        );
        
        // Register the main task
        project.getTasks().register(TASK_NAME, AiAttributionTask.class, task -> {
            task.setDescription("Analyzes Git commits and generates AI attribution reports");
            task.setGroup("reporting");
            task.configureFrom(extension);
        });
        
        // Optionally integrate with the Java plugin's check task
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            project.getTasks().named("check").configure(checkTask -> {
                // Don't add as dependency by default - run separately
                // Uncomment below to make it part of the check lifecycle:
                // checkTask.dependsOn(TASK_NAME);
            });
        });
        
        project.afterEvaluate(p -> {
            logPluginApplied(project);
        });
    }

    private void logPluginApplied(Project project) {
        project.getLogger().info(
            "AI Attribution Plugin applied to project '{}'. " +
            "Run './gradlew {}' to generate reports.",
            project.getName(),
            TASK_NAME
        );
    }
}
