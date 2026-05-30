<div align="center">

<img src="play-store/icon/icon_512.png" width="120" alt="ScamRadar icon">

# ScamRadar

### AI Scam & Phishing Detector

**Paste it. Check it. Don't get scammed.**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Download](https://img.shields.io/badge/Download-Latest_Release-blue.svg)](https://github.com/chartmann1590/ScamRadar/releases/latest)

[Download APK](https://github.com/chartmann1590/ScamRadar/releases/latest) · [Download AAB](https://github.com/chartmann1590/ScamRadar/releases/latest) · Google Play coming soon

![ScamRadar feature graphic](play-store/feature-graphic/feature_graphic_1024x500.png)

### Watch the 40-second demo

[![Watch the ScamRadar demo video](docs/assets/promo_poster.png)](https://chartmann1590.github.io/ScamRadar/#watch)

> [Watch the video on the website](https://chartmann1590.github.io/ScamRadar/#watch) · [Download the MP4](play-store/video/scamradar_promo.mp4) · [Captions (.srt)](play-store/video/scamradar_promo.srt)

</div>

---

## What is ScamRadar?

ScamRadar is a **free Android app** that tells you in 3 seconds whether a suspicious text message, email, or voicemail is a scam — and explains **exactly why**.

AI-generated scams surged **1,210% in 2025**. Deepfake voice phishing is up **1,633%**. Humans now detect AI deepfakes at less than **30% accuracy**. You need a second opinion that's faster than you.

### Why ScamRadar is Different

| Feature | ScamRadar | Competitors |
|---------|-----------|-------------|
| **100% On-Device** | Your messages never leave your phone | Cloud-based analysis |
| **No Account Required** | Open, paste, get an answer | Sign-up walls & trials |
| **Free Forever** | Core scanner is always free | Subscription-gated |
| **AI-Scam Specialist** | Built for the new wave of AI scams | Retrofitted spam filters |
| **Explains Why** | Highlights exact red-flag phrases | Binary safe/unsafe only |

---

## Features

### Live Shield — Ambient Protection (NEW)

**Notification Listener Auto-Scan**
ScamRadar reads incoming message previews from Google Messages, WhatsApp, Signal, Telegram, Gmail, Outlook, Instagram, Messenger, Discord, and Zoom — on-device — and surfaces a discreet alert when it catches a scam. Nothing is uploaded. Off by default; you choose which apps it watches and can pause for an hour or a day with one tap.

**Hybrid AI + Lite Classifier**
Stage 1: fast pattern matcher on every notification (<50ms). Stage 2: if the Lite engine isn't confidently safe, the full Gemma 4 AI takes a second look. Most safe notifications never touch the model — battery and RAM stay healthy.

**Smart Clipboard Chip**
When you copy a link, phone number, or money phrase like "Zelle" or "gift card", ScamRadar offers a one-tap "Check this?" heads-up. Foreground-only and opt-in.

**URL Pre-Click Guard**
Register ScamRadar as a link option. Before opening a suspicious URL, ScamRadar runs the classifier and shows a "Safe", "Suspicious", or "Likely scam" verdict — then lets you proceed or cancel.

### Scanning

**Text Scan**
Paste any suspicious message and get an instant verdict: **Safe**, **Suspicious**, or **Likely Scam** — with a confidence score and highlighted red-flag phrases.

**Email Screenshot Scan**
Share a screenshot of a suspicious email. Built-in OCR extracts the text and runs it through the same scam classifier.

**Voicemail Scan**
Import or record a voicemail. On-device speech recognition transcribes the audio and flags voice-cloning indicators.

**URL Scanner**
Enter or paste a suspicious URL. A hardened off-screen WebView captures the full page, OCR extracts the visible text, and the scam classifier runs on it — all on-device, with Google Safe Browsing protection.

**Link Microscope**
Every scanned URL gets a diagnostic card showing the domain anatomy, redirect chain, and risk signals — so you can see *why* a link is dangerous before you ever tap it.

### Recovery & Aftermath (NEW)

**"I've Been Scammed" Recovery Wizard**
Triggered from any Likely Scam result via a single "I already replied / clicked / paid" button. Stepped, scam-type-specific checklist (Phishing, Romance, IRS, Crypto, Family Emergency, Delivery, Job Offer, Tech Support, Lottery, Investment) walks you through freezing cards, changing passwords, contacting your carrier, filing authority reports, and preserving evidence. Resumable across sessions.

**Incident Report PDF**
Export the full recovery checklist as a one-page PDF — verdict, redacted excerpt, red flags, completed steps, your notes. Designed for bank disputes, police reports, and insurance claims. *(Premium)*

**One-Tap Authority Reporting Hub**
Pre-filled deep links to FTC ReportFraud, IC3, BBB Scam Tracker, Action Fraud (UK), ACCC Scamwatch (AU), CAFC (Canada), Cybercrime.gov.in (India), and the 7726 carrier-spam short code — auto-selected for your region and scam type.

**Evidence Locker**
One-tap export of a ZIP bundle: original message, redacted version, classifier verdict JSON, screenshots, and a markdown summary. Useful for bank disputes and law enforcement. *(Premium)*

### Quick Access

**System Share Sheet**
Scan any text from any app without opening ScamRadar. Share text, screenshots, or links directly to ScamRadar via Android's share sheet and get an instant overlay verdict.

**Material You Home Widget**
A one-tap scan widget that adapts to your wallpaper colors via Glance + Material You. Scan from your home screen.

**Quick Settings Tile**
Add ScamRadar to your quick settings pulldown for one-swipe access to scanning.

### Engagement

**Daily Scam Brief (Today Tab)**
A daily feed of trending scam alerts, new patterns, and safety tips — powered by Firebase Remote Config so the content stays fresh without app updates.

**"Spot the Scam" Daily Quiz**
Test your scam-detection skills with a daily interactive quiz. Build a streak and compete with yourself.

**Trust Score & Achievements**
Track your scanning habits with local-only stats: total scans, streaks, scam types encountered, and 15 unlockable achievement badges (First Catch, 7-Day Streak, Family Saver, Shield Activated, Recovery Hero, and more).

**Today Widget (NEW)**
A Material You Glance widget shows today's brief headline plus the daily quiz — tap an answer right from the widget without opening the app.

**Trending Scam Push Alerts (NEW)**
When a scam type spikes in your region (country-level only, never finer), ScamRadar pushes a one-line alert: "FedEx delivery scam trending in your area — here's what to watch for." Throttled to one alert per scam type per week.

**Warn Family & Friends**
After a Likely Scam verdict, a one-tap "Warn family or friends" CTA generates a share card with the verdict and top red flags — perfect for forwarding via SMS or WhatsApp to people who might receive the same scam.

### Community

**Community Scam Reports + Trending Feed**
Report scams anonymously to help others. Browse a trending feed of the latest scams reported by the community — powered by Firestore with client-side sanitization (no personal data or full messages ever leave your device).

### Family Protection

**Family Sync**
Connect with family members using a simple family code or QR code. When someone in your family group scans a scam, everyone gets notified.

**Care Mode — Senior-Friendly UX (EXPANDED)**
One toggle unifies the simpler-layout, larger-text, ad-free, family-auto-share elder UX. When on: 24pt+ body fonts, a single "Paste & Read" home screen, full-screen color-coded verdicts ("STOP", "BE CAREFUL", "LOOKS SAFE"), Text-to-Speech readout of every verdict ("Stop. This looks like a scam. Do not reply."), and a large "Call my family" emergency button wired to the contact you set during setup.

**Trusted Contact Verification — Anti-Deepfake (NEW)**
Solves voice-cloning impersonation. Any pod member can tap "It's really me" → ScamRadar generates a cryptographically signed link (HMAC-SHA256, keys stored in Android Keystore) → forward via SMS/WhatsApp. When the recipient opens it, ScamRadar validates against the pod's verifier roster and shows "✓ Verified from MOM" or rejects the link if tampered. 72-hour expiry.

**Family Weekly Digest (NEW)**
Pod organizers receive a Monday-morning push: "Mom ran 14 scans this week, 2 likely scams caught (USPS, IRS), 1 suspicious link blocked. Streak: 12 days." Aggregated from anonymous pod shares — no message content ever leaves devices. *(Family tier)*

**Remote Setup via QR (NEW)**
Help a relative set up their phone over a phone call. You generate a QR code with their phone name, Senior Mode on, Care Mode on, your number as emergency contact, and the pod join — they scan it, tap Confirm, done. Removes the #1 setup friction.

### Sharing & Learning

**Scam Library**
Browse the 12 most common scam patterns of 2026 with real annotated examples. Learn to spot scams on your own.

**Scan History**
Your last 50 scans, stored locally on your device. Filter by verdict, delete anytime. Never synced anywhere.

**Share Verdict Card**
One-tap export of a verdict card as an image — choose from three card themes (Minimal, Bold Alert, Educational). Perfect for warning family and friends in group chats.

### Accessibility & Internationalization

**Multilingual Support**
ScamRadar is available in English, Spanish, Portuguese, French, and German — with Hindi and Chinese translations ready for follow-up.

**Story Onboarding**
A swipeable, illustrated onboarding flow that walks new users through how ScamRadar works, the privacy promise, and device setup.

---

## How It Works

```
1. Input   →  Paste text, share a screenshot, import a voicemail, or enter a URL
2. Check   →  On-device AI (Gemma 4) analyzes the content privately on your phone
3. Verdict →  Get a clear verdict with highlighted red flags and recommended actions
```

**Your messages never leave your phone.** We literally cannot read what you paste, because we never receive it.

---

## Technical Details

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| On-Device AI | Gemma 4 E2B-it via LiteRT-LM |
| OCR | ML Kit Text Recognition v2 |
| Speech-to-Text | Android SpeechRecognizer (on-device) |
| URL Capture | Android WebKit WebView + Google Safe Browsing |
| QR Codes | ZXing ("ZXing Android Embedded") |
| Home Widget | Glance + glance-material3 (Material You) |
| Storage | Room + DataStore |
| Community Backend | Firebase Firestore (anonymous reports only) |
| Auth | Firebase Anonymous Auth (community features) |
| Analytics | Firebase Analytics (anonymous only) |
| Crash Reporting | Firebase Crashlytics |
| Performance | Firebase Performance Monitoring |
| App Integrity | Firebase App Check (Play Integrity) |
| Ads | Google AdMob |
| Notification Listener | Android `NotificationListenerService` (Play "core functionality: scam detection") |
| Push Messaging | Firebase Cloud Messaging (topic-based; country code only, no PII) |
| Billing | Google Play Billing v7 (Premium / Family tiers) |
| TTS | Android `TextToSpeech` (on-device system voices) |
| Crypto for Verification | HMAC-SHA256 in Android Keystore (Trusted Contact Verification) |
| PDF Export | Android `PdfDocument` (Incident Report PDF) |
| Backend | **None for scanning** — your messages never leave the device |

### ScamRadar Lite

On devices with less than 4 GB RAM, or before the AI model finishes downloading, ScamRadar runs in **Lite mode** — a fast heuristic classifier that uses regex, keyword matching, and URL reputation against 5,000+ known scam patterns. Still 100% private, still free, no download required.

### Two-Stage Classification (Live Shield)

Live Shield uses a hybrid pipeline so the AI doesn't burn battery on safe notifications:

1. **Stage 1 — Lite (every notification, <50ms)** — pattern matcher returns SAFE/SUSPICIOUS/LIKELY_SCAM
2. **Stage 2 — Gemma 4 AI (only if Lite isn't confidently safe)** — runs the full on-device LLM for a second opinion

The verdict stored in History and posted as an alert is the *final* verdict — Gemma's if it ran, Lite's if Gemma was skipped or unavailable. Each entry's `Classifier Tier` badge (LITE / GEMMA) tells you which engine produced it.

### Premium & Family Tiers

A small set of follow-up features unlock with a subscription. The core scanner is always free.

| Feature | Free | Premium | Family |
|---|---|---|---|
| All scanning modes (text, OCR, voice, URL, Live Shield) | ✅ | ✅ | ✅ |
| Recovery checklist + Authority Reporting Hub | ✅ | ✅ | ✅ |
| Incident Report PDF export | — | ✅ | ✅ |
| Evidence Locker ZIP export | — | ✅ | ✅ |
| Unlimited per-app Shield customization | 3 apps | unlimited | unlimited |
| Ad-free | — | ✅ | ✅ |
| Weekly Family Digest | — | — | ✅ |
| Trusted Contact Verification | — | — | ✅ |
| Family pod members covered | up to 8 (no premium) | individual only | all pod members covered by organizer |

---

## Screenshots

Captured live on a Pixel 8 Pro running the actual app.

<div align="center">

| Paste it. Check it. | On-device. Private. | Clear verdict. | What to do next. |
|---------------------|---------------------|----------------|------------------|
| ![Home](play-store/screenshots/phone/phone_01.png) | ![Scanning](play-store/screenshots/phone/phone_02.png) | ![Suspicious](play-store/screenshots/phone/phone_03.png) | ![Detail](play-store/screenshots/phone/phone_04.png) |

| Peace of mind. | Learn the patterns. | Track your scans. | Free. Offline. |
|----------------|---------------------|-------------------|----------------|
| ![Looks Safe](play-store/screenshots/phone/phone_05.png) | ![Library](play-store/screenshots/phone/phone_06.png) | ![History](play-store/screenshots/phone/phone_07.png) | ![Settings](play-store/screenshots/phone/phone_08.png) |

</div>

> Want the unframed device captures? See [`play-store/screenshots/raw/`](play-store/screenshots/raw/). Tablet sizes for the Play Store live alongside in [`tablet-7in/`](play-store/screenshots/tablet-7in/) and [`tablet-10in/`](play-store/screenshots/tablet-10in/).

---

## Privacy

ScamRadar is built with privacy as a core principle:

- **No cloud processing** — all scan analysis happens on your device
- **No account needed** — we don't collect emails or personal info
- **No message logging** — scan content is never sent to any server
- **Local history only** — stored on-device, deletable, never synced
- **Anonymous analytics** — only app interaction events (no message content)
- **Anonymous community reports** — fully sanitized client-side before submission; no personal data or full messages
- **Family data is anonymous** — family groups use random IDs and share only redacted verdict summaries
- **Camera for QR only** — used solely for scanning family QR codes; no photos or video stored

See our full [Privacy Policy](PRIVACY_POLICY.md).

---

## Building from Source

### Requirements

- Android Studio Hedgehog or later
- Android SDK with API 35 (Android 15)
- Min SDK 26 (Android 8.0)
- Kotlin 2.0+

### Setup

```bash
git clone https://github.com/chartmann1590/ScamRadar.git
cd ScamRadar
```

Open the project in Android Studio and run on a device or emulator.

> **Note:** The AI model (Gemma 4 E2B-it, ~3.1 GB) is downloaded on first run, not bundled in the APK. The app works immediately in Lite mode before the download completes.

---

## Project Structure

```
app/src/main/java/com/charles/scamradar/app/
├── ads/                  # AdMob banner, interstitial, native ad loader
├── analytics/            # Firebase Analytics event helpers
├── classifier/           # Scam classification (Gemma, Lite heuristic, Stub)
├── community/            # Community reports, sanitization, anonymous auth
├── data/
│   ├── db/               # Room DB (ScanHistoryDao, ScanHistoryEntity)
│   ├── datastore/        # UserPrefs (DataStore)
│   └── model/            # ScanResult models
├── download/             # Model download service & manager
├── engagement/           # Daily brief, quiz, Today models & repository
├── family/               # Family code generation, sync, QR rendering
├── ocr/                  # ML Kit OCR processing
├── share/                # Share utilities
├── speech/               # On-device speech recognition
├── webcapture/           # URL page capture, Safe Browsing, feature extraction
├── widget/               # Material You Glance home widget
├── quicksettings/        # Quick Settings tile service
└── ui/
    ├── components/       # Shared UI (BottomNavBar, PulsingShieldRings, ads)
    ├── navigation/       # Nav graph, screen routes, deep links
    ├── quickverdict/     # Share sheet overlay activity
    ├── screens/
    │   ├── home/         # Home / scan entry
    │   ├── scanning/     # Scanning animation state
    │   ├── result/       # Verdict, red flags, Link Microscope, share card, family share
    │   ├── urlscan/      # URL scan input + scanning state
    │   ├── library/      # Scam pattern library
    │   ├── history/      # Scan history
    │   ├── today/        # Daily brief + quiz
    │   ├── stats/        # Trust score & achievements
    │   ├── family/       # Family onboarding, create, join, activity
    │   ├── settings/     # App settings (theme, Care Mode, model management)
    │   ├── onboarding/   # Story onboarding + model download
    │   └── help/         # Help & FAQ
    └── theme/            # Material 3 theme, colors, shapes, typography
```

---

## Who is ScamRadar For?

- Anyone who gets weird texts and wants a second opinion
- Adult children helping older parents stay safe online
- Small business owners who receive fake invoice emails
- Anyone whose phone is buzzing with USPS/Amazon/IRS/"your bank" texts

---

## Support ScamRadar

If ScamRadar has helped you or someone you care about, consider buying me a coffee:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy_Me_A_Coffee-Support-orange.svg)](https://www.buymeacoffee.com/charleshartmann)

---

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

**Don't get scammed. Get ScamRadar.**

[Download](https://github.com/chartmann1590/ScamRadar/releases/latest) · [Website](https://chartmann1590.github.io/ScamRadar/) · [Privacy Policy](https://chartmann1590.github.io/ScamRadar/privacy) · [Report a Bug](https://github.com/chartmann1590/ScamRadar/issues) · [Support &#x2615;](https://www.buymeacoffee.com/charleshartmann)

</div>
