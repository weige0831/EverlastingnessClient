# Everlastingness Launcher — Windows installer build script (Inno Setup)
#
# Builds a self-contained launcher (win-x64 single-file) and wraps it in an
# Inno Setup installer. Run on a Windows machine that has:
#   - .NET 8 SDK
#   - Inno Setup 6 (https://jrsoftware.org/isdl.php), with ISCC on PATH
#
# Usage (PowerShell):
#   .\scripts\build-windows-installer.ps1
#
# Output:
#   dist\EverlastingnessSetup-<version>.exe

[CmdletBinding()]
param(
    [string]$Version = "1.0.0",
    [string]$Configuration = "Release"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$publishDir = Join-Path $repoRoot "publish\win-x64"
$distDir    = Join-Path $repoRoot "dist"
$issPath    = Join-Path $repoRoot "scripts\installer\Everlastingness.iss"

Write-Host "==> Building Everlastingness Launcher (win-x64, self-contained)..."
Push-Location $repoRoot
try {
    dotnet publish "launcher\Everlastingness.Launcher\Everlastingness.Launcher.csproj" `
        -c $Configuration `
        -r win-x64 `
        --self-contained true `
        -p:PublishSingleFile=true `
        -o $publishDir
    if ($LASTEXITCODE -ne 0) { throw "dotnet publish failed (exit $LASTEXITCODE)" }
} finally {
    Pop-Location
}

# Generate the Inno Setup script (templated so the version is baked in).
$issDir = Join-Path $repoRoot "scripts\installer"
New-Item -ItemType Directory -Force -Path $issDir | Out-Null
$issContent = @"
; Everlastingness Launcher — Inno Setup script (auto-generated)
#define MyAppName      "Everlastingness Launcher"
#define MyAppVersion   "$Version"
#define MyAppPublisher "Everlastingness"
#define MyAppURL       "https://github.com/weige0831/EverlastingnessClient"

[Setup]
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
DefaultDirName={autopf}\{#MyAppName}
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
OutputDir=..\..\dist
OutputBaseFilename=EverlastingnessSetup-$Version
Compression=lzma2
SolidCompression=yes
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
PrivilegesRequired=lowest
UninstallDisplayIcon={app}\Everlastingness.Launcher.exe

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "chinesesimplified"; MessagesFile: "compiler:Languages\ChineseSimplified.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked

[Files]
Source: "..\..\publish\win-x64\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\Everlastingness.Launcher.exe"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\Everlastingness.Launcher.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Everlastingness.Launcher.exe"; Description: "{cm:LaunchProgram,{#MyAppName}}"; Flags: nowait postinstall skipifsilent
"@
Set-Content -Path $issPath -Value $issContent -Encoding UTF8

Write-Host "==> Compiling installer with Inno Setup (ISCC)..."
$iscc = Get-Command ISCC.exe -ErrorAction SilentlyContinue
if (-not $iscc) {
    $iscc = Get-Command "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" -ErrorAction SilentlyContinue
}
if (-not $iscc) {
    throw "Inno Setup (ISCC.exe) not found. Install Inno Setup 6 and ensure ISCC is on PATH."
}

New-Item -ItemType Directory -Force -Path $distDir | Out-Null
& $iscc.Source $issPath
if ($LASTEXITCODE -ne 0) { throw "ISCC failed (exit $LASTEXITCODE)" }

$installer = Join-Path $distDir "EverlastingnessSetup-$Version.exe"
Write-Host ""
Write-Host "==> Done. Installer:" -ForegroundColor Green
Write-Host "    $installer" -ForegroundColor Green
