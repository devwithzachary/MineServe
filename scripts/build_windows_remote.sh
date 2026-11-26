#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.windows-builder.env"

if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

WIN_SSH_HOST="${WIN_SSH_HOST:-mineserve@127.0.0.1}"
WIN_SSH_PORT="${WIN_SSH_PORT:-2222}"
WIN_SSH_KEY="${WIN_SSH_KEY:-$HOME/.ssh/id_ed25519_mineserve}"
if [[ -z "${WIN_SSH_OPTS:-}" && -f "$WIN_SSH_KEY" ]]; then
  WIN_SSH_OPTS="-i $WIN_SSH_KEY -o IdentitiesOnly=yes"
else
  WIN_SSH_OPTS="${WIN_SSH_OPTS:-}"
fi
WIN_SSH_MUX_OPTS="-o StrictHostKeyChecking=accept-new -o ControlMaster=auto -o ControlPath=/tmp/mineserve-win-%r@%h:%p -o ControlPersist=10m"

usage() {
  cat <<'EOF'
MineServe Windows Remote Builder & Tester (Docker+KVM Windows 11 VM over SSH)

Usage:
  ./scripts/build_windows_remote.sh <command>

Commands:
  --setup-ssh  Install ~/.ssh/id_ed25519_mineserve.pub into the Windows VM (password: mineserve)
  --setup      Verify SSH, JDK 21, and WiX Toolset (or run oem/bootstrap.ps1)
  --sync       Mirror \\host.lan\Data to C:\Build\MineServe inside the VM via robocopy
  --test       Sync and run :core:test + :desktop:test inside Windows 11
  --package    Sync, build .msi + .exe + portable .zip, and copy to dist-windows/ (or --out <dir>)
  --out <dir>  Override local output directory for --package (e.g. --out release)
  --headless   Sync, build, and run headless WebUI (reachable at http://127.0.0.1:18080)
  --run        Sync, build, and launch native Windows GUI inside the VM desktop session
  --help, -h   Show this help message
EOF
}

encode_ps() {
  python3 -c 'import sys, base64; print(base64.b64encode(sys.stdin.read().encode("utf-16-le")).decode("ascii"))'
}

WIN_PS_ENV_PREAMBLE='$ProgressPreference = "SilentlyContinue"
$env:PATH = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
if (Test-Path "C:\Program Files (x86)\WiX Toolset v3.14\bin") { $env:PATH = "C:\Program Files (x86)\WiX Toolset v3.14\bin;" + $env:PATH }
if (Test-Path "C:\wix314") { $env:PATH = "C:\wix314;" + $env:PATH }
$jdk21 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
if ($jdk21) { $env:JAVA_HOME = $jdk21.FullName; $env:PATH = "$($jdk21.FullName)\bin;" + $env:PATH }
'

run_win_ps() {
  local body
  body="${WIN_PS_ENV_PREAMBLE}$(cat)"
  local b64
  b64="$(printf '%s' "$body" | encode_ps)"
  # shellcheck disable=SC2086
  ssh -p "$WIN_SSH_PORT" $WIN_SSH_MUX_OPTS $WIN_SSH_OPTS "$WIN_SSH_HOST" \
    "powershell -NoProfile -NonInteractive -EncodedCommand $b64"
}

run_win_ps_tty() {
  local body
  body="${WIN_PS_ENV_PREAMBLE}$(cat)"
  local b64
  b64="$(printf '%s' "$body" | encode_ps)"
  # shellcheck disable=SC2086
  ssh -t -p "$WIN_SSH_PORT" $WIN_SSH_MUX_OPTS $WIN_SSH_OPTS "$WIN_SSH_HOST" \
    "powershell -NoProfile -EncodedCommand $b64"
}

cmd_setup_ssh() {
  mkdir -p "$HOME/.ssh"
  chmod 700 "$HOME/.ssh"
  if [[ ! -f "$WIN_SSH_KEY" ]]; then
    echo "==> Generating Ed25519 SSH key at $WIN_SSH_KEY ..."
    ssh-keygen -t ed25519 -N "" -C "mineserve-builder" -f "$WIN_SSH_KEY"
  fi
  local pubkey
  pubkey="$(tr -d '\r\n' < "${WIN_SSH_KEY}.pub")"
  local b64
  b64="$(encode_ps <<EOF
\$ErrorActionPreference = 'Stop'
\$key = '$pubkey'
\$sshDir = Join-Path \$env:USERPROFILE '.ssh'
New-Item -ItemType Directory -Force -Path \$sshDir | Out-Null
\$userKeys = Join-Path \$sshDir 'authorized_keys'
\$existingUser = if (Test-Path \$userKeys) { Get-Content -Path \$userKeys -ErrorAction SilentlyContinue } else { @() }
if (\$existingUser -notcontains \$key) {
  Add-Content -Path \$userKeys -Value \$key -Encoding ascii
}
\$adminKeys = 'C:\ProgramData\ssh\administrators_authorized_keys'
\$existingAdmin = if (Test-Path \$adminKeys) { Get-Content -Path \$adminKeys -ErrorAction SilentlyContinue } else { @() }
if (\$existingAdmin -notcontains \$key) {
  Add-Content -Path \$adminKeys -Value \$key -Encoding ascii
}
icacls.exe \$adminKeys /inheritance:r /grant '*S-1-5-32-544:F' /grant '*S-1-5-18:F' | Out-Null
Write-Host 'SSH public key installed!'
EOF
)"
  echo "==> Installing public key on $WIN_SSH_HOST:$WIN_SSH_PORT (enter password 'mineserve' once)..."
  ssh -p "$WIN_SSH_PORT" -o StrictHostKeyChecking=accept-new -o PubkeyAuthentication=no "$WIN_SSH_HOST" \
    "powershell -NoProfile -NonInteractive -EncodedCommand $b64"
  echo "==> Verifying passwordless SSH..."
  ssh -p "$WIN_SSH_PORT" -i "$WIN_SSH_KEY" -o IdentitiesOnly=yes -o BatchMode=yes "$WIN_SSH_HOST" "echo SSH_KEY_OK"
}

sync_win() {
  echo "==> Syncing \\\\host.lan\\Data -> C:\\Build\\MineServe inside Windows VM ..."
  run_win_ps <<'EOF'
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path 'C:\Build\MineServe' | Out-Null
robocopy '\\host.lan\Data' 'C:\Build\MineServe' /MIR /XD .git .gradle build dist-macos dist-windows release /XF local.properties '*.keystore' '*.jks' /NFL /NDL /NJH /NJS /NC /NS
if ($LASTEXITCODE -ge 8) { exit $LASTEXITCODE }
exit 0
EOF
}

cmd_setup() {
  echo "==> Checking Windows 11 VM toolchain via SSH ($WIN_SSH_HOST:$WIN_SSH_PORT) ..."
  run_win_ps <<'EOF'
$ErrorActionPreference = 'Continue'
$hasJava = Get-Command java -ErrorAction SilentlyContinue
$hasWix = Get-Command candle.exe -ErrorAction SilentlyContinue
if (-not $hasJava -or -not $hasWix) {
  Write-Host 'JDK 21 or WiX not found yet; running bootstrap.ps1...'
  powershell -NoProfile -ExecutionPolicy Bypass -File '\\host.lan\Data\packaging\windows-vm\oem\bootstrap.ps1'
  $env:PATH = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
  if (Test-Path "C:\Program Files (x86)\WiX Toolset v3.14\bin") { $env:PATH = "C:\Program Files (x86)\WiX Toolset v3.14\bin;" + $env:PATH }
  if (Test-Path "C:\wix314") { $env:PATH = "C:\wix314;" + $env:PATH }
  $jdk21 = Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -Filter "jdk-21*" -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
  if ($jdk21) { $env:JAVA_HOME = $jdk21.FullName; $env:PATH = "$($jdk21.FullName)\bin;" + $env:PATH }
}
java -version
$wix = Get-Command candle.exe -ErrorAction SilentlyContinue
if ($wix) {
  Write-Host "WiX Toolset: $($wix.Source)"
} else {
  Write-Error "WiX candle.exe still not found."
  exit 1
}
EOF
}

cmd_test() {
  sync_win
  echo "==> Running unit tests inside Windows 11 ..."
  run_win_ps <<'EOF'
$ErrorActionPreference = 'Stop'
Set-Location 'C:\Build\MineServe'
.\gradlew.bat :core:test :desktop:test -PexcludeAndroid=true --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
EOF
}

cmd_package() {
  sync_win
  local out_rel="${OUT_DIR:-dist-windows}"
  local out_win="${out_rel//\//\\}"
  echo "==> Building Windows .msi, .exe, and portable .zip inside Windows 11 ..."
  run_win_ps <<EOF
\$ErrorActionPreference = 'Stop'
Set-Location 'C:\Build\MineServe'
.\gradlew.bat :desktop:packageMsi :desktop:packageExe :desktop:installDist -PexcludeAndroid=true --no-daemon
if (\$LASTEXITCODE -ne 0) { exit \$LASTEXITCODE }
\$verLine = Select-String -Path 'app\build.gradle.kts' -Pattern 'versionName\s*=\s*"([^"]+)"' | Select-Object -First 1
\$ver = if (\$verLine) { \$verLine.Matches[0].Groups[1].Value } else { '1.8.1' }
\$outDir = '\\host.lan\Data\\$out_win'
New-Item -ItemType Directory -Force -Path \$outDir | Out-Null
Copy-Item -Force 'desktop\build\compose\binaries\main\msi\*.msi' \$outDir -ErrorAction SilentlyContinue
Copy-Item -Force 'desktop\build\compose\binaries\main\exe\*.exe' \$outDir -ErrorAction SilentlyContinue
\$zipPath = Join-Path \$outDir "MineServe-\$ver-windows-x64-portable.zip"
if (Test-Path \$zipPath) { Remove-Item -Force \$zipPath }
Compress-Archive -Path 'desktop\build\install\desktop\*' -DestinationPath \$zipPath
Get-ChildItem \$outDir
EOF
  if [[ -d "$REPO_ROOT/$out_rel" ]]; then
    (
      cd "$REPO_ROOT/$out_rel"
      if command -v sha256sum >/dev/null 2>&1; then
        sha256sum *.msi *.exe *.zip > SHA256SUMS-windows.txt 2>/dev/null || true
      fi
      ls -lh
    )
  fi
}

cmd_headless() {
  sync_win
  echo "==> Building and launching headless WebUI inside Windows VM (forwarded to http://127.0.0.1:18082) ..."
  local body
  body="${WIN_PS_ENV_PREAMBLE}"'
$ErrorActionPreference = "Stop"
Set-Location "C:\Build\MineServe"
.\gradlew.bat :desktop:installDist -PexcludeAndroid=true --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host ""
Write-Host "==> Open http://127.0.0.1:18082 in your Linux browser!"
Write-Host ""
.\desktop\build\install\desktop\bin\mineserve.bat --headless --port 8080
'
  local b64
  b64="$(printf '%s' "$body" | encode_ps)"
  # shellcheck disable=SC2086
  ssh -t -p "$WIN_SSH_PORT" -L "18082:127.0.0.1:8080" $WIN_SSH_MUX_OPTS $WIN_SSH_OPTS "$WIN_SSH_HOST" \
    "powershell -NoProfile -EncodedCommand $b64"
}

cmd_run() {
  sync_win
  echo "==> Building and launching native Windows GUI inside VM desktop session (view at http://127.0.0.1:8006) ..."
  run_win_ps <<'EOF'
$ErrorActionPreference = 'Stop'
Set-Location 'C:\Build\MineServe'
.\gradlew.bat :desktop:installDist -PexcludeAndroid=true --no-daemon
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
schtasks /Create /TN 'MineServeBetaGUI' /TR 'C:\Build\MineServe\desktop\build\install\desktop\bin\mineserve.bat' /SC ONCE /ST 00:00 /IT /F | Out-Null
schtasks /Run /TN 'MineServeBetaGUI' | Out-Null
Write-Host 'Launched MineServe GUI in interactive Windows desktop session!'
EOF
}

ACTION=""
OUT_DIR=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --setup-ssh|--setup|--sync|--test|--package|--headless|--run)
      ACTION="$1"
      shift
      ;;
    --out)
      OUT_DIR="${2:-}"
      if [[ -z "$OUT_DIR" ]]; then
        echo "Error: --out requires a directory path" >&2
        exit 1
      fi
      shift 2
      ;;
    --help|-h)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

case "$ACTION" in
  --setup-ssh) cmd_setup_ssh ;;
  --setup) cmd_setup ;;
  --sync) sync_win ;;
  --test) cmd_test ;;
  --package) cmd_package ;;
  --headless) cmd_headless ;;
  --run) cmd_run ;;
  "") usage ;;
esac
