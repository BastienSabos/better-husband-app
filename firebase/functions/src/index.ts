import { initializeApp } from "firebase-admin/app";
import { FieldValue, Timestamp, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { setGlobalOptions } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { idleCutoffMillis, isMember, otherMember, syncMessage, tokenOf } from "./duo";

// Cloud Functions du mode duo (ADR 0004). Elles ne lisent jamais le contenu des blocs : il est chiffré de bout en
// bout. Région d'Europe, comme Firestore (`eur3`).
setGlobalOptions({ region: "europe-west1", maxInstances: 5 });
initializeApp();

const db = getFirestore();

/**
 * Un bloc a été écrit : on prévient l'autre téléphone par un message FCM de données, vide de contenu, pour qu'il se
 * synchronise. On note aussi l'activité du duo (effacement des duos abandonnés).
 */
export const onBlobWritten = onDocumentWritten("duos/{duoId}/blobs/{blobId}", async (event) => {
  const after = event.data?.after;
  if (!after?.exists) return;
  const author = after.get("author");
  if (typeof author !== "string") return;

  const duoRef = db.doc(`duos/${event.params.duoId}`);
  const duo = await duoRef.get();
  // Duo quitté entre-temps (`leaveDuo`) : rien à faire.
  if (!duo.exists) return;
  const members = (duo.get("members") ?? []) as string[];
  await duoRef.update({ lastActivity: FieldValue.serverTimestamp() });

  const other = otherMember(members, author);
  if (!other) return;
  const token = tokenOf((await duoRef.collection("devices").doc(other).get()).data());
  if (!token) return;
  try {
    await getMessaging().send(syncMessage(token));
  } catch {
    // Jeton périmé ou téléphone éteint : l'autre se synchronisera à l'ouverture ou à sa synchro périodique.
  }
});

/** Quitter le duo : efface le duo, ses blocs, ses appareils et ses invitations, sans l'accord de l'autre. */
export const leaveDuo = onCall({ enforceAppCheck: true }, async (request) => {
  const uid = request.auth?.uid;
  const duoId = request.data?.duoId;
  if (!uid) throw new HttpsError("unauthenticated", "Connexion requise.");
  if (typeof duoId !== "string" || duoId.length === 0 || duoId.includes("/")) {
    throw new HttpsError("invalid-argument", "Identifiant de duo invalide.");
  }

  const duoRef = db.doc(`duos/${duoId}`);
  const duo = await duoRef.get();
  if (!duo.exists) return { deleted: false };
  if (!isMember((duo.get("members") ?? []) as string[], uid)) {
    throw new HttpsError("permission-denied", "Tu ne fais pas partie de ce duo.");
  }

  const invites = await db.collection("invites").where("duoId", "==", duoId).get();
  await Promise.all(invites.docs.map((invite) => db.recursiveDelete(invite.ref)));
  await db.recursiveDelete(duoRef);
  return { deleted: true };
});

/** Chaque nuit : invitations expirées et duos sans activité depuis 12 mois. */
export const purgeExpired = onSchedule("every day 03:00", async () => {
  const now = Date.now();
  const expired = await db.collection("invites").where("expiresAt", "<", Timestamp.fromMillis(now)).get();
  await Promise.all(expired.docs.map((invite) => db.recursiveDelete(invite.ref)));

  const idle = await db.collection("duos").where("lastActivity", "<", Timestamp.fromMillis(idleCutoffMillis(now))).get();
  await Promise.all(idle.docs.map((duo) => db.recursiveDelete(duo.ref)));
});
