# Guide d'écriture UX

## Voix

L'app est un assistant fiable doublé d'un coach bienveillant. Elle parle comme un ami organisé qui veut ton bien.

| On est | On n'est pas |
|---|---|
| Direct, concret | Vague, abstrait |
| Bienveillant | Moralisateur, culpabilisant |
| Complice | Infantilisant (« bravo champion ! ») |
| Discret | Indiscret sur l'écran verrouillé |
| Sincère | Manipulateur (« fais-lui croire que… ») |

## Règles

- **Tutoiement** partout.
- **Verbe d'action** en tête des boutons : « Ajouter une date », « C'est fait », pas « OK » ou « Valider ».
- **Une idée par phrase**, 15 mots maximum dans les cartes.
- **Pas de culpabilisation** : « Pas encore d'idée aujourd'hui ? Voilà de quoi commencer. » plutôt que
  « Tu n'as rien fait depuis 3 jours. »
- **Inclusif** : c'est une appli de couple, pas une appli « mari ». Utiliser le prénom du ou de la partenaire,
  ou « ton/ta partenaire » accordé selon le genre choisi à l'onboarding (option neutre comprise). Ne jamais
  présupposer mariage, genre ou orientation. Pas de « mari », « femme », « elle » en dur dans `strings.xml`.
- **États vides utiles** : expliquer ce qui apparaîtra ici et proposer la première action.
- **Erreurs** : dire ce qui s'est passé et quoi faire, sans jargon ni code d'erreur.
- **Notifications** : texte public neutre (« Tu as un rappel »), détail visible seulement une fois déverrouillé.

## Typographie française

- Espace insécable avant `: ; ? !` et à l'intérieur des guillemets « » (` ` dans `strings.xml`).
- Apostrophe typographique ’ ou `\'` échappée dans `strings.xml`.
- Pluriels via `<plurals>`, jamais de « (s) ».
- Dates au format long localisé (« samedi 14 février »), heures en 24 h.
