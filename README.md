# Better husband (nom de travail)

Appli de couple Android : retenir ce qui compte pour l'autre et savoir quoi faire aujourd'hui pour que ça aille
encore mieux, en moins de 5 minutes par jour. Pour tous les couples. Local d'abord, données chiffrées sur l'appareil.

- Spec produit : https://claude.ai/artifact/NW89XjhsbHK3ChuPxDXFZu
- Architecture : [`docs/architecture.md`](docs/architecture.md) · Décisions : [`docs/adr/`](docs/adr/README.md)
- Cycle de dev : [`docs/sdlc.md`](docs/sdlc.md) · Config IA : [`docs/ai-harness.md`](docs/ai-harness.md)

## Démarrer

Prérequis : JDK 21, Android SDK (Android Studio récent).

```bash
./gradlew check            # format, Lint, tests unitaires
./gradlew installDebug     # installe sur un appareil ou émulateur
```

## Développer avec Claude Code

```bash
claude                     # à la racine : AGENTS.md, agents, skills et hooks sont chargés
/ship-feature #12          # une issue jusqu'à la PR
```

Dans GitHub, mentionner `@claude` dans une issue ou une PR. Chaque PR reçoit une revue automatique.
