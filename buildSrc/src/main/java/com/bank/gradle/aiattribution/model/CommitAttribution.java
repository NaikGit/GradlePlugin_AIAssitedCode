package com.bank.gradle.aiattribution.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents AI attribution data for a single Git commit.
 */
public class CommitAttribution {
    
    private final String commitHash;
    private final String shortHash;
    private final String author;
    private final String authorEmail;
    private final Instant commitTime;
    private final String message;
    private final boolean aiAssisted;
    private final AiTool aiTool;
    private final String aiConfidence;
    private final List<String> filesChanged;

    private CommitAttribution(Builder builder) {
        this.commitHash = builder.commitHash;
        this.shortHash = builder.commitHash != null && builder.commitHash.length() >= 7 
            ? builder.commitHash.substring(0, 7) : builder.commitHash;
        this.author = builder.author;
        this.authorEmail = builder.authorEmail;
        this.commitTime = builder.commitTime;
        this.message = builder.message;
        this.aiAssisted = builder.aiAssisted;
        this.aiTool = builder.aiTool != null ? builder.aiTool : AiTool.NONE;
        this.aiConfidence = builder.aiConfidence;
        this.filesChanged = builder.filesChanged != null 
            ? Collections.unmodifiableList(builder.filesChanged) 
            : Collections.emptyList();
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getShortHash() {
        return shortHash;
    }

    public String getAuthor() {
        return author;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public Instant getCommitTime() {
        return commitTime;
    }

    public String getMessage() {
        return message;
    }

    public boolean isAiAssisted() {
        return aiAssisted;
    }

    public AiTool getAiTool() {
        return aiTool;
    }

    public String getAiConfidence() {
        return aiConfidence;
    }

    public List<String> getFilesChanged() {
        return filesChanged;
    }

    public int getFileCount() {
        return filesChanged.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommitAttribution that = (CommitAttribution) o;
        return Objects.equals(commitHash, that.commitHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commitHash);
    }

    @Override
    public String toString() {
        return String.format("CommitAttribution{hash=%s, aiAssisted=%s, tool=%s}", 
            shortHash, aiAssisted, aiTool);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String commitHash;
        private String author;
        private String authorEmail;
        private Instant commitTime;
        private String message;
        private boolean aiAssisted;
        private AiTool aiTool;
        private String aiConfidence;
        private List<String> filesChanged;

        public Builder commitHash(String commitHash) {
            this.commitHash = commitHash;
            return this;
        }

        public Builder author(String author) {
            this.author = author;
            return this;
        }

        public Builder authorEmail(String authorEmail) {
            this.authorEmail = authorEmail;
            return this;
        }

        public Builder commitTime(Instant commitTime) {
            this.commitTime = commitTime;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder aiAssisted(boolean aiAssisted) {
            this.aiAssisted = aiAssisted;
            return this;
        }

        public Builder aiTool(AiTool aiTool) {
            this.aiTool = aiTool;
            return this;
        }

        public Builder aiConfidence(String aiConfidence) {
            this.aiConfidence = aiConfidence;
            return this;
        }

        public Builder filesChanged(List<String> filesChanged) {
            this.filesChanged = filesChanged;
            return this;
        }

        public CommitAttribution build() {
            return new CommitAttribution(this);
        }
    }
}
