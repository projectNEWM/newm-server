#!/bin/bash
set -euo pipefail

# macOS App Bundle and DMG Creator for NEWM Admin
# Usage: ./bundle.sh <version> <target>
# Example: ./bundle.sh 0.1.0 aarch64-apple-darwin
#
# Environment variables:
#   MACOS_SIGNING_IDENTITY - Code signing identity (optional, skips signing if not set)

VERSION="${1:-0.1.0}"
TARGET="${2:-aarch64-apple-darwin}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$PROJECT_DIR/target/$TARGET/release"
APP_NAME="NEWM Admin.app"
DMG_NAME="NEWM-Admin-${VERSION}-macos.dmg"

# Signing identity from environment (set by GitHub Actions)
SIGNING_IDENTITY="${MACOS_SIGNING_IDENTITY:-}"

echo "Creating macOS app bundle..."
echo "  Version: $VERSION"
echo "  Target: $TARGET"
echo "  Signing: ${SIGNING_IDENTITY:-(unsigned)}"

# Create app bundle structure
rm -rf "$BUILD_DIR/$APP_NAME"
mkdir -p "$BUILD_DIR/$APP_NAME/Contents/MacOS"
mkdir -p "$BUILD_DIR/$APP_NAME/Contents/Resources"

# Check for universal binary opportunity
INTEL_BINARY="$PROJECT_DIR/target/x86_64-apple-darwin/release/newm-admin"
ARM_BINARY="$PROJECT_DIR/target/aarch64-apple-darwin/release/newm-admin"

if [[ -f "$INTEL_BINARY" && -f "$ARM_BINARY" ]]; then
    echo "Creating universal binary (Intel + ARM)..."
    lipo -create -output "$BUILD_DIR/$APP_NAME/Contents/MacOS/newm-admin" \
        "$INTEL_BINARY" "$ARM_BINARY"
    echo "  Universal binary created successfully"
else
    # Fallback to single-arch binary
    echo "Using single-architecture binary for $TARGET"
    cp "$BUILD_DIR/newm-admin" "$BUILD_DIR/$APP_NAME/Contents/MacOS/"
fi

# Copy icon
cp "$PROJECT_DIR/assets/icon.icns" "$BUILD_DIR/$APP_NAME/Contents/Resources/icon.icns"

# Create Info.plist with version substitution
sed "s/\${VERSION}/$VERSION/g" "$SCRIPT_DIR/Info.plist" > "$BUILD_DIR/$APP_NAME/Contents/Info.plist"

echo "App bundle created at: $BUILD_DIR/$APP_NAME"

# Code signing (if identity is provided)
if [[ -n "$SIGNING_IDENTITY" ]]; then
    echo "Signing app bundle with: $SIGNING_IDENTITY"
    codesign --force --deep --sign "$SIGNING_IDENTITY" \
        --options runtime \
        --entitlements "$SCRIPT_DIR/Entitlements.plist" \
        --timestamp \
        "$BUILD_DIR/$APP_NAME"
    
    echo "Verifying signature..."
    codesign --verify --verbose=2 "$BUILD_DIR/$APP_NAME"
    echo "  Signature verified successfully"
else
    echo "WARNING: No signing identity provided, app will be unsigned"
fi

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
    
    # Sign the DMG as well if we have a signing identity
    if [[ -n "$SIGNING_IDENTITY" ]]; then
        echo "Signing DMG..."
        codesign --force --sign "$SIGNING_IDENTITY" --timestamp "$BUILD_DIR/$DMG_NAME"
    fi
    
    echo "DMG created at: $BUILD_DIR/$DMG_NAME"
else
    echo "create-dmg not found, creating simple DMG with hdiutil..."
    rm -f "$BUILD_DIR/$DMG_NAME"
    hdiutil create -volname "NEWM Admin" -srcfolder "$BUILD_DIR/$APP_NAME" -ov -format UDZO "$BUILD_DIR/$DMG_NAME"
    
    # Sign the DMG as well if we have a signing identity
    if [[ -n "$SIGNING_IDENTITY" ]]; then
        echo "Signing DMG..."
        codesign --force --sign "$SIGNING_IDENTITY" --timestamp "$BUILD_DIR/$DMG_NAME"
    fi
    
    echo "DMG created at: $BUILD_DIR/$DMG_NAME"
fi

echo "Done!"
