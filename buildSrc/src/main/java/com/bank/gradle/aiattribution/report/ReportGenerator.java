package com.bank.gradle.aiattribution.report;

import com.bank.gradle.aiattribution.model.AttributionReport;

import java.io.File;
import java.io.IOException;

/**
 * Interface for generating attribution reports in various formats.
 */
public interface ReportGenerator {

    /**
     * Generate a report and write it to the specified file.
     *
     * @param report The attribution report data
     * @param outputFile The file to write the report to
     * @throws IOException If writing fails
     */
    void generate(AttributionReport report, File outputFile) throws IOException;

    /**
     * Get the file extension for this report type.
     *
     * @return File extension without the dot (e.g., "json", "html")
     */
    String getFileExtension();
}
