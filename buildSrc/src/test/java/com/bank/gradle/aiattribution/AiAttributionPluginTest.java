package com.bank.gradle.aiattribution;

import com.bank.gradle.aiattribution.model.AiTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AI Attribution plugin components.
 */
class AiAttributionPluginTest {

    @Test
    @DisplayName("AiTool should parse GitHub Copilot variations")
    void testAiToolParseCopilot() {
        assertEquals(AiTool.GITHUB_COPILOT, AiTool.fromTrailerValue("github-copilot"));
        assertEquals(AiTool.GITHUB_COPILOT, AiTool.fromTrailerValue("GitHub Copilot"));
        assertEquals(AiTool.GITHUB_COPILOT, AiTool.fromTrailerValue("copilot"));
        assertEquals(AiTool.GITHUB_COPILOT, AiTool.fromTrailerValue("COPILOT"));
    }

    @Test
    @DisplayName("AiTool should parse Devin variations")
    void testAiToolParseDevin() {
        assertEquals(AiTool.DEVIN, AiTool.fromTrailerValue("devin"));
        assertEquals(AiTool.DEVIN, AiTool.fromTrailerValue("Devin AI"));
        assertEquals(AiTool.DEVIN, AiTool.fromTrailerValue("DEVIN"));
    }

    @Test
    @DisplayName("AiTool should parse Claude variations")
    void testAiToolParseClaude() {
        assertEquals(AiTool.CLAUDE, AiTool.fromTrailerValue("claude"));
        assertEquals(AiTool.CLAUDE, AiTool.fromTrailerValue("Claude"));
        assertEquals(AiTool.CLAUDE, AiTool.fromTrailerValue("claude-3"));
    }

    @Test
    @DisplayName("AiTool should return NONE for null or empty")
    void testAiToolParseNone() {
        assertEquals(AiTool.NONE, AiTool.fromTrailerValue(null));
        assertEquals(AiTool.NONE, AiTool.fromTrailerValue(""));
        assertEquals(AiTool.NONE, AiTool.fromTrailerValue("   "));
        assertEquals(AiTool.NONE, AiTool.fromTrailerValue("none"));
    }

    @Test
    @DisplayName("AiTool should return OTHER for unknown tools")
    void testAiToolParseOther() {
        assertEquals(AiTool.OTHER, AiTool.fromTrailerValue("custom-ai"));
        assertEquals(AiTool.OTHER, AiTool.fromTrailerValue("internal-tool"));
    }

    @Test
    @DisplayName("AiTool should have correct display names")
    void testAiToolDisplayNames() {
        assertEquals("GitHub Copilot", AiTool.GITHUB_COPILOT.getDisplayName());
        assertEquals("Devin AI", AiTool.DEVIN.getDisplayName());
        assertEquals("Claude", AiTool.CLAUDE.getDisplayName());
        assertEquals("No AI Assistance", AiTool.NONE.getDisplayName());
    }

    @Test
    @DisplayName("AiTool should have correct trailer IDs")
    void testAiToolTrailerIds() {
        assertEquals("github-copilot", AiTool.GITHUB_COPILOT.getTrailerId());
        assertEquals("devin", AiTool.DEVIN.getTrailerId());
        assertEquals("claude", AiTool.CLAUDE.getTrailerId());
        assertEquals("none", AiTool.NONE.getTrailerId());
    }
}
