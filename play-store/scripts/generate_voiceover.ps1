# Generates a WAV file per script line using Windows SAPI.
# Outputs to play-store/video/voice/segment_NN.wav
param(
    [string]$OutDir = "h:/new-app/play-store/video/voice"
)

Add-Type -AssemblyName System.Speech

if (-not (Test-Path $OutDir)) {
    New-Item -ItemType Directory -Path $OutDir | Out-Null
}

$lines = @(
    "Got a text you can't tell is real?",
    "AI scams jumped twelve hundred percent in twenty twenty five.",
    "Meet ScamRadar. The free AI scam detector for your phone.",
    "Paste any sketchy message, and get a clear answer in three seconds.",
    "Suspicious. Likely scam. Or, peace of mind.",
    "Everything stays on your phone. We never see your messages.",
    "Learn the top scam patterns. Track your scans. Share verdicts.",
    "Free forever. No account. No upload.",
    "Don't get scammed. Get ScamRadar."
)

$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
$synth.SelectVoice("Microsoft Zira Desktop")
$synth.Rate = -1  # slightly slower than default

for ($i = 0; $i -lt $lines.Count; $i++) {
    $out = Join-Path $OutDir ("segment_{0:D2}.wav" -f ($i + 1))
    $synth.SetOutputToWaveFile($out)
    $prompt = New-Object System.Speech.Synthesis.PromptBuilder
    $prompt.StartSentence()
    $prompt.AppendText($lines[$i])
    $prompt.EndSentence()
    $synth.Speak($prompt)
    Write-Output ("wrote " + $out)
}

$synth.SetOutputToNull()
$synth.Dispose()
