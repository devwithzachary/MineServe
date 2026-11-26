#!/usr/bin/env bash
set -euo pipefail

# MineServe Unified Multi-Platform Release Builder
# Builds Android (APK + Play Store AAB), Linux packages, macOS Beta, and Windows Beta into release/.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

SKIP_ANDROID=0
SKIP_LINUX=0
SKIP_MACOS=0
SKIP_WINDOWS=0

usage() {
    cat <<'EOF'
MineServe Unified Multi-Platform Release Builder

Usage:
  ./scripts/build_all_releases.sh [options]

Options:
  --skip-android        Skip Android APK / Play Store AAB build
  --skip-linux          Skip Linux Desktop / Headless package builds
  --skip-macos          Skip remote Mac Studio .dmg / .tar.gz build
  --skip-windows        Skip remote Windows 11 VM .msi / .exe / .zip build
  --help, -h            Show this help message
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --skip-android) SKIP_ANDROID=1; shift ;;
        --skip-linux)   SKIP_LINUX=1; shift ;;
        --skip-macos)   SKIP_MACOS=1; shift ;;
        --skip-windows) SKIP_WINDOWS=1; shift ;;
        --help|-h)      usage; exit 0 ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
mkdir -p "$PROJECT_ROOT/release"

if [ "$SKIP_ANDROID" -eq 0 ]; then
    echo "==> [1/4] Building Android Release APK & Play Store AAB..."
    ./scripts/build_release_linux.sh
fi

if [ "$SKIP_LINUX" -eq 0 ]; then
    echo "==> [2/4] Building Linux Packages (.tar.gz, .deb, .rpm, .AppImage, .pkg.tar.zst)..."
    ./scripts/build_linux_packages.sh
fi

if [ "$SKIP_MACOS" -eq 0 ]; then
    echo "==> [3/4] Building macOS Beta (.dmg & portable .tar.gz) on Mac Studio..."
    ./scripts/build_macos_remote.sh --package --out release
fi

if [ "$SKIP_WINDOWS" -eq 0 ]; then
    echo "==> [4/4] Building Windows Beta (.msi, .exe, portable .zip) on Windows 11 VM..."
    ./scripts/build_windows_remote.sh --package --out release
fi

echo "==> Generating release/SHA256SUMS.txt..."
(
    cd "$PROJECT_ROOT/release"
    rm -f SHA256SUMS.txt
    find . -maxdepth 1 -type f ! -name "SHA256SUMS.txt" | sort | sed 's|^\./||' | xargs -r sha256sum > SHA256SUMS.txt
)

echo ""
echo "============================================================"
echo "All Release Artifacts Ready in release/ (v${VERSION_NAME})"
echo "============================================================"
ls -lh "$PROJECT_ROOT/release"
echo ""
echo "Next Distribution Commands:"
echo "  1. Upload release/* to GitHub Release tag '${VERSION_NAME}'"
echo "  2. Docker:  ./scripts/publish_docker.sh --all"
echo "  3. APT/RPM: ./scripts/publish_apt_rpm_repos.sh"
echo "  4. AUR:     ./scripts/publish_aur.sh --stage-only  (then --push)"
echo "  5. Winget:  ./scripts/publish_winget.sh"
