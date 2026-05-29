import { cert, getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";

const projectId = process.env.FIREBASE_PROJECT_ID || "scamradar-prod";
const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;

if (!serviceAccountJson) {
  throw new Error("Missing FIREBASE_SERVICE_ACCOUNT_JSON GitHub secret.");
}

const serviceAccount = JSON.parse(serviceAccountJson);

if (!getApps().length) {
  initializeApp({
    credential: cert(serviceAccount),
    projectId,
  });
}

const db = getFirestore();
const cutoff = Timestamp.fromMillis(Date.now() - 7 * 24 * 60 * 60 * 1000);
const reports = await db.collection("reports").where("createdAt", ">=", cutoff).get();
const counts = new Map();

reports.forEach((doc) => {
  const data = doc.data();
  const scamType = typeof data.scamType === "string" && data.scamType ? data.scamType : "OTHER";
  counts.set(scamType, (counts.get(scamType) || 0) + 1);
});

const batch = db.batch();
const seenTypes = new Set(counts.keys());

for (const [scamType, count7d] of counts.entries()) {
  batch.set(db.collection("trending").doc(scamType), {
    count7d,
    updatedAt: FieldValue.serverTimestamp(),
    source: "github-actions",
  });
}

const existingTrending = await db.collection("trending").get();
existingTrending.forEach((doc) => {
  if (!seenTypes.has(doc.id)) {
    batch.set(doc.ref, {
      count7d: 0,
      updatedAt: FieldValue.serverTimestamp(),
      source: "github-actions",
    }, { merge: true });
  }
});

await batch.commit();

console.log(`Updated ${counts.size} trending scam type(s) from ${reports.size} report(s).`);
