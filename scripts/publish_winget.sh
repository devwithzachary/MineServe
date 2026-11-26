#!/usr/bin/env bash
set -euo pipefail

# MineServe Windows Winget Manifest Generator
# Generates winget-pkgs manifests in release/winget/ for DevWithZachary.MineServe.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
MSI_PATH=""

usage() {
    cat <<'EOF'
MineServe Windows Winget Manifest Generator

Usage:
  ./scripts/publish_winget.sh [options]

Options:
  --version <ver>       Override version (default: versionName in app/build.gradle.kts)
  --msi <path>          Override path to the .msi installer (default: release/MineServe-<ver>.msi)
  --help, -h            Show this help message
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --version)
            VERSION_NAME="${2:-}"
            shift 2
            ;;
        --msi)
            MSI_PATH="${2:-}"
            shift 2
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

if [ -z "$MSI_PATH" ]; then
    MSI_PATH="$PROJECT_ROOT/release/MineServe-${VERSION_NAME}.msi"
fi

if [ ! -f "$MSI_PATH" ]; then
    ALT_MSI=$(find "$PROJECT_ROOT/release" -maxdepth 1 -name "MineServe-*.msi" | head -n 1 || true)
    if [ -n "$ALT_MSI" ] && [ -f "$ALT_MSI" ]; then
        MSI_PATH="$ALT_MSI"
    else
        echo "Error: MSI installer not found at $MSI_PATH." >&2
        echo "Build Windows packages first with: ./scripts/build_windows_remote.sh --package --out release" >&2
        exit 1
    fi
fi

MSI_FILENAME=$(basename "$MSI_PATH")
MSI_SHA256=$(sha256sum "$MSI_PATH" | awk '{print toupper($1)}')
MANIFEST_DIR="$PROJECT_ROOT/release/winget/manifests/d/DevWithZachary/MineServe/${VERSION_NAME}"
mkdir -p "$MANIFEST_DIR"

INSTALLER_URL="https://github.com/devwithzachary/MineServe/releases/download/${VERSION_NAME}/${MSI_FILENAME}"

cat > "$MANIFEST_DIR/DevWithZachary.MineServe.yaml" <<EOF
PackageIdentifier: DevWithZachary.MineServe
PackageVersion: ${VERSION_NAME}
DefaultLocale: en-US
ManifestType: version
ManifestVersion: 1.6.0
EOF

cat > "$MANIFEST_DIR/DevWithZachary.MineServe.installer.yaml" <<EOF
PackageIdentifier: DevWithZachary.MineServe
PackageVersion: ${VERSION_NAME}
Platform:
  - Windows.Desktop
MinimumOSVersion: 10.0.17763.0
InstallerType: wix
Scope: user
InstallModes:
  - interactive
  - silent
  - silentWithProgress
UpgradeBehavior: install
AppsAndFeaturesEntries:
  - DisplayName: MineServe
    Publisher: DevWithZachary
    UpgradeCode: '{7E53F74B-2D49-4F7C-8A45-4C8D1F61B901}'
Installers:
  - Architecture: x64
    InstallerUrl: ${INSTALLER_URL}
    InstallerSha256: ${MSI_SHA256}
ManifestType: installer
ManifestVersion: 1.6.0
EOF

cat > "$MANIFEST_DIR/DevWithZachary.MineServe.locale.en-US.yaml" <<EOF
PackageIdentifier: DevWithZachary.MineServe
PackageVersion: ${VERSION_NAME}
PackageLocale: en-US
Publisher: DevWithZachary
PublisherUrl: https://mineserve.app
PublisherSupportUrl: https://github.com/devwithzachary/MineServe/issues
PackageName: MineServe
PackageUrl: https://mineserve.app
License: GPL-3.0
LicenseUrl: https://github.com/devwithzachary/MineServe/blob/main/LICENSE
ShortDescription: Cross-platform Minecraft Java, Modded, Proxy, and Bedrock Server Manager.
Description: MineServe is an open-source Minecraft server manager supporting Paper, Purpur, Folia, Fabric, NeoForge, Forge, Vanilla, Velocity, BungeeCord, and Bedrock Dedicated Servers with portable OpenJDK management, Bore/Playit/UPnP networking, and Web Remote Admin.
Tags:
  - minecraft
  - minecraft-server
  - server-manager
  - self-hosted
  - java
ReleaseNotesUrl: https://github.com/devwithzachary/MineServe/releases/tag/${VERSION_NAME}
ManifestType: defaultLocale
ManifestVersion: 1.6.0
EOF

echo "==> Generated Winget 1.6.0 manifests in $MANIFEST_DIR"
echo "    InstallerUrl: $INSTALLER_URL"
echo "    SHA256:       $MSI_SHA256"
echo ""
echo "Next steps to publish to microsoft/winget-pkgs:"
echo "  1. Ensure $MSI_FILENAME is uploaded to GitHub Release $VERSION_NAME."
echo "  2. Validate locally (on Windows): winget validate --manifest release\\winget\\manifests\\d\\DevWithZachary\\MineServe\\${VERSION_NAME}"
echo "  3. Submit PR using wingetcreate or by copying $MANIFEST_DIR into your fork of microsoft/winget-pkgs."
