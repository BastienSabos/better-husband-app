// Logique pure du mode duo côté serveur : testable sans Firebase (ADR 0004).

export const INVITE_LIFETIME_HOURS = 24;
export const IDLE_DUO_DAYS = 365;

/** L'autre membre du duo, ou `null` s'il n'est pas encore là (ou si l'auteur n'est pas membre). */
export function otherMember(members: readonly string[], author: string): string | null {
  if (!members.includes(author)) return null;
  return members.find((m) => m !== author) ?? null;
}

/** Vrai si [uid] fait partie des membres du duo. */
export function isMember(members: readonly string[], uid: string | undefined): uid is string {
  return uid !== undefined && members.includes(uid);
}

/** Instant (ms) avant lequel un duo sans activité est effacé : 12 mois, durée de la politique de confidentialité. */
export function idleCutoffMillis(nowMillis: number): number {
  return nowMillis - IDLE_DUO_DAYS * 24 * 3_600_000;
}

/**
 * Message de synchro envoyé à l'autre téléphone : des données seulement, rien de visible et aucun contenu (le
 * serveur n'en connaît pas). L'app lance alors sa synchro (`DuoSyncWorker`).
 */
export function syncMessage(token: string) {
  return {
    token,
    data: { type: "sync" },
    android: { priority: "high" as const, collapseKey: "duo-sync" },
  };
}

/** Jeton FCM lisible dans un document `devices/{uid}`, ou `null`. */
export function tokenOf(data: Record<string, unknown> | undefined): string | null {
  const token = data?.fcmToken;
  return typeof token === "string" && token.length > 0 ? token : null;
}
