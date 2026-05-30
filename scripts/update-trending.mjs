import { cert, getApps, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

const projectId = process.env.FIREBASE_PROJECT_ID || "scamradar-prod";
const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
const fcmBroadcast = process.env.FCM_BROADCAST === "1";

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
const msg = getMessaging();

const WEEK_MS = 7 * 24 * 60 * 60 * 1000;
const cutoff = Timestamp.fromMillis(Date.now() - WEEK_MS);
const reports = await db.collection("reports").where("createdAt", ">=", cutoff).get();

const counts = new Map(); // scamType -> { total, byCountry: Map }
reports.forEach((doc) => {
  const data = doc.data();
  const scamType = typeof data.scamType === "string" && data.scamType ? data.scamType : "OTHER";
  const country = (typeof data.country === "string" && data.country.length === 2) ? data.country.toUpperCase() : "GLOBAL";
  const bucket = counts.get(scamType) || { total: 0, byCountry: new Map() };
  bucket.total += 1;
  bucket.byCountry.set(country, (bucket.byCountry.get(country) || 0) + 1);
  counts.set(scamType, bucket);
});

const existingTrending = await db.collection("trending").get();
const previousCounts = new Map();
existingTrending.forEach((doc) => {
  const d = doc.data();
  previousCounts.set(doc.id, {
    count7d: typeof d.count7d === "number" ? d.count7d : 0,
    byCountry: d.byCountry || {},
  });
});

const batch = db.batch();
const seenTypes = new Set(counts.keys());
const broadcasts = [];

for (const [scamType, bucket] of counts.entries()) {
  const byCountryObj = Object.fromEntries(bucket.byCountry);
  batch.set(db.collection("trending").doc(scamType), {
    count7d: bucket.total,
    byCountry: byCountryObj,
    updatedAt: FieldValue.serverTimestamp(),
    source: "github-actions",
  });

  if (fcmBroadcast) {
    const prev = previousCounts.get(scamType) || { count7d: 0, byCountry: {} };
    // For each country, if delta crosses threshold, queue a broadcast.
    for (const [country, total] of bucket.byCountry.entries()) {
      const prevForCountry = (prev.byCountry?.[country]) || 0;
      const delta = total - prevForCountry;
      const isFirstSurge = prevForCountry === 0 && total >= 5;
      const isBigJump = total >= 10 && delta >= total * 0.5;
      if (isFirstSurge || isBigJump) {
        broadcasts.push({ scamType, country, total });
      }
    }
  }
}

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

if (fcmBroadcast && broadcasts.length > 0) {
  // Last-push throttle: skip if pushed within the past 7 days.
  const throttleRef = db.collection("trending_broadcasts");
  for (const b of broadcasts) {
    const key = `${b.scamType}_${b.country}`;
    const throttleDoc = await throttleRef.doc(key).get();
    if (throttleDoc.exists) {
      const last = throttleDoc.data().lastSentAt?.toMillis?.() || 0;
      if (Date.now() - last < WEEK_MS) {
        console.log(`Throttled: ${key}`);
        continue;
      }
    }
    const topic = `trending_${b.country}`;
    const friendly = b.scamType.replace(/_/g, " ").toLowerCase();
    await msg.send({
      topic,
      data: {
        type: "trending",
        scamType: b.scamType,
        title: `${friendly} trending in your area`,
        body: `${b.total} reports this week. Tap to see what to watch for.`,
      },
    }).catch((err) => console.warn("FCM trending send failed:", key, err.message));
    await throttleRef.doc(key).set({
      lastSentAt: FieldValue.serverTimestamp(),
      total: b.total,
    });
    console.log(`Broadcast: ${key} (total=${b.total})`);
  }
}
