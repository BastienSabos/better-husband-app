#!/usr/bin/env bash
# PostToolUse(Edit|Write) : formate le fichier Kotlin modifié avec ktlint (rapide, sans Gradle).
# Silencieux si ktlint n'est pas installé : spotlessCheck reste la référence en CI.
set -uo pipefail

path="$(jq -r '.tool_input.file_path // ""')"
case "$path" in
  *.kt|*.kts) ;;
  *) exit 0 ;;
esac
[ -f "$path" ] || exit 0
command -v ktlint >/dev/null 2>&1 || exit 0

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
rules=()
[ -f "$HOME/.ktlint/compose-rules.jar" ] && rules=(-R "$HOME/.ktlint/compose-rules.jar")

if ! out="$(ktlint -F "${rules[@]}" "$path" 2>&1)"; then
  # Erreurs non corrigeables automatiquement : on les remonte à Claude sans bloquer.
  echo "ktlint a trouvé des problèmes non corrigés dans $path :" >&2
  echo "$out" | tail -20 >&2
  exit 2
fi
exit 0
