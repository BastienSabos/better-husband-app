#!/usr/bin/env bash
# PreToolUse(Bash) : refuse les commandes dangereuses pour le repo ou les secrets.
# Code 2 = blocage, le message sur stderr est renvoyé à Claude.
set -euo pipefail

cmd="$(jq -r '.tool_input.command // ""')"

block() {
  echo "Bloqué par .claude/hooks/guard-bash.sh : $1" >&2
  exit 2
}

# Historique partagé
if grep -Eq 'git[[:space:]]+push([^|;&]*)[[:space:]](-f|--force|--force-with-lease)([[:space:]]|$)' <<<"$cmd"; then
  block "pas de push forcé. Crée un nouveau commit à la place."
fi
if grep -Eq 'git[[:space:]]+push([^|;&]*)[[:space:]](origin[[:space:]]+)?(HEAD:)?(main|master)([[:space:]]|$)' <<<"$cmd"; then
  block "pas de push direct sur main. Passe par une branche et une PR."
fi
if grep -Eq -- '--no-verify|--no-gpg-sign' <<<"$cmd"; then
  block "ne contourne pas les hooks git ni la signature."
fi
if grep -Eq 'git[[:space:]]+(reset[[:space:]]+--hard|clean[[:space:]]+-[a-z]*f)' <<<"$cmd"; then
  block "commande destructive sur l'arbre de travail : demande d'abord à l'utilisateur."
fi

# Secrets
if grep -Eq '(\.jks|\.keystore|keystore\.properties|play-service-account[^[:space:]]*\.json|google-services\.json|(^|[[:space:]/])\.env([[:space:]]|$|\.))' <<<"$cmd"; then
  block "accès à un fichier secret (keystore, compte de service, .env)."
fi

# Système
if grep -Eq 'rm[[:space:]]+-[a-zA-Z]*r[a-zA-Z]*f?[[:space:]]+(/|~|\$HOME)([[:space:]]|$)' <<<"$cmd"; then
  block "suppression récursive hors du projet."
fi

exit 0
