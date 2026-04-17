#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

GOCMD="${PORTAL_EVOLUI_MONITOR_GO:-}"
if [[ -z "$GOCMD" ]] && command -v go >/dev/null 2>&1; then
  GOCMD=go
fi
if [[ -z "$GOCMD" ]]; then
  for d in "$HOME"/sdk/go*; do
    if [[ -x "$d/bin/go" ]]; then GOCMD="$d/bin/go"; break; fi
  done
fi
if [[ -z "$GOCMD" ]]; then
  echo "Go nao encontrado. Defina PORTAL_EVOLUI_MONITOR_GO ou adicione go ao PATH."
  exit 1
fi

"$GOCMD" version >/dev/null

mkdir -p dist

echo
echo "[build-dist] Windows amd64..."
GOOS=windows GOARCH=amd64 "$GOCMD" build -trimpath -ldflags="-s -w" -o dist/portal-evolui-monitor-windows-amd64.exe ./cmd/monitor

echo "[build-dist] Linux amd64..."
GOOS=linux GOARCH=amd64 "$GOCMD" build -trimpath -ldflags="-s -w" -o dist/portal-evolui-monitor-linux-amd64 ./cmd/monitor

echo
echo "Concluido. Saida em $(pwd)/dist/"
ls -la dist/portal-evolui-monitor-windows-amd64.exe dist/portal-evolui-monitor-linux-amd64 2>/dev/null || true
