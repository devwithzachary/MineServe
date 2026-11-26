#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.macos-builder.env"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

MACOS_HOST="${MACOS_HOST:-}"
MACOS_REPO_DIR="${MACOS_REPO_DIR:-~/Code/MineServe-macbuild}"
MACOS_SSH_OPTS="${MACOS_SSH_OPTS:-}"
MACOS_WEB_PORT="${MACOS_WEB_PORT:-18081}"
MACOS_SSH_KEY="${MACOS_SSH_KEY:-$HOME/.ssh/id_ed25519_mineserve}"
SSH_MUX_OPTS="-o ControlMaster=auto -o ControlPath=/tmp/mineserve-macos-%r@%h:%p -o ControlPersist=10m"

usage() {
  cat <<'EOF'
MineServe macOS Remote Builder & Tester (Mac Studio over LAN SSH)

Usage:
  ./scripts/build_macos_remote.sh <command>

Commands:
  --setup-ssh     Generate ~/.ssh/id_ed25519_mineserve (if missing) and copy it to the Mac Studio
  --setup         Verify SSH login, macOS architecture, JDK 21, and jpackage
  --install-jdk   Install Eclipse Temurin JDK 21 via Homebrew on the Mac Studio
  --sync          Sync MineServer-App sources to the Mac Studio via rsync
  --test          Sync and run :core:test + :desktop:test on the Mac Studio
  --run           Sync and launch the native macOS Compose GUI on the Mac Studio
  --headless      Sync, build, and run headless WebUI with SSH port forward to localhost
  --package       Sync, build .dmg + portable .tar.gz, and pull artifacts into dist-macos/ (or --out <dir>)
  --out <dir>     Override local output directory for --package (e.g. --out release)
  --help, -h      Show this help message

Configuration:
  Copy scripts/macos-builder.env.example to scripts/.macos-builder.env
  and set MACOS_HOST (e.g. zachary@mac-studio.local or zachary@192.168.1.50).
EOF
}

require_host() {
  if [[ -z "$MACOS_HOST" ]]; then
    echo "Error: MACOS_HOST is not set." >&2
    echo "Copy scripts/macos-builder.env.example to scripts/.macos-builder.env and set MACOS_HOST." >&2
    exit 1
  fi
}

ssh_run() {
  # shellcheck disable=SC2086
  ssh $SSH_MUX_OPTS $MACOS_SSH_OPTS "$MACOS_HOST" "$@"
}

cmd_setup_ssh() {
  require_host
  mkdir -p "$HOME/.ssh"
  chmod 700 "$HOME/.ssh"
  if [[ ! -f "$MACOS_SSH_KEY" ]]; then
    echo "==> Generating Ed25519 SSH key at $MACOS_SSH_KEY ..."
    ssh-keygen -t ed25519 -N "" -C "mineserve-macos-builder" -f "$MACOS_SSH_KEY"
  fi
  echo "==> Copying public key to $MACOS_HOST (enter your Mac Studio password once)..."
  ssh-copy-id -i "${MACOS_SSH_KEY}.pub" "$MACOS_HOST"
  echo "==> Verifying passwordless SSH..."
  ssh -i "$MACOS_SSH_KEY" -o IdentitiesOnly=yes -o BatchMode=yes "$MACOS_HOST" "echo 'SSH key auth OK on' \$(hostname)"
}

sync_repo() {
  require_host
  echo "==> Syncing MineServer-App to $MACOS_HOST:$MACOS_REPO_DIR ..."
  ssh_run "mkdir -p $MACOS_REPO_DIR"
  # shellcheck disable=SC2086
  rsync -az --delete \
    -e "ssh $SSH_MUX_OPTS $MACOS_SSH_OPTS" \
    --exclude '.git/' \
    --exclude '.gradle/' \
    --exclude 'build/' \
    --exclude '*/build/' \
    --exclude 'dist-macos/' \
    --exclude 'dist-windows/' \
    --exclude 'release/' \
    --exclude 'local.properties' \
    --exclude '*.keystore' \
    --exclude '*.jks' \
    "$REPO_ROOT/" "$MACOS_HOST:$MACOS_REPO_DIR/"
}

cmd_setup() {
  require_host
  echo "==> Checking SSH connectivity and toolchain on $MACOS_HOST ..."
  ssh_run '
    set -e
    export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
    echo "Host: $(hostname) ($(uname -m), macOS $(sw_vers -productVersion))"
    if /usr/libexec/java_home -v 21 >/dev/null 2>&1; then
      export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
      echo "JDK 21: $JAVA_HOME"
      "$JAVA_HOME/bin/java" -version 2>&1 | head -n 2
      "$JAVA_HOME/bin/jpackage" --version >/dev/null && echo "jpackage: OK"
    else
      echo "JDK 21: NOT FOUND"
      echo "Run: ./scripts/build_macos_remote.sh --install-jdk (or brew install --cask temurin@21)"
      exit 2
    fi
  '
}

cmd_install_jdk() {
  require_host
  echo "==> Installing Temurin JDK 21 on $MACOS_HOST via Homebrew ..."
  ssh_run '
    export PATH="/opt/homebrew/bin:/usr/local/bin:$PATH"
    if ! command -v brew >/dev/null 2>&1; then
      echo "Error: Homebrew is not installed on the Mac Studio. Install from https://brew.sh first." >&2
      exit 1
    fi
    brew install --cask temurin@21
    export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
    "$JAVA_HOME/bin/java" -version
  '
}

cmd_test() {
  sync_repo
  echo "==> Running unit tests natively on macOS ..."
  ssh_run "
    set -e
    export PATH=\"/opt/homebrew/bin:/usr/local/bin:\$PATH\"
    export JAVA_HOME=\"\$(/usr/libexec/java_home -v 21)\"
    cd $MACOS_REPO_DIR
    chmod +x ./gradlew
    ./gradlew :core:test :desktop:test -PexcludeAndroid=true --no-daemon
  "
}

cmd_run() {
  sync_repo
  echo "==> Building and launching native macOS MineServe.app on Mac Studio..."
  ssh_run "
    set -e
    export PATH=\"/opt/homebrew/bin:/usr/local/bin:\$PATH\"
    export JAVA_HOME=\"\$(/usr/libexec/java_home -v 21)\"
    cd $MACOS_REPO_DIR
    chmod +x ./gradlew
    ./gradlew :desktop:createDistributable -PexcludeAndroid=true -Pcompose.desktop.packaging.checkJdkVendor=false --no-daemon
    open -n -W \"\$PWD/desktop/build/compose/binaries/main/app/MineServe.app\"
  "
}

cmd_headless() {
  sync_repo
  echo "==> Building macOS installDist and starting headless WebUI (forwarded to http://127.0.0.1:$MACOS_WEB_PORT) ..."
  # shellcheck disable=SC2086
  ssh -t $MACOS_SSH_OPTS -L "${MACOS_WEB_PORT}:127.0.0.1:8080" "$MACOS_HOST" "
    set -e
    export PATH=\"/opt/homebrew/bin:/usr/local/bin:\$PATH\"
    export JAVA_HOME=\"\$(/usr/libexec/java_home -v 21)\"
    cd $MACOS_REPO_DIR
    chmod +x ./gradlew
    ./gradlew :desktop:installDist -PexcludeAndroid=true --no-daemon
    echo \"\"
    echo \"==> Open http://127.0.0.1:${MACOS_WEB_PORT} in your Linux browser!\"
    echo \"\"
    ./desktop/build/install/desktop/bin/mineserve --headless --port 8080
  "
}

cmd_package() {
  sync_repo
  echo "==> Packaging macOS .dmg and portable .tar.gz on $MACOS_HOST ..."
  ssh_run "
    set -e
    export PATH=\"/opt/homebrew/bin:/usr/local/bin:\$PATH\"
    export JAVA_HOME=\"\$(/usr/libexec/java_home -v 21)\"
    cd $MACOS_REPO_DIR
    chmod +x ./gradlew
    ./gradlew :desktop:packageDmg :desktop:installDist -PexcludeAndroid=true -Pcompose.desktop.packaging.checkJdkVendor=false --no-daemon
    ARCH=\$(uname -m)
    VER=\$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed -E 's/.*\"([^\"]+)\".*/\\1/')
    mkdir -p dist-macos
    cp -f desktop/build/compose/binaries/main/dmg/*.dmg dist-macos/ 2>/dev/null || true
    tar -C desktop/build/install -czf \"dist-macos/MineServe-\${VER}-macos-\${ARCH}-portable.tar.gz\" desktop
    ls -lh dist-macos/
  "
  local out_rel="${OUT_DIR:-dist-macos}"
  local out_abs="$REPO_ROOT/$out_rel"
  mkdir -p "$out_abs"
  echo "==> Pulling macOS artifacts back to $out_abs/ ..."
  # shellcheck disable=SC2086
  rsync -az -e "ssh $SSH_MUX_OPTS $MACOS_SSH_OPTS" "$MACOS_HOST:$MACOS_REPO_DIR/dist-macos/" "$out_abs/"
  (
    cd "$out_abs"
    if command -v sha256sum >/dev/null 2>&1; then
      sha256sum *.dmg *.tar.gz > SHA256SUMS-macos.txt 2>/dev/null || true
    fi
    ls -lh
  )
}

ACTION=""
OUT_DIR=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --setup-ssh|--setup|--install-jdk|--sync|--test|--run|--headless|--package)
      ACTION="$1"
      shift
      ;;
    --out)
      OUT_DIR="${2:-}"
      if [[ -z "$OUT_DIR" ]]; then
        echo "Error: --out requires a directory path" >&2
        exit 1
      fi
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

case "$ACTION" in
  --setup-ssh) cmd_setup_ssh ;;
  --setup) cmd_setup ;;
  --install-jdk) cmd_install_jdk ;;
  --sync) sync_repo ;;
  --test) cmd_test ;;
  --run) cmd_run ;;
  --headless) cmd_headless ;;
  --package) cmd_package ;;
  "") usage ;;
esac
