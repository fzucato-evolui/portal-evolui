#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

resolve_go() {
  if command -v go >/dev/null 2>&1; then
    echo "go"
    return
  fi
  read -r -p "Go não encontrado no PATH. Informe o caminho completo do binário go (ex.: /usr/local/go/bin/go): " line
  gopath="${line#"${line%%[![:space:]]*}"}"
  gopath="${gopath%"${gopath##*[![:space:]]}"}"
  if [[ -z "$gopath" ]]; then
    echo "Caminho vazio. Abortando." >&2
    exit 1
  fi
  if [[ ! -x "$gopath" && -x "${gopath}/go" ]]; then
    gopath="${gopath}/go"
  fi
  if [[ ! -x "$gopath" ]]; then
    echo "Não é um executável válido: $gopath" >&2
    exit 1
  fi
  echo "$gopath"
}

GOCMD="$(resolve_go)"
if ! "$GOCMD" version >/dev/null 2>&1; then
  echo "Não foi possível executar Go: $GOCMD" >&2
  exit 1
fi

mkdir -p dist

echo ""
echo "[build-dist] Windows amd64..."
GOOS=windows GOARCH=amd64 "$GOCMD" build -trimpath -ldflags="-s -w" -o dist/portal-evolui-runner-installer-windows-amd64.exe .

echo "[build-dist] Linux amd64..."
GOOS=linux GOARCH=amd64 "$GOCMD" build -trimpath -ldflags="-s -w" -o dist/portal-evolui-runner-installer-linux-amd64 .

echo ""
echo "Concluído. Saída em $(pwd)/dist/"
ls -la dist/portal-evolui-runner-installer-windows-amd64.exe dist/portal-evolui-runner-installer-linux-amd64
