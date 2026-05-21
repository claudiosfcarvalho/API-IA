#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_DIR="$ROOT_DIR/.run"
LOG_DIR="$ROOT_DIR/logs"

mkdir -p "$RUN_DIR" "$LOG_DIR"
cd "$ROOT_DIR"

usage() {
  echo "Uso: ./scripts/start.sh [ia|api|frontend]"
}

is_port_listening() {
  local port="$1"
  if command -v netstat >/dev/null 2>&1; then
    netstat -ano 2>/dev/null | grep -Eq "[:.]${port}[[:space:]]" && return 0
  fi
  if command -v ss >/dev/null 2>&1; then
    ss -ltn 2>/dev/null | grep -Eq ":${port}[[:space:]]" && return 0
  fi
  return 1
}

wait_for_url() {
  local url="$1"
  local label="$2"
  local attempts="$3"

  echo "[start] Aguardando $label em $url ..."
  for ((i = 1; i <= attempts; i++)); do
    if curl -sf "$url" >/dev/null 2>&1; then
      echo "[start] $label pronto."
      return 0
    fi
    sleep 2
  done

  echo "[start] Timeout aguardando $label." >&2
  return 1
}

find_backend_jar() {
  find "$ROOT_DIR/target" -maxdepth 1 -type f -name "*.jar" \
    ! -name "original-*.jar" \
    ! -name "*-sources.jar" \
    ! -name "*-javadoc.jar" | head -n 1
}

start_ia() {
  echo "[start] Subindo containers de IA..."
  docker compose up -d ollama whisperx
  wait_for_url "http://localhost:11434/" "Ollama" 60
  wait_for_url "http://localhost:9000/health" "WhisperX" 120
}

start_api() {
  local jar_path

  if is_port_listening 8080; then
    echo "[start] API Java já está em execução na porta 8080."
    return 0
  fi

  echo "[start] Compilando backend com Maven..."
  mvn clean package -f "$ROOT_DIR/pom.xml"

  jar_path="$(find_backend_jar)"
  if [[ -z "$jar_path" ]]; then
    echo "[start] Nenhum JAR executável encontrado em target/." >&2
    return 1
  fi

  echo "[start] Iniciando API Java..."
  nohup java -jar "$jar_path" > "$LOG_DIR/backend.log" 2>&1 &
  echo $! > "$RUN_DIR/backend.pid"
  echo "[start] Backend iniciado com PID=$(cat "$RUN_DIR/backend.pid")"
  wait_for_url "http://localhost:8080/actuator/health" "backend" 120
}

start_frontend() {
  if is_port_listening 4200; then
    echo "[start] Frontend já está em execução na porta 4200."
    return 0
  fi

  echo "[start] Iniciando frontend Angular..."
  (
    cd "$ROOT_DIR/frontend"
    nohup npm start > "$LOG_DIR/frontend.log" 2>&1 &
    echo $! > "$RUN_DIR/frontend.pid"
  )
  echo "[start] Frontend iniciado com PID=$(cat "$RUN_DIR/frontend.pid")"
}

TARGET="${1:-}"

case "$TARGET" in
  ia)
    start_ia
    ;;
  api)
    start_api
    ;;
  frontend)
    start_frontend
    ;;
  *)
    usage
    exit 1
    ;;
esac
