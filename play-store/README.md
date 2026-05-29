# ScamRadar — Play Store listing assets

Everything you need to publish ScamRadar to the Google Play Console, all
generated from the actual app running on a Pixel 8 Pro.

## What's in this folder

```
play-store/
├── icon/
│   ├── icon_512.png         ← Upload to Play Console (required)
│   └── icon_1024.png        ← Hi-res master
│
├── feature-graphic/
│   └── feature_graphic_1024x500.png   ← Upload to Play Console (required)
│
├── screenshots/
│   ├── raw/                 ← Untouched ADB captures from the Pixel 8 Pro
│   ├── phone/               ← 8 finished phone screenshots (1242×2208)
│   ├── tablet-7in/          ← 8 finished 7-inch tablet screenshots (1920×1200)
│   └── tablet-10in/         ← 8 finished 10-inch tablet screenshots (2560×1600)
│
├── video/
│   ├── scamradar_promo.mp4               ← Clean version (upload to YouTube)
│   ├── scamradar_promo_captions_burned.mp4 ← With burned-in captions
│   ├── scamradar_promo.srt               ← Caption file for YouTube
│   ├── voice.wav                          ← Concatenated voiceover
│   ├── voice/                             ← Per-line WAV files
│   ├── frames/                            ← Source PNGs for each scene
│   └── script.txt                         ← The voiceover script
│
├── listing/
│   ├── app_title.txt                     ← 27 chars (limit 30)
│   ├── short_description.txt             ← 72 chars (limit 80)
│   ├── full_description.txt              ← 3890 chars (limit 4000)
│   ├── aso_keywords.txt                  ← Keyword research
│   ├── category_and_metadata.txt         ← Category, contact, etc.
│   ├── data_safety.txt                   ← Data Safety form answers
│   └── promo_text.txt                    ← Tweet, Reddit, elevator pitch
│
└── scripts/                              ← Python + PowerShell generators
    ├── make_icon.py
    ├── make_feature_graphic.py
    ├── make_phone_screenshots.py
    ├── make_tablet_screenshots.py
    ├── make_video_frames.py
    ├── generate_voiceover.ps1
    └── build_video.py
```

## Upload checklist

| Slot | File | Notes |
|------|------|-------|
| App icon | `icon/icon_512.png` | 512×512, 32-bit PNG, ≤1 MB |
| Feature graphic | `feature-graphic/feature_graphic_1024x500.png` | 1024×500, JPG or 24-bit PNG |
| Phone screenshots | `screenshots/phone/phone_01..08.png` | 8 portrait images, ≥2 required |
| 7-inch tablet | `screenshots/tablet-7in/tablet7_01..08.png` | Optional but recommended |
| 10-inch tablet | `screenshots/tablet-10in/tablet10_01..08.png` | Optional but recommended |
| Promo video URL | YouTube → upload `video/scamradar_promo.mp4` | Use the `.srt` for captions |
| Title | `listing/app_title.txt` | Paste into "App name" |
| Short description | `listing/short_description.txt` | Paste into "Short description" |
| Full description | `listing/full_description.txt` | Paste into "Full description" |
| Data safety | `listing/data_safety.txt` | Use as a checklist |
| Category | `listing/category_and_metadata.txt` | Tools / Communication |

## How to regenerate

```powershell
# Icon
python play-store/scripts/make_icon.py

# Feature graphic
python play-store/scripts/make_feature_graphic.py

# Marketing screenshots (from the raw captures)
python play-store/scripts/make_phone_screenshots.py
python play-store/scripts/make_tablet_screenshots.py

# Promo video (voice + frames + captions)
powershell -ExecutionPolicy Bypass -File play-store/scripts/generate_voiceover.ps1
python play-store/scripts/make_video_frames.py
python play-store/scripts/build_video.py
```

## How the screenshots were captured

The raw screenshots in `screenshots/raw/` were taken from a real
Pixel 8 Pro (`adb -s 37220DLJG001ML exec-out screencap -p > FILE.png`)
after enabling System UI demo mode for a clean status bar. The marketing
templates mask the device notification icons and the AdMob native ad
validator overlay that appears during debug builds before placing each
screenshot in a brand-styled frame.
