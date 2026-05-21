#!/usr/bin/env bash

################################################################################
# Menu interativo para gerenciar os serviços da aplicação API-IA
#
# Responsabilidades:
#   1. Permitir ao usuário iniciar ou parar serviços específicos
#   2. Retornar ao menu após cada operação
#
# Uso: ./menu.sh
#
################################################################################

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

while true; do
    echo "Selecione uma opção:"
    echo "1. Iniciar containers de IA"
    echo "2. Parar containers de IA"
    echo "3. Iniciar API Java"
    echo "4. Parar API Java"
    echo "5. Iniciar Frontend"
    echo "6. Parar Frontend"
    echo "7. Sair"

    read -rp "Digite sua escolha: " escolha

    case $escolha in
        1)
            ./scripts/start.sh ia
            ;;
        2)
            ./scripts/stop.sh ia
            ;;
        3)
            ./scripts/start.sh api
            ;;
        4)
            ./scripts/stop.sh api
            ;;
        5)
            ./scripts/start.sh frontend
            ;;
        6)
            ./scripts/stop.sh frontend
            ;;
        7)
            echo "Saindo..."
            exit 0
            ;;
        *)
            echo "Opção inválida. Tente novamente."
            ;;
    esac

done