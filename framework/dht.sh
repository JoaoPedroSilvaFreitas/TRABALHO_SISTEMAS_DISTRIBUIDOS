#!/usr/bin/env bash
#
# Script auxiliar para compilar, rodar e depurar o anel DHT deste projeto.
#
# Uso:
#   ./dht.sh compilar
#   ./dht.sh no <porta> [ipBootstrap] [portaBootstrap]
#   ./dht.sh status-server [porta]
#   ./dht.sh visualizador
#   ./dht.sh listar
#   ./dht.sh parar-tudo
#
set -euo pipefail

DIR_SCRIPT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DIR_SRC="$DIR_SCRIPT/src/framework"
DIR_BUILD="$DIR_SCRIPT/build"

compilar() {
  echo "Compilando classes Java em $DIR_BUILD ..."
  mkdir -p "$DIR_BUILD"
  javac -d "$DIR_BUILD" "$DIR_SRC"/*.java
  echo "OK. Rode a partir de uma pasta de trabalho, ex:"
  echo "  cd /tmp/dht_test && java -cp $DIR_BUILD framework.Main 8080"
}

no() {
  local porta="${1:-}"
  if [ -z "$porta" ]; then
    echo "Uso: ./dht.sh no <porta> [ipBootstrap] [portaBootstrap]"
    exit 1
  fi
  if [ ! -d "$DIR_BUILD/framework" ]; then
    echo "Classes ainda não compiladas. Rodando './dht.sh compilar' primeiro..."
    compilar
  fi
  shift
  echo "Iniciando nó DHT na porta $porta (Ctrl+C ou 'sair' para encerrar)..."
  java -cp "$DIR_BUILD" framework.Main "$porta" "$@"
}

status_server() {
  local porta="${1:-9000}"
  echo "Servindo dht_status/ em http://localhost:$porta"
  python3 "$DIR_SCRIPT/status_server.py" "$porta"
}

visualizador() {
  local html="$DIR_SCRIPT/dht_ring_viz.html"
  echo "Abrindo $html ..."
  if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$html"
  else
    echo "Abra manualmente no navegador: $html"
  fi
}

listar() {
  echo "Processos framework.Main em execução:"
  jps -l 2>/dev/null | grep framework.Main || echo "  (nenhum)"
}

parar_tudo() {
  local pids
  pids="$(jps -l 2>/dev/null | grep framework.Main | awk '{print $1}')"
  if [ -z "$pids" ]; then
    echo "Nenhum nó em execução."
    return
  fi
  echo "Encerrando: $pids"
  kill $pids
}

case "${1:-}" in
  compilar) compilar ;;
  no) shift; no "$@" ;;
  status-server) shift; status_server "$@" ;;
  visualizador) visualizador ;;
  listar) listar ;;
  parar-tudo) parar_tudo ;;
  *)
    echo "Uso: $0 {compilar|no <porta> [ipBootstrap] [portaBootstrap]|status-server [porta]|visualizador|listar|parar-tudo}"
    echo
    echo "Exemplo de fluxo completo (em terminais separados):"
    echo "  ./dht.sh compilar"
    echo "  ./dht.sh status-server"
    echo "  ./dht.sh no 8080"
    echo "  ./dht.sh no 8081 127.0.0.1 8080"
    echo "  ./dht.sh visualizador"
    exit 1
    ;;
esac
