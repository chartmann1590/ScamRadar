param(
    [string]$ScriptsJson = "h:/new-app/play-store/video/feature_scripts.json",
    [string]$OutRoot = "h:/new-app/play-store/video/feature_videos"
)

Add-Type -AssemblyName System.Speech

$json = Get-Content $ScriptsJson -Raw | ConvertFrom-Json

foreach ($video in $json.videos) {
    $voiceDir = Join-Path $OutRoot ($video.id + "/voice")
    if (-not (Test-Path $voiceDir)) {
        New-Item -ItemType Directory -Path $voiceDir -Force | Out-Null
    }

    $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
    try { $synth.SelectVoice("Microsoft Zira Desktop") } catch { try { $synth.SelectVoice("Microsoft David Desktop") } catch {} }
    $synth.Rate = -1

    for ($i = 0; $i -lt $video.scenes.Count; $i++) {
        $out = Join-Path $voiceDir ("segment_{0:D2}.wav" -f ($i + 1))
        $synth.SetOutputToWaveFile($out)
        $prompt = New-Object System.Speech.Synthesis.PromptBuilder
        $prompt.StartSentence()
        $prompt.AppendText($video.scenes[$i].line)
        $prompt.EndSentence()
        $synth.Speak($prompt)
        Write-Output ("[" + $video.id + "] " + $out)
    }

    $synth.SetOutputToNull()
    $synth.Dispose()
}

Write-Output "All voiceovers generated."
