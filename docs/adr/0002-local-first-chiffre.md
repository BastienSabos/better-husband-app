# 0002. Local d'abord, Room + SQLCipher, aucune sauvegarde cloud

- Statut : proposé
- Date : 2026-11-06

## Contexte

L'app stocke des données intimes sur l'utilisateur et sur un tiers (son ou sa partenaire) : goûts, envies, dates,
idées de surprise, humeur, éventuellement cycle. Le MVP est mono-utilisateur, sans serveur.

## Décision

- Base Room unique chiffrée par SQLCipher ; passphrase aléatoire protégée par une clé Android Keystore.
- Aucune permission `INTERNET` au MVP.
- `allowBackup=false` et règles d'extraction excluant tout : rien dans Google Drive ni dans les transferts.
- Export manuel (fichier) et suppression totale disponibles depuis l'app (RGPD).

## Options écartées

- **Room sans chiffrement** : le chiffrement du disque Android ne protège pas d'un appareil déverrouillé ou d'une
  sauvegarde.
- **Firebase / Supabase dès le MVP** : comptes, serveur et conformité à gérer avant d'avoir validé l'usage.
- **Sauvegarde Android Auto Backup** : copie les données chez Google sans chiffrement de bout en bout côté app.

## Conséquences

- Changer de téléphone = export/import manuel jusqu'à la synchro (v2, mode duo).
- Migrations Room obligatoires et testées : jamais de migration destructive.
- Le formulaire « Sécurité des données » du Play Store est simple : aucune donnée collectée ni partagée.
