#!/usr/bin/env bash
# PreToolUse(Edit|Write) : empêche de modifier les secrets et les fichiers générés.
set -euo pipefail

path="$(jq -r '.tool_input.file_path // ""')"
rel="${path#"${CLAUDE_PROJECT_DIR:-$PWD}"/}"

case "$rel" in
  *.jks|*.keystore|*keystore.properties|*play-service-account*.json|*google-services.json|.env|.env.*|local.properties)
    echo "Bloqué : $rel est un secret ou un fichier local, il ne doit pas être modifié par un agent." >&2
    exit 2 ;;
  gradle/wrapper/gradle-wrapper.jar|gradlew|gradlew.bat)
    echo "Bloqué : le wrapper Gradle se met à jour avec './gradlew wrapper --gradle-version <v>', pas à la main." >&2
    exit 2 ;;
  */build/*|build/*)
    echo "Bloqué : $rel est généré par le build." >&2
    exit 2 ;;
  app/schemas/*)
    echo "Bloqué : les schémas Room sont exportés par le build ; modifie l'entité et ajoute une migration (skill room-data)." >&2
    exit 2 ;;
esac
exit 0
