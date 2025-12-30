package com.bank.gradle.aiattribution.model;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete AI attribution report for a project.
 * This is the main output of the attribution analysis.
 */
public class AttributionReport {

    private final String projectName;
    private final String projectVersion;
    private final String branch;
    private final String headCommit;
    private final Instant generatedAt;
    private final String analyzedRange;
    private final List<CommitAttribution> commits;
    
    // Computed statistics
    private final int totalCommits;
    private final int aiAssistedCommits;
    private final double aiAssistedPercentage;
    private final int totalFilesChanged;
    private final int aiAssistedFilesChanged;
    private final Map<AiTool, Integer> commitsByTool;
    private final Map<String, ModuleStats> moduleBreakdown;

    private AttributionReport(Builder builder) {
        this.projectName = builder.projectName;
        this.projectVersion = builder.projectVersion;
        this.branch = builder.branch;
        this.headCommit = builder.headCommit;
        this.generatedAt = Instant.now();
        this.analyzedRange = builder.analyzedRange;
        this.commits = Collections.unmodifiableList(
            builder.commits != null ? builder.commits : new ArrayList<>()
        );
        
        // Compute statistics
        this.totalCommits = commits.size();
        this.aiAssistedCommits = (int) commits.stream()
            .filter(CommitAttribution::isAiAssisted)
            .count();
        this.aiAssistedPercentage = totalCommits > 0 
            ? (aiAssistedCommits * 100.0) / totalCommits 
            : 0.0;
        
        this.totalFilesChanged = commits.stream()
            .mapToInt(CommitAttribution::getFileCount)
            .sum();
        this.aiAssistedFilesChanged = commits.stream()
            .filter(CommitAttribution::isAiAssisted)
            .mapToInt(CommitAttribution::getFileCount)
            .sum();
        
        this.commitsByTool = computeCommitsByTool();
        this.moduleBreakdown = computeModuleBreakdown();
    }

    private Map<AiTool, Integer> computeCommitsByTool() {
        Map<AiTool, Integer> result = new EnumMap<>(AiTool.class);
        for (CommitAttribution commit : commits) {
            if (commit.isAiAssisted()) {
                result.merge(commit.getAiTool(), 1, Integer::sum);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, ModuleStats> computeModuleBreakdown() {
        Map<String, ModuleStats.Builder> builders = new HashMap<>();
        
        for (CommitAttribution commit : commits) {
            for (String file : commit.getFilesChanged()) {
                String module = extractModule(file);
                builders.computeIfAbsent(module, k -> new ModuleStats.Builder(k))
                    .addCommit(commit);
            }
        }
        
        return builders.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().build()
            ));
    }

    private String extractModule(String filePath) {
        // Extract module/package from file path
        // e.g., "src/main/java/com/bank/payments/Service.java" -> "com.bank.payments"
        if (filePath.contains("src/main/java/")) {
            String packagePath = filePath.substring(
                filePath.indexOf("src/main/java/") + 14
            );
            int lastSlash = packagePath.lastIndexOf('/');
            if (lastSlash > 0) {
                return packagePath.substring(0, lastSlash).replace('/', '.');
            }
        }
        // Fallback to directory
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash > 0 ? filePath.substring(0, lastSlash) : "root";
    }

    // Getters
    public String getProjectName() { return projectName; }
    public String getProjectVersion() { return projectVersion; }
    public String getBranch() { return branch; }
    public String getHeadCommit() { return headCommit; }
    public Instant getGeneratedAt() { return generatedAt; }
    public String getAnalyzedRange() { return analyzedRange; }
    public List<CommitAttribution> getCommits() { return commits; }
    public int getTotalCommits() { return totalCommits; }
    public int getAiAssistedCommits() { return aiAssistedCommits; }
    public double getAiAssistedPercentage() { return aiAssistedPercentage; }
    public int getTotalFilesChanged() { return totalFilesChanged; }
    public int getAiAssistedFilesChanged() { return aiAssistedFilesChanged; }
    public Map<AiTool, Integer> getCommitsByTool() { return commitsByTool; }
    public Map<String, ModuleStats> getModuleBreakdown() { return moduleBreakdown; }

    public List<CommitAttribution> getAiAssistedCommitsList() {
        return commits.stream()
            .filter(CommitAttribution::isAiAssisted)
            .collect(Collectors.toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Statistics for a specific module/package.
     */
    public static class ModuleStats {
        private final String moduleName;
        private final int totalFiles;
        private final int aiAssistedFiles;
        private final double aiAssistedPercentage;

        private ModuleStats(Builder builder) {
            this.moduleName = builder.moduleName;
            this.totalFiles = builder.totalFiles;
            this.aiAssistedFiles = builder.aiAssistedFiles;
            this.aiAssistedPercentage = totalFiles > 0 
                ? (aiAssistedFiles * 100.0) / totalFiles 
                : 0.0;
        }

        public String getModuleName() { return moduleName; }
        public int getTotalFiles() { return totalFiles; }
        public int getAiAssistedFiles() { return aiAssistedFiles; }
        public double getAiAssistedPercentage() { return aiAssistedPercentage; }

        static class Builder {
            private final String moduleName;
            private int totalFiles = 0;
            private int aiAssistedFiles = 0;
            private final Set<String> seenFiles = new HashSet<>();

            Builder(String moduleName) {
                this.moduleName = moduleName;
            }

            void addCommit(CommitAttribution commit) {
                for (String file : commit.getFilesChanged()) {
                    if (seenFiles.add(file)) {
                        totalFiles++;
                        if (commit.isAiAssisted()) {
                            aiAssistedFiles++;
                        }
                    }
                }
            }

            ModuleStats build() {
                return new ModuleStats(this);
            }
        }
    }

    public static class Builder {
        private String projectName;
        private String projectVersion;
        private String branch;
        private String headCommit;
        private String analyzedRange;
        private List<CommitAttribution> commits;

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder projectVersion(String projectVersion) {
            this.projectVersion = projectVersion;
            return this;
        }

        public Builder branch(String branch) {
            this.branch = branch;
            return this;
        }

        public Builder headCommit(String headCommit) {
            this.headCommit = headCommit;
            return this;
        }

        public Builder analyzedRange(String analyzedRange) {
            this.analyzedRange = analyzedRange;
            return this;
        }

        public Builder commits(List<CommitAttribution> commits) {
            this.commits = commits;
            return this;
        }

        public AttributionReport build() {
            return new AttributionReport(this);
        }
    }
}
