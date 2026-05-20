#!/usr/bin/env bash

################################################################################
# Script de inicialização da aplicação API-IA para Linux/macOS (Bash)
#
# Responsabilidades:
#   1. Criar diretórios necessários (.run para PIDs, logs para saída)
#   2. Iniciar infraestrutura (Docker Compose com Ollama e WhisperX)
#   3. Aguardar disponibilidade de cada serviço (health checks)
#   4. Iniciar backend Spring Boot na porta 8080
#   5. Iniciar frontend Angular na porta 4200
#
# Pré-requisitos:
#   - Docker instalado e rodando
#   - Maven 3.9+ instalado e no PATH
#   - Node.js / npm instalado e no PATH
#   - curl instalado e no PATH (para health checks)
#
# Uso: ./start.sh
#
################################################################################

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Criar diretórios necessários
mkdir -p "$ROOT_DIR/.run" "$ROOT_DIR/logs"

################################################################################
# Etapa 1: Infraestrutura (Docker Compose)
################################################################################
echo "[start] Subindo infraestrutura com docker compose..."
docker compose up -d

################################################################################
# Etapa 2: Aguardar Ollama (LLM local)
################################################################################
echo "[start] Aguardando Ollama em http://localhost:11434 ..."
for i in {1..60}; do
  if curl -sf http://localhost:11434/ >/dev/null; then
    echo "[start] Ollama pronto."
    break
  fi
  sleep 2
  if [[ "$i" -eq 60 ]]; then
    echo "[start] Timeout aguardando Ollama." >&2
    exit 1
  fi
done

################################################################################
# Etapa 3: Aguardar WhisperX (serviço de transcrição)
################################################################################
echo "[start] Aguardando WhisperX em http://localhost:9000/health ..."
for i in {1..120}; do
  if curl -sf http://localhost:9000/health >/dev/null; then
    echo "[start] WhisperX pronto."
    break
  fi
  sleep 2
  if [[ "$i" -eq 120 ]]; then
    echo "[start] Timeout aguardando WhisperX." >&2
    exit 1
  fi
done

echo "[start] Infra OK."

################################################################################
# Etapa 4: Backend Spring Boot
################################################################################
echo "[start] Iniciando backend Spring Boot na porta 8080..."
nohup java -jar "$ROOT_DIR/backend/target/api-ia.jar" > "$ROOT_DIR/logs/backend.log" 2>&1 &
echo $! > "$ROOT_DIR/.run/backend.pid"
echo "[start] Backend iniciado com PID=$(cat "$ROOT_DIR/.run/backend.pid")"

echo "[start] Aguardando backend em http://localhost:8080/actuator/health ..."
for i in {1..120}; do
  if curl -sf http://localhost:8080/actuator/health >/dev/null; then
    echo "[start] Backend pronto."
    break
  fi
  sleep 2
  if [[ "$i" -eq 120 ]]; then
    echo "[start] Timeout aguardando backend. Consulte logs/backend.log" >&2
    exit 1
  fi
done

################################################################################
# Etapa 5: Frontend Angular
################################################################################
echo "[start] Iniciando frontend Angular na porta 4200..."
cd "$ROOT_DIR/frontend"
npm start > "$ROOT_DIR/logs/frontend.log" 2>&1 &
FRONT_BOOT_PID=$!

# Em Git Bash no Windows, o PID de "npm start" pode nao ser o processo final do servidor.
# Aguarda a porta 4200 e tenta capturar o PID real via netstat.
FRONT_PORT_PID=""
for i in {1..60}; do
  FRONT_PORT_PID="$(netstat -ano 2>/dev/null | grep -E "[:.]4200" | awk '/LISTENING|ESTABLISHED/ {print $NF; exit}' | tr -d '\r' || true)"
  if [[ -n "$FRONT_PORT_PID" ]]; then
    break
  fi
  sleep 1
done

if [[ -n "$FRONT_PORT_PID" ]]; then
  echo "$FRONT_PORT_PID" > "$ROOT_DIR/.run/frontend.pid"
else
  echo "$FRONT_BOOT_PID" > "$ROOT_DIR/.run/frontend.pid"
fi
echo "[start] Frontend iniciado com PID=$(cat "$ROOT_DIR/.run/frontend.pid")"

cd "$ROOT_DIR"

################################################################################
# Resumo de inicialização
################################################################################
echo "[start] Ambiente pronto."
echo "[start] URLs:"
echo "  Backend: http://localhost:8080"
echo "  Frontend: http://localhost:4200"
echo "  Health backend: http://localhost:8080/actuator/health"
echo "  Health whisperx: http://localhost:9000/health"
echo "[start] Logs: logs/backend.log e logs/frontend.log"
