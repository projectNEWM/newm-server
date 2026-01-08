#!/bin/bash
set -euo pipefail

# macOS App Bundle and DMG Creator for NEWM Admin
# Usage: ./bundle.sh <version> <target>
# Example: ./bundle.sh 0.1.0 aarch64-apple-darwin

VERSION="${1:-0.1.0}"
TARGET="${2:-aarch64-apple-darwin}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$PROJECT_DIR/target/$TARGET/release"
APP_NAME="NEWM Admin.app"
DMG_NAME="NEWM-Admin-${VERSION}-macos.dmg"

echo "Creating macOS app bundle..."

# Create app bundle structure
rm -rf "$BUILD_DIR/$APP_NAME"
mkdir -p "$BUILD_DIR/$APP_NAME/Contents/MacOS"
mkdir -p "$BUILD_DIR/$APP_NAME/Contents/Resources"

# Copy executable
cp "$BUILD_DIR/newm-admin" "$BUILD_DIR/$APP_NAME/Contents/MacOS/"

# Copy icon
cp "$PROJECT_DIR/assets/icon.icns" "$BUILD_DIR/$APP_NAME/Contents/Resources/icon.icns"

# Create Info.plist with version substitution
sed "s/\${VERSION}/$VERSION/g" "$SCRIPT_DIR/Info.plist" > "$BUILD_DIR/$APP_NAME/Contents/Info.plist"

echo "App bundle created at: $BUILD_DIR/$APP_NAME"

# Create DMG if create-dmg is available
if command -v create-dmg &> /dev/null; then
    echo "Creating DMG..."
    rm -f "$BUILD_DIR/$DMG_NAME"
    
    create-dmg \
        --volname "NEWM Admin" \
        --volicon "$PROJECT_DIR/assets/icon.icns" \
        --window-pos 200 120 \
        --window-size 600 400 \
        --icon-size 100 \
        --icon "NEWM Admin.app" 150 185 \
        --app-drop-link 450 185 \
        --hide-extension "NEWM Admin.app" \
        "$BUILD_DIR/$DMG_NAME" \
        "$BUILD_DIR/$APP_NAME"
    
    echo "DMG created at: $BUILD_DIR/$DMG_NAME"
else
    echo "create-dmg not found, creating simple DMG with hdiutil..."
    rm -f "$BUILD_DIR/$DMG_NAME"
    hdiutil create -volname "NEWM Admin" -srcfolder "$BUILD_DIR/$APP_NAME" -ov -format UDZO "$BUILD_DIR/$DMG_NAME"
    echo "DMG created at: $BUILD_DIR/$DMG_NAME"
fi

echo "Done!"
