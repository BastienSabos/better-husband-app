---
name: adr
description: Rédige un Architecture Decision Record dans docs/adr pour une décision technique structurante (stack, stockage, dépendance majeure, module, sécurité). À utiliser quand une décision engage le projet au-delà d'une PR.
argument-hint: "[titre de la décision]"
---

# Écrire un ADR

1. Numéro suivant : `ls docs/adr/` puis `NNNN-titre-en-kebab.md` (4 chiffres).
2. Modèle :

```markdown
# NNNN. Titre

- Statut : proposé | accepté | remplacé par NNNN
- Date : AAAA-MM-JJ

## Contexte
Le problème, les contraintes (produit, vie privée, solo dev, Play Store), ce qui force à décider maintenant.

## Décision
Ce qu'on fait, en une ou deux phrases affirmatives.

## Options écartées
Chaque alternative sérieuse et pourquoi elle perd.

## Conséquences
Ce qui devient plus facile, plus difficile, et ce qu'il faudra surveiller.
```

3. Statut `proposé` tant que l'utilisateur n'a pas validé. Un ADR accepté ne se réécrit pas : on en crée un
   nouveau qui le remplace.
4. Ajoute une ligne dans l'index `docs/adr/README.md`.
