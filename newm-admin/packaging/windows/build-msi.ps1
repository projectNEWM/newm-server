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

Write-Host "Creating Windows MSI installer..."

# Check if cargo-wix is installed
$cargoWix = Get-Command cargo-wix -ErrorAction SilentlyContinue
if (-not $cargoWix) {
    Write-Host "Installing cargo-wix..."
    cargo install cargo-wix
}

# Initialize WiX if not already done
$wixDir = Join-Path $ProjectDir "wix"
if (-not (Test-Path $wixDir)) {
    Write-Host "Initializing WiX configuration..."
    Push-Location $ProjectDir
    cargo wix init
    Pop-Location
}

# Build MSI
Write-Host "Building MSI..."
Push-Location $ProjectDir
cargo wix --nocapture --target $Target --output "$BuildDir\$MsiName"
Pop-Location

Write-Host "MSI created at: $BuildDir\$MsiName"
Write-Host "Done!"
