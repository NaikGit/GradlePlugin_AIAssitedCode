package com.bank.gradle.aiattribution.report;

import com.bank.gradle.aiattribution.model.AiTool;
import com.bank.gradle.aiattribution.model.AttributionReport;
import com.bank.gradle.aiattribution.model.CommitAttribution;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates JSON format attribution report.
 */
public class JsonReportGenerator implements ReportGenerator {

    private final Gson gson;

    public JsonReportGenerator() {
        this.gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .create();
    }

    @Override
    public void generate(AttributionReport report, File outputFile) throws IOException {
        Map<String, Object> jsonReport = buildJsonStructure(report);
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            gson.toJson(jsonReport, writer);
        }
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    private Map<String, Object> buildJsonStructure(AttributionReport report) {
        Map<String, Object> root = new LinkedHashMap<>();
        
        // Metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("projectName", report.getProjectName());
        metadata.put("projectVersion", report.getProjectVersion());
        metadata.put("branch", report.getBranch());
        metadata.put("headCommit", report.getHeadCommit());
        metadata.put("generatedAt", report.getGeneratedAt().toString());
        metadata.put("analyzedRange", report.getAnalyzedRange());
        root.put("metadata", metadata);
        
        // Summary statistics
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCommits", report.getTotalCommits());
        summary.put("aiAssistedCommits", report.getAiAssistedCommits());
        summary.put("aiAssistedPercentage", round(report.getAiAssistedPercentage()));
        summary.put("totalFilesChanged", report.getTotalFilesChanged());
        summary.put("aiAssistedFilesChanged", report.getAiAssistedFilesChanged());
        root.put("summary", summary);
        
        // Breakdown by AI tool
        Map<String, Integer> toolBreakdown = new LinkedHashMap<>();
        for (Map.Entry<AiTool, Integer> entry : report.getCommitsByTool().entrySet()) {
            toolBreakdown.put(entry.getKey().getDisplayName(), entry.getValue());
        }
        root.put("toolBreakdown", toolBreakdown);
        
        // Module breakdown
        List<Map<String, Object>> modules = report.getModuleBreakdown().entrySet().stream()
            .map(entry -> {
                Map<String, Object> module = new LinkedHashMap<>();
                module.put("name", entry.getKey());
                module.put("totalFiles", entry.getValue().getTotalFiles());
                module.put("aiAssistedFiles", entry.getValue().getAiAssistedFiles());
                module.put("aiAssistedPercentage", round(entry.getValue().getAiAssistedPercentage()));
                return module;
            })
            .collect(Collectors.toList());
        root.put("moduleBreakdown", modules);
        
        // Individual commits
        List<Map<String, Object>> commits = report.getCommits().stream()
            .map(this::commitToMap)
            .collect(Collectors.toList());
        root.put("commits", commits);
        
        return root;
    }

    private Map<String, Object> commitToMap(CommitAttribution commit) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("hash", commit.getShortHash());
        map.put("fullHash", commit.getCommitHash());
        map.put("author", commit.getAuthor());
        map.put("authorEmail", commit.getAuthorEmail());
        map.put("commitTime", commit.getCommitTime().toString());
        map.put("message", commit.getMessage());
        map.put("aiAssisted", commit.isAiAssisted());
        
        if (commit.isAiAssisted()) {
            map.put("aiTool", commit.getAiTool().getDisplayName());
            if (commit.getAiConfidence() != null) {
                map.put("aiConfidence", commit.getAiConfidence());
            }
        }
        
        map.put("filesChanged", commit.getFilesChanged());
        map.put("fileCount", commit.getFileCount());
        
        return map;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * Gson adapter for Java Instant.
     */
    private static class InstantAdapter extends TypeAdapter<Instant> {
        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            out.value(value != null ? value.toString() : null);
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            String value = in.nextString();
            return value != null ? Instant.parse(value) : null;
        }
    }
}
