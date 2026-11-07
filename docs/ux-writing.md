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
  - **Tous les couples** : mariés, pacsés, en union libre, ensemble ou chacun chez soi, de même genre ou non.
    Les dates de couple s'appellent par ce que la personne a vécu (rencontre, PACS, mariage…), jamais
    « anniversaire de mariage » par défaut. L'appli ne demande pas le statut du couple : rien ne s'en sert.
  - **Toutes les familles** : sans enfant, avec des enfants communs, recomposée (enfants de l'un ou de l'autre),
    en garde alternée. Écrire « les enfants » plutôt que « vos enfants ».
  - **Accord** : seul le genre du ou de la partenaire est connu (choix à l'onboarding). On n'accorde jamais au
    genre de qui lit (« tu es fier » est interdit). Les listes `attention_bank_partner_masculine` et
    `_feminine` sont les seules à accorder ; elles évitent les clichés (pas de « des fleurs pour elle »).
- **États vides utiles** : expliquer ce qui apparaîtra ici et proposer la première action.
- **Erreurs** : dire ce qui s'est passé et quoi faire, sans jargon ni code d'erreur.
- **Notifications** : texte public neutre (« Tu as un rappel »), détail visible seulement une fois déverrouillé.

## Typographie française

- Espace insécable avant `: ; ? !` et à l'intérieur des guillemets « » (` ` dans `strings.xml`).
- Apostrophe typographique ’ ou `\'` échappée dans `strings.xml`.
- Pluriels via `<plurals>`, jamais de « (s) ».
- Dates au format long localisé (« samedi 14 février »), heures en 24 h.
