#!/bin/bash
set -euo pipefail

# Linux AppImage Creator for NEWM Admin
# Usage: ./build-appimage.sh <version> <target>
# Example: ./build-appimage.sh 0.1.0 x86_64-unknown-linux-gnu

VERSION="${1:-0.1.0}"
TARGET="${2:-x86_64-unknown-linux-gnu}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$PROJECT_DIR/target/$TARGET/release"
APPDIR="$BUILD_DIR/AppDir"
APPIMAGE_NAME="NEWM-Admin-${VERSION}-x86_64.AppImage"

# Application ID (must match WM_CLASS set in main.rs via gpui app_id)
APP_ID="io.newm.newm-admin"

echo "Creating AppImage..."

# Clean and create AppDir structure
rm -rf "$APPDIR"
mkdir -p "$APPDIR/usr/bin"
mkdir -p "$APPDIR/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$APPDIR/usr/share/applications"
mkdir -p "$APPDIR/usr/share/metainfo"

# Copy executable
cp "$BUILD_DIR/newm-admin" "$APPDIR/usr/bin/"

# Copy icon with correct app_id naming for desktop integration
cp "$PROJECT_DIR/assets/icon_256.png" "$APPDIR/usr/share/icons/hicolor/256x256/apps/${APP_ID}.png"
cp "$PROJECT_DIR/assets/icon_256.png" "$APPDIR/${APP_ID}.png"
# Also create .DirIcon symlink for AppImage thumbnail
ln -sf "${APP_ID}.png" "$APPDIR/.DirIcon"

# Copy desktop file (must use app_id naming)
cp "$SCRIPT_DIR/${APP_ID}.desktop" "$APPDIR/usr/share/applications/"
cp "$SCRIPT_DIR/${APP_ID}.desktop" "$APPDIR/"

# Copy AppStream metainfo
cp "$SCRIPT_DIR/${APP_ID}.appdata.xml" "$APPDIR/usr/share/metainfo/"

# Create AppRun
cat > "$APPDIR/AppRun" << 'EOF'
#!/bin/bash
SELF=$(readlink -f "$0")
HERE=${SELF%/*}
export PATH="${HERE}/usr/bin:${PATH}"
export LD_LIBRARY_PATH="${HERE}/usr/lib:${LD_LIBRARY_PATH:-}"
exec "${HERE}/usr/bin/newm-admin" "$@"
EOF
chmod +x "$APPDIR/AppRun"

# Download appimagetool if not present
APPIMAGETOOL="$BUILD_DIR/appimagetool"
if [ ! -f "$APPIMAGETOOL" ]; then
    echo "Downloading appimagetool..."
    curl -L -o "$APPIMAGETOOL" "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
    chmod +x "$APPIMAGETOOL"
fi

# Create AppImage
cd "$BUILD_DIR"
ARCH=x86_64 "$APPIMAGETOOL" "$APPDIR" "$APPIMAGE_NAME"

echo "AppImage created at: $BUILD_DIR/$APPIMAGE_NAME"
echo "Done!"
