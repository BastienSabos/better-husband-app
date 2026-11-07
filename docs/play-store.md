# Publier sur le Play Store

Dossier de la fiche Play. Le dépôt fabrique l'AAB ; tout le reste se fait à la main dans la
[Play Console](https://play.google.com/console). Repris du dépôt `budget--planner-` (`docs/play-store.md`), adapté à
cette appli. Les formulaires de la Console changent : relire leur aide en les remplissant.

## À décider par Bastien avant de commencer

- **Le nom** (pistes de la spec : « Complices », « Nous Deux », « Attentions », « Duo »). Vérifier qu'il est libre :
  [data.inpi.fr](https://data.inpi.fr) (classes 9 et 45 ou 41 selon l'usage), [TMview](https://www.tmdn.org/tmview) et le
  Play Store. L'identifiant d'application ne change plus une fois publié : le fixer en même temps que le nom.
- **L'icône définitive** : l'icône actuelle (cœur blanc, `drawable/ic_launcher_foreground.xml`) est provisoire. Il faut
  une icône 512 × 512 et une image de présentation 1024 × 500 à l'identité du nom choisi.
- **Compte Play Console** (25 $, vérification d'identité) et une adresse e-mail de contact publique pour la fiche.
- **Où héberger la politique de confidentialité** : la Console veut une adresse publique. Si le dépôt devient public,
  l'adresse GitHub de [`politique-de-confidentialite.md`](politique-de-confidentialite.md) suffit, sinon une page simple.

## Une fois

1. **Créer l'application** : nom, langue par défaut français (France), application, gratuite. Catégorie **Style de vie**
   ou **Social** (à choisir à la lecture des catégories de la Console).
2. **Signature** : laisser Google gérer la clé de publication (Play App Signing), garder la clé d'envoi dans les
   secrets GitHub. Aucune Release GitHub n'est installée à la main avant la sortie, donc pas de contrainte de clé
   comme dans le budget planner.
3. **Politique de confidentialité** : renseigner l'adresse publique.
4. **Contenu de l'application** :

| Rubrique | Réponse |
|---|---|
| Publicités | **Non** |
| Accès à l'application | toutes les fonctions sont accessibles sans identifiant ; pas de compte |
| Classification du contenu | questionnaire IARC : ni violence, ni jeu d'argent, ni contenu généré par les utilisateurs partagé, ni position → tout public |
| Public cible | **18 ans et plus** (appli de couple) ; non conçue pour les enfants |
| Sécurité des données | **aucune donnée collectée, aucune donnée partagée** (tant que la permission `INTERNET` n'existe pas, voir AGENTS.md) ; suppression : Mes données › « Supprimer toutes mes données », ou désinstaller. L'export en fichier se fait par le sélecteur Android, vers l'endroit choisi : ce n'est pas une collecte (à vérifier dans l'aide du formulaire) |
| Santé, finance, actualités, gouvernement | non concernée. La fiche partenaire peut contenir des notes personnelles mais l'appli ne traite ni santé ni cycle en v1 |

5. **Test fermé : 12 testeurs, 14 jours d'affilée** : c'est le délai le plus long, recruter tôt. Créer une piste
   (Tests › Test fermé), une liste d'au moins 12 adresses Google, envoyer l'AAB, partager le lien d'inscription ;
   chaque testeur s'inscrit **et installe**. Puis demander l'accès à la production.

## À chaque version

1. `versionCode` incrémenté, `./gradlew check` vert, AAB signé (`bundleRelease`).
2. Play Console › piste voulue › Créer une release › importer l'AAB, notes de version en français.
3. Après publication : Android vitals (plantages et ANR remontés par le système, aucun code dans l'appli).

## Fiche

À remplacer par le nom choisi.

**Description courte** (80 caractères au plus) :

> Une attention par jour pour ton couple, en 5 minutes. Tout reste sur ton téléphone.

**Description complète** :

> Être plus présent pour la personne que tu aimes, sans y passer ta soirée.
>
> L'application te propose chaque jour une attention concrète à faire aujourd'hui, te rappelle les dates qui comptent
> et garde au même endroit ce que tu ne veux pas oublier. Cinq minutes par jour suffisent.
>
> • L'attention du jour : un geste simple, et un avis si tu veux une autre idée
> • La fiche de ton ou ta partenaire : goûts, tailles, proches importants
> • Les dates et les rappels : anniversaires, fêtes, voyages, avec une notification discrète
> • Un carnet d'idées : cadeaux, sorties, attentions, ajouté depuis n'importe quelle appli par le menu Partager
> • Les tâches du foyer : qui fait quoi, à quel rythme, ce qui est à faire aujourd'hui
> • Une série de jours, sans points ni classement
> • Verrouillage par empreinte, visage ou code, notifications neutres sur l'écran verrouillé
> • Export de tes données et suppression complète en un geste
>
> Pour tous les couples, mariés ou non. Pas de compte, pas de publicité, pas de mesure d'audience : tes données sont
> chiffrées et ne quittent pas ton téléphone.

### Visuels

- Icône 512 × 512 et image de présentation 1024 × 500 : à faire avec l'identité du nom choisi.
- Au moins deux captures de téléphone, prises sur un vrai téléphone **avec des données fictives** (prénom inventé, aucune
  vraie date). Le verrouillage active `FLAG_SECURE` : le désactiver le temps des captures.
