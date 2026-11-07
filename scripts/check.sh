#!/usr/bin/env bash
# Rejoue en local ce que fait la CI (.github/workflows/ci.yml).
#
#   scripts/check.sh          # tout, avec lint, tests et APK de :app (SDK Android requis)
#   scripts/check.sh --jvm    # formatage et domaine seulement (sans SDK Android)
set -euo pipefail
cd "$(dirname "$0")/.."

gradle="${GRADLE:-./gradlew}"
jvm_only=false
[ "${1-}" = "--jvm" ] && jvm_only=true
[ "${ORG_GRADLE_PROJECT_jvmOnly:-}" = true ] && jvm_only=true

step() { printf '\n\033[1m==> %s\033[0m\n' "$*"; }

step "formatage, domaine (tests, couverture, detekt) et règles d'architecture"
"$gradle" -PjvmOnly=true spotlessCheck :core:domain:test :core:domain:koverVerify :core:domain:detekt :architecture:test

if [ "$jvm_only" = true ]; then
  echo "check : :app laissé à la CI (sans SDK Android, ou --jvm)." >&2
  exit 0
fi

# verifyRoborazziDebug : tests unitaires + comparaison des captures (app/src/test/screenshots/). Un écran modifié
# exprès se ré-enregistre avec ./gradlew recordRoborazziDebug, puis on regarde et on commite les PNG.
step "lint, tests unitaires, captures d'écran et APK de debug de :app"
"$gradle" lintDebug verifyRoborazziDebug assembleDebug
step "vert"
