#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$REPO_ROOT/packaging/windows-vm/docker-compose.yml"
export WIN_VM_STORAGE="${WIN_VM_STORAGE:-$HOME/VMs/mineserve-win11}"

usage() {
  cat <<'EOF'
MineServe Windows 11 KVM VM Manager (dockurr/windows)

Usage:
  ./scripts/setup_windows_vm.sh <command>

Commands:
  up       Start (or create) the Windows 11 KVM VM in the background
  down     Gracefully stop the Windows 11 VM
  status   Show container status and forwarded ports
  logs     Tail VM boot / install logs
  rdp      Launch an RDP client (xfreerdp / remmina) to the Windows VM
  help     Show this help message
EOF
}

cmd_up() {
  if [[ ! -e /dev/kvm ]]; then
    echo "Error: /dev/kvm not found. Enable VT-x / AMD-V virtualization in BIOS." >&2
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "Error: Docker daemon is not running. Start it with: sudo systemctl start docker" >&2
    exit 1
  fi
  mkdir -p "$WIN_VM_STORAGE"
  echo "==> Starting Windows 11 VM (storage: $WIN_VM_STORAGE)..."
  docker compose -f "$COMPOSE_FILE" up -d
  cat <<EOF

Windows 11 VM is running!
  - Live Browser Viewer (noVNC): http://127.0.0.1:8006
  - RDP:                         127.0.0.1:3389 (user: mineserve, pass: mineserve)
  - SSH (after first boot):      ssh -p 2222 mineserve@127.0.0.1
  - MineServe WebUI (inside VM): http://127.0.0.1:18080
  - Shared Repo Inside Windows:  \\\\host.lan\\Data

First boot downloads Windows 11 (~5-10 min) and runs oem/bootstrap.ps1 automatically.
Watch progress at http://127.0.0.1:8006 or run: ./scripts/setup_windows_vm.sh logs
EOF
}

cmd_down() {
  docker compose -f "$COMPOSE_FILE" down
}

cmd_status() {
  docker compose -f "$COMPOSE_FILE" ps
}

cmd_logs() {
  docker compose -f "$COMPOSE_FILE" logs -f --tail 100
}

cmd_rdp() {
  if command -v xfreerdp3 >/dev/null 2>&1; then
    xfreerdp3 /v:127.0.0.1:3389 /u:mineserve /p:mineserve /dynamic-resolution +clipboard &
  elif command -v xfreerdp >/dev/null 2>&1; then
    xfreerdp /v:127.0.0.1:3389 /u:mineserve /p:mineserve /dynamic-resolution +clipboard &
  elif command -v remmina >/dev/null 2>&1; then
    remmina "rdp://mineserve:mineserve@127.0.0.1:3389" &
  else
    echo "Connect via RDP to 127.0.0.1:3389 (user: mineserve, pass: mineserve) or open http://127.0.0.1:8006"
  fi
}

case "${1:-up}" in
  up) cmd_up ;;
  down) cmd_down ;;
  status) cmd_status ;;
  logs) cmd_logs ;;
  rdp) cmd_rdp ;;
  help|--help|-h) usage ;;
  *)
    echo "Unknown command: $1" >&2
    usage
    exit 1
    ;;
esac
