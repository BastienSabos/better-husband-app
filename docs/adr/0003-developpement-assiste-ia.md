# 0003. Développement assisté par IA avec garde-fous

- Statut : proposé
- Date : 2026-11-06

## Contexte

Un développeur seul veut livrer et maintenir une app de qualité industrielle en s'appuyant fortement sur des
agents IA (Claude Code en local, dans le cloud et dans GitHub).

## Décision

- Instructions versionnées dans le dépôt : `AGENTS.md` (portable entre outils) importé par `CLAUDE.md`.
- Spécialisation par sous-agents (plan, revue, tests, vie privée, UX) et procédures en skills.
- Garde-fous déterministes par hooks (pas de push sur `main`, pas de force push, pas d'accès aux secrets,
  formatage automatique) plutôt que par simples consignes.
- Même porte de qualité pour humains et agents : PR, CI verte, revue (IA + humaine), squash merge.
- Claude GitHub Action pour les demandes `@claude` et une revue automatique de chaque PR.

## Options écartées

- **Prompts ad hoc sans config versionnée** : résultats irréguliers, savoir perdu entre sessions.
- **Agent autonome qui merge seul** : le merge et la publication restent des décisions humaines.

## Conséquences

- La config IA est du code : elle se relit, se teste et évolue par PR.
- Coût d'usage de l'API ou de l'abonnement pour la revue automatique (désactivable).
