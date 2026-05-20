#!/usr/bin/env bash

################################################################################
# Script de parada da aplicação API-IA para Linux/macOS (Bash)
#
# Responsabilidades:
#   1. Parar backend Spring Boot (lê PID de .run/backend.pid e mata processo)
#   2. Parar frontend Angular (lê PID de .run/frontend.pid e mata processo)
#   3. Derrubar infraestrutura (Docker Compose down)
#
# Pré-requisitos:
#   - Scripts de inicialização (start.sh) foram executados anteriormente
#   - Docker instalado e rodando
#
# Uso: ./stop.sh
#
################################################################################

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Parar backend Spring Boot
if [[ -f "$ROOT_DIR/.run/backend.pid" ]]; then
	BACK_PID="$(cat "$ROOT_DIR/.run/backend.pid")"
	if kill -0 "$BACK_PID" >/dev/null 2>&1; then
		echo "[stop] Encerrando backend PID=$BACK_PID"
		kill "$BACK_PID" || true
	fi
	rm -f "$ROOT_DIR/.run/backend.pid"
fi

# Parar frontend Angular
if [[ -f "$ROOT_DIR/.run/frontend.pid" ]]; then
	FRONT_PID="$(cat "$ROOT_DIR/.run/frontend.pid")"
	if kill -0 "$FRONT_PID" >/dev/null 2>&1; then
		echo "[stop] Encerrando frontend PID=$FRONT_PID"
		kill "$FRONT_PID" || true
	fi
	rm -f "$ROOT_DIR/.run/frontend.pid"
fi

# Derrubar infraestrutura (Docker Compose)
echo "[stop] Derrubando infraestrutura..."
docker compose down
echo "[stop] Infra encerrada."
