# ScamRadar AI — Build Plan

> Single source of truth for the build agent. Plan-only document; no code yet.
> Owner: Charles. Date frozen: 2026-05-28. Platform: Android.

---

## 1. Context — Why This App

AI-generated scams are the fastest-growing crime category of 2026. The numbers are the entire thesis for this app:

- AI-driven scams **surged 1,210% in 2025**, vs. 195% for traditional fraud.
- Deepfake voice phishing (vishing) **surged 1,633% in Q1 2025** alone.
- Projected losses: **$40 billion by 2027**.
- Human ability to detect high-quality AI voice deepfakes is **below 30% accuracy** — meaning users *cannot* defend themselves unaided.
- Family-emergency synthetic voice scams (fake "your kid is in jail" calls) up **45% in 2025**.

The existing market is owned by big-security-suite players — McAfee Scam Detector, Norton Genie, Trend Micro ScamCheck, Bitdefender Scamio. Independent testing (KSLTV, 2026) found **none of them were perfect**; only Norton + McAfee reliably caught complex scams, and all four require accounts, are subscription-gated or upsell-heavy, and are **cloud-based** (you upload your private texts to their servers).

That's the gap. There is no free, no-account, **on-device-only**, AI-scam-specialist on the Play Store. With Gemini Nano running locally on modern Android (Pixel 9+, Galaxy Z Fold7, Xiaomi 15, etc.), we can build it with **zero server cost** and a uniquely defensible privacy story.

**Intended outcome:** a popular, useful, AdMob-monetized Android utility that ranks for high-volume scam-related searches, retains users via genuine repeat-use behavior (people scan suspicious messages multiple times per week), and generates ad revenue with healthy unit economics from day one because there is no backend to pay for.

---

## 2. App Identity

| Field | Value |
|---|---|
| **Brand name** | **ScamRadar** |
| **Play Store title** | `ScamRadar: AI Scam & Phishing Detector` (50/50 chars) |
| **Tagline** | *"Paste it. Check it. Don't get scammed."* |
| **Package name** | `com.scamradar.app` |
| **One-line positioning** | The free, private, on-device AI that tells you in 3 seconds whether that text, email, or voicemail is a scam — and exactly why. |

**Why this name:** "Scam" is the highest-intent search keyword in the category; per 2026 Google Play ASO research, the title is the single most heavily weighted ranking signal, and Brand-Name: Primary-Keyword is the proven pattern. "Radar" connotes *detection* (not blocking, which carries permission baggage), is short, brandable, memorable, and leaves room to expand into "VoiceRadar" / "EmailRadar" sub-features later.

---

## 3. Unique Differentiation

Five pillars, in priority order:

1. **On-device only.** Your suspicious texts never leave the phone. Marketing line: *"A security app that doesn't read your messages on a server is the only kind worth trusting."* Direct counter to Bitdefender Scamio / Norton Genie which both require cloud round-trips.
2. **AI-scam specialist.** Tuned specifically for AI-generated content patterns: deepfake voice transcripts, LLM-written romance scams, AI-personalized phishing. Competitors are general-purpose and were built before the 2025 AI-scam explosion.
3. **Free + no account.** Open the app, paste, get an answer. No sign-up wall, no trial countdown. Funded by AdMob.
4. **Explanation, not just verdict.** Every result shows *which exact phrases* triggered the warning ("Says you've won a lottery you didn't enter," "Creates artificial urgency: 'within 24 hours'," "Cloned-voice telltale: unnatural pause at 0:04"). This is what makes screenshots shareable → free viral marketing.
5. **Works on a "weird text" in 3 seconds.** No setup, no learning curve, no permissions beyond clipboard. The "Shazam moment" is the entire UX.

---

## 4. Feature Set

### MVP (v1.0 — first release)

Ship these and nothing else. The whole point of an indie launch is to validate the core loop before adding surface area.

1. **Text Scan** — Paste or share-target a message → on-device Gemini Nano classifies it (Safe / Suspicious / Likely Scam) and returns a structured explanation: scam *type* (phishing, romance, IRS impersonation, crypto, family-emergency, package-delivery, job-offer, etc.), the *specific red-flag phrases* it found (highlighted in the original text), and a *what to do next* recommendation.
2. **Voicemail Scan** — User records or imports an audio file → on-device ML Kit Speech Recognition transcribes → same classifier pipeline runs on the transcript → same result format. Critical: flags voice-cloning indicators in the transcript (urgency + family member name + payment method).
3. **Email Screenshot Scan** — User shares a screenshot → on-device ML Kit OCR extracts text → classifier runs. Solves the "I can't paste this email properly" pain point that KSL's test exposed as a competitor weakness.
4. **Scam Library** — A static, curated library of the 12 most common 2026 scam patterns with real example screenshots, so users learn to spot them unaided. Great for SEO/sharing and gives non-suspicious sessions a reason to open the app.
5. **History (local-only)** — Last 50 scans stored on device, with verdict. Deletable. Never synced.
6. **Share Result Card** — One-tap export of a verdict card (verdict + redacted message + reason) as an image, designed to be screenshot-shared in family group chats. This is our viral loop.

### Explicitly NOT in MVP

- Real-time call blocking (requires `READ_PHONE_STATE` + carrier integration; defer to v2)
- SMS auto-scanning (`READ_SMS` is restricted by Play policy and kills review approval odds)
- Accounts, cloud sync, multi-device
- iOS

### v1.1 (4–6 weeks post-launch, contingent on >5k installs)

- "Trusted contact verification" — paste a number, app tells you if it matches the real bank/IRS/USPS contact numbers (static curated list)
- Daily "Scam of the Day" notification (free organic re-engagement)
- Rewarded ad to unlock unlimited scans/day above a 10/day soft cap (only introduce a cap if usage data justifies it)

---

## 5. Technical Architecture

| Layer | Choice | Why |
|---|---|---|
| Language | **Kotlin** | Standard for modern Android; first-class on Play |
| UI | **Jetpack Compose** + Material 3 | Fastest iteration, matches Stitch output, theming for light/dark |
| Min SDK | **API 26 (Android 8.0)** | LiteRT-LM Android baseline |
| Target SDK | **API 35 (Android 15)** | Play requirement as of 2026 |
| On-device LLM | **LiteRT-LM + Gemma 4 E2B-it (int4)** | Open-weights, free, fully offline, runs CPU/GPU. Dependency: `com.google.ai.edge.litertlm:litertlm-android:latest.release`. Model: `gemma-4-E2B-it-litertlm` from HuggingFace `litert-community`, ~3.1 GB int4, **downloaded at onboarding** — not bundled in the APK. |
| OCR | **ML Kit Text Recognition v2** | Free, on-device, no API key, ships in APK |
| Speech-to-text | **Android `SpeechRecognizer` with `onDevice=true`** (API 31+) → fallback `RECOGNIZER_LANGUAGE_MODEL_FREE_FORM` | Free, on-device on Android 12+, no model download, no upload |
| Ads | **Google AdMob** SDK | Banner + interstitial + rewarded interstitial |
| Storage | **Room** (history) + **DataStore** (prefs) | Standard, no backend |
| Backend | **None** | This is the moat |
| Analytics | **Firebase Analytics** (anonymous events only) | Free, gives us retention/funnel numbers |
| Crash reporting | **Firebase Crashlytics** | Free |

### Device support strategy

LiteRT-LM runs on any Android 8.0+ device, but Gemma 4 E2B-it (int4) realistically needs **≥4 GB RAM** and **≥4 GB free storage** to install and run comfortably. Strategy:

1. **At onboarding**, run a device capability check: RAM (`ActivityManager.MemoryInfo.totalMem`), free storage (`StatFs`), CPU features. Classify the device as **Full**, **Lite-recommended**, or **Lite-only**.
2. **Full devices** → prompt user to download the model (see §5a Onboarding flow). Once downloaded, all scans use Gemma 4 via LiteRT-LM.
3. **Lite-recommended / Lite-only devices** → default to **ScamRadar Lite**: a deterministic heuristic classifier (regex + keyword list + URL-reputation checks against a bundled list of ~5,000 known-scam patterns) that runs locally with no AI and no model download. Less accurate but instant, ~0 storage cost, and keeps the "no cloud" promise. User can still attempt the full download from Settings if they want.
4. **Until the model finishes downloading**, every device runs in Lite mode — the app is *immediately useful* with zero wait. The AI upgrade happens silently in the background and switches the active classifier when ready.
5. **Do NOT** add a cloud fallback in v1. Adding a server breaks our entire positioning and adds cost.

### Prompt design (Gemma 4)

Single structured prompt, JSON-out, called from a `ScamClassifier` class. Sketch:

```
You are a scam detection expert. Analyze the following message and return ONLY JSON matching this schema:
{ "verdict": "SAFE" | "SUSPICIOUS" | "LIKELY_SCAM",
  "confidence": 0.0-1.0,
  "scam_type": "PHISHING" | "ROMANCE" | "IRS_IMPERSONATION" | "CRYPTO" | "FAMILY_EMERGENCY" | "PACKAGE_DELIVERY" | "JOB_OFFER" | "TECH_SUPPORT" | "OTHER" | "NONE",
  "red_flags": [ { "phrase": "...", "reason": "..." } ],
  "ai_generated_indicators": [ "..." ],
  "recommended_action": "..." }

Message:
"""{userMessage}"""
```

Cap output tokens ~512 (`EngineConfig.maxNumTokens`). Parse defensively — small models can produce malformed JSON; have a regex-based fallback parser. Run on **CPU backend** for text (faster cold start, fewer compatibility issues than GPU/NNAPI in beta).

---

## 5a. Onboarding & Model Download Flow

The model is **not** bundled in the APK (would blow past the 200 MB Play Store cap and 3 GB Dynamic Delivery cap). It is downloaded on first run.

### Onboarding screen sequence

1. **Welcome** — Brand intro, tagline, one-line value prop. Single "Get started" button.
2. **Privacy promise** — Big visual: "Your messages never leave your phone." Bullets: no account, no cloud, no SMS auto-scanning. Continue.
3. **How it works** — 3-step illustration: Paste → AI checks on your phone → Verdict + why. Continue.
4. **Device check + model download offer** — App shows the device class (Full / Lite-recommended). Two paths:
   - **"Download AI now (~3.1 GB, Wi-Fi recommended)"** → primary CTA on Full devices, secondary on Lite-recommended.
   - **"Start with Lite mode"** → primary on Lite-recommended, secondary on Full. Lets the user use the app *immediately* with the heuristic classifier; they can enable AI anytime from Settings.
5. **(If download chosen) Downloading…** — Progress bar with: % complete, MB downloaded / total, estimated time remaining, "Pause" and "Continue in background" buttons. Wi-Fi-only toggle (default on). User can leave this screen — download continues with a foreground service + persistent notification (required by Play policy for long downloads).
6. **Ready to scan** — On completion, the app routes them to the home screen with a one-time toast: "AI detection active. Welcome to ScamRadar."

### Download implementation requirements

- **Source URL**: HuggingFace `https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm` (verify exact filename at build time; keep URL in a remote-config-style JSON on GitHub Pages so we can hot-swap if HuggingFace moves the model — no app update required).
- **Mechanism**: `DownloadManager` (system-managed, resumable, survives reboot) or `WorkManager` + foreground service.
- **Storage location**: `context.filesDir/models/gemma-4-E2B-it.litertlm`. Internal storage, not external — private to the app, no extra permissions, auto-cleaned on uninstall.
- **Integrity check**: SHA-256 hash verification against a value pinned in `BuildConfig`. If mismatch → delete + retry once → fall back to Lite mode with an in-app notice.
- **Resumability**: must survive process death and network drops. `DownloadManager` handles this for free.
- **Wi-Fi default**: ON. Show explicit "Use mobile data" toggle with a "this is 3+ GB" warning.
- **Pause/cancel**: user-controllable from the Downloading screen and from the persistent notification.
- **Settings → Storage**: show model size, last verified hash, "Delete model" button (reverts to Lite), "Re-download" button.

### Why download instead of bundle

- Play APK size limit (200 MB without Dynamic Delivery, 3 GB hard cap with) makes a 3 GB bundle impractical and tanks install conversion.
- Download-on-demand lets users on small-storage devices opt out and still get value via Lite mode.
- Model can be updated (Gemma 4.x patch releases) without an app update.

---

## 6. Build Phases (for the build agent)

Each phase ends in a runnable APK. Don't move on until the prior phase runs.

| # | Phase | Deliverable |
|---|---|---|
| 1 | **Project skeleton** | New Android Studio project (Kotlin, Compose, min SDK 26), package `com.scamradar.app`. Empty single-activity app launches to a "Hello" screen. |
| 2 | **Theme + nav shell** | Material 3 theme matching the palette in §7. Bottom nav (Scan / Library / History / Settings) with empty Composables. Light + dark theme work. |
| 3 | **Text-scan happy path** | Paste box → "Analyze" button → calls a `ScamClassifier` interface with a **stub** implementation (returns hard-coded `LIKELY_SCAM` after 1s delay) → result screen renders verdict + red flags + recommendation. Verify UI/UX end-to-end before wiring real AI. |
| 3a | **Lite classifier (heuristic)** | `LiteClassifier` implementing `ScamClassifier`: regex + keyword list + URL reputation against a bundled JSON of ~5,000 known-scam patterns. This is the default classifier — works on every device, ships in APK, makes the app immediately useful before the model downloads. |
| 4 | **Onboarding + model download** | Welcome → Privacy → How-it-works → Device check → Download flow per §5a. `ModelDownloader` using `DownloadManager` + foreground service + SHA-256 verification + Wi-Fi-default + resumable. Settings → Storage screen for delete/re-download. |
| 4a | **LiteRT-LM Gemma 4 wiring** | `GemmaClassifier` implementing `ScamClassifier`, using `litertlm-android` SDK with the prompt in §5. `EngineConfig(modelPath, backend=CPU, maxNumTokens=512)`. `ClassifierRouter` selects `GemmaClassifier` if the model file exists and hash matches, else `LiteClassifier`. |
| 5 | **OCR scan path** | "Scan email screenshot" entry point → ML Kit Text Recognition → feed extracted text into the same `ScamClassifier`. |
| 6 | **Voicemail scan path** | Audio picker + record button → ML Kit GenAI Speech Recognition → transcript → classifier. |
| 7 | **History + share card** | Room DB for last 50 scans. Share-card composable rendered to bitmap → system share sheet. |
| 8 | **Scam Library** | Static JSON of 12 scam patterns with example screenshots (asset folder). Renders as a scrollable gallery. |
| 9 | **AdMob integration** | Banner on Library + History screens. Interstitial after every 3rd scan completion (not before — never delay the verdict). Rewarded interstitial reserved for v1.1. Test ad unit IDs only until launch. |
| 10 | **Firebase Analytics + Crashlytics** | Events: `scan_started`, `scan_completed{verdict,scam_type,classifier_tier}`, `share_card_exported`, `library_viewed`. **No PII**, no message contents ever logged. |
| 11 | **Polish + Play Console** | Icon, feature graphic, screenshots (see §9), privacy policy page (hosted free on GitHub Pages), data safety form, internal testing track. |
| 12 | **Closed beta → production** | 20-tester closed track for 1 week, fix top issues, then staged 10% production rollout. |

Estimated indie-dev calendar time: **3–5 weeks** to MVP submission for an experienced Android dev; **5–8 weeks** with AdMob/Play Console first-timer friction.

---

## 7. Design Assets (Google Drive)

**Primary source: Google Drive.** The Stitch-generated layouts, screen mockups, color tokens, and any iterated design files live in:

> **Drive folder:** `<PASTE DRIVE FOLDER URL OR NAME HERE>`
> *(Charles to fill in — or run `/mcp` in Claude Code to authenticate the Google Drive connector and I'll populate this automatically next turn.)*

The build agent **must** pull design assets from this Drive folder before starting Phase 2 (theme + nav shell). It should not regenerate or re-design from scratch.

### What the build agent should look for in the Drive folder

- Stitch screen exports (PNG/SVG) for the 6 core screens listed below
- Color tokens / design system file if present
- App icon and feature graphic source files if present
- Any onboarding-flow screens (welcome, privacy, model download progress)

### Core screens the design covers (reference list for the build agent)

1. **Home / Scan** — Big "Paste & Check" CTA, secondary icons for screenshot/voicemail/manual, "Trending scams this week" row, bottom nav (Scan / Library / History / Settings).
2. **Scanning** — Radar-sweep animation, "Analyzing on your device…", "Your message never leaves your phone" reassurance.
3. **Result: Likely Scam** — Coral verdict header, original message with red-flag phrases highlighted, "Why we think so" reasons card, "What to do" actions, Share + Save buttons.
4. **Result: Looks Safe** — Emerald verdict header, calm reassurance card.
5. **Scam Library** — 2-column grid of scam patterns with filter chips, tap into detail with annotated example screenshots.
6. **History** — Timeline of past scans with verdict pills and filter chips.

Plus the **onboarding flow** per §5a (Welcome → Privacy → How-it-works → Device check → Download progress → Ready).

### Brand visual system (authoritative — must match Drive assets)

- Primary: `#2D4FE0` (trust blue)
- Safe: `#10B981` (emerald)
- Warning: `#F59E0B` (amber)
- Danger: `#EF4444` (coral)
- Light bg / surface: `#F8FAFC` / `#FFFFFF`
- Dark bg / surface: `#0B1220` / `#1A2335`
- Text light: `#0F172A` / `#475569`
- Text dark: `#F1F5F9` / `#94A3B8`
- Corner radius: 20 dp cards, 28 dp primary CTA
- Typography: Inter 400/600/700
- Style: Material 3 expressive, soft shadows, generous whitespace, no gradients except one subtle radial behind the primary CTA
- Icon set: Phosphor-style rounded

If anything in the Drive assets conflicts with the brand visual system above, **Drive wins** — update this section in a follow-up.

### Fallback Stitch prompt (only if Drive folder is empty or assets are missing)

<details>
<summary>Click to expand the original Stitch prompt</summary>

```
Design a mobile Android app called "ScamRadar" — a free, on-device AI tool that
detects scam text messages, emails, and voicemails and explains why each one is
suspicious. The audience is anyone with a smartphone, ages 18–75, including
non-technical users and older adults who are the most-targeted scam demographic.
The brand feel is: calm, confident, trustworthy — the opposite of a panicky
"VIRUS DETECTED!" antivirus aesthetic. It should feel like a smart friend who
calmly tells you "this one's fake, here's how I know."

Visual system:
- Primary: deep trust blue #2D4FE0
- Accent / safe: emerald #10B981
- Warning: amber #F59E0B
- Danger: coral #EF4444
- Background light: #F8FAFC, surface card #FFFFFF
- Background dark: #0B1220, surface card #1A2335
- Text primary light: #0F172A, secondary #475569
- Text primary dark: #F1F5F9, secondary #94A3B8
- Corner radius: 20dp on cards, 28dp on the main scan button
- Typography: Inter, weights 400/600/700
- Iconography: rounded, friendly (Phosphor-style), never alarm-clock-red
- Style: Material 3 expressive, soft shadows, generous whitespace, no gradients
  except one subtle radial behind the primary CTA

Design these 6 screens, in both light and dark mode, plus the onboarding flow
(Welcome, Privacy promise, How it works, Device check, Download progress, Ready):

SCREEN 1 — Home / Scan
SCREEN 2 — Scanning (loading state)
SCREEN 3 — Result: LIKELY SCAM
SCREEN 4 — Result: SAFE
SCREEN 5 — Scam Library
SCREEN 6 — History

Layout: Android, 360dp width, 8dp grid, Material 3 expressive. Bottom nav
height 80dp, safe-area aware.
```

</details>

---

## 8. AdMob Monetization Strategy

Based on 2026 benchmarks: utility apps see ~$7.40 US eCPM / ~$2.10 global. Tier-1 countries (US/UK/CA/AU) generate 3–5× emerging-market rates. Per Google AdMob's 2026 guidance, **timing is everything for utility apps** — poor placement drops D7 retention 10–25%.

### Ad formats and placement

| Format | Placement | Why |
|---|---|---|
| **Banner (adaptive)** | Bottom of Library, History, Settings screens | Passive screens where users browse; banners convert without breaking task flow |
| **Interstitial** | After every **3rd** scan result is fully viewed AND the user taps "Done" / back | Logical task-completion transition (Google's own best-practice trigger). Never before the verdict — that's how you die. |
| **Native ad** | One slot inside the Scam Library grid (every 8th tile) | Browsing context, ad format Google specifically recommends for utility apps |
| **Rewarded interstitial** | *v1.1 only*, unlock "premium scam patterns" detail screens | Opt-in, user-initiated, highest eCPM format |

**Never**:
- Show ads before or during a scan
- Show ads on the result screen of a `LIKELY_SCAM` (it undermines trust at the worst moment)
- Use full-screen ads on cold launch

### Revenue model (rough sanity check)

Assume 10k MAU, 60% US/Tier-1, average 4 scans/week = ~170k scans/month. With interstitial after every 3rd scan + banners on browse screens, expect ~250k–400k impressions/month at blended ~$4 eCPM → **$1k–1.6k MRR at 10k MAU**. The unit economics work because there is *no* server cost. Scale to 100k MAU = $10k–16k MRR.

These are estimates, not promises. Validate with real data after 30 days.

### AdMob setup checklist

1. Create AdMob account, link to Google Play Console.
2. Create 4 ad units (banner, interstitial, native, rewarded-interstitial).
3. Use **test ad unit IDs** during development (Google publishes these — never use live units in dev or you'll get banned).
4. Implement Google's UMP SDK for **GDPR + iOS-style ATT consent prompt** (required even for Android in EU).
5. Configure mediation only after 30 days of solo-AdMob baseline data.

---

## 9. Google Play Store Listing

All copy is final-ready; the build agent should drop these into Play Console verbatim.

### App title (50 char max)

```
ScamRadar: AI Scam & Phishing Detector
```

### Short description (80 char max)

```
Paste any text, email, or voicemail. AI tells you if it's a scam, on your phone.
```

### Long description (4,000 char max)

```
ScamRadar tells you in 3 seconds whether that suspicious text, email, or
voicemail is a scam — and explains exactly why. Powered by on-device AI.
Nothing you paste ever leaves your phone.

AI-generated scams exploded 1,210% last year. Fake IRS texts, cloned-voice
"emergency" calls from your "grandkid," romance scams written by ChatGPT,
package-delivery phishing that looks exactly like a real USPS notification.
Humans now spot AI deepfakes less than 30% of the time. You need a second
opinion that's faster than you.

WHAT SCAMRADAR DOES

• Paste any suspicious message and get an instant verdict: Safe, Suspicious,
  or Likely Scam — with a confidence score
• See the exact red-flag phrases highlighted in the original message
• Get a plain-English explanation of WHY it's a scam (artificial urgency,
  AI-written pattern, impersonation, fake link, etc.)
• Scan a screenshot of an email — built-in OCR reads the text for you
• Check a voicemail — built-in speech recognition transcribes the audio
  and flags voice-cloning telltales
• Get a recommended action: don't reply, report to 7726, block sender
• Share a verdict card with one tap — perfect for warning family in
  group chats

WHY SCAMRADAR IS DIFFERENT

• 100% ON-DEVICE. Your messages never touch a server. We literally cannot
  read what you paste, because we never receive it. Powered by Google's
  Gemma 4 open model, running locally on your phone via LiteRT.
• NO ACCOUNT REQUIRED. No sign-up, no email, no trial countdown. Open the
  app, paste, get an answer.
• FREE FOREVER for the core scanner. Supported by unobtrusive ads on
  browsing screens — never on a scam verdict.
• AI-SCAM SPECIALIST. Built from the ground up to detect the new wave of
  AI-generated scams (LLM-written phishing, deepfake voice scripts), not
  retrofitted from a 2015 spam filter.

WHO IT'S FOR

• Anyone who gets weird texts
• Adult children helping older parents stay safe online
• Small business owners who get fake invoice emails
• Anyone whose phone is buzzing with USPS / Amazon / IRS / "your bank" texts

A SCAM LIBRARY, BUILT IN

Browse the 12 most common 2026 scam patterns with real annotated examples,
so you learn to spot them on your own. Share any pattern with one tap.

PRIVACY YOU CAN VERIFY

ScamRadar requires zero permissions to scan a pasted message. Optional
permissions for screenshot OCR and voicemail import are explained in plain
language at the moment they're needed. No background scanning of your SMS
or email. No analytics on message content. Ever.

DEVICE REQUIREMENTS

Full AI detection downloads the Gemma 4 model on first run (~3.1 GB, Wi-Fi
recommended) and runs best on devices with 4 GB+ RAM and 4 GB+ free
storage. On smaller devices, or before the download completes, ScamRadar
Lite uses a fast on-device heuristic detector — still 100% private, still
free, no download required.

Don't get scammed. Get ScamRadar.
```

### Keywords / metadata strategy

Primary high-intent terms to land in title + short desc + opening of long desc + screenshot captions:

`scam detector`, `scam checker`, `phishing detector`, `spam text detector`, `AI scam`, `scam call detector`, `text scam check`, `is this a scam`, `scam shield`, `scam protection`

Per 2026 ASO research, the Play algorithm weighs **title > short description > long description**; precision beats density. Refresh the long description every 3–6 weeks to add seasonal scam terms (e.g. "tax refund scam" in spring, "package delivery scam" in November/December).

### Visual assets (briefs for the design agent)

| Asset | Spec | Brief |
|---|---|---|
| App icon | 512×512 PNG | Rounded square in #2D4FE0, white shield silhouette overlaid with a radar-sweep arc. Recognizable at 48dp. |
| Feature graphic | 1024×500 PNG | Left half: phone mockup showing the "LIKELY SCAM" result screen. Right half: tagline "Paste it. Check it. Don't get scammed." in white on #2D4FE0 with the wordmark. |
| Screenshots (8) | 1080×1920 portrait | (1) Hero with tagline overlay, (2) Paste-and-check action shot, (3) "LIKELY SCAM" result with red flags highlighted, (4) "LOOKS SAFE" result, (5) Voicemail scan, (6) Email screenshot scan, (7) Scam Library grid, (8) "Your messages never leave your phone" privacy promise. Each screenshot has a 1-line caption at the top using primary keywords. |
| Promo video (optional, recommended) | 30s landscape | 4 quick scenes: receive sketchy text → paste into ScamRadar → red verdict appears → share to family group chat. |

### Data safety form answers

- Data collected: **anonymous app interaction events only** (Firebase Analytics: scan started/completed verdict, NO message content).
- Data shared: none.
- Data encrypted in transit: yes (Firebase default).
- User can request deletion: yes (settings → clear all local data).
- Critical: declare that message content is **not collected, not shared, not stored on any server**.

### Required policies

- Privacy policy: host a one-page policy on GitHub Pages (free) at `scamradar.github.io/privacy`. Template-able; must match the data safety form exactly.
- Permissions justification: only `INTERNET` (for ads + Crashlytics) at install; runtime prompts for `READ_MEDIA_IMAGES` (screenshot scan) and `RECORD_AUDIO`/`READ_MEDIA_AUDIO` (voicemail) with in-context explanations.

---

## 10. Launch & ASO Strategy

### Pre-launch (1 week before submission)

1. Reserve handles: `@scamradarapp` on TikTok, Instagram, X, Reddit.
2. Build a 1-page landing site (GitHub Pages, free) with email signup → drives an initial "Notify me on launch" cohort.
3. Post in r/Scams, r/AndroidApps, r/assistance with **value-first content** (not promo): "I'm building a free on-device scam detector — what scams have hit you recently?" Collect real examples to seed the Scam Library.

### Launch day (staged rollout at 10%)

1. Submit to **r/AndroidApps** flair "I made this", **Product Hunt**, and **Hacker News** ("Show HN: A free on-device AI scam detector for Android — your texts never leave your phone").
2. TikTok: a 20-second "paste this real scam I got" demo. The shareable-verdict-card feature *is* the TikTok hook.
3. Email the pre-launch signup list.
4. Reach out to 5 Android tech YouTubers with free pre-release access.

### Ongoing ASO (every 3–6 weeks per 2026 Play algorithm cadence)

- Update long description to reflect seasonal scam patterns (tax season, holiday delivery scams, summer travel scams).
- Add 1 new entry to the Scam Library per fortnight — drives "update" engagement signal which Play weighs as a freshness ranking factor.
- A/B test screenshots in Play Console (Listing Experiments) — title #1 vs. variant, screenshot order variants. Conversion rate is the second-largest ranking signal after keywords.
- Monitor competitor keyword movement weekly (free tools: AppBrain, Sensor Tower free tier).

### Viral loop

The **share verdict card** is the only growth feature that matters in v1. Every shared card carries the ScamRadar wordmark + a "Scan yours free" footer. Family-group-chat shares are the most powerful acquisition channel for cross-generational apps.

---

## 11. Verification — How I'll Validate Later

When the build agent reports done, I will check the following against the built artifact:

1. **Brand & metadata**: title, short desc, long desc match §9 exactly; package name = `com.scamradar.app`; min SDK 26 / target SDK 35.
2. **App launches** on a high-end device (≥4 GB RAM, e.g. Pixel 7+ or Galaxy S22+) and on a low-end device (e.g. 3 GB RAM emulator). Onboarding device check correctly routes high-end to download offer and low-end to Lite-default.
2a. **Onboarding model download**: starts, shows progress, survives backgrounding, survives a forced network drop and resumes, completes with SHA-256 verification, model lands at `filesDir/models/gemma-4-E2B-it.litertlm`. "Skip for now" path routes to Lite mode and home screen.
2b. **Classifier routing**: with model present + hash valid → scans use `GemmaClassifier`; with model absent or hash invalid → scans use `LiteClassifier`. Settings → Storage → Delete model reverts to Lite without restart.
3. **Core scan path**: paste a known scam text → verdict screen renders within ~3 seconds (Gemma) or <500 ms (Lite) → result JSON parses → red flags are highlighted in the original text → share card exports.
4. **OCR path**: share-target a screenshot of an email → extracted text drives the same classifier.
5. **Voicemail path**: pick a local audio file → transcript appears → classifier runs.
6. **No network calls** on the scan path (verify via charlesproxy / Android Studio network inspector — only allowed network traffic during a scan is AdMob impression beacons, and even those only fire on completion screens, never on the result screen of a `LIKELY_SCAM`). Model download is allowed only during onboarding / explicit re-download.
7. **AdMob**: test ad unit IDs in debug build; live unit IDs in release; interstitial fires only after 3rd scan, not before. UMP consent prompt appears in EU locales.
8. **Privacy**: Firebase Analytics events contain zero PII and zero message content; Crashlytics scrubbed of message bodies.
9. **Play Console**: Data Safety form filled, privacy policy URL live, all screenshots present, feature graphic uploaded, internal testing track has at least one successful test signup.
10. **Smoke tests**: 20 known-scam example messages → ≥18 flagged as Suspicious or Likely Scam. 20 known-benign messages → ≥18 flagged as Safe. (These will live in `app/src/androidTest/assets/scam_fixtures.json` for the build agent to assemble.)

If any of these fail, I'll send the build agent a fix list with specific files and line numbers.

---

## 12. Open Decisions to Confirm Before Build Starts

The build agent should pause and ask Charles on any of these:

- **AdMob account**: already created? If not, create before phase 9.
- **Google Play Developer account** ($25 one-time): purchased? If not, purchase before phase 11.
- **Firebase project**: name it `scamradar-prod`, free Spark tier is sufficient through 100k MAU.
- **Privacy policy hosting**: confirm `scamradar.github.io/privacy` is acceptable, or supply a custom domain.
- **App icon + screenshots**: build agent should generate via Stitch using §7 and §9 briefs unless Charles wants a designer pass first.

---

## Sources (market research backing this plan)

- [9 Types of Apps That Make the Most Money in 2026 — Adapty](https://adapty.io/blog/what-apps-make-the-most-money/)
- [The 2026 AdMob & Mobile Monetization Playbook — MonetizeMore](https://www.monetizemore.com/blog/admob-monetization/)
- [AdMob eCPM Benchmarks — Playwire](https://www.playwire.com/blog/admob-ecpm-benchmarks-what-publishers-should-expect)
- [48 Profitable App Niches in 2026 — Niches Hunter](https://nicheshunter.app/blog/profitable-app-niches-2026)
- [Untapped & Underserved Micro SaaS Niches for 2026 — Superframeworks](https://superframeworks.com/articles/untapped-underserved-micro-saas-niches)
- [LiteRT for Android — Google AI Edge](https://ai.google.dev/edge/litert/android)
- [Blazing fast on-device GenAI with LiteRT-LM — Google Developers Blog](https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/)
- [Running Gemma 4:E2B on Android: A Minimal Kotlin App — Medium](https://medium.com/@gabi.preda/running-gemma-4-e2b-on-android-a-minimal-kotlin-app-4272609bedc9)
- [gemma-4-E2B-it-litert-lm — HuggingFace](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
- [LLM Inference guide for Android — Google AI Edge / MediaPipe](https://ai.google.dev/edge/mediapipe/solutions/genai/llm_inference/android)
- [Deploy Gemma on mobile devices — Google AI for Developers](https://ai.google.dev/gemma/docs/integrations/mobile)
- [Stitch — Google Labs](https://stitch.withgoogle.com/)
- [Stitch Prompt Guide — Google AI Developers Forum](https://discuss.ai.google.dev/t/stitch-prompt-guide/83844)
- [Play Store keyword research in 2026 — AppTweak](https://www.apptweak.com/en/aso-blog/play-store-keyword-research)
- [Google Play ASO in 2026 — The IOn Project](https://theionproject.com/blog/google-play-aso-guide-2026/)
- [Interstitial ad guidance — Google AdMob Help](https://support.google.com/admob/answer/6066980?hl=en)
- [Rewarded interstitial ads — Google AdMob Developers](https://developers.google.com/admob/android/next-gen/rewarded-interstitial)
- [Deepfake Statistics & Trends 2026 — Keepnet](https://keepnetlabs.com/blog/deepfake-statistics-and-trends)
- [Deepfake Attacks & AI-Generated Phishing: 2026 Statistics — ZeroThreat](https://zerothreat.ai/blog/deepfake-and-ai-phishing-statistics)
- [AI Voice Cloning Fraud Statistics 2026 — SQ Magazine](https://sqmagazine.co.uk/ai-voice-cloning-fraud-statistics/)
- [Internet Scams 2026: $16.6B Crisis & AI Deepfake Threats — Axis Intelligence](https://axis-intelligence.com/internet-scams-2026-crisis-ai-deepfakes/)
- [We tested 4 popular scam detection apps — KSL Investigates](https://ksltv.com/ksl-investigates/scam-detection-apps/872145/)
- [Trend Micro ScamCheck — Google Play](https://play.google.com/store/apps/details?id=com.trendmicro.fraudbuster&hl=en_US)
