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
	if tasklist /FI "PID eq $BACK_PID" 2>/dev/null | grep -q "$BACK_PID" || kill -0 "$BACK_PID" >/dev/null 2>&1; then
		echo "[stop] Encerrando backend PID=$BACK_PID"
		if command -v taskkill >/dev/null 2>&1; then
			taskkill /PID "$BACK_PID" /F >/dev/null 2>&1 || true
		else
			kill "$BACK_PID" || true
		fi
	fi
	rm -f "$ROOT_DIR/.run/backend.pid"
fi

# Parar frontend Angular (PID salvo em start.sh)
if [[ -f "$ROOT_DIR/.run/frontend.pid" ]]; then
	FRONT_PID="$(cat "$ROOT_DIR/.run/frontend.pid")"
	if tasklist /FI "PID eq $FRONT_PID" 2>/dev/null | grep -q "$FRONT_PID" || kill -0 "$FRONT_PID" >/dev/null 2>&1; then
		echo "[stop] Encerrando frontend PID=$FRONT_PID"
		if command -v taskkill >/dev/null 2>&1; then
			taskkill /PID "$FRONT_PID" /F >/dev/null 2>&1 || true
		else
			kill "$FRONT_PID" || true
			sleep 1
			if kill -0 "$FRONT_PID" >/dev/null 2>&1; then
				echo "[stop] Forcando encerramento frontend PID=$FRONT_PID"
				kill -9 "$FRONT_PID" || true
			fi
		fi
	fi
	rm -f "$ROOT_DIR/.run/frontend.pid"
fi

# Fallback para Windows/Git Bash: encerra todos os processos relacionados a porta 4200
FRONT_PORT_PIDS="$(netstat -ano 2>/dev/null | grep -E "[:.]4200" | awk '{print $NF}' | tr -d '\r' | sort -u || true)"
if [[ -n "${FRONT_PORT_PIDS// /}" ]]; then
	echo "[stop] Encerrando processos da porta 4200..."
	while IFS= read -r PID; do
		if [[ -z "$PID" || "$PID" == "0" || ! "$PID" =~ ^[0-9]+$ ]]; then
			continue
		fi
		echo "[stop] Encerrando PID=$PID (porta 4200)"
		if command -v taskkill >/dev/null 2>&1; then
			taskkill /PID "$PID" /T /F >/dev/null 2>&1 || true
		else
			kill -9 "$PID" >/dev/null 2>&1 || true
		fi
	done <<< "$FRONT_PORT_PIDS"
else
	echo "[stop] Nenhum processo encontrado na porta 4200."
fi

# Derrubar infraestrutura (Docker Compose)
echo "[stop] Derrubando infraestrutura..."
docker compose down
echo "[stop] Infra encerrada."
