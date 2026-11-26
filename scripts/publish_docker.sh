#!/usr/bin/env bash
set -euo pipefail

# MineServe Multi-Arch Docker Builder & Publisher
# Builds and optionally pushes linux/amd64 + linux/arm64 headless images to GHCR and Docker Hub.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

VERSION_NAME=$(grep -E 'versionName\s*=' app/build.gradle.kts | head -n 1 | sed 's/.*"\(.*\)".*/\1/')
PLATFORMS="linux/amd64,linux/arm64"
MODE="build-only"

usage() {
    cat <<'EOF'
MineServe Multi-Arch Docker Publisher

Usage:
  ./scripts/publish_docker.sh [options]

Options:
  --build-only          Build local host-architecture image only (default; no push)
  --ghcr                Build and push multi-arch images to ghcr.io/devwithzachary/mineserve
  --dockerhub           Build and push multi-arch images to docker.io/devwithzachary/mineserve
  --all                 Build and push multi-arch images to both GHCR and Docker Hub
  --version <ver>       Override version tag (default: versionName in app/build.gradle.kts)
  --platforms <list>    Override target platforms (default: linux/amd64,linux/arm64)
  --help, -h            Show this help message

One-Time Registry Auth:
  GHCR:       echo "$GITHUB_TOKEN" | docker login ghcr.io -u devwithzachary --password-stdin
  Docker Hub: docker login -u devwithzachary
EOF
}

while [ $# -gt 0 ]; do
    case "$1" in
        --build-only)
            MODE="build-only"
            shift
            ;;
        --ghcr)
            MODE="ghcr"
            shift
            ;;
        --dockerhub)
            MODE="dockerhub"
            shift
            ;;
        --all)
            MODE="all"
            shift
            ;;
        --version)
            VERSION_NAME="${2:-}"
            shift 2
            ;;
        --platforms)
            PLATFORMS="${2:-}"
            shift 2
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 1
            ;;
    esac
done

if ! command -v docker >/dev/null 2>&1 || ! docker info >/dev/null 2>&1; then
    echo "Error: Docker daemon is not running or 'docker' is not installed." >&2
    exit 1
fi

if [ "$MODE" = "build-only" ]; then
    echo "==> Building local Docker image mineserve:${VERSION_NAME} (mineserve:local)..."
    docker build -t "mineserve:${VERSION_NAME}" -t "mineserve:local" .
    echo "==> Local Docker build complete: mineserve:${VERSION_NAME}"
    exit 0
fi

if ! docker buildx version >/dev/null 2>&1; then
    echo "Error: 'docker buildx' is required for multi-arch builds." >&2
    exit 1
fi

BUILDER_NAME="mineserve-multiarch"
if ! docker buildx inspect "$BUILDER_NAME" >/dev/null 2>&1; then
    echo "==> Creating docker buildx builder '$BUILDER_NAME'..."
    docker buildx create --name "$BUILDER_NAME" --driver docker-container --use >/dev/null
else
    docker buildx use "$BUILDER_NAME" >/dev/null
fi
docker buildx inspect --bootstrap >/dev/null

TAG_ARGS=()
if [ "$MODE" = "ghcr" ] || [ "$MODE" = "all" ]; then
    TAG_ARGS+=(-t "ghcr.io/devwithzachary/mineserve:${VERSION_NAME}" -t "ghcr.io/devwithzachary/mineserve:latest")
fi
if [ "$MODE" = "dockerhub" ] || [ "$MODE" = "all" ]; then
    TAG_ARGS+=(-t "docker.io/devwithzachary/mineserve:${VERSION_NAME}" -t "docker.io/devwithzachary/mineserve:latest")
fi

echo "==> Building and pushing multi-arch Docker images ($PLATFORMS)..."
if ! docker buildx build \
    --platform "$PLATFORMS" \
    "${TAG_ARGS[@]}" \
    --push \
    .; then
    echo "" >&2
    echo "Push failed. Ensure you are logged into the target registries:" >&2
    echo "  GHCR:       echo \"\$GITHUB_TOKEN\" | docker login ghcr.io -u devwithzachary --password-stdin" >&2
    echo "  Docker Hub: docker login -u devwithzachary" >&2
    exit 1
fi

echo "==> Published multi-arch Docker images for v${VERSION_NAME}!"
