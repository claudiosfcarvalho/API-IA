#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"

cd "$ROOT_DIR"

usage() {
	echo "Uso: ./scripts/stop.sh [ia|api|frontend]"
}

kill_pid_file() {
	local pid_file="$1"
	local label="$2"

	if [[ ! -f "$pid_file" ]]; then
		return 1
	fi

	local pid
	pid="$(cat "$pid_file")"
	if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
		echo "[stop] Encerrando $label PID=$pid"
		kill "$pid" >/dev/null 2>&1 || true
	fi

	rm -f "$pid_file"
	return 0
}

kill_port_processes() {
	local port="$1"
	local pids
	pids="$(netstat -ano 2>/dev/null | grep -E "[:.]${port}[[:space:]]" | awk '{print $NF}' | sort -u | tr -d '\r' || true)"

	if [[ -z "${pids// /}" ]]; then
		return 1
	fi

	while IFS= read -r pid; do
		if [[ -n "$pid" && "$pid" =~ ^[0-9]+$ && "$pid" != "0" ]]; then
			echo "[stop] Encerrando PID=$pid da porta $port"
			if command -v taskkill >/dev/null 2>&1; then
				taskkill /PID "$pid" /T /F >/dev/null 2>&1 || true
			else
				kill -9 "$pid" >/dev/null 2>&1 || true
			fi
		fi
	done <<< "$pids"

	return 0
}

stop_ia() {
	echo "[stop] Parando containers de IA..."
	docker compose stop ollama whisperx
}

stop_api() {
	if ! kill_pid_file "$RUN_DIR/backend.pid" "backend"; then
		kill_port_processes 8080 || echo "[stop] Nenhum processo da API encontrado."
	fi
}

stop_frontend() {
	if ! kill_pid_file "$RUN_DIR/frontend.pid" "frontend"; then
		kill_port_processes 4200 || echo "[stop] Nenhum processo do frontend encontrado."
	fi
}

TARGET="${1:-}"

case "$TARGET" in
	ia)
		stop_ia
		;;
	api)
		stop_api
		;;
	frontend)
		stop_frontend
		;;
	*)
		usage
		exit 1
		;;
esac
