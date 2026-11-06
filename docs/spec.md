# Spec produit : app Android de couple (nom à trouver)

Mise à jour : 2026-11-06. Version de travail : [Claude Doc](https://claude.ai/artifact/NW89XjhsbHK3ChuPxDXFZu).

## Vision

L'app aide chaque personne en couple, quel que soit son genre ou celui de son ou sa partenaire, à être un partenaire et un parent plus présent, plus attentionné et plus fiable, au quotidien, sans y passer plus de 5 minutes par jour.

Elle joue deux rôles. En **assistant**, elle retient à sa place ce qui compte (dates, goûts, envies, tâches, engagements pris) et le rappelle au bon moment. En **coach**, elle suggère des gestes concrets, l'aide à préparer les moments importants et l'aide à prendre du recul après une tension.

**Proposition de valeur :** « Ne plus jamais oublier ce qui compte pour l'autre, et savoir quoi faire aujourd'hui pour que ça aille encore mieux. »

**Cible de publication :** Play Store, grand public, francophone d'abord. Positionnement : une app de couple, pas une app « pour maris ». Elle s'adresse à tous les couples, mariés ou non, de tous genres. Le nom de travail « Better husband » est abandonné.

## Personas

Le persona principal est la personne qui installe l'app ; son ou sa partenaire est un persona secondaire dont la présence dans l'app est optionnelle : il ou elle peut la connaître et la rejoindre, sans que ce soit nécessaire.

| Persona | Situation | Ce qu'il ou elle veut | Frein principal |
| --- | --- | --- | --- |
| **Thomas, 34 ans, le distrait de bonne volonté** | Marié à Julie, 2 enfants, job prenant | Ne plus oublier les dates, les rendez-vous des enfants, ce que Julie a dit vouloir | Manque de temps, charge mentale mal répartie |
| **Léa, 29 ans, le jeune couple** | En couple avec Sam depuis 2 ans, pas d'enfant | Entretenir la flamme, trouver des idées de sorties et de surprises | Routine qui s'installe, peu d'inspiration |
| **Nadia, 45 ans, le couple installé** | 15 ans de vie commune avec Marc, ados à la maison | Retrouver du temps à deux, mieux communiquer après des tensions | Habitudes figées, difficulté à parler de soi |
| **Le ou la partenaire (secondaire)** | Partenaire de l'un d'eux | Être vu ou vue et entendu, partager la charge du foyer | Ne veut pas d'une relation « gérée par une app » |

## Principes produit et ton

Le ton est celui d'un assistant fiable doublé d'un coach bienveillant : direct, concret, jamais moralisateur.

1. **Actionnable avant tout.** Chaque écran propose une action faisable aujourd'hui, pas un conseil abstrait.
2. **5 minutes par jour maximum.** Un rituel du matin court, des rappels ciblés, zéro tableau de bord à remplir.
3. **Sincérité, pas manipulation.** L'app aide à être attentif, pas à simuler l'attention. Pas de messages envoyés à la place de l'utilisateur sans validation.
4. **Discrétion par défaut.** Notifications neutres sur l'écran de verrouillage, verrouillage biométrique possible, données très personnelles protégées.
5. **Valoriser sans infantiliser.** Une progression visible (séries, bilan mensuel) mais pas de points ni de classement.
6. **Coach, pas thérapeute.** Face à un vrai mal-être ou à des violences, l'app oriente vers des professionnels.

## Domaines fonctionnels

L'app couvre six domaines, reliés par un socle commun : la **fiche partenaire** (ce que l'autre aime, ses tailles, ses envies, ses proches) et le **flux du jour** qui pioche dans tous les domaines.

| Domaine | Ce que l'app fait | Exemples de features |
| --- | --- | --- |
| **Dates et cadeaux** | Retient toutes les dates qui comptent et anticipe | Anniversaires, fête des mères ou des pères, anniversaire de rencontre ; rappel J-14 et J-3 ; liste d'idées cadeaux alimentée au fil de l'eau ; budget |
| **Attentions et surprises** | Suggère des petits gestes réguliers | Idée du jour (mot doux, café au lit, fleurs) ; banque d'idées filtrée par goûts et budget ; historique pour ne pas se répéter |
| **Charge mentale et foyer** | Rend visible et partage les tâches du quotidien | Tâches récurrentes (courses, lessives, papiers) ; check-list « ce qui traîne » ; répartition visible si la partenaire participe |
| **Communication et moments à deux** | Entretient le lien et aide après une tension | Rituel hebdo « point couple » guidé ; questions pour se découvrir ; planificateur de soirées en amoureux ; aide à préparer une discussion difficile |
| **Parent et famille** | Garde le fil des enfants et de la belle-famille | Agenda des enfants (école, médecin, activités) ; idées d'activités parent-enfant ; dates et goûts de la belle-famille |
| **Santé et bien-être** | Prendre soin de soi pour prendre soin de l'autre | Humeur du jour (la sienne, et ressenti du couple) ; rappels sport, sommeil ; suivi de cycle si la partenaire le partage ; orientation vers des pros |

## MVP

Le MVP est mono-utilisateur (une seule personne du couple), local d'abord, gratuit et sans IA, et tient en cinq briques livrables en 8 à 10 semaines par une personne. Le coach IA arrive en v2.

1. **Onboarding et fiche partenaire.** Prénom, dates clés, goûts (fleurs, plats, couleurs, marques), tailles, proches importants.
2. **Dates et rappels.** Calendrier des dates qui comptent, rappels J-14 / J-3 / jour J avec une suggestion d'action.
3. **Carnet d'idées.** Capture rapide (« il ou elle a dit aimer… ») depuis un widget ou le partage Android, classée en cadeaux, sorties, attentions.
4. **Flux du jour.** Une carte par jour : une attention suggérée, les tâches du foyer à faire, la prochaine date à préparer. Marquer « fait » nourrit la série.
5. **Tâches du foyer.** Tâches récurrentes simples, sans partage dans un premier temps.

**Hors MVP :** coach IA (v2), compte partenaire, synchro cloud des données, rituel couple guidé, module enfants, monétisation.

**Critère de succès :** 40 % des utilisateurs actifs ouvrent l'app au moins 4 jours sur 7 après 30 jours.

## Idées de features pour plus tard

- **Mode duo :** le ou la partenaire a son propre accès, partage sa liste d'envies et les tâches du foyer, sans voir le carnet privé de l'autre (surprises, idées cadeaux). Rien ne change pour l'utilisateur s'il ne l'invite jamais.
- **Coach IA :** suggestions personnalisées à partir de la fiche partenaire, aide à écrire un mot (l'utilisateur garde la main sur le texte final), débriefing après une dispute.
- **Rituel « point couple » hebdo :** 10 minutes guidées, trois questions, une action pour la semaine.
- **Planificateur de soirées :** idées selon la ville, la météo, le budget et la garde des enfants.
- **Mode charge mentale :** inventaire des tâches invisibles du foyer et répartition visible à deux.
- **Module enfants :** agenda, rendez-vous médicaux, idées d'activités par âge.
- **Bilan mensuel :** ce qui a été fait, ce qui a marqué, une intention pour le mois suivant.
- **Intégrations :** Google Agenda, Contacts, liens vers fleuristes ou réservation de restaurants.
- **Widget écran d'accueil :** l'attention du jour en un coup d'œil.

## Choix techniques à challenger

Ma recommandation : Kotlin natif avec Jetpack Compose, stockage local chiffré, et aucun serveur en v1 ; un backend léger arrive en v2 avec le coach IA et le mode duo.

| Sujet | Recommandation | Alternative | Pourquoi |
| --- | --- | --- | --- |
| Langage / UI | Kotlin + Jetpack Compose | Flutter | Android seul visé ; widgets, notifications et partage natifs plus simples |
| Stockage | Room + SQLCipher, local ; modèle de données pensé dès le départ pour le partage à deux (listes de cadeaux, tâches) | Cloud dès le départ | Données intimes ; zéro serveur au MVP |
| Synchro (v1.1) | Supabase ou Firebase | Backend maison | Comptes, temps réel et partage à deux sans infra à gérer |
| Rappels | WorkManager + notifications | AlarmManager | Fiable, respecte la batterie |
| IA (v2) | API Claude via un proxy serveur | Modèle embarqué | Qualité des suggestions ; clé jamais dans l'app ; seul le contexte utile est envoyé, rien n'est stocké côté serveur |
| Monétisation | Aucune au lancement ; freemium envisagé en v2 (coach IA et mode duo payants) | Paiement unique | Couvre le coût de l'IA et du serveur |
| Conformité | RGPD : consentement explicite, export et suppression | — | Données sur un tiers (la partenaire) et données de santé possibles |

## Roadmap v1

Sortie visée sur le Play Store la semaine du 8 février 2027 : 10 semaines de travail à partir du 9 novembre 2026, une fois la conf de dev IA en place (harness, SDLC, agents, skills, traitée à part), 2 semaines de pause pendant les fêtes, puis 2 semaines de bêta fermée. Google l'impose avant la mise en production pour un nouveau compte développeur personnel : 12 testeurs pendant 14 jours.

| Phase | Dates | Contenu | Livrable |
| --- | --- | --- | --- |
| 0. Fondations | 9 – 13 nov. | Projet Kotlin + Compose, architecture, CI, design system, maquettes des 5 écrans clés | App vide qui build et passe la CI |
| 1. Fiche partenaire | 16 – 27 nov. | Onboarding, fiche partenaire, stockage Room + SQLCipher | On crée et modifie la fiche |
| 2. Dates et rappels | 30 nov. – 11 déc. | Calendrier des dates, rappels J-14 / J-3 / jour J (WorkManager), notifications neutres | Rappels fiables, testés sur 3 appareils |
| 3. Carnet d'idées | 14 – 18 déc. | Capture rapide, partage Android vers l'app, catégories | On capture une idée en moins de 10 secondes |
| Pause | 21 déc. – 1er janv. | Fêtes | — |
| 4. Tâches du foyer | 4 – 8 janv. | Tâches récurrentes, fait / pas fait | Liste des tâches qui tourne |
| 5. Flux du jour | 11 – 15 janv. | Carte du jour, banque d'idées d'attentions (environ 150, rédigées à la main), séries | Écran d'accueil complet |
| 6. Finitions | 18 – 22 janv. | Verrouillage biométrique, export et suppression RGPD, accessibilité, icône, fiche Play Store | Version candidate |
| 7. Bêta fermée | 25 janv. – 5 fév. | 12 testeurs minimum, 14 jours, corrections | Retours traités, crash-free ≥ 99 % |
| Sortie | Semaine du 8 fév. | Publication en production | v1 sur le Play Store |

**À prévoir avant la bêta :** compte Google Play Console (25 $), nom définitif de l'app, politique de confidentialité hébergée, 12 testeurs recrutés.

**Ensuite, v1.1 mode duo (environ 4 semaines, mi-février à mi-mars 2027) :** comptes, invitation du ou de la partenaire, synchro Supabase, listes de cadeaux et d'envies partagées, tâches du foyer partagées, carnet privé conservé de chaque côté.

## Questions ouvertes

- [x] Décidé : la partenaire peut connaître l'app, sans que ce soit requis. L'app est complète en solo ; le mode duo est une invitation optionnelle.
- [x] Décidé : positionnement app de couple, pour tous les couples. Reste à choisir le nom ; pistes : « Complices », « Nous Deux », « Attentions », « Duo » (disponibilité Play Store et marque à vérifier).
- [x] Décidé : l'IA arrive en v2, la v1 reste simple.
- [x] Décidé : pas de monétisation au lancement ; à rediscuter avec l'arrivée de l'IA.
- [ ] Qui développe : toi, moi via un repo GitHub, ou les deux ?
- [ ] Quels domaines passent en premier après le MVP ?
