#!/usr/bin/env bash
set -euo pipefail

# MineServe GitHub Actions Secrets Setup Helper
# Uploads optional/release signing and publishing secrets to devwithzachary/MineServe via `gh secret set`.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

REPO="devwithzachary/MineServe"

usage() {
    cat <<'EOF'
MineServe GitHub Actions Secrets Setup Helper

Usage:
  ./scripts/setup_github_secrets.sh [options]

Options:
  --android             Upload Android release keystore + key.properties secrets
  --dockerhub           Upload DOCKERHUB_USERNAME and DOCKERHUB_TOKEN
  --gpg                 Upload GPG_PRIVATE_KEY, GPG_KEY_ID, and WEB_REPO_PAT for APT/RPM repos
  --aur                 Upload AUR_SSH_PRIVATE_KEY for AUR mineserve-bin
  --winget              Upload WINGET_PAT for microsoft/winget-pkgs PRs
  --all                 Run all setup prompts interactively
  --help, -h            Show this help message
EOF
}

require_gh() {
    if ! command -v gh >/dev/null 2>&1; then
        echo "Error: GitHub CLI ('gh') is not installed." >&2
        exit 1
    fi
    if ! gh auth status >/dev/null 2>&1; then
        echo "Error: Not logged in to GitHub CLI. Run 'gh auth login' first." >&2
        exit 1
    fi
}

setup_android() {
    if [ ! -f "$PROJECT_ROOT/key.properties" ]; then
        echo "Error: key.properties not found in $PROJECT_ROOT." >&2
        return 1
    fi
    local store_file store_pass key_alias key_pass ks_path
    store_file=$(grep -E '^storeFile=' "$PROJECT_ROOT/key.properties" | cut -d= -f2- | tr -d '\r')
    store_pass=$(grep -E '^storePassword=' "$PROJECT_ROOT/key.properties" | cut -d= -f2- | tr -d '\r')
    key_alias=$(grep -E '^keyAlias=' "$PROJECT_ROOT/key.properties" | cut -d= -f2- | tr -d '\r')
    key_pass=$(grep -E '^keyPassword=' "$PROJECT_ROOT/key.properties" | cut -d= -f2- | tr -d '\r')

    if [ -f "$PROJECT_ROOT/app/$store_file" ]; then
        ks_path="$PROJECT_ROOT/app/$store_file"
    elif [ -f "$PROJECT_ROOT/$store_file" ]; then
        ks_path="$PROJECT_ROOT/$store_file"
    else
        echo "Error: Keystore file '$store_file' not found in app/ or repo root." >&2
        return 1
    fi

    echo "==> Uploading Android release signing secrets to $REPO..."
    base64 -w 0 "$ks_path" | gh secret set ANDROID_KEYSTORE_BASE64 --repo "$REPO"
    printf '%s' "$store_pass" | gh secret set ANDROID_KEYSTORE_PASSWORD --repo "$REPO"
    printf '%s' "$key_alias" | gh secret set ANDROID_KEY_ALIAS --repo "$REPO"
    printf '%s' "$key_pass" | gh secret set ANDROID_KEY_PASSWORD --repo "$REPO"
    echo "==> Android signing secrets uploaded!"
}

setup_dockerhub() {
    read -r -p "Docker Hub username: " dh_user
    read -r -s -p "Docker Hub access token: " dh_token
    echo ""
    printf '%s' "$dh_user" | gh secret set DOCKERHUB_USERNAME --repo "$REPO"
    printf '%s' "$dh_token" | gh secret set DOCKERHUB_TOKEN --repo "$REPO"
    echo "==> Docker Hub secrets uploaded!"
}

setup_gpg() {
    local env_file="$SCRIPT_DIR/.gpg-signing.env"
    if [ ! -f "$env_file" ]; then
        "$SCRIPT_DIR/publish_apt_rpm_repos.sh" --init-gpg
    fi
    # shellcheck disable=SC1090
    source "$env_file"
    if [ -z "${MINESERVE_GPG_KEY:-}" ]; then
        echo "Error: MINESERVE_GPG_KEY missing in $env_file." >&2
        return 1
    fi
    echo "==> Uploading GPG_KEY_ID and GPG_PRIVATE_KEY to $REPO..."
    printf '%s' "$MINESERVE_GPG_KEY" | gh secret set GPG_KEY_ID --repo "$REPO"
    gpg --batch --armor --export-secret-keys "$MINESERVE_GPG_KEY" | gh secret set GPG_PRIVATE_KEY --repo "$REPO"
    read -r -s -p "GitHub PAT with write access to devwithzachary/MineServe-web (WEB_REPO_PAT): " web_pat
    echo ""
    printf '%s' "$web_pat" | gh secret set WEB_REPO_PAT --repo "$REPO"
    echo "==> GPG & MineServe-web secrets uploaded!"
}

setup_aur() {
    local default_key="$HOME/.ssh/id_ed25519"
    read -r -p "Path to AUR SSH private key [$default_key]: " key_path
    key_path="${key_path:-$default_key}"
    if [ ! -f "$key_path" ]; then
        echo "Error: SSH key not found at $key_path." >&2
        return 1
    fi
    gh secret set AUR_SSH_PRIVATE_KEY --repo "$REPO" < "$key_path"
    echo "==> AUR_SSH_PRIVATE_KEY uploaded!"
}

setup_winget() {
    read -r -s -p "GitHub PAT with public_repo scope for Winget PRs (WINGET_PAT): " w_pat
    echo ""
    printf '%s' "$w_pat" | gh secret set WINGET_PAT --repo "$REPO"
    echo "==> WINGET_PAT uploaded!"
}

if [ $# -eq 0 ]; then
    usage
    exit 0
fi

require_gh

while [ $# -gt 0 ]; do
    case "$1" in
        --android)   setup_android; shift ;;
        --dockerhub) setup_dockerhub; shift ;;
        --gpg)       setup_gpg; shift ;;
        --aur)       setup_aur; shift ;;
        --winget)    setup_winget; shift ;;
        --all)
            setup_android
            setup_dockerhub
            setup_gpg
            setup_aur
            setup_winget
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done
