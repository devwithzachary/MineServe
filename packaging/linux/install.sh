#!/usr/bin/env bash
set -euo pipefail

# MineServe Universal Linux 1-Line Installer
# Usage:
#   curl -fsSL https://mineserve.app/install.sh | bash
#   curl -fsSL https://mineserve.app/install.sh | bash -s -- --headless
#   curl -fsSL https://mineserve.app/install.sh | bash -s -- --user
#   curl -fsSL https://mineserve.app/install.sh | bash -s -- --uninstall

REPO="devwithzachary/MineServe"
GITHUB_API_LATEST="https://api.github.com/repos/${REPO}/releases/latest"
GITHUB_DOWNLOAD_BASE="https://github.com/${REPO}/releases/download"

MODE_HEADLESS=0
MODE_USER=0
MODE_UNINSTALL=0
TARGET_VERSION=""

while [ $# -gt 0 ]; do
    case "$1" in
        --headless)
            MODE_HEADLESS=1
            shift
            ;;
        --user|--rootless)
            MODE_USER=1
            shift
            ;;
        --uninstall)
            MODE_UNINSTALL=1
            shift
            ;;
        --version|-v)
            TARGET_VERSION="${2:-}"
            shift 2
            ;;
        --help|-h)
            cat << 'EOF'
MineServe Universal Linux Installer

Usage:
  curl -fsSL https://mineserve.app/install.sh | bash
  curl -fsSL https://mineserve.app/install.sh | bash -s -- [options]

Options:
  --headless          Install and immediately enable/start the headless systemd service (port 8080)
  --user, --rootless  Install to ~/.local/opt/mineserve without sudo/root
  --version <ver>     Install a specific release version (e.g. 1.8.0)
  --uninstall         Remove MineServe binaries, desktop launcher, and systemd service (keeps server data)
  --help, -h          Show this help message
EOF
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

echo "============================================================"
echo "  MineServe for Linux - Universal Installer"
echo "============================================================"

run_privileged() {
    if [ "$(id -u)" -eq 0 ]; then
        "$@"
    elif command -v sudo >/dev/null 2>&1; then
        sudo "$@"
    else
        echo "Error: root privileges or 'sudo' required (or pass --user for a rootless install)."
        exit 1
    fi
}

if [ "$MODE_UNINSTALL" -eq 1 ]; then
    echo "Uninstalling MineServe (your server worlds in ~/.local/share/mineserve are preserved)..."
    if [ "$MODE_USER" -eq 1 ]; then
        if command -v systemctl >/dev/null 2>&1; then
            systemctl --user disable --now mineserve.service 2>/dev/null || true
            systemctl --user daemon-reload 2>/dev/null || true
        fi
        rm -rf "$HOME/.local/opt/mineserve" \
               "$HOME/.local/bin/mineserve" \
               "$HOME/.local/share/applications/mineserve.desktop" \
               "$HOME/.local/share/icons/hicolor/256x256/apps/mineserve.png" \
               "$HOME/.local/share/icons/hicolor/512x512/apps/mineserve.png" \
               "$HOME/.config/systemd/user/mineserve.service"
    else
        run_privileged systemctl disable --now mineserve.service 2>/dev/null || true
        if command -v pacman >/dev/null 2>&1 && pacman -Qi mineserve >/dev/null 2>&1; then
            run_privileged pacman -R --noconfirm mineserve
        elif command -v dpkg >/dev/null 2>&1 && dpkg -s mineserve >/dev/null 2>&1; then
            run_privileged apt-get remove -y mineserve || run_privileged dpkg -r mineserve
        fi
        run_privileged rm -rf /opt/mineserve \
                              /usr/bin/mineserve \
                              /usr/share/applications/mineserve.desktop \
                              /usr/share/icons/hicolor/256x256/apps/mineserve.png \
                              /usr/share/icons/hicolor/512x512/apps/mineserve.png \
                              /usr/share/pixmaps/mineserve.png \
                              /etc/systemd/system/mineserve.service \
                              /lib/systemd/system/mineserve.service \
                              /usr/lib/systemd/system/mineserve.service \
                              /usr/lib/sysusers.d/mineserve.conf
        run_privileged systemctl daemon-reload 2>/dev/null || true
    fi
    echo "MineServe uninstalled."
    exit 0
fi

# 1. Detect Architecture
UNAME_ARCH="$(uname -m)"
case "$UNAME_ARCH" in
    x86_64|amd64)
        ARCH_LABEL="x64"
        DEB_ARCH="amd64"
        PACMAN_ARCH="x86_64"
        ;;
    aarch64|arm64)
        ARCH_LABEL="arm64"
        DEB_ARCH="arm64"
        PACMAN_ARCH="aarch64"
        ;;
    *)
        echo "Error: Unsupported CPU architecture: $UNAME_ARCH"
        exit 1
        ;;
esac

# 2. Resolve Release Version & Tag
if [ -z "$TARGET_VERSION" ]; then
    echo "-> Checking latest MineServe release on GitHub..."
    LATEST_JSON="$(curl -fsSL "$GITHUB_API_LATEST" 2>/dev/null || true)"
    RAW_TAG="$(printf '%s' "$LATEST_JSON" | grep -m1 '"tag_name"' | sed -E 's/.*"tag_name"\s*:\s*"([^"]+)".*/\1/' || true)"
    if [ -z "$RAW_TAG" ]; then
        RAW_TAG="1.8.1"
    fi
    TARGET_VERSION="${RAW_TAG#v}"
else
    TARGET_VERSION="${TARGET_VERSION#v}"
    RAW_TAG="$TARGET_VERSION"
fi

echo "-> Target Version: v${TARGET_VERSION} (${UNAME_ARCH})"

TMP_DIR="$(mktemp -d)"
cleanup() {
    rm -rf "$TMP_DIR"
}
trap cleanup EXIT

download_release_asset() {
    local asset_name="$1"
    local out_path="$2"
    # Support both v1.8.0 and 1.8.0 tag conventions on GitHub Releases
    if curl -fsSL -o "$out_path" "${GITHUB_DOWNLOAD_BASE}/v${TARGET_VERSION}/${asset_name}" 2>/dev/null; then
        return 0
    elif curl -fsSL -o "$out_path" "${GITHUB_DOWNLOAD_BASE}/${TARGET_VERSION}/${asset_name}" 2>/dev/null; then
        return 0
    fi
    return 1
}

install_from_tarball() {
    local tarball_url_name="MineServe-${TARGET_VERSION}-linux-${ARCH_LABEL}.tar.gz"
    local fallback_name="MineServe-${TARGET_VERSION}-linux-portable-nojre.tar.gz"
    local archive_file="$TMP_DIR/mineserve.tar.gz"

    echo "-> Downloading ${tarball_url_name}..."
    if ! download_release_asset "$tarball_url_name" "$archive_file"; then
        echo "-> Falling back to portable archive ${fallback_name}..."
        if ! download_release_asset "$fallback_name" "$archive_file"; then
            echo "Error: Could not download MineServe v${TARGET_VERSION} from GitHub Releases."
            exit 1
        fi
    fi

    tar -C "$TMP_DIR" -xzf "$archive_file"
    local extracted_dir
    extracted_dir="$(find "$TMP_DIR" -mindepth 1 -maxdepth 1 -type d | head -n 1)"

    if [ "$MODE_USER" -eq 1 ]; then
        "$extracted_dir/install.sh"
    else
        run_privileged "$extracted_dir/install.sh"
    fi
}

# 3. Install via Native Package Manager or Self-Contained Bundle
if [ "$MODE_USER" -eq 1 ]; then
    install_from_tarball
elif [ "$UNAME_ARCH" = "x86_64" ] && command -v dpkg >/dev/null 2>&1 && command -v apt-get >/dev/null 2>&1; then
    DEB_NAME="mineserve_${TARGET_VERSION}_${DEB_ARCH}.deb"
    DEB_FILE="$TMP_DIR/$DEB_NAME"
    echo "-> Detected Debian/Ubuntu system. Downloading ${DEB_NAME}..."
    if download_release_asset "$DEB_NAME" "$DEB_FILE"; then
        run_privileged dpkg -i "$DEB_FILE" || run_privileged apt-get install -f -y
    else
        install_from_tarball
    fi
elif [ "$UNAME_ARCH" = "x86_64" ] && command -v pacman >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    PKG_NAME="mineserve-${TARGET_VERSION}-1-${PACMAN_ARCH}.pkg.tar.zst"
    PKG_FILE="$TMP_DIR/$PKG_NAME"
    echo "-> Detected Arch/CachyOS system. Downloading ${PKG_NAME}..."
    if download_release_asset "$PKG_NAME" "$PKG_FILE"; then
        run_privileged pacman -U --noconfirm "$PKG_FILE"
    else
        install_from_tarball
    fi
else
    install_from_tarball
fi

# 4. Optional Headless Service Activation
if [ "$MODE_HEADLESS" -eq 1 ]; then
    echo "-> Configuring MineServe Headless Web Admin service..."
    SERVICE_USER="${SUDO_USER:-$(whoami)}"
    SERVICE_HOME="$(eval echo "~$SERVICE_USER")"
    DATA_DIR="${SERVICE_HOME}/.local/share/mineserve"
    BIN_PATH="/usr/bin/mineserve"
    if [ "$MODE_USER" -eq 1 ]; then
        BIN_PATH="$HOME/.local/bin/mineserve"
    fi

    if [ "$MODE_USER" -eq 0 ] && command -v systemctl >/dev/null 2>&1; then
        UNIT_FILE="$TMP_DIR/mineserve.service"
        cat > "$UNIT_FILE" << EOF
[Unit]
Description=MineServe Headless Minecraft Server Manager
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${SERVICE_HOME}
ExecStart=${BIN_PATH} --headless --bind 0.0.0.0 --port 8080 --data-dir ${DATA_DIR}
Restart=on-failure
RestartSec=5
TimeoutStopSec=45
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
EOF
        run_privileged install -Dm644 "$UNIT_FILE" /etc/systemd/system/mineserve.service
        run_privileged systemctl daemon-reload
        run_privileged systemctl enable --now mineserve.service
        echo "-> Headless service started (systemctl status mineserve.service)"
        echo "-> View initial 'admin' password with: journalctl -u mineserve.service -n 30"
        echo "-> Or reset it anytime with: mineserve --reset-admin"
    elif [ "$MODE_USER" -eq 1 ] && command -v systemctl >/dev/null 2>&1; then
        UNIT_FILE="$TMP_DIR/mineserve.service"
        cat > "$UNIT_FILE" << EOF
[Unit]
Description=MineServe Headless Minecraft Server Manager (User)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
WorkingDirectory=${SERVICE_HOME}
ExecStart=${BIN_PATH} --headless --bind 0.0.0.0 --port 8080 --data-dir ${DATA_DIR}
Restart=on-failure
RestartSec=5
TimeoutStopSec=45
LimitNOFILE=65536

[Install]
WantedBy=default.target
EOF
        install -Dm644 "$UNIT_FILE" "$HOME/.config/systemd/user/mineserve.service"
        systemctl --user daemon-reload
        systemctl --user enable --now mineserve.service
        echo "-> User headless service started (systemctl --user status mineserve.service)"
        echo "-> View initial 'admin' password with: journalctl --user -u mineserve.service -n 30"
        echo "-> Or reset it anytime with: ${BIN_PATH} --reset-admin --data-dir ${DATA_DIR}"
    else
        echo "-> Warning: 'systemctl' is not available on this host; start headless mode manually with '${BIN_PATH} --headless'."
    fi
fi

LAN_IP="$(hostname -I 2>/dev/null | awk '{print $1}' || echo "127.0.0.1")"
echo ""
echo "============================================================"
echo "  MineServe v${TARGET_VERSION} Installed Successfully!"
echo "  - Launch Desktop GUI:  mineserve"
echo "  - Launch Headless CLI: mineserve --headless"
echo "  - Reset Admin Pass:    mineserve --reset-admin"
echo "  - Web Admin Panel:     http://${LAN_IP:-127.0.0.1}:8080"
echo "============================================================"
