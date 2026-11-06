#!/usr/bin/env bash
# SessionStart : prépare l'environnement et donne du contexte à Claude.
# - En session cloud (CLAUDE_CODE_REMOTE=true) : installe ktlint et, si le réseau l'autorise, le SDK Android.
# - Partout : affiche branche, état git et rappels utiles (stdout est injecté dans le contexte de Claude).
# Idempotent et non bloquant : un échec d'installation ne casse jamais la session.
set -uo pipefail

KTLINT_VERSION="1.8.0"          # garder aligné avec gradle/libs.versions.toml
COMPOSE_RULES_VERSION="0.6.8"   # idem
CMDLINE_TOOLS_BUILD="13114758"
ANDROID_PLATFORM="android-36"
BUILD_TOOLS="36.0.0"

log() { echo "[session-start] $*" >&2; }
persist_env() { [ -n "${CLAUDE_ENV_FILE:-}" ] && echo "export $1=\"$2\"" >> "$CLAUDE_ENV_FILE"; }

install_ktlint() {
  local bin="$HOME/.local/bin"
  mkdir -p "$bin" "$HOME/.ktlint"
  if ! command -v ktlint >/dev/null 2>&1; then
    curl -fsSL "https://github.com/pinterest/ktlint/releases/download/${KTLINT_VERSION}/ktlint" -o "$bin/ktlint" \
      && chmod +x "$bin/ktlint" && log "ktlint ${KTLINT_VERSION} installé" || log "ktlint non installé (réseau ?)"
  fi
  if [ ! -f "$HOME/.ktlint/compose-rules.jar" ]; then
    curl -fsSL "https://repo1.maven.org/maven2/io/nlopez/compose/rules/ktlint/${COMPOSE_RULES_VERSION}/ktlint-${COMPOSE_RULES_VERSION}.jar" \
      -o "$HOME/.ktlint/compose-rules.jar" || log "règles Compose ktlint non installées"
  fi
  persist_env PATH "$bin:\$PATH"
}

install_android_sdk() {
  local sdk="${ANDROID_HOME:-$HOME/android-sdk}"
  if [ ! -x "$sdk/cmdline-tools/latest/bin/sdkmanager" ]; then
    if ! curl -fsS -o /dev/null --max-time 10 https://dl.google.com/android/repository/repository2-3.xml; then
      log "dl.google.com inaccessible : SDK Android non installé. Ajoute dl.google.com et maven.google.com"
      log "à la politique réseau de l'environnement pour pouvoir builder ici. La CI reste la référence."
      return 0
    fi
    local tmp; tmp="$(mktemp -d)"
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip" -o "$tmp/tools.zip" \
      && mkdir -p "$sdk/cmdline-tools" && unzip -q "$tmp/tools.zip" -d "$tmp" && rm -rf "$sdk/cmdline-tools/latest" \
      && mv "$tmp/cmdline-tools" "$sdk/cmdline-tools/latest" || { log "échec téléchargement SDK"; return 0; }
    rm -rf "$tmp"
  fi
  yes | "$sdk/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
  "$sdk/cmdline-tools/latest/bin/sdkmanager" "platforms;${ANDROID_PLATFORM}" "build-tools;${BUILD_TOOLS}" "platform-tools" >/dev/null 2>&1 \
    && log "SDK Android prêt dans $sdk" || log "sdkmanager a échoué"
  persist_env ANDROID_HOME "$sdk"
  [ -f "$CLAUDE_PROJECT_DIR/local.properties" ] || echo "sdk.dir=$sdk" > "$CLAUDE_PROJECT_DIR/local.properties"
}

if [ "${CLAUDE_CODE_REMOTE:-}" = "true" ]; then
  install_ktlint
  install_android_sdk
fi

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0
branch="$(git branch --show-current 2>/dev/null)"
echo "Contexte du dépôt :"
echo "- Branche : ${branch:-inconnue}"
[ "$branch" = "main" ] && echo "- Tu es sur main : crée une branche (feat/…, fix/…) avant toute modification."
changes="$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
echo "- Fichiers modifiés non commités : $changes"
if [ -n "${ANDROID_HOME:-}" ] || [ -f local.properties ]; then
  echo "- SDK Android configuré : './gradlew check' est disponible."
else
  echo "- SDK Android absent : impossible de builder ici, seule la CI pourra vérifier."
fi
echo "- Workflow : skill ship-feature. Porte de qualité : ./gradlew check."
exit 0
