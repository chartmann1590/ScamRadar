// Family Weekly Digest aggregator
// Runs on a cron schedule (see .github/workflows/update-digest.yml).
// For each active pod, computes the last 7 days' share counts and writes
// pods/{code}/digests/{week-starting-iso}.
//
// Required env:
//   FIREBASE_SERVICE_ACCOUNT  base64-encoded service-account JSON
//   FCM_BROADCAST             "1" to also send FCM topic broadcasts (optional)

import { initializeApp, cert } from 'firebase-admin/app';
import { getFirestore, FieldValue, Timestamp } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';

const ENV = process.env;
if (!ENV.FIREBASE_SERVICE_ACCOUNT) {
  console.error('FIREBASE_SERVICE_ACCOUNT not set');
  process.exit(1);
}

const sa = JSON.parse(Buffer.from(ENV.FIREBASE_SERVICE_ACCOUNT, 'base64').toString('utf8'));
initializeApp({ credential: cert(sa) });
const db = getFirestore();
const msg = getMessaging();

const ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000;
const weekStart = (() => {
  const d = new Date();
  d.setUTCHours(0, 0, 0, 0);
  d.setUTCDate(d.getUTCDate() - d.getUTCDay()); // Sunday-aligned
  return d;
})();
const weekStartIso = weekStart.toISOString().slice(0, 10);

async function processPod(podDoc) {
  const code = podDoc.id;
  const sharesSnap = await db.collection(`families/${code}/shares`)
    .where('sharedAt', '>=', Timestamp.fromMillis(Date.now() - ONE_WEEK_MS))
    .get();

  let total = 0;
  let likely = 0;
  let suspicious = 0;
  const byType = new Map();
  const byMember = new Map();
  sharesSnap.forEach((doc) => {
    const d = doc.data();
    total += 1;
    if (d.verdict === 'LIKELY_SCAM') likely += 1;
    if (d.verdict === 'SUSPICIOUS') suspicious += 1;
    const t = d.scamType || 'OTHER';
    byType.set(t, (byType.get(t) || 0) + 1);
    const m = d.sharedByLabel || 'Member';
    const cur = byMember.get(m) || { scansThisWeek: 0, scamsCaught: 0 };
    cur.scansThisWeek += 1;
    if (d.verdict === 'LIKELY_SCAM') cur.scamsCaught += 1;
    byMember.set(m, cur);
  });

  const topScamTypes = [...byType.entries()]
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
    .map(([k]) => k);

  const perMember = [...byMember.entries()].map(([memberLabel, stats]) => ({
    memberLabel,
    scansThisWeek: stats.scansThisWeek,
    scamsCaught: stats.scamsCaught,
  }));

  const digestRef = db.doc(`families/${code}/digests/${weekStartIso}`);
  await digestRef.set({
    weekStarting: weekStartIso,
    totalShares: total,
    likelyScams: likely,
    suspicious,
    topScamTypes,
    perMember,
    generatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });

  if (ENV.FCM_BROADCAST === '1' && total > 0) {
    const topic = `pod_${code.toLowerCase()}_digest`;
    await msg.send({
      topic,
      data: {
        type: 'digest',
        title: `Family digest — ${total} scans this week`,
        body: `${likely} likely scams, ${suspicious} suspicious. Tap to see the full report.`,
        weekStarting: weekStartIso,
      },
    }).catch((err) => console.warn('FCM digest send failed:', code, err.message));
  }

  console.log(`pod ${code}: ${total} shares, ${likely} likely, ${suspicious} suspicious`);
}

async function main() {
  const pods = await db.collection('families').get();
  let processed = 0;
  for (const doc of pods.docs) {
    try {
      await processPod(doc);
      processed += 1;
    } catch (err) {
      console.error('pod failed', doc.id, err);
    }
  }
  console.log(`processed ${processed} pods for week ${weekStartIso}`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
