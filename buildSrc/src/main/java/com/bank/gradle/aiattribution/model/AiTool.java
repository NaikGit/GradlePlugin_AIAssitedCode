package com.bank.gradle.aiattribution.model;

/**
 * Enum representing supported AI tools that can be tracked.
 */
public enum AiTool {
    GITHUB_COPILOT("github-copilot", "GitHub Copilot"),
    DEVIN("devin", "Devin AI"),
    CLAUDE("claude", "Claude"),
    CHATGPT("chatgpt", "ChatGPT"),
    CODEWHISPERER("codewhisperer", "AWS CodeWhisperer"),
    OTHER("other", "Other AI Tool"),
    NONE("none", "No AI Assistance");

    private final String trailerId;
    private final String displayName;

    AiTool(String trailerId, String displayName) {
        this.trailerId = trailerId;
        this.displayName = displayName;
    }

    public String getTrailerId() {
        return trailerId;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse an AI tool from a Git trailer value.
     */
    public static AiTool fromTrailerValue(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        
        String normalized = value.toLowerCase().trim();
        
        for (AiTool tool : values()) {
            if (tool.trailerId.equals(normalized) || 
                tool.displayName.toLowerCase().equals(normalized)) {
                return tool;
            }
        }
        
        // Handle common variations
        if (normalized.contains("copilot")) return GITHUB_COPILOT;
        if (normalized.contains("devin")) return DEVIN;
        if (normalized.contains("claude")) return CLAUDE;
        if (normalized.contains("gpt") || normalized.contains("openai")) return CHATGPT;
        if (normalized.contains("whisperer") || normalized.contains("amazon")) return CODEWHISPERER;
        
        return OTHER;
    }
}
