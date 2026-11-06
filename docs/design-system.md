# Design system

Source visuelle : les [maquettes v1](https://claude.ai/artifact/NDawahJc68Zk7pafzyJgBY) (9 écrans, prototype
cliquable). Le code vit dans `ui/theme/` ; toute nouvelle couleur ou taille passe par ces jetons.

## Couleurs

Material 3, réglé à la main (la couleur dynamique est désactivée par défaut pour garder l'identité).

| Rôle | Clair | Sombre | Usage |
|---|---|---|---|
| `primary` | `#8E3B46` bordeaux | `#FFB2BA` | Action principale, carte du jour |
| `primaryContainer` | `#F7DEE1` | `#73303A` | Onglet actif, bouton secondaire |
| `secondary*` | abricot `#FBE6D4` / `#7A3B0E` | `#6A3A17` / `#FFDCC6` | Cadeaux, dates famille |
| `tertiary*` | sauge `#DCEAE1` / `#24513C` | `#2A4F3B` / `#C2EDD1` | Fait, foyer, confirmation |
| `background` / `surface` | `#FAF6F2` | `#1A1517` | Fond des écrans |
| `surfaceContainerLow` | `#FFFFFF` | `#221C1E` | Cartes |
| `outline` | `#8C7D82` | `#9C8D91` | Cases à cocher, bordures de contrôles |

Couleurs de catégorie (`AppTheme.categoryColors`) : `care` (rose), `gift` (abricot), `outing` (bleu), `done`
(sauge). Toujours avec un libellé : la couleur seule ne porte jamais le sens.

Chaque paire texte / fond est testée au contraste WCAG AA (`ColorContrastTest`) : 4,5:1 pour le texte, 3:1
pour les contours. Une nouvelle couleur s'ajoute avec son test.

## Typographie

- Titres : **Bricolage Grotesque** (`display*`, `headline*`, `titleLarge`, `titleMedium`).
- Texte : **Inter** (`titleSmall`, `body*`, `label*`).
- Polices variables embarquées dans `res/font/`, réduites au latin (environ 250 Ko au total), aucune
  police téléchargée. Licence SIL OFL 1.1, textes dans `assets/licenses/` (à afficher dans l'écran
  « Licences » avant la sortie).
- Tailles en `sp` uniquement : la police agrandie du système doit toujours fonctionner (preview à 150 %).

## Formes et espacements

- Coins : 8 (pastilles), 12, 16 (champs), 24 (cartes), 28 (carte du jour, feuilles du bas).
- Espacements sur une grille de 4 dp (`AppTheme.spacing`), marge d'écran 20 dp, cible tactile 48 dp minimum.

## Principes visuels

1. Une seule action mise en avant par écran.
2. Les cartes sont blanches sur fond lin ; la carte du jour est la seule en bordeaux plein.
3. Pas de dégradé, pas d'emoji, pas de points ni de classement : une série en pastilles suffit.
4. Navigation : barre du bas à 4 onglets (Aujourd'hui, Dates, Idées, Foyer), fiche partenaire par l'avatar.
