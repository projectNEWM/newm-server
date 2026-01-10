# Windows MSI Installer Creator for NEWM Admin
# Usage: .\build-msi.ps1 -Version "0.1.0" -Target "x86_64-pc-windows-msvc"
# Requires: WiX Toolset v3 or cargo-wix

param(
    [string]$Version = "0.1.0",
    [string]$Target = "x86_64-pc-windows-msvc"
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Split-Path -Parent (Split-Path -Parent $ScriptDir)
$BuildDir = Join-Path $ProjectDir "target\$Target\release"
$MsiName = "NEWM-Admin-$Version-windows.msi"

Write-Host "Creating Windows MSI installer for version $Version..."

# Check if cargo-wix is installed
$cargoWix = Get-Command cargo-wix -ErrorAction SilentlyContinue
if (-not $cargoWix) {
    Write-Host "Installing cargo-wix..."
    cargo install cargo-wix
}

# Verify wix/main.wxs exists (cargo-wix expects this default location)
$wixFile = Join-Path $ProjectDir "wix\main.wxs"
if (-not (Test-Path $wixFile)) {
    Write-Error "WiX source file not found: $wixFile"
    exit 1
}

# Build MSI (version is read from Cargo.toml automatically by cargo-wix)
Write-Host "Building MSI..."
Push-Location $ProjectDir
cargo wix --nocapture --target $Target --output "$BuildDir\$MsiName"
Pop-Location

Write-Host "MSI created at: $BuildDir\$MsiName"
Write-Host "Done!"
