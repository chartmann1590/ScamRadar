"""Concatenate the 9 still frames with the per-segment voiceovers into the
final 1920x1080 ScamRadar promo MP4, then generate matching SRT captions.

Outputs:
  play-store/video/scamradar_promo.mp4
  play-store/video/scamradar_promo.srt

Process:
  1. ffprobe each voice/segment_NN.wav for its duration
  2. concat all voiceover WAVs into voice.wav (no extra silence)
  3. build a concat list for the frames with each frame held for its
     corresponding segment duration
  4. ffmpeg encodes the video + audio into MP4 with H.264 + AAC
  5. write captions.srt with cumulative segment times
  6. burn-in subtitle copy of the same SRT for the optimized "captioned" mp4
"""
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path("h:/new-app/play-store")
VOICE_DIR = ROOT / "video" / "voice"
FRAME_DIR = ROOT / "video" / "frames"
VIDEO_DIR = ROOT / "video"
OUT_MP4 = VIDEO_DIR / "scamradar_promo.mp4"
OUT_MP4_CAPTIONED = VIDEO_DIR / "scamradar_promo_captions_burned.mp4"
OUT_SRT = VIDEO_DIR / "scamradar_promo.srt"
OUT_VOICE = VIDEO_DIR / "voice.wav"
CONCAT_LIST = VIDEO_DIR / "_concat.txt"
AUDIO_CONCAT_LIST = VIDEO_DIR / "_audio_concat.txt"

LINES = [
    "Got a text you can't tell is real?",
    "AI scams jumped twelve hundred percent in 2025.",
    "Meet ScamRadar — the free AI scam detector for your phone.",
    "Paste any sketchy message, and get a clear answer in three seconds.",
    "Suspicious. Likely scam. Or, peace of mind.",
    "Everything stays on your phone. We never see your messages.",
    "Learn the top scam patterns. Track your scans. Share verdicts.",
    "Free forever. No account. No upload.",
    "Don't get scammed. Get ScamRadar.",
]


def run(cmd, **kw):
    print(">", " ".join(str(c) for c in cmd))
    subprocess.run(cmd, check=True, **kw)


def probe_duration(p):
    out = subprocess.check_output(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "csv=p=0", str(p)], text=True)
    return float(out.strip())


def seconds_to_srt(t):
    h = int(t // 3600)
    m = int((t % 3600) // 60)
    s = t - h * 3600 - m * 60
    return f"{h:02d}:{m:02d}:{int(s):02d},{int((s - int(s)) * 1000):03d}"


def main():
    seg_wavs = sorted(VOICE_DIR.glob("segment_*.wav"))
    assert len(seg_wavs) == 9 == len(LINES)
    frames = sorted(FRAME_DIR.glob("frame_*.png"))
    assert len(frames) == 9

    durations = [probe_duration(p) for p in seg_wavs]
    print("Durations:", durations, "total =", sum(durations))

    # 1) Concat voice WAVs into one voice.wav (we add 0.4s silence between
    #    each segment to give the visual a breath)
    pause_s = 0.35
    # Build a list of inputs alternating: wav, silence, wav, silence...
    AUDIO_CONCAT_LIST.write_text("\n".join(
        f"file '{p.as_posix()}'" for p in seg_wavs), encoding="utf-8")
    # We'll just concat WAVs directly without pauses for simplicity; pauses
    # are added by frame holds (frames last segment_dur + pause).
    run(["ffmpeg", "-y", "-f", "concat", "-safe", "0",
         "-i", str(AUDIO_CONCAT_LIST),
         "-c", "copy", str(OUT_VOICE)])

    # Recompute total runtime including small inter-segment pauses for video
    frame_holds = [d + pause_s for d in durations]
    frame_holds[-1] += 0.6  # extra hold on the CTA card

    # 2) Build the frame concat list (each frame held for its hold duration)
    lines = []
    for f, hold in zip(frames, frame_holds):
        lines.append(f"file '{f.as_posix()}'")
        lines.append(f"duration {hold:.3f}")
    # ffmpeg concat demuxer requires the last file repeated
    lines.append(f"file '{frames[-1].as_posix()}'")
    CONCAT_LIST.write_text("\n".join(lines), encoding="utf-8")

    # Total visual duration
    total_visual = sum(frame_holds)
    # Total audio duration
    total_audio = sum(durations)

    # 3) Build the silent video from frames
    silent_video = VIDEO_DIR / "_silent.mp4"
    run([
        "ffmpeg", "-y",
        "-f", "concat", "-safe", "0",
        "-i", str(CONCAT_LIST),
        "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,"
               "pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1",
        "-r", "30",
        "-pix_fmt", "yuv420p",
        "-c:v", "libx264", "-crf", "18", "-preset", "medium",
        str(silent_video),
    ])

    # 4) Build the audio track padded to total_visual so video doesn't cut off
    padded_audio = VIDEO_DIR / "_voice_padded.wav"
    pad_amount = max(0.0, total_visual - total_audio)
    if pad_amount > 0:
        run([
            "ffmpeg", "-y", "-i", str(OUT_VOICE),
            "-af", f"apad=pad_dur={pad_amount:.3f}",
            "-c:a", "pcm_s16le",
            str(padded_audio),
        ])
    else:
        padded_audio = OUT_VOICE

    # 5) Mux video + audio into the final clean MP4
    run([
        "ffmpeg", "-y",
        "-i", str(silent_video),
        "-i", str(padded_audio),
        "-c:v", "copy",
        "-c:a", "aac", "-b:a", "192k",
        "-shortest",
        str(OUT_MP4),
    ])

    # 6) Build the SRT captions. Each line shown for its segment_duration
    #    starting from cumulative time.
    cur = 0.0
    srt_chunks = []
    for i, (line, dur) in enumerate(zip(LINES, durations), 1):
        start = cur
        end = cur + dur + pause_s * 0.6
        srt_chunks.append(
            f"{i}\n{seconds_to_srt(start)} --> {seconds_to_srt(end)}\n{line}\n"
        )
        cur += dur + pause_s
    OUT_SRT.write_text("\n".join(srt_chunks), encoding="utf-8")
    print("Wrote SRT:", OUT_SRT)

    # 7) Build the captioned variant via subtitles filter (libass)
    # Escape backslashes/quotes for the filter argument
    srt_path = str(OUT_SRT).replace("\\", "/").replace(":", "\\:")
    sub_filter = (
        f"subtitles='{srt_path}':"
        "force_style='Fontname=Segoe UI Semibold,Fontsize=18,"
        "PrimaryColour=&H00FFFFFF,OutlineColour=&HD0000000,"
        "BackColour=&HA0000000,BorderStyle=4,Outline=3,Shadow=0,"
        "Alignment=2,MarginV=40'"
    )
    run([
        "ffmpeg", "-y",
        "-i", str(OUT_MP4),
        "-vf", sub_filter,
        "-c:v", "libx264", "-crf", "18", "-preset", "medium",
        "-c:a", "copy",
        str(OUT_MP4_CAPTIONED),
    ])

    # Cleanup
    for p in [silent_video, CONCAT_LIST, AUDIO_CONCAT_LIST]:
        try:
            p.unlink()
        except FileNotFoundError:
            pass

    print("DONE")
    print(" Clean:", OUT_MP4)
    print(" Captioned:", OUT_MP4_CAPTIONED)
    print(" SRT:", OUT_SRT)


if __name__ == "__main__":
    main()
