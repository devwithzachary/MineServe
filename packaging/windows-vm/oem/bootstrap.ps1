$ErrorActionPreference = "Continue"

Write-Host "==> Bootstrapping MineServe Windows 11 Build & Test VM..."
New-Item -ItemType Directory -Force -Path "C:\Build\MineServe" | Out-Null

# 1. Enable OpenSSH Server so Linux host can trigger builds & tests over localhost:2222
try {
    $cap = Get-WindowsCapability -Online | Where-Object Name -like 'OpenSSH.Server*'
    if ($cap -and $cap.State -ne 'Installed') {
        Write-Host "==> Installing OpenSSH Server..."
        Add-WindowsCapability -Online -Name OpenSSH.Server~~~~0.0.1.0 | Out-Null
    }
    Set-Service -Name sshd -StartupType Automatic
    Start-Service sshd
} catch {
    Write-Warning "OpenSSH Server setup warning: $_"
}

# 2. Open firewall ports for SSH (22), MineServe WebUI (8080), and Minecraft test server (25565)
netsh advfirewall firewall add rule name="MineServe VM SSH" dir=in action=allow protocol=TCP localport=22 | Out-Null
netsh advfirewall firewall add rule name="MineServe VM WebUI" dir=in action=allow protocol=TCP localport=8080 | Out-Null
netsh advfirewall firewall add rule name="MineServe VM Minecraft" dir=in action=allow protocol=TCP localport=25565 | Out-Null

# 3. Install Eclipse Temurin JDK 21 and WiX Toolset 3.14 (required by jpackage for .msi/.exe)
if (Get-Command winget -ErrorAction SilentlyContinue) {
    if (-not (Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue)) {
        Write-Host "==> Installing Eclipse Temurin JDK 21 via winget..."
        winget install --id EclipseAdoptium.Temurin.21.JDK --exact --silent --accept-package-agreements --accept-source-agreements
    }
    if (-not (Test-Path "C:\Program Files (x86)\WiX Toolset v3.14\bin\candle.exe") -and -not (Test-Path "C:\wix314\candle.exe")) {
        Write-Host "==> Installing WiX Toolset 3.14 via winget (--force)..."
        winget install --id WiXToolset.WiXToolset --version 3.14.1.8722 --exact --force --silent --accept-package-agreements --accept-source-agreements
    }
}

if (-not (Test-Path "C:\Program Files (x86)\WiX Toolset v3.14\bin\candle.exe") -and -not (Test-Path "C:\wix314\candle.exe")) {
    Write-Host "==> Downloading WiX Toolset 3.14 binaries fallback to C:\wix314..."
    $wixZip = "$env:TEMP\wix314-binaries.zip"
    Invoke-WebRequest -Uri "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip" -OutFile $wixZip -UseBasicParsing
    New-Item -ItemType Directory -Force -Path "C:\wix314" | Out-Null
    Expand-Archive -Path $wixZip -DestinationPath "C:\wix314" -Force
    Remove-Item -Force $wixZip -ErrorAction SilentlyContinue
}

Write-Host "==> Bootstrap complete!"
