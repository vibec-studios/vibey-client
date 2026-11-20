#!/usr/bin/env bash
set -euo pipefail

PORT="${VUI_PORT:-7591}"
BASE="http://127.0.0.1:${PORT}"
CURL=(curl -fsS --max-time 15)

usage() {
  cat <<'USAGE'
usage: scripts/vui-dev.sh <command> [args]
  wait [seconds]          wait until the bridge answers (default 300 s)
  open <ns:path>          open a document as a screen, print its tree
  tree [ns:path]          element tree of the active (or given) document
  styles [ns:path]        tree with computed styles
  errors                  diagnostics of all open documents
  reload                  reload all documents now
  click <id|selector>     click an element (#id or plain id, or a selector like .tile:first-child)
  key <name> [mods]       press a key (tab, enter, esc, up, a ...), mods: shift,ctrl,alt
  type <text>             type text into the focused element
  screenshot [out.png]    save a screenshot of the game window
  loop [out.png]          reload + errors + screenshot, prints the time it took
USAGE
}

get() {
  "${CURL[@]}" -G "${BASE}$1" "${@:2}"
}

cmd="${1:-help}"
shift || true

case "$cmd" in
  wait)
    limit="${1:-300}"
    for ((i = 0; i < limit; i++)); do
      if "${CURL[@]}" "${BASE}/" >/dev/null 2>&1; then echo "bridge up on ${BASE}"; exit 0; fi
      sleep 1
    done
    echo "bridge not reachable on ${BASE}" >&2
    exit 1
    ;;
  open) get /open --data-urlencode "doc=${1:?document id}" ;;
  tree) if [ -n "${1:-}" ]; then get /tree --data-urlencode "doc=$1"; else get /tree; fi ;;
  styles) if [ -n "${1:-}" ]; then get /tree --data-urlencode "doc=$1" -d styles; else get /tree -d styles; fi ;;
  errors) get /errors ;;
  reload) get /reload ;;
  click)
    target="${1:?id or selector}"
    if [[ "$target" =~ ^#?[A-Za-z0-9_-]+$ ]]; then get /click --data-urlencode "id=${target#\#}"
    else get /click --data-urlencode "selector=${target}"; fi
    ;;
  key) get /key --data-urlencode "key=${1:?key name}" --data-urlencode "mods=${2:-}" ;;
  type) get /key --data-urlencode "text=${1:?text}" ;;
  screenshot)
    out="${1:-vui-shot.png}"
    "${CURL[@]}" "${BASE}/screenshot" -o "$out"
    echo "$out"
    ;;
  loop)
    out="${1:-vui-shot.png}"
    start=$(date +%s%N)
    "${CURL[@]}" "${BASE}/reload" -o /dev/null
    errors=$("${CURL[@]}" "${BASE}/errors")
    "${CURL[@]}" "${BASE}/screenshot" -o "$out"
    end=$(date +%s%N)
    count=$( (printf '%s' "$errors" | grep -o '"message"' || true) | wc -l | tr -d ' ')
    echo "$out  errors=${count}  $(( (end - start) / 1000000 )) ms"
    if [ "$count" != "0" ]; then printf '%s\n' "$errors"; fi
    ;;
  help | -h | --help) usage ;;
  *) usage >&2; exit 2 ;;
esac
