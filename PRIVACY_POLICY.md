# ScamRadar Privacy Policy

**Last updated: May 29, 2026 (rev. 4)**

---

## Introduction

ScamRadar ("we," "our," or "the App") is committed to protecting your privacy. This Privacy Policy explains how ScamRadar handles your information when you use our Android application.

Our fundamental promise: **your messages never leave your phone.** Community reports are fully anonymized before submission — no personal data or full message content ever leaves your device.

---

## Core Privacy Principles

1. **100% On-Device Processing.** All scam detection and analysis is performed locally on your device. Your text messages, emails, voicemails, and screenshots are never transmitted to any external server for processing.

2. **No Account Required.** ScamRadar does not require you to create an account, provide an email address, or submit any personal information to use the core scanning features.

3. **No Cloud Backend.** ScamRadar operates without a backend server. There is no server that receives, stores, or processes your message content.

---

## Information We Collect

### What We DO NOT Collect

- Message content (texts, emails, voicemails, or screenshots you scan)
- Personal identification information (name, email, phone number)
- Contact list or address book data
- SMS messages or call logs
- Location data
- Full message content in community reports (only sanitized, redacted summaries)

### What We DO Collect (Anonymous Analytics)

ScamRadar uses **Firebase Analytics** to collect anonymous, aggregated app usage data to help us improve the app:

- **Scan events:** When a scan is started or completed (verdict type only — no message content)
- **Feature usage:** Which features are used (Library, History, Share Card)
- **Crash reports:** Via Firebase Crashlytics (stack traces, device model, OS version — no message content)
- **Performance traces:** Via Firebase Performance Monitoring (app start time, screen render times, classifier latency by tier — no message content)
- **Device info:** Device model, OS version, app version (standard Firebase collection)

All analytics data is anonymous and cannot be linked to you personally. We never log, transmit, or store the content of any message you scan.

---

## Community Scam Reports

ScamRadar allows users to anonymously report scams to a community feed:

1. **Anonymous authentication.** Community features use Firebase Anonymous Auth to generate a random, non-identifying user ID. No email, phone number, or personal information is associated with this ID.

2. **Client-side sanitization.** Before any report is submitted, the app strips all personal information, full message content, and identifying details. Only a sanitized scam type and redacted summary are sent.

3. **Firebase Firestore.** Anonymous reports are stored in Firestore, a serverless database. Reports contain no personal data and cannot be traced back to you.

4. **Reporting is optional.** You can use all scanning features without ever submitting a community report.

---

## Family Sync

ScamRadar offers optional family protection features:

- **Family groups:** You can create or join a family group using a shareable code or QR code. Family group membership is stored anonymously in Firebase Firestore.
- **Scan sharing:** When enabled, scam verdicts can be shared with your family group. Only the verdict type (e.g., "Likely Scam") and a redacted summary are shared — never the full original message.
- **Care Mode:** An optional simplified interface for elderly users. Care Mode settings are stored locally on the device. Auto-share to family is opt-in and can be disabled at any time.
- **Camera usage:** Camera is used solely for scanning QR codes to join a family group. No photos or video are recorded or stored.
- **Family data:** Family group data in Firestore contains only anonymous IDs and redacted verdict summaries — no personal information, no full messages.

### Advertising Data

ScamRadar uses **Google AdMob** to display advertisements. AdMob may collect device identifiers and usage data to serve personalized or non-personalized ads. This is governed by [Google's Privacy Policy](https://policies.google.com/privacy).

We display three ad formats:

- **Banner ad** — anchored to the bottom of the app and visible on browse screens (Library, History, Settings).
- **Interstitial ad** — a full-screen ad shown after every 3rd scan result is viewed (never before or during a scan).
- **Native ad** — an in-feed ad styled to match the Scam Library grid layout.

You may see a consent prompt regarding ad personalization as required by applicable law (e.g., GDPR, CCPA).

---

## Data Storage

### Local Data Stored On Your Device

- **Scan history:** Your last 50 scan results (verdict + redacted metadata) are stored locally using Room database. This data never leaves your device.
- **AI model file:** The Gemma 4 model (~3.1 GB) is downloaded to your device's internal storage during onboarding. It is stored in the app's private directory and is deleted when you uninstall the app.
- **User preferences:** Theme preference (light/dark), onboarding completion status, Care Mode settings, family group membership, streak data, and similar settings stored via DataStore.
- **Achievement data:** Trust score, quiz streaks, and achievement badges are stored locally and never synced to any server.

You can delete your scan history at any time from within the app (History screen → Clear All) or by clearing app data in Android Settings.

### Data We Never Store

- The full text content of scanned messages
- Audio recordings or voicemail files
- Screenshots or images you scan
- Any data on external servers

---

## Permissions

ScamRadar requests the following permissions:

| Permission | Purpose | When Requested |
|-----------|---------|----------------|
| `INTERNET` | AdMob ads, Firebase Analytics, model download, community reports | Automatic at install |
| `ACCESS_NETWORK_STATE` | Check connectivity for model download | Automatic at install |
| `READ_MEDIA_IMAGES` | Screenshot OCR scanning | Only when you use the screenshot scan feature |
| `RECORD_AUDIO` | Voicemail recording | Only when you use the voicemail scan feature |
| `READ_MEDIA_AUDIO` | Voicemail file import | Only when you import an audio file |
| `CAMERA` | QR code scanning to join a family group | Only when you tap "Scan QR" in Family Join |
| `POST_NOTIFICATIONS` | Model download progress | Only during model download |
| `FOREGROUND_SERVICE` | Background model download | Only during model download |

**We never request:** SMS access, contacts, location, or call logs.

All runtime permissions are requested in-context with a plain-language explanation of why the permission is needed.

---

## Third-Party Services

### Google AdMob
- Serves advertisements within the app
- Ad formats used: banner, interstitial (after every 3rd scan), and native in-feed ads
- May collect device advertising identifiers
- Privacy policy: [https://policies.google.com/privacy](https://policies.google.com/privacy)

### Firebase Analytics, Crashlytics & Performance Monitoring
- Collects anonymous app usage statistics
- Reports app crashes to help us fix bugs
- Measures app performance and stability (start time, render time, classifier latency) to help us improve responsiveness — no message content is included in performance traces
- Privacy policy: [https://firebase.google.com/policies/analytics](https://firebase.google.com/policies/analytics)

### Firebase Firestore & Anonymous Authentication
- Stores anonymous community scam reports and family group metadata
- Uses Firebase Anonymous Auth for community features — generates a random, non-identifying user ID with no personal information
- All data is sanitized client-side before writing to Firestore (no full messages, no personal data)
- Privacy policy: [https://firebase.google.com/policies/firestore](https://firebase.google.com/policies/firestore)

### Firebase App Check
- Uses Google Play Integrity to verify that requests come from a genuine, unmodified copy of the app
- Protects community report endpoints from abuse
- No personal data is transmitted through App Check

### Google LiteRT / Gemma 4
- On-device AI model running entirely locally
- No data is sent to Google or any server
- Model is downloaded from HuggingFace once during onboarding

### Android WebKit / Safe Browsing
- Used for URL scanning feature: a hardened off-screen WebView captures page content for analysis
- Google Safe Browsing checks URLs against known threat lists before any page is loaded
- Page content is processed entirely on-device via OCR and never uploaded

---

## Children's Privacy

ScamRadar is a general-audience utility app. We do not knowingly collect personal information from children under 13. Since we do not collect personal information from any user, our services are inherently COPPA-compliant.

---

## Your Rights

Depending on your jurisdiction, you may have the following rights:

- **Right to access:** Request a copy of any data we hold about you (minimal — only anonymous analytics)
- **Right to deletion:** Delete all local data by clearing the app's data in Android Settings or using the in-app "Clear History" function
- **Right to opt out of personalized ads:** Adjust through the Google Ads Settings page or the consent prompt in the app
- **Right to data portability:** Export your scan history from the app's History screen

To exercise any of these rights, contact us at the email address below.

---

## Data Retention

- **Local scan history:** Stored until you delete it manually or uninstall the app
- **Anonymous analytics:** Retained by Firebase for approximately 60 days, then automatically aggregated/deleted per Google's standard retention policies
- **Ad data:** Governed by Google AdMob's data retention policies

---

## International Users

ScamRadar is available worldwide. All processing occurs on your device, so your data does not cross international borders. Anonymous analytics data is processed by Firebase under Google's standard data processing terms, which comply with GDPR and other applicable regulations.

---

## Changes to This Policy

We may update this Privacy Policy from time to time. We will notify you of any material changes by:

- Updating the "Last updated" date at the top of this page
- Posting a notice within the app for significant changes

Continued use of the app after changes constitutes acceptance of the updated policy.

---

## Contact

If you have questions or concerns about this Privacy Policy or ScamRadar's data practices, please contact us:

- **GitHub:** [https://github.com/chartmann1590/ScamRadar/issues](https://github.com/chartmann1590/ScamRadar/issues)

We will respond to all privacy-related inquiries within 30 days.

---

## California Consumer Privacy Act (CCPA) Disclosure

ScamRadar does not "sell" personal information as defined by the CCPA. We do not share personal information for cross-context behavioral advertising. The anonymous analytics we collect fall under the CCPA's "deidentified information" exemption.

---

## Google Play Data Safety Disclosure

As declared in our Google Play Data Safety form:

- **Data collected:** Anonymous app interaction events (scan started/completed, feature usage), crash reports, and anonymous app performance traces (no message content). Anonymous community scam reports (sanitized, no personal data). Anonymous family group metadata (random IDs, redacted verdict summaries).
- **Data shared:** None
- **Data encrypted in transit:** Yes (Firebase default)
- **User can request deletion:** Yes (in-app or by clearing app data)
- **Message content:** Not collected, not shared, not stored on any server

---

*This privacy policy is effective as of May 29, 2026 (rev. 4).*
