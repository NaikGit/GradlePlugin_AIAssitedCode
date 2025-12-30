# AI Attribution Git Hooks

These Git hooks help enforce AI attribution practices by prompting developers to declare AI assistance when committing code.

## Quick Install

```bash
# Install to current repository
./install-hooks.sh

# Install globally (all new repos)
./install-hooks.sh --global
```

## Hooks Included

### `prepare-commit-msg`
**Interactive prompt** that asks developers about AI assistance before finalizing the commit message.

Features:
- Prompts: "Was this commit AI-assisted? [y/n/s]"
- Asks which tool (Copilot, Devin, Claude, etc.)
- Optionally asks for confidence level (high/medium/low)
- Automatically appends Git trailers to commit message
- Skips merge commits and non-interactive sessions

### `commit-msg`
**Validation hook** that checks for AI attribution trailers.

Features:
- Warns if trailers are missing (configurable)
- Can block commits without attribution (enforcement mode)
- Validates trailer format and values

## Configuration

Set these environment variables to customize behavior:

| Variable | Default | Description |
|----------|---------|-------------|
| `AI_ATTRIBUTION_ENABLED` | `true` | Enable/disable the hooks entirely |
| `AI_ATTRIBUTION_REQUIRED` | `false` | Block commits without attribution |
| `AI_ATTRIBUTION_SKIP_MERGE` | `true` | Skip prompts for merge commits |
| `AI_ATTRIBUTION_WARN_MISSING` | `true` | Show warning if trailers missing |

### Examples

```bash
# Disable temporarily
AI_ATTRIBUTION_ENABLED=false git commit -m "Quick fix"

# Require attribution (enforcement)
export AI_ATTRIBUTION_REQUIRED=true

# Skip for this commit
git commit --no-verify -m "Emergency hotfix"
```

## Git Trailer Format

The hooks add trailers in this format:

```
Fix payment validation edge case

This commit fixes the amount rounding issue.

AI-Tool: github-copilot
AI-Assisted: true
AI-Confidence: high
```

### Supported Trailers

| Trailer | Required | Values |
|---------|----------|--------|
| `AI-Tool` | Yes | `github-copilot`, `devin`, `claude`, `chatgpt`, `codewhisperer`, `other`, `none` |
| `AI-Assisted` | Yes | `true`, `false` |
| `AI-Confidence` | No | `high`, `medium`, `low` |

## Team Rollout Strategies

### Option 1: Repository Template
1. Add hooks to a Git template directory
2. Developers clone with template

```bash
# One-time setup by admin
./install-hooks.sh --global

# Now all 'git init' and 'git clone' get the hooks automatically
```

### Option 2: Shared Repository
1. Add `hooks/` directory to your repo (as we've done here)
2. Add setup script to project README
3. Developers run setup after cloning

```bash
# In project README:
# After cloning, run:
./hooks/install-hooks.sh
```

### Option 3: Husky (for npm projects)
```bash
npm install husky --save-dev
npx husky install
cp hooks/prepare-commit-msg .husky/
cp hooks/commit-msg .husky/
```

### Option 4: Pre-commit Framework
```yaml
# .pre-commit-config.yaml
repos:
  - repo: local
    hooks:
      - id: ai-attribution
        name: AI Attribution
        entry: ./hooks/prepare-commit-msg
        language: script
        stages: [prepare-commit-msg]
```

## Uninstall

```bash
./install-hooks.sh --uninstall
```

Or manually:
```bash
rm .git/hooks/prepare-commit-msg
rm .git/hooks/commit-msg
```

## Troubleshooting

### Hook not running?
- Check it's executable: `chmod +x .git/hooks/prepare-commit-msg`
- Verify it's in the right location: `.git/hooks/`

### Prompts not appearing?
- Hooks require an interactive terminal
- In CI/CD, hooks run in non-interactive mode (no prompts)

### Want to skip once?
```bash
git commit --no-verify -m "Skip hooks this time"
```

## Integration with AI Attribution Plugin

These hooks work seamlessly with the Gradle AI Attribution Plugin:

1. Hooks add trailers to commits
2. Plugin reads trailers and generates reports
3. Reports show AI assistance breakdown

```bash
# After committing with hooks...
./gradlew aiAttributionReport

# See results
open build/reports/ai-attribution/ai-attribution-report.html
```
