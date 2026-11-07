# Bêta fermée (phase 7)

Google impose, pour un nouveau compte personnel, **12 testeurs qui gardent l'appli installée 14 jours d'affilée** avant
d'ouvrir la production. Fenêtre visée : 25 janvier – 5 février 2027 (spec). Les étapes de la Console sont dans
[`play-store.md`](play-store.md) ; ici, ce qu'il faut préparer, tester et mesurer. Cadre repris de
`budget--planner-` (`docs/recette.md`, `docs/essai-deux-appareils.md`).

## Avant d'inviter qui que ce soit

Dans l'ordre, parce que chaque étape débloque la suivante :

1. `./gradlew check` vert **en local** : `:app` n'a jamais été compilé ni testé (voir « Dettes connues »).
2. Les schémas Room `1.json` à `7.json` générés puis committés (`MigrationTest` en dépend).
3. Une première installation sur un vrai téléphone, avec la recette ci-dessous.
4. Nom et icône définitifs, politique de confidentialité hébergée (`play-store.md`).
5. Un AAB signé sur la piste de test fermé, `versionCode` incrémenté à chaque envoi.

## Qui recruter

- **12 adresses Google** minimum, en comptant plus large (15 à 20) : un testeur qui désinstalle avant 14 jours fait
  retomber le compteur.
- Des **couples réels**, de situations variées : avec ou sans enfants, ensemble ou à distance, mariés ou non, de tout
  genre. L'appli doit convenir à tous : c'est ce que la bêta vérifie le mieux.
- Un seul testeur par couple : il n'y a pas de mode duo en v1, le second apporterait peu.
- Prévenir que les données sont locales : **si l'appli est désinstallée, tout disparaît**, et que l'export JSON
  n'est pas chiffré.

## Message d'invitation

> Je teste une appli de couple (une attention par jour, les dates qui comptent, les tâches du foyer), sans compte ni
> publicité : tout reste sur ton téléphone. Peux-tu l'installer via ce lien et la garder **deux semaines** ? Utilise-la
> comme tu veux, 5 minutes par jour suffisent. Dis-moi ce qui t'agace, ce qui manque, et si une idée proposée t'a mis mal
> à l'aise. Tes retours me sont utiles même s'ils sont durs.

## Recette sur appareil (à faire avant la bêta, puis à chaque version)

Données fictives uniquement. Cocher chaque ligne ; une ligne qui échoue est une issue.

**Premier lancement**
- [ ] L'onboarding se déroule et demande 5 minutes au plus ; l'écran Aujourd'hui s'ouvre avec une attention.
- [ ] La carte « Notre couple » invite à remplir le profil ; une fois rempli, elle disparaît et les idées changent.

**Fiche, dates, rappels**
- [ ] Prénom, anniversaire, début de relation, goûts, tailles, proches : tout se retrouve après fermeture de l'appli.
- [ ] Une date à J-3 : la permission de notification est demandée, une notification neutre arrive à 9 h, sans prénom ni
      détail sur l'écran verrouillé.
- [ ] Une date récurrente le 29 février se comporte (28 février les années non bissextiles).

**Idées, tâches, flux du jour**
- [ ] Partager un lien depuis le navigateur vers l'appli l'ajoute au carnet d'idées.
- [ ] Une tâche à faire apparaît sur Aujourd'hui, la cocher la fait disparaître et repousse son échéance.
- [ ] « Autre idée » change l'attention, jamais deux fois la même ; cocher « fait » fait monter la série.
- [ ] Autour d'un temps fort (anniversaire saisi à J-7, Saint-Valentin, Noël), l'attention du jour vient de la liste du
      temps fort. Essayer en changeant la date du téléphone.

**Vie privée**
- [ ] Verrouillage : activé, l'appli demande l'empreinte, le visage ou le code à l'ouverture et après une minute en
      arrière-plan ; l'aperçu des applis récentes est noir ; les captures sont bloquées.
- [ ] Export : le fichier JSON contient la fiche, les dates, les idées, les tâches, le profil.
- [ ] Suppression : tout disparaît, l'appli repart du premier lancement, plus aucune notification n'arrive.
- [ ] Mes données › Politique de confidentialité : le texte s'affiche.

**Accessibilité**
- [ ] TalkBack : les titres se parcourent de titre en titre, chaque bouton est annoncé, rien n'est lu deux fois.
- [ ] Police à 200 % : aucun texte coupé, aucun bouton inatteignable ; cibles tactiles confortables.
- [ ] Mode sombre lisible.

**Mise à jour**
- [ ] Installer une version par-dessus la précédente : les données sont conservées (migration Room).

## Ce qu'on mesure

| Mesure | Seuil | Où la lire |
|---|---|---|
| Testeurs actifs 14 jours d'affilée | au moins 12 | Play Console › Test fermé |
| Taux sans plantage | 99 % ou plus | Play Console › Android vitals (aucun SDK dans l'appli) |
| Attentions « faites » par testeur | à noter, pas de seuil | retour oral : l'idée du jour est-elle faisable ? |
| Idées jugées déplacées ou genrées | zéro à la fin | retours des testeurs, à corriger dans `attentions.xml` |

Aucun outil de mesure n'est ajouté à l'appli pour ça (principes de vie privée) : les chiffres viennent de la Console et
des retours, par message ou courte conversation à mi-parcours (jour 7) et à la fin (jour 14).

## Pendant les 14 jours

- Corriger les plantages et les idées déplacées ; **pas de nouvelle fonctionnalité** (la bêta ne fait pas repartir le
  compteur, mais chaque envoi change la version sur laquelle les testeurs se basent).
- Une release par correctif groupé, jamais une par bug, avec notes de version en français.
- Si un retour touche au ton ou à la vie privée, le traiter avant le reste.

## Dettes connues à lever avant la bêta

- `:app` jamais compilé (SDK Android et Actions bloqués) : erreurs de compilation attendues au premier `check`.
- `androidx.hilt:hilt-work` et `hilt-compiler` 1.2.0 jamais résolus.
- Workers, notifications, biométrie : aucun test automatisé, seulement la recette ci-dessus.
- Le texte des 914 idées de la banque n'a pas été relu par une personne : le faire avant d'inviter des testeurs.
- Relecture `privacy-guardian` des notifications jamais faite.
