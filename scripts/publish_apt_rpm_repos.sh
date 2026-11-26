#!/usr/bin/env bash
set -euo pipefail

# MineServe GPG-Signed Static APT & RPM Repository Publisher
# Generates https://mineserve.app/apt and https://mineserve.app/rpm inside MineServer-Web.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

GPG_ENV_FILE="$SCRIPT_DIR/.gpg-signing.env"
if [ -f "$GPG_ENV_FILE" ]; then
    # shellcheck disable=SC1090
    source "$GPG_ENV_FILE"
fi

VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
MINESERVE_GPG_KEY="${MINESERVE_GPG_KEY:-}"
WEB_DIR="$PROJECT_ROOT/../MineServer-Web"
DRY_RUN=0
INIT_GPG=0

usage() {
    cat <<'EOF'
MineServe GPG-Signed Static APT & RPM Repository Publisher

Usage:
  ./scripts/publish_apt_rpm_repos.sh [options]

Options:
  --init-gpg            Interactively create or select a GPG key and save scripts/.gpg-signing.env
  --gpg-key <key-id>    Use a specific GPG key fingerprint or email
  --version <ver>       Override version (default: versionName in app/build.gradle.kts)
  --web-dir <path>      Target website repo directory (default: ../MineServer-Web)
  --dry-run             Stage into release/repo-staging instead of ../MineServer-Web
  --help, -h            Show this help message
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --init-gpg)
            INIT_GPG=1
            shift
            ;;
        --gpg-key)
            MINESERVE_GPG_KEY="${2:-}"
            shift 2
            ;;
        --version)
            VERSION_NAME="${2:-}"
            shift 2
            ;;
        --web-dir)
            WEB_DIR="${2:-}"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=1
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

if [ "$INIT_GPG" -eq 1 ]; then
    echo "==> Existing GPG secret keys:"
    gpg --list-secret-keys --keyid-format=long || true
    echo ""
    read -r -p "Generate a new RSA-4096 key 'MineServe Package Signing Key <packages@mineserve.app>'? [y/N] " ans
    if [[ "$ans" =~ ^[Yy]$ ]]; then
        gpg --batch --passphrase '' --quick-gen-key "MineServe Package Signing Key <packages@mineserve.app>" rsa4096 sign 0
    fi
    echo ""
    gpg --list-secret-keys --keyid-format=long
    read -r -p "Enter the GPG key fingerprint or ID to use: " chosen_key
    chosen_key="${chosen_key// /}"
    if [ -z "$chosen_key" ]; then
        echo "Error: No key ID provided." >&2
        exit 1
    fi
    printf 'MINESERVE_GPG_KEY="%s"\n' "$chosen_key" > "$GPG_ENV_FILE"
    chmod 600 "$GPG_ENV_FILE"
    echo "==> Saved MINESERVE_GPG_KEY to $GPG_ENV_FILE"
    exit 0
fi

if [ -z "$MINESERVE_GPG_KEY" ]; then
    echo "Error: MINESERVE_GPG_KEY is not configured." >&2
    echo "Run ./scripts/publish_apt_rpm_repos.sh --init-gpg or pass --gpg-key <id>." >&2
    exit 1
fi

TARGET_ROOT="$WEB_DIR"
if [ "$DRY_RUN" -eq 1 ]; then
    TARGET_ROOT="$PROJECT_ROOT/release/repo-staging"
fi

DEB_FILE="$PROJECT_ROOT/release/mineserve_${VERSION_NAME}_amd64.deb"
if [ ! -f "$DEB_FILE" ]; then
    echo "Error: Missing $DEB_FILE. Run ./scripts/build_linux_packages.sh first." >&2
    exit 1
fi

RPM_FILES=()
while IFS= read -r -d '' f; do
    RPM_FILES+=("$f")
done < <(find "$PROJECT_ROOT/release" -maxdepth 1 -name "mineserve-${VERSION_NAME}-*.rpm" -print0)

APT_DIR="$TARGET_ROOT/apt"
RPM_DIR="$TARGET_ROOT/rpm"
mkdir -p "$APT_DIR/pool/main/m/mineserve" "$APT_DIR/dists/stable/main/binary-amd64"

echo "==> Generating APT repository in $APT_DIR..."
cp -f "$DEB_FILE" "$APT_DIR/pool/main/m/mineserve/"

if command -v dpkg-scanpackages >/dev/null 2>&1 && command -v apt-ftparchive >/dev/null 2>&1; then
    (
        cd "$APT_DIR"
        dpkg-scanpackages --multiversion pool/ > dists/stable/main/binary-amd64/Packages
        gzip -kf -9 dists/stable/main/binary-amd64/Packages
        apt-ftparchive \
            -o APT::FTPArchive::Release::Origin="MineServe" \
            -o APT::FTPArchive::Release::Label="MineServe" \
            -o APT::FTPArchive::Release::Suite="stable" \
            -o APT::FTPArchive::Release::Codename="stable" \
            -o APT::FTPArchive::Release::Architectures="amd64" \
            -o APT::FTPArchive::Release::Components="main" \
            release dists/stable > dists/stable/Release
    )
elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    HOST_UID=$(id -u)
    HOST_GID=$(id -g)
    docker run --rm -v "$APT_DIR:/apt" debian:bookworm-slim bash -c "
        set -e
        apt-get update -qq && apt-get install -y -qq dpkg-dev apt-utils >/dev/null
        cd /apt
        dpkg-scanpackages --multiversion pool/ > dists/stable/main/binary-amd64/Packages
        gzip -kf -9 dists/stable/main/binary-amd64/Packages
        apt-ftparchive \
            -o APT::FTPArchive::Release::Origin='MineServe' \
            -o APT::FTPArchive::Release::Label='MineServe' \
            -o APT::FTPArchive::Release::Suite='stable' \
            -o APT::FTPArchive::Release::Codename='stable' \
            -o APT::FTPArchive::Release::Architectures='amd64' \
            -o APT::FTPArchive::Release::Components='main' \
            release dists/stable > dists/stable/Release
        chown -R ${HOST_UID}:${HOST_GID} /apt
    "
else
    echo "Error: Neither dpkg-scanpackages/apt-ftparchive nor Docker is available." >&2
    exit 1
fi

gpg --batch --yes --export "$MINESERVE_GPG_KEY" | gpg --batch --yes --dearmor -o "$APT_DIR/mineserve-archive-keyring.gpg"
gpg --batch --yes --armor --export "$MINESERVE_GPG_KEY" > "$APT_DIR/KEY.gpg"
rm -f "$APT_DIR/dists/stable/Release.gpg" "$APT_DIR/dists/stable/InRelease"
gpg --batch --yes -u "$MINESERVE_GPG_KEY" --detach-sign --armor -o "$APT_DIR/dists/stable/Release.gpg" "$APT_DIR/dists/stable/Release"
gpg --batch --yes -u "$MINESERVE_GPG_KEY" --clearsign -o "$APT_DIR/dists/stable/InRelease" "$APT_DIR/dists/stable/Release"

cat > "$APT_DIR/README.txt" <<'EOF'
MineServe Official APT Repository (Debian / Ubuntu)

Install commands:
  curl -fsSL https://mineserve.app/apt/mineserve-archive-keyring.gpg | sudo tee /usr/share/keyrings/mineserve-archive-keyring.gpg >/dev/null
  echo "deb [arch=amd64 signed-by=/usr/share/keyrings/mineserve-archive-keyring.gpg] https://mineserve.app/apt stable main" | sudo tee /etc/apt/sources.list.d/mineserve.list >/dev/null
  sudo apt-get update && sudo apt-get install mineserve
EOF

if [ "${#RPM_FILES[@]}" -gt 0 ]; then
    echo "==> Generating RPM repository in $RPM_DIR..."
    mkdir -p "$RPM_DIR/packages"
    cp -f "${RPM_FILES[@]}" "$RPM_DIR/packages/"

    if command -v createrepo_c >/dev/null 2>&1; then
        createrepo_c --update "$RPM_DIR"
    elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
        HOST_UID=$(id -u)
        HOST_GID=$(id -g)
        docker run --rm -v "$RPM_DIR:/rpm" fedora:41 bash -c "
            set -e
            dnf install -y -q createrepo_c >/dev/null
            createrepo_c --update /rpm
            chown -R ${HOST_UID}:${HOST_GID} /rpm
        "
    else
        echo "Error: Neither createrepo_c nor Docker is available." >&2
        exit 1
    fi

    gpg --batch --yes --armor --export "$MINESERVE_GPG_KEY" > "$RPM_DIR/RPM-GPG-KEY-mineserve"
    rm -f "$RPM_DIR/repodata/repomd.xml.asc"
    gpg --batch --yes -u "$MINESERVE_GPG_KEY" --detach-sign --armor -o "$RPM_DIR/repodata/repomd.xml.asc" "$RPM_DIR/repodata/repomd.xml"

    cat > "$RPM_DIR/mineserve.repo" <<'EOF'
[mineserve]
name=MineServe Official Repository
baseurl=https://mineserve.app/rpm/
enabled=1
repo_gpgcheck=1
gpgcheck=0
gpgkey=https://mineserve.app/rpm/RPM-GPG-KEY-mineserve
EOF

    cat > "$RPM_DIR/README.txt" <<'EOF'
MineServe Official RPM Repository (Fedora / RHEL)

Install commands:
  sudo curl -fsSL https://mineserve.app/rpm/mineserve.repo -o /etc/yum.repos.d/mineserve.repo
  sudo dnf install mineserve
EOF
else
    echo "Warning: No RPM files matching release/mineserve-${VERSION_NAME}-*.rpm found; skipped RPM repo generation." >&2
fi

echo "==> Signed APT & RPM repositories ready under $TARGET_ROOT"
