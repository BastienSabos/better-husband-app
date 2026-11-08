import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
  type RulesTestEnvironment,
} from "@firebase/rules-unit-testing";
import { readFileSync } from "node:fs";
import { Bytes, Timestamp, addDoc, collection, deleteDoc, doc, getDoc, getDocs, serverTimestamp, setDoc, updateDoc } from "firebase/firestore";
import { afterAll, beforeAll, beforeEach, describe, expect, it } from "vitest";

// Règles du mode duo (ADR 0004) : qui peut lire et écrire quoi. Hôte = alex, invitée = camille, intruse = zoe.
let env: RulesTestEnvironment;

const bytes = (n: number) => Bytes.fromUint8Array(new Uint8Array(n).fill(7));
const hex32 = "0f8b2c1e6a4d4c3b9e2f1a2b3c4d5e6f";
const inHours = (h: number) => Timestamp.fromMillis(Date.now() + h * 3_600_000);

beforeAll(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-duo",
    firestore: { rules: readFileSync("firestore.rules", "utf8") },
  });
});

afterAll(() => env.cleanup());

beforeEach(() => env.clearFirestore());

const db = (uid: string) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();

/** Un duo déjà formé de alex et camille, avec une invitation et un bloc, écrits sans passer par les règles. */
async function seed(options: { members?: string[]; invitesExpireInH?: number } = {}) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const admin = ctx.firestore();
    await setDoc(doc(admin, "duos/d1"), {
      members: options.members ?? ["alex", "camille"],
      createdAt: Timestamp.now(),
      lastActivity: Timestamp.now(),
    });
    await setDoc(doc(admin, `duos/d1/blobs/${hex32}`), { ciphertext: bytes(300), updatedAt: Timestamp.now(), author: "alex" });
    await setDoc(doc(admin, "invites/i1"), {
      duoId: "d1",
      createdBy: "alex",
      publicKeyA: bytes(32),
      expiresAt: inHours(options.invitesExpireInH ?? 24),
    });
  });
}

describe("duo", () => {
  it("is created by a signed-in phone with itself as the only member", async () => {
    await assertSucceeds(
      setDoc(doc(db("alex"), "duos/d1"), { members: ["alex"], createdAt: serverTimestamp(), lastActivity: serverTimestamp() }),
    );
  });

  it("cannot be created anonymously, with someone else, or with extra fields", async () => {
    const fields = { createdAt: serverTimestamp(), lastActivity: serverTimestamp() };
    await assertFails(setDoc(doc(anon(), "duos/d1"), { members: ["alex"], ...fields }));
    await assertFails(setDoc(doc(db("alex"), "duos/d1"), { members: ["alex", "camille"], ...fields }));
    await assertFails(setDoc(doc(db("alex"), "duos/d1"), { members: ["camille"], ...fields }));
    await assertFails(setDoc(doc(db("alex"), "duos/d1"), { members: ["alex"], name: "Nous", ...fields }));
  });

  it("is readable by its members only", async () => {
    await seed();
    await assertSucceeds(getDoc(doc(db("alex"), "duos/d1")));
    await assertSucceeds(getDoc(doc(db("camille"), "duos/d1")));
    await assertFails(getDoc(doc(db("zoe"), "duos/d1")));
    await assertFails(getDoc(doc(anon(), "duos/d1")));
  });

  it("gets its second member from the host, once", async () => {
    await seed({ members: ["alex"] });
    await assertSucceeds(updateDoc(doc(db("alex"), "duos/d1"), { members: ["alex", "camille"] }));
  });

  it("refuses a second member added by anyone but the host, or a third member", async () => {
    await seed({ members: ["alex"] });
    await assertFails(updateDoc(doc(db("zoe"), "duos/d1"), { members: ["alex", "zoe"] }));
    await assertFails(updateDoc(doc(db("alex"), "duos/d1"), { members: ["alex", "camille", "zoe"] }));
    await assertFails(updateDoc(doc(db("alex"), "duos/d1"), { members: ["camille", "alex"] }));
    await seed();
    await assertFails(updateDoc(doc(db("alex"), "duos/d1"), { members: ["alex", "zoe"] }));
  });

  it("cannot be deleted from the app", async () => {
    await seed();
    await assertFails(deleteDoc(doc(db("alex"), "duos/d1")));
  });
});

describe("blobs", () => {
  const blob = (uid: string, extra: object = {}) => ({
    ciphertext: bytes(300),
    updatedAt: serverTimestamp(),
    author: uid,
    ...extra,
  });

  it("are written and read by both members", async () => {
    await seed();
    const other = "a".repeat(32);
    await assertSucceeds(setDoc(doc(db("camille"), `duos/d1/blobs/${other}`), blob("camille")));
    await assertSucceeds(setDoc(doc(db("alex"), `duos/d1/blobs/${hex32}`), blob("alex")));
    await assertSucceeds(getDoc(doc(db("alex"), `duos/d1/blobs/${other}`)));
    await assertSucceeds(getDocs(collection(db("camille"), "duos/d1/blobs")));
  });

  it("are neither readable nor writable by a stranger", async () => {
    await seed();
    await assertFails(getDoc(doc(db("zoe"), `duos/d1/blobs/${hex32}`)));
    await assertFails(getDocs(collection(db("zoe"), "duos/d1/blobs")));
    await assertFails(setDoc(doc(db("zoe"), `duos/d1/blobs/${"b".repeat(32)}`), blob("zoe")));
    await assertFails(getDoc(doc(anon(), `duos/d1/blobs/${hex32}`)));
  });

  it("must be signed by their writer and stamped by the server", async () => {
    await seed();
    await assertFails(setDoc(doc(db("camille"), `duos/d1/blobs/${hex32}`), blob("alex")));
    await assertFails(setDoc(doc(db("camille"), `duos/d1/blobs/${hex32}`), blob("camille", { updatedAt: Timestamp.fromMillis(1) })));
  });

  it("are bounded in size and shape", async () => {
    await seed();
    const path = (id: string) => doc(db("alex"), `duos/d1/blobs/${id}`);
    await assertSucceeds(setDoc(path(hex32), blob("alex", { ciphertext: bytes(16384) })));
    await assertFails(setDoc(path(hex32), blob("alex", { ciphertext: bytes(16385) })));
    await assertFails(setDoc(path(hex32), blob("alex", { ciphertext: bytes(0) })));
    await assertFails(setDoc(path(hex32), blob("alex", { ciphertext: "texte en clair" })));
    await assertFails(setDoc(path(hex32), blob("alex", { title: "Poubelles" })));
    await assertFails(setDoc(path("idea-1"), blob("alex")));
    await assertFails(setDoc(path("POUBELLES"), blob("alex")));
  });

  it("cannot be deleted from the app", async () => {
    await seed();
    await assertFails(deleteDoc(doc(db("alex"), `duos/d1/blobs/${hex32}`)));
  });
});

describe("devices", () => {
  it("keep each phone's notification token to itself", async () => {
    await seed();
    await assertSucceeds(setDoc(doc(db("alex"), "duos/d1/devices/alex"), { fcmToken: "tok" }));
    await assertSucceeds(getDoc(doc(db("alex"), "duos/d1/devices/alex")));
    await assertFails(getDoc(doc(db("camille"), "duos/d1/devices/alex")));
    await assertFails(setDoc(doc(db("camille"), "duos/d1/devices/alex"), { fcmToken: "tok" }));
    await assertFails(setDoc(doc(db("zoe"), "duos/d1/devices/zoe"), { fcmToken: "tok" }));
    await assertFails(setDoc(doc(db("alex"), "duos/d1/devices/alex"), { fcmToken: "x".repeat(4097) }));
    await assertFails(setDoc(doc(db("alex"), "duos/d1/devices/alex"), { fcmToken: "tok", name: "Pixel" }));
  });
});

describe("invites", () => {
  const invite = (extra: object = {}) => ({
    duoId: "d1",
    createdBy: "alex",
    publicKeyA: bytes(32),
    expiresAt: inHours(24),
    ...extra,
  });

  it("are created by the host of a duo that has no second member yet", async () => {
    await seed({ members: ["alex"] });
    await assertSucceeds(setDoc(doc(db("alex"), "invites/i9"), invite()));
  });

  it("are refused for someone else's duo, a formed duo, a long life or a bad key", async () => {
    await seed({ members: ["alex"] });
    await assertFails(setDoc(doc(db("zoe"), "invites/i9"), invite({ createdBy: "zoe" })));
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite({ createdBy: "zoe" })));
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite({ expiresAt: inHours(25) })));
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite({ expiresAt: inHours(-1) })));
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite({ publicKeyA: bytes(31) })));
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite({ secret: "abc" })));
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite({ duoId: "inconnu" })));
    await seed();
    await assertFails(setDoc(doc(db("alex"), "invites/i9"), invite()));
  });

  it("can be fetched by id by any signed-in phone, but not listed", async () => {
    await seed({ members: ["alex"] });
    await assertSucceeds(getDoc(doc(db("camille"), "invites/i1")));
    await assertFails(getDocs(collection(db("camille"), "invites")));
    await assertFails(getDoc(doc(anon(), "invites/i1")));
  });

  it("cannot be fetched once expired", async () => {
    await seed({ members: ["alex"], invitesExpireInH: -1 });
    await assertFails(getDoc(doc(db("camille"), "invites/i1")));
  });

  it("are deleted by their host only", async () => {
    await seed({ members: ["alex"] });
    await assertFails(deleteDoc(doc(db("camille"), "invites/i1")));
    await assertSucceeds(deleteDoc(doc(db("alex"), "invites/i1")));
  });
});

describe("join requests", () => {
  const request = (extra: object = {}) => ({ publicKeyB: bytes(32), proof: bytes(32), ...extra });

  it("are written by the guest for herself, and read by the host", async () => {
    await seed({ members: ["alex"] });
    await assertSucceeds(setDoc(doc(db("camille"), "invites/i1/join/camille"), request()));
    await assertSucceeds(getDoc(doc(db("camille"), "invites/i1/join/camille")));
    await assertSucceeds(getDoc(doc(db("alex"), "invites/i1/join/camille")));
    await assertSucceeds(getDocs(collection(db("alex"), "invites/i1/join")));
  });

  it("are neither written for someone else nor read by a third phone", async () => {
    await seed({ members: ["alex"] });
    await assertFails(setDoc(doc(db("zoe"), "invites/i1/join/camille"), request()));
    await assertSucceeds(setDoc(doc(db("camille"), "invites/i1/join/camille"), request()));
    await assertFails(getDoc(doc(db("zoe"), "invites/i1/join/camille")));
    await assertFails(getDocs(collection(db("zoe"), "invites/i1/join")));
  });

  it("need a live invite and well-formed keys", async () => {
    await seed({ members: ["alex"], invitesExpireInH: -1 });
    await assertFails(setDoc(doc(db("camille"), "invites/i1/join/camille"), request()));
    await seed({ members: ["alex"] });
    await assertFails(setDoc(doc(db("camille"), "invites/i1/join/camille"), request({ proof: bytes(16) })));
    await assertFails(setDoc(doc(db("camille"), "invites/i1/join/camille"), request({ publicKeyB: "texte" })));
    await assertFails(setDoc(doc(db("camille"), "invites/i1/join/camille"), request({ note: "salut" })));
  });

  it("are removed by the host", async () => {
    await seed({ members: ["alex"] });
    await assertSucceeds(setDoc(doc(db("camille"), "invites/i1/join/camille"), request()));
    await assertFails(deleteDoc(doc(db("camille"), "invites/i1/join/camille")));
    await assertSucceeds(deleteDoc(doc(db("alex"), "invites/i1/join/camille")));
  });
});

describe("everything else", () => {
  it("is closed", async () => {
    await assertFails(addDoc(collection(db("alex"), "autre"), { a: 1 }));
    await assertFails(getDoc(doc(db("alex"), "autre/x")));
    expect(true).toBe(true);
  });
});
