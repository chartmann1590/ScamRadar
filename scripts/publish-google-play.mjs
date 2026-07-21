// Publishes a signed Android App Bundle to Google Play and updates the
// en-US store listing in a single Play Developer API edit.
//
// Driven entirely by environment variables so it can run from CI without
// any third-party action:
//   GOOGLE_PLAY_SERVICE_ACCOUNT_JSON  (required) service account key JSON
//   PACKAGE_NAME                       default com.charles.scamradar.app
//   AAB_PATH                           default app/build/outputs/bundle/release/app-release.aab
//   MAPPING_PATH                       default app/build/outputs/mapping/release/mapping.txt
//   TRACK                              default production
//   RELEASE_STATUS                     completed | draft  (default completed)
//   LISTING_DIR                        default play-store/listing
//   LISTING_LANGUAGE                   default en-US
//   RELEASE_NOTES_PATH                 default distribution/whatsnew/whatsnew-en-US
//   CHANGES_NOT_SENT_FOR_REVIEW        "true" commits without sending for review;
//                                      otherwise the script attempts auto-review and
//                                      falls back to no-review if Play refuses.
//
// Listing text is the source of truth in the repo (play-store/listing/*.txt),
// so this keeps the Play Console listing in sync with the codebase.

import { google } from "googleapis";
import { readFileSync, createReadStream, existsSync } from "node:fs";
import path from "node:path";

const PACKAGE_NAME = process.env.PACKAGE_NAME || "com.charles.scamradar.app";
const AAB_PATH = process.env.AAB_PATH || "app/build/outputs/bundle/release/app-release.aab";
const MAPPING_PATH = process.env.MAPPING_PATH || "app/build/outputs/mapping/release/mapping.txt";
const TRACK = process.env.TRACK || "production";
const RELEASE_STATUS = (process.env.RELEASE_STATUS || "completed").toLowerCase() === "draft" ? "draft" : "completed";
const LISTING_DIR = process.env.LISTING_DIR || "play-store/listing";
const LISTING_LANGUAGE = process.env.LISTING_LANGUAGE || "en-US";
const RELEASE_NOTES_PATH = process.env.RELEASE_NOTES_PATH || "distribution/whatsnew/whatsnew-en-US";
const SERVICE_ACCOUNT_JSON = process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_JSON;
const PREFER_AUTO_REVIEW = process.env.CHANGES_NOT_SENT_FOR_REVIEW !== "true";

const LIMITS = { title: 30, shortDescription: 80, fullDescription: 4000 };

if (!SERVICE_ACCOUNT_JSON) {
  throw new Error("Missing GOOGLE_PLAY_SERVICE_ACCOUNT_JSON environment variable.");
}
if (!existsSync(AAB_PATH)) {
  throw new Error(`AAB not found at ${AAB_PATH}. Run the bundleRelease task first.`);
}

function cleanText(value) {
  return String(value).replace(/\r\n/g, "\n").replace(/\r/g, "\n").trim();
}

function readListingFile(name) {
  const file = path.join(LISTING_DIR, name);
  if (!existsSync(file)) {
    throw new Error(`Listing file not found: ${file}`);
  }
  return cleanText(readFileSync(file, "utf8"));
}

function assertLength(label, value, max) {
  if (!value) {
    throw new Error(`${label} is empty (file: ${LISTING_DIR}).`);
  }
  if (value.length > max) {
    throw new Error(`${label} is ${value.length} chars, exceeds max ${max}.`);
  }
  console.log(`${label}: ${value.length}/${max} chars OK`);
}

const title = readListingFile("app_title.txt");
const shortDescription = readListingFile("short_description.txt");
const fullDescription = readListingFile("full_description.txt");
assertLength("title", title, LIMITS.title);
assertLength("shortDescription", shortDescription, LIMITS.shortDescription);
assertLength("fullDescription", fullDescription, LIMITS.fullDescription);

const auth = new google.auth.GoogleAuth({
  credentials: JSON.parse(SERVICE_ACCOUNT_JSON),
  scopes: ["https://www.googleapis.com/auth/androidpublisher"],
});
const client = await auth.getClient();
google.options({ auth: client });
const pub = google.androidpublisher("v3");

const edit = await pub.edits.insert({ packageName: PACKAGE_NAME });
const editId = edit.data.id;
console.log(`Created Play edit ${editId}`);

try {
  console.log(`Uploading AAB: ${AAB_PATH}`);
  const bundle = await pub.edits.bundles.upload({
    packageName: PACKAGE_NAME,
    editId,
    media: { mimeType: "application/octet-stream", body: createReadStream(AAB_PATH) },
  });
  const versionCode = String(bundle.data.versionCode);
  console.log(`Uploaded bundle versionCode=${versionCode}`);

  if (existsSync(MAPPING_PATH)) {
    try {
      await pub.edits.deobfuscationfiles.upload({
        packageName: PACKAGE_NAME,
        editId,
        apkVersionCode: versionCode,
        deobfuscationFileType: "proguard",
        media: { mimeType: "application/octet-stream", body: createReadStream(MAPPING_PATH) },
      });
      console.log("Uploaded ProGuard mapping file.");
    } catch (err) {
      console.warn(`Skipping mapping upload: ${err.message}`);
    }
  } else {
    console.log(`No mapping file at ${MAPPING_PATH}; skipping.`);
  }

  let existingVideo;
  try {
    const current = await pub.edits.listings.get({
      packageName: PACKAGE_NAME,
      editId,
      language: LISTING_LANGUAGE,
    });
    existingVideo = current.data?.video;
  } catch (err) {
    console.warn(`Could not read existing ${LISTING_LANGUAGE} listing (continuing): ${err.message}`);
  }

  const listingBody = {
    language: LISTING_LANGUAGE,
    title,
    fullDescription,
    shortDescription,
  };
  if (existingVideo) {
    listingBody.video = existingVideo;
  }
  await pub.edits.listings.update({
    packageName: PACKAGE_NAME,
    editId,
    language: LISTING_LANGUAGE,
    requestBody: listingBody,
  });
  console.log(`Updated ${LISTING_LANGUAGE} store listing.`);

  let releaseNotes = "";
  if (existsSync(RELEASE_NOTES_PATH)) {
    releaseNotes = cleanText(readFileSync(RELEASE_NOTES_PATH, "utf8")).slice(0, 500);
  }
  const release = {
    versionCodes: [versionCode],
    status: RELEASE_STATUS,
  };
  if (releaseNotes) {
    release.releaseNotes = [{ language: LISTING_LANGUAGE, text: releaseNotes }];
  }
  await pub.edits.tracks.update({
    packageName: PACKAGE_NAME,
    editId,
    track: TRACK,
    requestBody: { track: TRACK, releases: [release] },
  });
  console.log(`Assigned versionCode=${versionCode} to track '${TRACK}' (status=${RELEASE_STATUS}).`);

  const commit = (changesNotSentForReview) =>
    pub.edits.commit({ packageName: PACKAGE_NAME, editId, changesNotSentForReview });

  if (PREFER_AUTO_REVIEW) {
    try {
      await commit(false);
      console.log("Edit committed and sent for review automatically.");
      console.log(JSON.stringify({ committed: true, sentForReview: true }));
    } catch (err) {
      const message =
        err?.response?.data?.error?.message || err?.message || "";
      if (/changes? cannot be sent for review automatically/i.test(message)) {
        console.warn("Play refused auto-review (active policy state). Retrying commit with changesNotSentForReview=true.");
        await commit(true);
        console.log("Edit committed WITHOUT auto-review. Submit for review from Play Console.");
        console.log(JSON.stringify({ committed: true, sentForReview: false, fellBack: true }));
      } else {
        throw err;
      }
    }
  } else {
    await commit(true);
    console.log("Edit committed WITHOUT auto-review (per CHANGES_NOT_SENT_FOR_REVIEW=true).");
    console.log(JSON.stringify({ committed: true, sentForReview: false }));
  }
} catch (err) {
  console.error(`Publish failed; edit ${editId} was not committed.`);
  throw err;
}
