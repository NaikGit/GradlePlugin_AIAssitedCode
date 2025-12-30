package com.bank.gradle.aiattribution;

import com.bank.gradle.aiattribution.model.AiTool;
import com.bank.gradle.aiattribution.model.AttributionReport;
import com.bank.gradle.aiattribution.model.CommitAttribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for model classes.
 */
class ModelTest {

    @Test
    @DisplayName("CommitAttribution builder should create valid object")
    void testCommitAttributionBuilder() {
        Instant now = Instant.now();
        List<String> files = Arrays.asList("src/Main.java", "src/Service.java");
        
        CommitAttribution commit = CommitAttribution.builder()
            .commitHash("abc123def456")
            .author("John Developer")
            .authorEmail("john@bank.com")
            .commitTime(now)
            .message("Fix payment bug")
            .aiAssisted(true)
            .aiTool(AiTool.GITHUB_COPILOT)
            .aiConfidence("high")
            .filesChanged(files)
            .build();
        
        assertEquals("abc123def456", commit.getCommitHash());
        assertEquals("abc123d", commit.getShortHash());
        assertEquals("John Developer", commit.getAuthor());
        assertEquals("john@bank.com", commit.getAuthorEmail());
        assertEquals(now, commit.getCommitTime());
        assertEquals("Fix payment bug", commit.getMessage());
        assertTrue(commit.isAiAssisted());
        assertEquals(AiTool.GITHUB_COPILOT, commit.getAiTool());
        assertEquals("high", commit.getAiConfidence());
        assertEquals(2, commit.getFileCount());
        assertEquals(files, commit.getFilesChanged());
    }

    @Test
    @DisplayName("CommitAttribution should default to NONE tool")
    void testCommitAttributionDefaultTool() {
        CommitAttribution commit = CommitAttribution.builder()
            .commitHash("abc123def456")
            .aiAssisted(false)
            .build();
        
        assertEquals(AiTool.NONE, commit.getAiTool());
    }

    @Test
    @DisplayName("CommitAttribution should handle empty files list")
    void testCommitAttributionEmptyFiles() {
        CommitAttribution commit = CommitAttribution.builder()
            .commitHash("abc123def456")
            .build();
        
        assertNotNull(commit.getFilesChanged());
        assertTrue(commit.getFilesChanged().isEmpty());
        assertEquals(0, commit.getFileCount());
    }

    @Test
    @DisplayName("AttributionReport should compute statistics correctly")
    void testAttributionReportStatistics() {
        List<CommitAttribution> commits = Arrays.asList(
            CommitAttribution.builder()
                .commitHash("commit1")
                .aiAssisted(true)
                .aiTool(AiTool.GITHUB_COPILOT)
                .filesChanged(Arrays.asList("file1.java", "file2.java"))
                .build(),
            CommitAttribution.builder()
                .commitHash("commit2")
                .aiAssisted(true)
                .aiTool(AiTool.DEVIN)
                .filesChanged(Arrays.asList("file3.java"))
                .build(),
            CommitAttribution.builder()
                .commitHash("commit3")
                .aiAssisted(false)
                .filesChanged(Arrays.asList("file4.java", "file5.java"))
                .build()
        );
        
        AttributionReport report = AttributionReport.builder()
            .projectName("test-project")
            .projectVersion("1.0.0")
            .branch("main")
            .headCommit("abc123")
            .commits(commits)
            .build();
        
        assertEquals(3, report.getTotalCommits());
        assertEquals(2, report.getAiAssistedCommits());
        assertEquals(66.67, report.getAiAssistedPercentage(), 0.01);
        assertEquals(5, report.getTotalFilesChanged());
        assertEquals(3, report.getAiAssistedFilesChanged());
        
        // Tool breakdown
        assertEquals(1, report.getCommitsByTool().get(AiTool.GITHUB_COPILOT));
        assertEquals(1, report.getCommitsByTool().get(AiTool.DEVIN));
    }

    @Test
    @DisplayName("AttributionReport should handle empty commits list")
    void testAttributionReportEmpty() {
        AttributionReport report = AttributionReport.builder()
            .projectName("empty-project")
            .projectVersion("1.0.0")
            .build();
        
        assertEquals(0, report.getTotalCommits());
        assertEquals(0, report.getAiAssistedCommits());
        assertEquals(0.0, report.getAiAssistedPercentage());
        assertTrue(report.getCommits().isEmpty());
        assertTrue(report.getCommitsByTool().isEmpty());
    }

    @Test
    @DisplayName("AttributionReport should filter AI-assisted commits")
    void testGetAiAssistedCommitsList() {
        List<CommitAttribution> commits = Arrays.asList(
            CommitAttribution.builder()
                .commitHash("commit1")
                .aiAssisted(true)
                .build(),
            CommitAttribution.builder()
                .commitHash("commit2")
                .aiAssisted(false)
                .build(),
            CommitAttribution.builder()
                .commitHash("commit3")
                .aiAssisted(true)
                .build()
        );
        
        AttributionReport report = AttributionReport.builder()
            .projectName("test")
            .commits(commits)
            .build();
        
        List<CommitAttribution> aiCommits = report.getAiAssistedCommitsList();
        assertEquals(2, aiCommits.size());
        assertTrue(aiCommits.stream().allMatch(CommitAttribution::isAiAssisted));
    }
}
