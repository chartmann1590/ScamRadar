"""Stitch frames + voiceovers into 4 feature MP4s + SRT captions.

Reads play-store/video/feature_scripts.json. For each video:
  feature_videos/<id>/frames/frame_NN.png
  feature_videos/<id>/voice/segment_NN.wav

Writes:
  feature_videos/<id>/<id>.mp4
  feature_videos/<id>/<id>.srt
  feature_videos/<id>/<id>.vtt    (WebVTT — used by HTML5 video tag)
  feature_videos/<id>/<id>_captions_burned.mp4

Replaces tabs in the SRT/VTT block since some Compose-like text could
include whitespace; rendering is left to the HTML5 player.
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path("h:/new-app/play-store/video")
SCRIPTS = ROOT / "feature_scripts.json"


def run(cmd, **kw):
    print(">", " ".join(str(c) for c in cmd))
    r = subprocess.run(cmd, check=False, capture_output=True, text=True, **kw)
    if r.returncode != 0:
        sys.stderr.write(r.stdout + "\n" + r.stderr + "\n")
        raise RuntimeError(f"ffmpeg failed (exit {r.returncode})")


def probe_duration(p):
    out = subprocess.check_output(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "csv=p=0", str(p)], text=True)
    return float(out.strip())


def fmt_srt(t):
    h = int(t // 3600)
    m = int((t % 3600) // 60)
    s = t - h * 3600 - m * 60
    return f"{h:02d}:{m:02d}:{int(s):02d},{int((s - int(s)) * 1000):03d}"


def fmt_vtt(t):
    h = int(t // 3600)
    m = int((t % 3600) // 60)
    s = t - h * 3600 - m * 60
    return f"{h:02d}:{m:02d}:{int(s):02d}.{int((s - int(s)) * 1000):03d}"


def build_video(video_cfg):
    vid = video_cfg["id"]
    base = ROOT / "feature_videos" / vid
    frames_dir = base / "frames"
    voice_dir = base / "voice"
    out_mp4 = base / f"{vid}.mp4"
    out_mp4_cap = base / f"{vid}_captions_burned.mp4"
    out_srt = base / f"{vid}.srt"
    out_vtt = base / f"{vid}.vtt"
    voice_concat = base / "voice.wav"
    audio_list = base / "_audio_concat.txt"
    video_list = base / "_video_concat.txt"

    seg_wavs = sorted(voice_dir.glob("segment_*.wav"))
    frames = sorted(frames_dir.glob("frame_*.png"))
    assert len(seg_wavs) == len(frames) == len(video_cfg["scenes"]), \
        f"{vid}: mismatch {len(seg_wavs)} wavs, {len(frames)} frames, {len(video_cfg['scenes'])} scenes"

    print(f"\n=== [{vid}] {len(seg_wavs)} scenes ===")

    durations = [probe_duration(p) for p in seg_wavs]
    pause = 0.35
    frame_holds = [d + pause for d in durations]
    frame_holds[-1] += 0.6  # extra hold on CTA

    # Audio concat list
    audio_list.write_text(
        "\n".join(f"file '{p.as_posix()}'" for p in seg_wavs),
        encoding="utf-8"
    )
    run(["ffmpeg", "-y", "-f", "concat", "-safe", "0",
         "-i", str(audio_list), "-c", "copy", str(voice_concat)])

    # Video concat list — each frame held for its hold duration
    vlines = []
    for f, hold in zip(frames, frame_holds):
        vlines.append(f"file '{f.as_posix()}'")
        vlines.append(f"duration {hold:.3f}")
    vlines.append(f"file '{frames[-1].as_posix()}'")
    video_list.write_text("\n".join(vlines), encoding="utf-8")

    total_visual = sum(frame_holds)
    total_audio = sum(durations)

    silent = base / "_silent.mp4"
    run([
        "ffmpeg", "-y",
        "-f", "concat", "-safe", "0",
        "-i", str(video_list),
        "-vf", "scale=1920:1080:force_original_aspect_ratio=decrease,"
               "pad=1920:1080:(ow-iw)/2:(oh-ih)/2,setsar=1,format=yuv420p",
        "-r", "30",
        "-c:v", "libx264", "-crf", "20", "-preset", "medium",
        str(silent),
    ])

    pad = max(0.0, total_visual - total_audio)
    padded = base / "_voice_padded.wav"
    if pad > 0:
        run([
            "ffmpeg", "-y", "-i", str(voice_concat),
            "-af", f"apad=pad_dur={pad:.3f}",
            "-c:a", "pcm_s16le",
            str(padded),
        ])
    else:
        padded = voice_concat

    run([
        "ffmpeg", "-y",
        "-i", str(silent), "-i", str(padded),
        "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
        "-shortest", str(out_mp4),
    ])

    # SRT + VTT
    cur = 0.0
    srt_chunks, vtt_chunks = [], ["WEBVTT", ""]
    for i, (scene, dur) in enumerate(zip(video_cfg["scenes"], durations), 1):
        start = cur
        end = cur + dur + pause * 0.6
        srt_chunks.append(f"{i}\n{fmt_srt(start)} --> {fmt_srt(end)}\n{scene['line']}\n")
        vtt_chunks.append(f"{fmt_vtt(start)} --> {fmt_vtt(end)}\n{scene['line']}\n")
        cur += dur + pause
    out_srt.write_text("\n".join(srt_chunks), encoding="utf-8")
    out_vtt.write_text("\n".join(vtt_chunks), encoding="utf-8")

    # Burned-captions variant
    srt_path = str(out_srt).replace("\\", "/").replace(":", "\\:")
    sub_filter = (
        f"subtitles='{srt_path}':"
        "force_style='Fontname=Segoe UI Semibold,Fontsize=20,"
        "PrimaryColour=&H00FFFFFF,OutlineColour=&HD0000000,"
        "BackColour=&HA0000000,BorderStyle=4,Outline=3,Shadow=0,"
        "Alignment=2,MarginV=46'"
    )
    run([
        "ffmpeg", "-y", "-i", str(out_mp4),
        "-vf", sub_filter,
        "-c:v", "libx264", "-crf", "20", "-preset", "medium",
        "-c:a", "copy",
        str(out_mp4_cap),
    ])

    # cleanup
    for p in [silent, video_list, audio_list]:
        try:
            p.unlink()
        except FileNotFoundError:
            pass

    print(f" OK: {out_mp4} ({total_audio:.1f}s audio, {total_visual:.1f}s video)")
    return {
        "id": vid,
        "title": video_cfg["title"],
        "mp4": out_mp4.name,
        "vtt": out_vtt.name,
        "srt": out_srt.name,
        "duration": total_visual,
        "burned": out_mp4_cap.name,
    }


def main():
    cfg = json.loads(SCRIPTS.read_text(encoding="utf-8"))
    results = []
    for v in cfg["videos"]:
        results.append(build_video(v))

    print("\n=== summary ===")
    for r in results:
        print(f"  {r['id']:<14} {r['duration']:5.1f}s  {r['mp4']}")


if __name__ == "__main__":
    main()
