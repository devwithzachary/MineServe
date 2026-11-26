#!/usr/bin/env bash
set -euo pipefail

# MineServe Native Linux Packaging Script
# Builds:
#   1. Self-contained Linux .tar.gz archive (with bundled minimal JRE via jpackage createDistributable)
#   2. Portable lightweight .tar.gz archive (for headless servers / hosts with Java 21+)
#   3. Debian / Ubuntu .deb package (via dpkg-deb or portable Debian 2.0 ar+tar builder)
#   4. AppDir + AppImage (when appimagetool is available)
#   5. Fedora / RHEL .rpm package (when rpmbuild is available)
#   6. Arch Linux / CachyOS PKGBUILD in release/

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

VERSION_NAME="${RELEASE_VERSION:-$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')}"
DEB_VERSION="$(printf '%s' "$VERSION_NAME" | tr '[:upper:]' '[:lower:]' | sed 's/-/~/g')"
RPM_VERSION="${VERSION_NAME%%-*}"
if [[ "$VERSION_NAME" == *-* ]]; then
    RPM_SUFFIX="$(printf '%s' "${VERSION_NAME#*-}" | tr '[:upper:]' '[:lower:]' | tr '-' '.')"
    RPM_RELEASE="0.1.${RPM_SUFFIX}%{?dist}"
else
    RPM_RELEASE="1%{?dist}"
fi
ARCH_PKGVER="${VERSION_NAME//-/_}"
UNAME_ARCH="$(uname -m)"
case "$UNAME_ARCH" in
    x86_64|amd64)
        ARCH_LABEL="x64"
        DEB_ARCH="amd64"
        ;;
    aarch64|arm64)
        ARCH_LABEL="arm64"
        DEB_ARCH="arm64"
        ;;
    *)
        ARCH_LABEL="$UNAME_ARCH"
        DEB_ARCH="$UNAME_ARCH"
        ;;
esac

echo "============================================================"
echo "MineServe Linux Packaging Pipeline (v${VERSION_NAME} - ${ARCH_LABEL})"
echo "============================================================"

RELEASE_DIR="$PROJECT_ROOT/release"
BUILD_STAGE="$PROJECT_ROOT/desktop/build/linux-packages-stage"
rm -rf "$BUILD_STAGE"
mkdir -p "$RELEASE_DIR" "$BUILD_STAGE"

echo "[1/5] Running unit tests and compiling Linux Desktop & Headless distributions..."
./gradlew :core:test :desktop:test :desktop:installDist :desktop:createDistributable -PexcludeAndroid=true --no-daemon

ICON_SRC="$PROJECT_ROOT/app/src/main/res/drawable/ic_launcher_logo.png"
DESKTOP_ENTRY_SRC="$PROJECT_ROOT/packaging/linux/mineserve.desktop"
SYSTEMD_UNIT_SRC="$PROJECT_ROOT/packaging/linux/mineserve.service"
SYSUSERS_SRC="$PROJECT_ROOT/packaging/linux/mineserve.sysusers"

# Helper script written into tar.gz distributions for optional system/user installation
create_install_scripts() {
    local target_dir="$1"
    cat > "$target_dir/install.sh" << 'EOF'
#!/usr/bin/env bash
set -e
SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ "$(id -u)" -eq 0 ]; then
    echo "Installing MineServe system-wide to /opt/mineserve..."
    rm -rf /opt/mineserve
    mkdir -p /opt/mineserve
    cp -a "$SRC_DIR"/* /opt/mineserve/
    ln -sf /opt/mineserve/bin/mineserve /usr/bin/mineserve
    chmod 755 /opt/mineserve/bin/mineserve 2>/dev/null || true
    install -Dm644 "$SRC_DIR/share/mineserve.desktop" /usr/share/applications/mineserve.desktop
    install -Dm644 "$SRC_DIR/share/mineserve.png" /usr/share/icons/hicolor/512x512/apps/mineserve.png
    install -Dm644 "$SRC_DIR/share/mineserve.png" /usr/share/pixmaps/mineserve.png
    install -Dm644 "$SRC_DIR/share/mineserve.service" /lib/systemd/system/mineserve.service
    install -Dm644 "$SRC_DIR/share/mineserve.sysusers" /usr/lib/sysusers.d/mineserve.conf
    systemd-sysusers /usr/lib/sysusers.d/mineserve.conf 2>/dev/null || true
    echo "Installed! Launch 'mineserve' from your application menu or terminal, or run 'mineserve --headless'."
else
    echo "Installing MineServe for current user to ~/.local/opt/mineserve..."
    rm -rf "$HOME/.local/opt/mineserve"
    mkdir -p "$HOME/.local/opt/mineserve" "$HOME/.local/bin" "$HOME/.local/share/applications" "$HOME/.local/share/icons/hicolor/512x512/apps"
    cp -a "$SRC_DIR"/* "$HOME/.local/opt/mineserve/"
    chmod 755 "$HOME/.local/opt/mineserve/bin/mineserve" 2>/dev/null || true
    ln -sf "$HOME/.local/opt/mineserve/bin/mineserve" "$HOME/.local/bin/mineserve"
    sed "s|Exec=/usr/bin/mineserve|Exec=$HOME/.local/bin/mineserve|g" "$SRC_DIR/share/mineserve.desktop" > "$HOME/.local/share/applications/mineserve.desktop"
    cp "$SRC_DIR/share/mineserve.png" "$HOME/.local/share/icons/hicolor/512x512/apps/mineserve.png"
    echo "Installed! Launch MineServe from your application launcher or run '$HOME/.local/bin/mineserve'."
fi
EOF
    chmod +x "$target_dir/install.sh"
}

# 2. Package Portable Lightweight .tar.gz (No bundled JRE)
echo "[2/5] Building portable lightweight archive (MineServe-${VERSION_NAME}-linux-portable-nojre.tar.gz)..."
PORTABLE_DIR_NAME="MineServe-${VERSION_NAME}-linux-portable-nojre"
PORTABLE_STAGE="$BUILD_STAGE/$PORTABLE_DIR_NAME"
mkdir -p "$PORTABLE_STAGE/share"
cp -a "$PROJECT_ROOT/desktop/build/install/desktop/"* "$PORTABLE_STAGE/"
cp "$ICON_SRC" "$PORTABLE_STAGE/share/mineserve.png"
cp "$DESKTOP_ENTRY_SRC" "$PORTABLE_STAGE/share/mineserve.desktop"
cp "$SYSTEMD_UNIT_SRC" "$PORTABLE_STAGE/share/mineserve.service"
cp "$SYSUSERS_SRC" "$PORTABLE_STAGE/share/mineserve.sysusers"
create_install_scripts "$PORTABLE_STAGE"

tar -C "$BUILD_STAGE" -czf "$RELEASE_DIR/${PORTABLE_DIR_NAME}.tar.gz" "$PORTABLE_DIR_NAME"
cp "$PROJECT_ROOT/packaging/linux/PKGBUILD" "$RELEASE_DIR/PKGBUILD"
sed -i "s/^pkgver=.*/pkgver=${ARCH_PKGVER}/" "$RELEASE_DIR/PKGBUILD"
if [ "$VERSION_NAME" != "$ARCH_PKGVER" ]; then
    sed -i "s/\${pkgver}/${VERSION_NAME}/g" "$RELEASE_DIR/PKGBUILD"
fi
cp "$PROJECT_ROOT/packaging/linux/install.sh" "$RELEASE_DIR/install.sh"
chmod +x "$PROJECT_ROOT/packaging/linux/install.sh" "$RELEASE_DIR/install.sh"
if [ -d "$PROJECT_ROOT/../MineServer-Web" ]; then
    cp "$PROJECT_ROOT/packaging/linux/install.sh" "$PROJECT_ROOT/../MineServer-Web/install.sh" 2>/dev/null || true
elif [ -d "$PROJECT_ROOT/../MineServe-web" ]; then
    cp "$PROJECT_ROOT/packaging/linux/install.sh" "$PROJECT_ROOT/../MineServe-web/install.sh" 2>/dev/null || true
fi

# 3. Package Self-Contained Native Linux .tar.gz (With Embedded JRE from createDistributable)
echo "[3/5] Building self-contained Linux bundle with embedded JRE (MineServe-${VERSION_NAME}-linux-${ARCH_LABEL}.tar.gz)..."
DIST_APP_DIR="$PROJECT_ROOT/desktop/build/compose/binaries/main/app/mineserve"
SELF_CONTAINED_NAME="MineServe-${VERSION_NAME}-linux-${ARCH_LABEL}"
SELF_CONTAINED_STAGE="$BUILD_STAGE/$SELF_CONTAINED_NAME"
mkdir -p "$SELF_CONTAINED_STAGE/share"

if [ -d "$DIST_APP_DIR" ]; then
    cp -a "$DIST_APP_DIR/"* "$SELF_CONTAINED_STAGE/"
else
    cp -a "$PROJECT_ROOT/desktop/build/install/desktop/"* "$SELF_CONTAINED_STAGE/"
fi
cp "$ICON_SRC" "$SELF_CONTAINED_STAGE/share/mineserve.png"
cp "$DESKTOP_ENTRY_SRC" "$SELF_CONTAINED_STAGE/share/mineserve.desktop"
cp "$SYSTEMD_UNIT_SRC" "$SELF_CONTAINED_STAGE/share/mineserve.service"
cp "$SYSUSERS_SRC" "$SELF_CONTAINED_STAGE/share/mineserve.sysusers"
create_install_scripts "$SELF_CONTAINED_STAGE"

tar -C "$BUILD_STAGE" -czf "$RELEASE_DIR/${SELF_CONTAINED_NAME}.tar.gz" "$SELF_CONTAINED_NAME"

# 4. Build Debian / Ubuntu .deb Package (mineserve_<version>_<arch>.deb)
echo "[4/5] Building Debian / Ubuntu package (mineserve_${VERSION_NAME}_${DEB_ARCH}.deb)..."
DEB_ROOT="$BUILD_STAGE/deb-root"
mkdir -p "$DEB_ROOT/DEBIAN" \
         "$DEB_ROOT/opt/mineserve" \
         "$DEB_ROOT/usr/bin" \
         "$DEB_ROOT/usr/share/applications" \
         "$DEB_ROOT/usr/share/icons/hicolor/512x512/apps" \
         "$DEB_ROOT/usr/share/pixmaps" \
         "$DEB_ROOT/lib/systemd/system" \
         "$DEB_ROOT/usr/lib/sysusers.d"

cp -a "$SELF_CONTAINED_STAGE/"* "$DEB_ROOT/opt/mineserve/"
chmod 755 "$DEB_ROOT/opt/mineserve/bin/mineserve" 2>/dev/null || true
ln -sf /opt/mineserve/bin/mineserve "$DEB_ROOT/usr/bin/mineserve"
cp "$DESKTOP_ENTRY_SRC" "$DEB_ROOT/usr/share/applications/mineserve.desktop"
cp "$ICON_SRC" "$DEB_ROOT/usr/share/icons/hicolor/512x512/apps/mineserve.png"
cp "$ICON_SRC" "$DEB_ROOT/usr/share/pixmaps/mineserve.png"
cp "$SYSTEMD_UNIT_SRC" "$DEB_ROOT/lib/systemd/system/mineserve.service"
cp "$SYSUSERS_SRC" "$DEB_ROOT/usr/lib/sysusers.d/mineserve.conf"

cat > "$DEB_ROOT/DEBIAN/postinst" << 'EOF'
#!/usr/bin/env bash
set -e
if command -v systemd-sysusers >/dev/null 2>&1; then
    systemd-sysusers /usr/lib/sysusers.d/mineserve.conf || true
fi
if ! getent group mineserve >/dev/null 2>&1; then
    groupadd -r mineserve || true
fi
if ! getent passwd mineserve >/dev/null 2>&1; then
    useradd -r -g mineserve -d /var/lib/mineserve -s /usr/sbin/nologin mineserve || true
fi
EOF
chmod 755 "$DEB_ROOT/DEBIAN/postinst"

INSTALLED_KB=$(du -sk "$DEB_ROOT/opt/mineserve" | awk '{print $1}')
cat > "$DEB_ROOT/DEBIAN/control" << EOF
Package: mineserve
Version: ${DEB_VERSION}
Section: games
Priority: optional
Architecture: ${DEB_ARCH}
Installed-Size: ${INSTALLED_KB}
Maintainer: Zachary <https://github.com/devwithzachary/MineServe>
Homepage: https://mineserve.app
Recommends: python3-dbus, python3-gi, xdg-utils
Description: Native Linux Minecraft Java, Modded, Proxy, and Bedrock Server Manager
 MineServe is a free and open-source Minecraft server manager for Linux and Android.
 Includes an adaptive multi-pane Desktop GUI with Linux System Tray integration
 and a --headless Web Remote Admin daemon mode with portable OpenJDK 8/17/21/25 provisioning.
EOF

DEB_OUT="$RELEASE_DIR/mineserve_${VERSION_NAME}_${DEB_ARCH}.deb"
rm -f "$DEB_OUT"

if command -v dpkg-deb >/dev/null 2>&1; then
    dpkg-deb --build --root-owner-group "$DEB_ROOT" "$DEB_OUT"
else
    # Portable Debian 2.0 binary package builder using standard ar + tar
    DEB_WORK="$BUILD_STAGE/deb-ar"
    mkdir -p "$DEB_WORK"
    echo "2.0" > "$DEB_WORK/debian-binary"
    tar --owner=0 --group=0 -C "$DEB_ROOT/DEBIAN" -czf "$DEB_WORK/control.tar.gz" .
    tar --owner=0 --group=0 --exclude='./DEBIAN' -C "$DEB_ROOT" -czf "$DEB_WORK/data.tar.gz" .
    (cd "$DEB_WORK" && ar rcs "$DEB_OUT" debian-binary control.tar.gz data.tar.gz)
fi

# 5. Build Native Arch Linux / CachyOS Package (.pkg.tar.zst) if makepkg is available
if command -v makepkg >/dev/null 2>&1; then
    echo "[5/6] Building Arch Linux / CachyOS package (.pkg.tar.zst via makepkg)..."
    ARCH_STAGE="$BUILD_STAGE/arch-pkg"
    mkdir -p "$ARCH_STAGE"
    cp "$RELEASE_DIR/PKGBUILD" "$ARCH_STAGE/PKGBUILD"
    cp "$RELEASE_DIR/${PORTABLE_DIR_NAME}.tar.gz" "$ARCH_STAGE/"
    (cd "$ARCH_STAGE" && PKGEXT='.pkg.tar.zst' makepkg -fd --noconfirm)
    find "$ARCH_STAGE" -name "*.pkg.tar.*" -exec cp {} "$RELEASE_DIR/" \;
fi

# 6. Stage AppDir + Build AppImage & RPM Locally (using host tools, cached appimagetool, or local Docker)
echo "[6/6] Building AppImage and Fedora/RHEL .rpm packages locally..."
APPDIR="$BUILD_STAGE/MineServe.AppDir"
mkdir -p "$APPDIR/usr"
cp -a "$SELF_CONTAINED_STAGE/"* "$APPDIR/usr/"
cp "$DESKTOP_ENTRY_SRC" "$APPDIR/mineserve.desktop"
sed -i 's|Exec=/usr/bin/mineserve|Exec=mineserve|g' "$APPDIR/mineserve.desktop"
cp "$ICON_SRC" "$APPDIR/mineserve.png"
ln -sf mineserve.png "$APPDIR/.DirIcon"

cat > "$APPDIR/AppRun" << 'EOF'
#!/usr/bin/env bash
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "$HERE/usr/bin/mineserve" "$@"
EOF
chmod +x "$APPDIR/AppRun"

APPIMAGE_OUT="$RELEASE_DIR/MineServe-${VERSION_NAME}-${UNAME_ARCH}.AppImage"
CACHE_TOOLS_DIR="$HOME/.cache/mineserve-packaging"
mkdir -p "$CACHE_TOOLS_DIR"
APPIMAGETOOL_BIN=""

if command -v appimagetool >/dev/null 2>&1; then
    APPIMAGETOOL_BIN="$(command -v appimagetool)"
elif [ -x "$CACHE_TOOLS_DIR/appimagetool-${UNAME_ARCH}.AppImage" ]; then
    APPIMAGETOOL_BIN="$CACHE_TOOLS_DIR/appimagetool-${UNAME_ARCH}.AppImage"
elif command -v curl >/dev/null 2>&1; then
    echo "  -> Downloading appimagetool to $CACHE_TOOLS_DIR/appimagetool-${UNAME_ARCH}.AppImage..."
    if curl -fsSL -o "$CACHE_TOOLS_DIR/appimagetool-${UNAME_ARCH}.AppImage" \
        "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${UNAME_ARCH}.AppImage"; then
        chmod +x "$CACHE_TOOLS_DIR/appimagetool-${UNAME_ARCH}.AppImage"
        APPIMAGETOOL_BIN="$CACHE_TOOLS_DIR/appimagetool-${UNAME_ARCH}.AppImage"
    fi
fi

if [ -n "$APPIMAGETOOL_BIN" ]; then
    rm -f "$APPIMAGE_OUT"
    ARCH="$UNAME_ARCH" APPIMAGE_EXTRACT_AND_RUN=1 "$APPIMAGETOOL_BIN" "$APPDIR" "$APPIMAGE_OUT" >/dev/null 2>&1 || \
        ARCH="$UNAME_ARCH" "$APPIMAGETOOL_BIN" "$APPDIR" "$APPIMAGE_OUT" || true
fi

RPM_TOP="$BUILD_STAGE/rpmbuild"
mkdir -p "$RPM_TOP/BUILD" "$RPM_TOP/RPMS" "$RPM_TOP/SOURCES/mineserve-dist" "$RPM_TOP/SPECS" "$RPM_TOP/SRPMS"
cp -a "$SELF_CONTAINED_STAGE/"* "$RPM_TOP/SOURCES/mineserve-dist/"
cp "$DESKTOP_ENTRY_SRC" "$RPM_TOP/SOURCES/mineserve.desktop"
cp "$ICON_SRC" "$RPM_TOP/SOURCES/mineserve.png"
cp "$SYSTEMD_UNIT_SRC" "$RPM_TOP/SOURCES/mineserve.service"
cp "$SYSUSERS_SRC" "$RPM_TOP/SOURCES/mineserve.sysusers"
cp "$PROJECT_ROOT/packaging/linux/mineserve.spec" "$RPM_TOP/SPECS/mineserve.spec"
sed -i "s/^Version:.*/Version:        ${RPM_VERSION}/" "$RPM_TOP/SPECS/mineserve.spec"
sed -i "s/^Release:.*/Release:        ${RPM_RELEASE}/" "$RPM_TOP/SPECS/mineserve.spec"

if command -v rpmbuild >/dev/null 2>&1; then
    rpmbuild --define "_topdir $RPM_TOP" -bb "$RPM_TOP/SPECS/mineserve.spec"
    find "$RPM_TOP/RPMS" -name "*.rpm" -exec cp {} "$RELEASE_DIR/" \;
elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    echo "  -> 'rpmbuild' not installed locally; building .rpm via fedora:41 Docker container..."
    HOST_UID=$(id -u)
    HOST_GID=$(id -g)
    docker run --rm \
        -v "$RPM_TOP:/rpmbuild" \
        fedora:41 \
        bash -c "dnf install -y -q rpm-build && rpmbuild --define '_topdir /rpmbuild' -bb /rpmbuild/SPECS/mineserve.spec && chown -R ${HOST_UID}:${HOST_GID} /rpmbuild/RPMS"
    find "$RPM_TOP/RPMS" -name "*.rpm" -exec cp {} "$RELEASE_DIR/" \;
else
    echo "  -> Skipping .rpm build: neither 'rpmbuild' nor a running Docker daemon is available."
fi

echo ""
echo "============================================================"
echo "Linux Packages Built Successfully in $RELEASE_DIR:"
ls -lh "$RELEASE_DIR"
echo "============================================================"
