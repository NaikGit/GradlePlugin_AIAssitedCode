package com.bank.gradle.aiattribution.parser;

import com.bank.gradle.aiattribution.model.AiTool;
import com.bank.gradle.aiattribution.model.CommitAttribution;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Git commits and extracts AI attribution information from trailers.
 */
public class GitCommitParser {

    private static final Logger LOGGER = Logging.getLogger(GitCommitParser.class);

    // Patterns for Git trailers
    private static final Pattern TRAILER_PATTERN = Pattern.compile(
        "^([A-Za-z][A-Za-z0-9-]*):\\s*(.+)$", Pattern.MULTILINE
    );
    
    // Supported trailer names (case-insensitive matching)
    private static final String TRAILER_AI_TOOL = "AI-Tool";
    private static final String TRAILER_AI_ASSISTED = "AI-Assisted";
    private static final String TRAILER_AI_CONFIDENCE = "AI-Confidence";

    private final File projectDir;
    private final int maxCommits;
    private final String sinceCommit;
    private final String untilCommit;

    public GitCommitParser(File projectDir, int maxCommits, String sinceCommit, String untilCommit) {
        this.projectDir = projectDir;
        this.maxCommits = maxCommits;
        this.sinceCommit = sinceCommit;
        this.untilCommit = untilCommit;
    }

    /**
     * Parse commits and extract AI attribution data.
     */
    public List<CommitAttribution> parseCommits() throws IOException {
        List<CommitAttribution> attributions = new ArrayList<>();
        
        try (Git git = Git.open(projectDir);
             Repository repository = git.getRepository()) {
            
            LogCommand logCommand = git.log();
            
            // Configure commit range
            if (untilCommit != null && !untilCommit.isBlank()) {
                ObjectId until = repository.resolve(untilCommit);
                if (until != null) {
                    logCommand.add(until);
                }
            } else {
                logCommand.all();
            }
            
            if (sinceCommit != null && !sinceCommit.isBlank()) {
                ObjectId since = repository.resolve(sinceCommit);
                if (since != null) {
                    logCommand.not(since);
                }
            }
            
            logCommand.setMaxCount(maxCommits);
            
            Iterable<RevCommit> commits = logCommand.call();
            
            for (RevCommit commit : commits) {
                CommitAttribution attribution = parseCommit(commit, repository);
                attributions.add(attribution);
            }
            
        } catch (Exception e) {
            throw new IOException("Failed to parse Git commits", e);
        }
        
        LOGGER.lifecycle("Parsed {} commits, {} with AI attribution", 
            attributions.size(),
            attributions.stream().filter(CommitAttribution::isAiAssisted).count());
        
        return attributions;
    }

    /**
     * Get the current branch name.
     */
    public String getCurrentBranch() throws IOException {
        try (Git git = Git.open(projectDir)) {
            return git.getRepository().getBranch();
        }
    }

    /**
     * Get the HEAD commit hash.
     */
    public String getHeadCommit() throws IOException {
        try (Git git = Git.open(projectDir)) {
            ObjectId head = git.getRepository().resolve("HEAD");
            return head != null ? head.getName() : null;
        }
    }

    private CommitAttribution parseCommit(RevCommit commit, Repository repository) {
        String fullMessage = commit.getFullMessage();
        Map<String, String> trailers = extractTrailers(fullMessage);
        
        // Determine AI assistance
        boolean aiAssisted = isAiAssisted(trailers);
        AiTool aiTool = extractAiTool(trailers);
        String aiConfidence = trailers.getOrDefault(TRAILER_AI_CONFIDENCE, null);
        
        // Get files changed
        List<String> filesChanged = getFilesChanged(commit, repository);
        
        return CommitAttribution.builder()
            .commitHash(commit.getName())
            .author(commit.getAuthorIdent().getName())
            .authorEmail(commit.getAuthorIdent().getEmailAddress())
            .commitTime(Instant.ofEpochSecond(commit.getCommitTime()))
            .message(commit.getShortMessage())
            .aiAssisted(aiAssisted)
            .aiTool(aiTool)
            .aiConfidence(aiConfidence)
            .filesChanged(filesChanged)
            .build();
    }

    /**
     * Extract Git trailers from commit message.
     * Trailers are key-value pairs at the end of commit messages.
     */
    private Map<String, String> extractTrailers(String fullMessage) {
        Map<String, String> trailers = new HashMap<>();
        
        // Trailers are typically in the last paragraph of the commit message
        String[] paragraphs = fullMessage.split("\n\n");
        if (paragraphs.length == 0) {
            return trailers;
        }
        
        // Check each line of the last paragraph for trailer format
        String lastParagraph = paragraphs[paragraphs.length - 1];
        Matcher matcher = TRAILER_PATTERN.matcher(lastParagraph);
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            trailers.put(key, value);
        }
        
        return trailers;
    }

    private boolean isAiAssisted(Map<String, String> trailers) {
        // Check AI-Assisted trailer
        for (Map.Entry<String, String> entry : trailers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(TRAILER_AI_ASSISTED)) {
                String value = entry.getValue().toLowerCase();
                return "true".equals(value) || "yes".equals(value) || "1".equals(value);
            }
        }
        
        // Check if AI-Tool trailer exists (implies AI assistance)
        for (String key : trailers.keySet()) {
            if (key.equalsIgnoreCase(TRAILER_AI_TOOL)) {
                String value = trailers.get(key).toLowerCase();
                return !value.equals("none") && !value.equals("false");
            }
        }
        
        return false;
    }

    private AiTool extractAiTool(Map<String, String> trailers) {
        for (Map.Entry<String, String> entry : trailers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(TRAILER_AI_TOOL)) {
                return AiTool.fromTrailerValue(entry.getValue());
            }
        }
        return AiTool.NONE;
    }

    private List<String> getFilesChanged(RevCommit commit, Repository repository) {
        List<String> files = new ArrayList<>();
        
        try {
            if (commit.getParentCount() == 0) {
                // Initial commit - compare with empty tree
                return files;
            }
            
            RevCommit parent = commit.getParent(0);
            
            try (ObjectReader reader = repository.newObjectReader();
                 DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                
                diffFormatter.setRepository(repository);
                
                CanonicalTreeParser oldTreeParser = new CanonicalTreeParser();
                oldTreeParser.reset(reader, parent.getTree());
                
                CanonicalTreeParser newTreeParser = new CanonicalTreeParser();
                newTreeParser.reset(reader, commit.getTree());
                
                List<DiffEntry> diffs = diffFormatter.scan(oldTreeParser, newTreeParser);
                
                for (DiffEntry diff : diffs) {
                    String path = diff.getNewPath();
                    if ("/dev/null".equals(path)) {
                        path = diff.getOldPath();
                    }
                    files.add(path);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get files changed for commit {}: {}", 
                commit.getName().substring(0, 7), e.getMessage());
        }
        
        return files;
    }
}
