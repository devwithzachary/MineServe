#!/usr/bin/env bash
set -euo pipefail

# MineServe AUR (mineserve-bin) Staging & Publishing Script

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
PKGREL="1"
MODE="stage-only"

usage() {
    cat <<'EOF'
MineServe AUR (mineserve-bin) Publisher

Usage:
  ./scripts/publish_aur.sh [options]

Options:
  --stage-only          Stage PKGBUILD + .SRCINFO into release/aur-mineserve-bin/ (default)
  --push                Stage, commit, and push to ssh://aur@aur.archlinux.org/mineserve-bin.git
  --version <ver>       Override version (default: versionName in app/build.gradle.kts)
  --pkgrel <n>          Override pkgrel (default: 1)
  --help, -h            Show this help message

One-Time AUR SSH Setup:
  1. Add your SSH public key to https://aur.archlinux.org/account/
  2. Configure ~/.ssh/config for aur.archlinux.org:
       Host aur.archlinux.org
         IdentityFile ~/.ssh/id_ed25519
         User aur
  3. Create the empty package repo on AUR if first publish:
       ssh aur@aur.archlinux.org setup-repo mineserve-bin
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --stage-only)
            MODE="stage-only"
            shift
            ;;
        --push)
            MODE="push"
            shift
            ;;
        --version)
            VERSION_NAME="${2:-}"
            shift 2
            ;;
        --pkgrel)
            PKGREL="${2:-1}"
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

TARBALL_NAME="MineServe-${VERSION_NAME}-linux-portable-nojre.tar.gz"
LOCAL_TARBALL="$PROJECT_ROOT/release/$TARBALL_NAME"
STAGE_DIR="$PROJECT_ROOT/release/aur-mineserve-bin"
mkdir -p "$STAGE_DIR"

if [ ! -f "$LOCAL_TARBALL" ]; then
    RELEASE_URL="https://github.com/devwithzachary/MineServe/releases/download/${VERSION_NAME}/${TARBALL_NAME}"
    echo "==> Local tarball not found at $LOCAL_TARBALL; downloading from $RELEASE_URL..."
    curl -fsSL -o "$LOCAL_TARBALL" "$RELEASE_URL"
fi

SHA256_VAL=$(sha256sum "$LOCAL_TARBALL" | awk '{print $1}')
ARCH_PKGVER="${VERSION_NAME//-/_}"

cp "$PROJECT_ROOT/packaging/linux/aur/PKGBUILD" "$STAGE_DIR/PKGBUILD"
sed -i "s/^pkgver=.*/pkgver=${ARCH_PKGVER}/" "$STAGE_DIR/PKGBUILD"
sed -i "s/^pkgrel=.*/pkgrel=${PKGREL}/" "$STAGE_DIR/PKGBUILD"
sed -i "s/^sha256sums=.*/sha256sums=('${SHA256_VAL}')/" "$STAGE_DIR/PKGBUILD"
if [ "$VERSION_NAME" != "$ARCH_PKGVER" ]; then
    sed -i "s/\${pkgver}/${VERSION_NAME}/g" "$STAGE_DIR/PKGBUILD"
fi

if command -v makepkg >/dev/null 2>&1; then
    (cd "$STAGE_DIR" && makepkg --printsrcinfo > .SRCINFO)
elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    echo "==> 'makepkg' not installed locally; generating .SRCINFO via archlinux:latest Docker container..."
    docker run --rm -v "$STAGE_DIR:/pkg" archlinux:latest bash -c "
        set -e
        pacman -Sy --noconfirm --needed pacman-contrib >/dev/null 2>&1 || true
        useradd -m builder
        cp /pkg/PKGBUILD /home/builder/PKGBUILD
        chown -R builder:builder /home/builder
        su - builder -c 'cd /home/builder && makepkg --printsrcinfo' > /pkg/.SRCINFO
    "
else
    echo "Error: Neither 'makepkg' nor a running Docker daemon is available to generate .SRCINFO." >&2
    exit 1
fi

echo "==> Staged AUR files in $STAGE_DIR (SHA256: $SHA256_VAL)"

if [ "$MODE" = "stage-only" ]; then
    echo "To publish to AUR, run: ./scripts/publish_aur.sh --push --version $VERSION_NAME --pkgrel $PKGREL"
    exit 0
fi

AUR_CACHE="$HOME/.cache/mineserve-packaging/aur-mineserve-bin"
mkdir -p "$(dirname "$AUR_CACHE")"
if [ ! -d "$AUR_CACHE/.git" ]; then
    echo "==> Cloning AUR repo mineserve-bin..."
    git clone "ssh://aur@aur.archlinux.org/mineserve-bin.git" "$AUR_CACHE"
else
    echo "==> Updating AUR repo mineserve-bin..."
    git -C "$AUR_CACHE" pull --ff-only || true
fi

cp "$STAGE_DIR/PKGBUILD" "$AUR_CACHE/PKGBUILD"
cp "$STAGE_DIR/.SRCINFO" "$AUR_CACHE/.SRCINFO"

echo "==> Diff in AUR repo:"
git -C "$AUR_CACHE" diff || true

git -C "$AUR_CACHE" add PKGBUILD .SRCINFO
if git -C "$AUR_CACHE" diff --cached --quiet; then
    echo "==> No changes to commit in AUR repo."
    exit 0
fi

git -C "$AUR_CACHE" commit -m "Update mineserve-bin to ${VERSION_NAME}-${PKGREL}"
git -C "$AUR_CACHE" push origin master
echo "==> Published mineserve-bin ${VERSION_NAME}-${PKGREL} to AUR!"
