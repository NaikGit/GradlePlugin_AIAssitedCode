#!/bin/bash
#
# AI Attribution Hooks - Installation Script
#
# This script installs the AI attribution Git hooks into your repository.
#
# Usage:
#   ./install-hooks.sh              # Install to current repo
#   ./install-hooks.sh /path/to/repo # Install to specific repo
#   ./install-hooks.sh --global     # Install as Git template (all new repos)
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'
BOLD='\033[1m'

print_banner() {
    echo ""
    echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}     ${BOLD}AI Attribution Git Hooks Installer${NC}                     ${CYAN}║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

install_to_repo() {
    local repo_path="$1"
    local hooks_dir="$repo_path/.git/hooks"
    
    if [ ! -d "$repo_path/.git" ]; then
        echo -e "${RED}Error: '$repo_path' is not a Git repository.${NC}"
        exit 1
    fi
    
    echo -e "${CYAN}Installing hooks to:${NC} $repo_path"
    
    # Backup existing hooks
    for hook in prepare-commit-msg commit-msg; do
        if [ -f "$hooks_dir/$hook" ]; then
            echo -e "${YELLOW}Backing up existing $hook to $hook.backup${NC}"
            cp "$hooks_dir/$hook" "$hooks_dir/$hook.backup"
        fi
    done
    
    # Copy hooks
    cp "$SCRIPT_DIR/prepare-commit-msg" "$hooks_dir/"
    cp "$SCRIPT_DIR/commit-msg" "$hooks_dir/"
    
    # Make executable
    chmod +x "$hooks_dir/prepare-commit-msg"
    chmod +x "$hooks_dir/commit-msg"
    
    echo -e "${GREEN}✓ Hooks installed successfully!${NC}"
    echo ""
    echo "Configuration (set as environment variables):"
    echo "  AI_ATTRIBUTION_ENABLED=true      # Enable/disable prompts"
    echo "  AI_ATTRIBUTION_REQUIRED=false    # Require trailers (blocks commit if missing)"
    echo "  AI_ATTRIBUTION_SKIP_MERGE=true   # Skip for merge commits"
    echo ""
}

install_global() {
    local template_dir="$HOME/.git-templates/hooks"
    
    echo -e "${CYAN}Installing hooks globally (Git template)...${NC}"
    
    mkdir -p "$template_dir"
    
    cp "$SCRIPT_DIR/prepare-commit-msg" "$template_dir/"
    cp "$SCRIPT_DIR/commit-msg" "$template_dir/"
    chmod +x "$template_dir/prepare-commit-msg"
    chmod +x "$template_dir/commit-msg"
    
    # Configure Git to use template
    git config --global init.templateDir "$HOME/.git-templates"
    
    echo -e "${GREEN}✓ Global template installed!${NC}"
    echo ""
    echo "All new repositories created with 'git init' or 'git clone' will"
    echo "automatically have the AI attribution hooks."
    echo ""
    echo "To apply to an existing repo:"
    echo "  cd /path/to/repo && git init"
    echo ""
}

uninstall_from_repo() {
    local repo_path="$1"
    local hooks_dir="$repo_path/.git/hooks"
    
    echo -e "${CYAN}Removing hooks from:${NC} $repo_path"
    
    for hook in prepare-commit-msg commit-msg; do
        if [ -f "$hooks_dir/$hook" ]; then
            rm "$hooks_dir/$hook"
            echo -e "${GREEN}✓ Removed $hook${NC}"
        fi
        # Restore backup if exists
        if [ -f "$hooks_dir/$hook.backup" ]; then
            mv "$hooks_dir/$hook.backup" "$hooks_dir/$hook"
            echo -e "${GREEN}✓ Restored $hook from backup${NC}"
        fi
    done
    
    echo -e "${GREEN}✓ Hooks uninstalled.${NC}"
}

# Main
print_banner

case "${1:-}" in
    --global|-g)
        install_global
        ;;
    --uninstall|-u)
        repo_path="${2:-.}"
        uninstall_from_repo "$repo_path"
        ;;
    --help|-h)
        echo "Usage:"
        echo "  ./install-hooks.sh              Install to current repository"
        echo "  ./install-hooks.sh /path/to/repo Install to specific repository"
        echo "  ./install-hooks.sh --global      Install as Git template (all new repos)"
        echo "  ./install-hooks.sh --uninstall   Remove hooks from current repository"
        echo ""
        ;;
    *)
        repo_path="${1:-.}"
        install_to_repo "$repo_path"
        ;;
esac
