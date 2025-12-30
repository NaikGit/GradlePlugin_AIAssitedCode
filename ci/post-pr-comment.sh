#!/bin/bash
#
# AI Attribution PR Comment Poster
#
# A generic script to post AI attribution comments to PRs.
# Works with GitHub, Bitbucket, GitLab, and Azure DevOps.
#
# Usage:
#   ./post-pr-comment.sh                    # Auto-detect from environment
#   ./post-pr-comment.sh --github           # Force GitHub
#   ./post-pr-comment.sh --bitbucket        # Force Bitbucket
#   ./post-pr-comment.sh --gitlab           # Force GitLab
#   ./post-pr-comment.sh --azure            # Force Azure DevOps
#
# Environment Variables Required:
#   GitHub:    GITHUB_TOKEN, GITHUB_REPOSITORY, PR_NUMBER (or CHANGE_ID)
#   Bitbucket: BITBUCKET_USER, BITBUCKET_PASS, BITBUCKET_REPO_FULL_NAME, BITBUCKET_PR_ID
#   GitLab:    GITLAB_TOKEN, CI_PROJECT_ID, CI_MERGE_REQUEST_IID
#   Azure:     AZURE_TOKEN, SYSTEM_TEAMFOUNDATIONCOLLECTIONURI, BUILD_REPOSITORY_ID, SYSTEM_PULLREQUEST_PULLREQUESTID
#

set -e

COMMENT_FILE="${COMMENT_FILE:-build/reports/ai-attribution/ai-attribution-report.md}"
SCRIPT_NAME=$(basename "$0")

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_file() {
    if [ ! -f "$COMMENT_FILE" ]; then
        log_error "Comment file not found: $COMMENT_FILE"
        log_info "Run './gradlew aiAttributionReport -PoutputFormats=pr-comment' first"
        exit 1
    fi
}

detect_platform() {
    if [ -n "$GITHUB_ACTIONS" ] || [ -n "$GITHUB_TOKEN" ]; then
        echo "github"
    elif [ -n "$BITBUCKET_PIPELINE" ] || [ -n "$BITBUCKET_REPO_FULL_NAME" ]; then
        echo "bitbucket"
    elif [ -n "$GITLAB_CI" ] || [ -n "$CI_MERGE_REQUEST_IID" ]; then
        echo "gitlab"
    elif [ -n "$AZURE_PIPELINES" ] || [ -n "$SYSTEM_PULLREQUEST_PULLREQUESTID" ]; then
        echo "azure"
    elif [ -n "$JENKINS_URL" ]; then
        # Jenkins - try to detect from GIT_URL
        if echo "$GIT_URL" | grep -q "github.com"; then
            echo "github"
        elif echo "$GIT_URL" | grep -q "bitbucket"; then
            echo "bitbucket"
        elif echo "$GIT_URL" | grep -q "gitlab"; then
            echo "gitlab"
        else
            echo "unknown"
        fi
    else
        echo "unknown"
    fi
}

post_github_comment() {
    log_info "Posting comment to GitHub..."
    
    local token="${GITHUB_TOKEN}"
    local repo="${GITHUB_REPOSITORY}"
    local pr="${PR_NUMBER:-$CHANGE_ID}"
    
    if [ -z "$token" ]; then
        log_error "GITHUB_TOKEN not set"
        exit 1
    fi
    
    if [ -z "$repo" ]; then
        log_error "GITHUB_REPOSITORY not set"
        exit 1
    fi
    
    if [ -z "$pr" ]; then
        log_warn "No PR number found (PR_NUMBER or CHANGE_ID). Skipping comment."
        return 0
    fi
    
    local comment
    comment=$(cat "$COMMENT_FILE")
    
    # Check for existing comment
    local existing_id
    existing_id=$(curl -s -H "Authorization: token $token" \
        "https://api.github.com/repos/$repo/issues/$pr/comments" | \
        jq -r '.[] | select(.body | contains("AI Attribution Summary")) | .id' | head -1)
    
    local json_payload
    json_payload=$(jq -n --arg body "$comment" '{"body": $body}')
    
    if [ -n "$existing_id" ] && [ "$existing_id" != "null" ]; then
        # Update existing comment
        curl -s -X PATCH \
            -H "Authorization: token $token" \
            -H "Accept: application/vnd.github.v3+json" \
            -d "$json_payload" \
            "https://api.github.com/repos/$repo/issues/comments/$existing_id" > /dev/null
        log_info "Updated existing comment on PR #$pr"
    else
        # Create new comment
        curl -s -X POST \
            -H "Authorization: token $token" \
            -H "Accept: application/vnd.github.v3+json" \
            -d "$json_payload" \
            "https://api.github.com/repos/$repo/issues/$pr/comments" > /dev/null
        log_info "Posted new comment on PR #$pr"
    fi
}

post_bitbucket_comment() {
    log_info "Posting comment to Bitbucket..."
    
    local user="${BITBUCKET_USER}"
    local pass="${BITBUCKET_PASS}"
    local repo="${BITBUCKET_REPO_FULL_NAME}"
    local pr="${BITBUCKET_PR_ID}"
    
    if [ -z "$user" ] || [ -z "$pass" ]; then
        log_error "BITBUCKET_USER and BITBUCKET_PASS must be set"
        exit 1
    fi
    
    if [ -z "$repo" ]; then
        log_error "BITBUCKET_REPO_FULL_NAME not set"
        exit 1
    fi
    
    if [ -z "$pr" ]; then
        log_warn "No PR ID found (BITBUCKET_PR_ID). Skipping comment."
        return 0
    fi
    
    local comment
    comment=$(cat "$COMMENT_FILE")
    
    local json_payload
    json_payload=$(jq -n --arg body "$comment" '{"content": {"raw": $body}}')
    
    curl -s -X POST \
        -u "$user:$pass" \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        "https://api.bitbucket.org/2.0/repositories/$repo/pullrequests/$pr/comments" > /dev/null
    
    log_info "Posted comment on Bitbucket PR #$pr"
}

post_gitlab_comment() {
    log_info "Posting comment to GitLab..."
    
    local token="${GITLAB_TOKEN}"
    local project="${CI_PROJECT_ID}"
    local mr="${CI_MERGE_REQUEST_IID}"
    
    if [ -z "$token" ]; then
        log_error "GITLAB_TOKEN not set"
        exit 1
    fi
    
    if [ -z "$project" ]; then
        log_error "CI_PROJECT_ID not set"
        exit 1
    fi
    
    if [ -z "$mr" ]; then
        log_warn "No MR ID found (CI_MERGE_REQUEST_IID). Skipping comment."
        return 0
    fi
    
    local comment
    comment=$(cat "$COMMENT_FILE")
    
    curl -s -X POST \
        -H "PRIVATE-TOKEN: $token" \
        -H "Content-Type: application/json" \
        -d "{\"body\": $(echo "$comment" | jq -Rs .)}" \
        "https://gitlab.com/api/v4/projects/$project/merge_requests/$mr/notes" > /dev/null
    
    log_info "Posted comment on GitLab MR !$mr"
}

post_azure_comment() {
    log_info "Posting comment to Azure DevOps..."
    
    local token="${AZURE_TOKEN}"
    local org_url="${SYSTEM_TEAMFOUNDATIONCOLLECTIONURI}"
    local repo_id="${BUILD_REPOSITORY_ID}"
    local pr="${SYSTEM_PULLREQUEST_PULLREQUESTID}"
    
    if [ -z "$token" ]; then
        log_error "AZURE_TOKEN not set"
        exit 1
    fi
    
    if [ -z "$pr" ]; then
        log_warn "No PR ID found (SYSTEM_PULLREQUEST_PULLREQUESTID). Skipping comment."
        return 0
    fi
    
    local comment
    comment=$(cat "$COMMENT_FILE")
    
    # Extract project from org_url
    local project="${SYSTEM_TEAMPROJECT}"
    
    curl -s -X POST \
        -u ":$token" \
        -H "Content-Type: application/json" \
        -d "{\"comments\": [{\"content\": $(echo "$comment" | jq -Rs .)}], \"status\": 1}" \
        "${org_url}${project}/_apis/git/repositories/${repo_id}/pullRequests/${pr}/threads?api-version=6.0" > /dev/null
    
    log_info "Posted comment on Azure DevOps PR #$pr"
}

print_usage() {
    echo "Usage: $SCRIPT_NAME [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --github      Post to GitHub"
    echo "  --bitbucket   Post to Bitbucket"
    echo "  --gitlab      Post to GitLab"
    echo "  --azure       Post to Azure DevOps"
    echo "  --auto        Auto-detect platform (default)"
    echo "  --help        Show this help"
    echo ""
    echo "Environment:"
    echo "  COMMENT_FILE  Path to comment file (default: build/reports/ai-attribution/ai-attribution-report.md)"
}

# Main
main() {
    local platform=""
    
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --github) platform="github"; shift ;;
            --bitbucket) platform="bitbucket"; shift ;;
            --gitlab) platform="gitlab"; shift ;;
            --azure) platform="azure"; shift ;;
            --auto) platform=""; shift ;;
            --help) print_usage; exit 0 ;;
            *) log_error "Unknown option: $1"; print_usage; exit 1 ;;
        esac
    done
    
    # Check comment file exists
    check_file
    
    # Detect platform if not specified
    if [ -z "$platform" ]; then
        platform=$(detect_platform)
        log_info "Auto-detected platform: $platform"
    fi
    
    # Post comment
    case $platform in
        github) post_github_comment ;;
        bitbucket) post_bitbucket_comment ;;
        gitlab) post_gitlab_comment ;;
        azure) post_azure_comment ;;
        *)
            log_warn "Could not detect platform. Please specify --github, --bitbucket, --gitlab, or --azure"
            log_info "Comment saved to: $COMMENT_FILE"
            exit 0
            ;;
    esac
}

main "$@"
