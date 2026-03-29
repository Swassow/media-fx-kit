# Media FX Kit — FX API Implementation Guide

## Overview

This document describes the **FX API** added to Media FX Kit to support the NeuralSound karaoke app. The implementation provides **8 individual effect APIs** + **1 chain API** that compose audio effects using FFmpeg under the hood.

All APIs:
- Accept input/output file paths
- Execute FFmpeg commands internally (callers never build FFmpeg commands)
- Return `FxResult` (success with output path, or failure with error message)
- Support async execution with cancellation via session IDs
- Are safe to call from coroutines on `Dispatchers.IO`

---

## Package Structure

```
com.neuralsound.mediafxkit/
├── fx/                          ← NEW: FX API for karaoke app
│   ├── FxApi.kt                 ← Main API object (8 individual + 1 chain)
│   ├── FxEffect.kt              ← Sealed class for chain effect configs
│   ├── FxResult.kt              ← Result wrapper (success/failure)
│   ├── FxSession.kt             ← Async session handle for cancellation
│   └── OutputFormat.kt          ← Output format enum (M4A, MP3, WAV)
├── effects/                     ← AudioEffect subclasses (extended)
│   ├── Chorus.kt                ← NEW: FFmpeg chorus filter
│   ├── Echo.kt                  ← NEW: FFmpeg aecho filter
│   ├── Flanger.kt               ← NEW: FFmpeg flanger filter
│   ├── Tremolo.kt               ← NEW: FFmpeg tremolo filter
│   ├── HighPass.kt              ← NEW: FFmpeg highpass filter
│   ├── LoudnessNorm.kt          ← NEW: FFmpeg loudnorm filter
│   ├── Compressor.kt            ← Existing
│   ├── PitchShift.kt            ← Existing
│   └── ...
├── MediaFxKit.kt                ← Updated: exposes FX API methods
└── core/
    └── FfmpegExecutor.kt        ← Updated: cancel(sessionId) added
```

---

## API Types

### FxResult

```kotlin
data class FxResult(
    val success: Boolean,
    val outputPath: String?,   // non-null on success
    val error: String? = null  // non-null on failure
)
```

### FxSession

```kotlin
data class FxSession(
    val sessionId: Long        // pass to cancel() to stop processing
)
```

### FxEffect (Sealed Class)

Used to configure effects for `applyChain()`:

| Subtype | FFmpeg Filter | Description |
|---------|---------------|-------------|
| `FxEffect.PitchShift` | `asetrate + aresample + atempo` | Pitch shift without tempo change |
| `FxEffect.Chorus` | `chorus` | Multi-voice chorus |
| `FxEffect.Echo` | `aecho` | Echo/delay |
| `FxEffect.Flanger` | `flanger` | Flanger modulation |
| `FxEffect.Tremolo` | `tremolo` | Amplitude modulation |
| `FxEffect.Compressor` | `acompressor` | Dynamic range compression |
| `FxEffect.HighPass` | `highpass` | High-pass filter |
| `FxEffect.LoudnessNorm` | `loudnorm` | EBU R128 loudness normalization |

### OutputFormat

```kotlin
enum class OutputFormat {
    M4A,  // AAC 192kbps, -movflags +faststart
    MP3,  // libmp3lame 192kbps
    WAV   // PCM 16-bit
}
```

---

## Individual Effect APIs

All are `suspend` functions accessible via `MediaFxKit` or `FxApi`:

### 1. pitchShift

```kotlin
suspend fun pitchShift(
    inputPath: String,
    outputPath: String,
    semitones: Double,       // e.g. 0.5, 2.0, -1.0
    sampleRate: Int = 44100
): FxResult
```

### 2. addChorus

```kotlin
suspend fun addChorus(
    inputPath: String,
    outputPath: String,
    inGain: Float = 0.5f,
    outGain: Float = 0.9f,
    delays: List<Int>,       // ms per voice, e.g. [40, 48, 32]
    decays: List<Float>,
    speeds: List<Float>,
    depths: List<Float>
): FxResult
```

### 3. addEcho

```kotlin
suspend fun addEcho(
    inputPath: String,
    outputPath: String,
    inGain: Float = 0.8f,
    outGain: Float = 0.88f,
    delayMs: Int = 60,
    decay: Float = 0.4f
): FxResult
```

### 4. addFlanger

```kotlin
suspend fun addFlanger(
    inputPath: String,
    outputPath: String,
    delayMs: Float = 1f,
    depth: Float = 2f,
    speed: Float = 10f,
    width: Float = 80f,
    shape: String = "sinusoidal"
): FxResult
```

### 5. addTremolo

```kotlin
suspend fun addTremolo(
    inputPath: String,
    outputPath: String,
    frequency: Double = 5.0,
    depth: Double = 0.5
): FxResult
```

### 6. compress

```kotlin
suspend fun compress(
    inputPath: String,
    outputPath: String,
    thresholdDb: Float = -20f,
    ratio: Float = 2f,
    attackMs: Float = 5f,
    releaseMs: Float = 100f,
    makeupDb: Float = 0f
): FxResult
```

### 7. highPass

```kotlin
suspend fun highPass(
    inputPath: String,
    outputPath: String,
    cutoffHz: Int = 80
): FxResult
```

### 8. normalizeLoudness

```kotlin
suspend fun normalizeLoudness(
    inputPath: String,
    outputPath: String,
    integratedLoudness: Float = -14f,
    truePeak: Float = -1f,
    loudnessRange: Float = 11f
): FxResult
```

---

## Chain API ⭐ (Primary API)

```kotlin
suspend fun applyChain(
    inputPath: String,
    outputPath: String,
    effects: List<FxEffect>,
    outputFormat: OutputFormat = OutputFormat.M4A
): FxResult
```

**Key behaviors:**
- Builds a **single** `-af` filter chain from all effects in order
- Executes **one** FFmpeg command (no intermediate files)
- M4A output uses AAC 192kbps with `-movflags +faststart`
- Input validation: returns error if file doesn't exist or is empty
- Handles `-y` flag internally (overwrites output)

### Async Variant

```kotlin
fun applyChainAsync(
    inputPath: String,
    outputPath: String,
    effects: List<FxEffect>,
    outputFormat: OutputFormat = OutputFormat.M4A,
    onComplete: (FxResult) -> Unit
): FxSession
```

---

## Cancellation

```kotlin
// Cancel a specific session
MediaFxKit.cancel(sessionId: Long)

// Cancel all running sessions
MediaFxKit.cancelAll()
```

---

## Preset Usage Examples

### Hard Tune (strength 54)

```kotlin
val result = MediaFxKit.applyChain(
    inputPath = "/path/to/vocal.m4a",
    outputPath = "/path/to/output.m4a",
    effects = listOf(
        FxEffect.PitchShift(semitones = 1.08, sampleRate = 44100),
        FxEffect.Chorus(
            delays = listOf(55),
            decays = listOf(0.4f),
            speeds = listOf(0.25f),
            depths = listOf(2f)
        ),
        FxEffect.Compressor(thresholdDb = -15f, ratio = 3f, attackMs = 5f, releaseMs = 50f)
    )
)
if (result.success) {
    // use result.outputPath
}
```

### Natural Tune

```kotlin
val result = MediaFxKit.applyChain(
    inputPath = input,
    outputPath = output,
    effects = listOf(
        FxEffect.PitchShift(semitones = 0.3),
        FxEffect.Echo(delayMs = 60, decay = 0.4f)
    )
)
```

### Big Chorus

```kotlin
val result = MediaFxKit.applyChain(
    inputPath = input,
    outputPath = output,
    effects = listOf(
        FxEffect.Chorus(
            delays = listOf(40, 48, 32),
            decays = listOf(0.4f, 0.32f, 0.3f),
            speeds = listOf(0.25f, 0.4f, 0.3f),
            depths = listOf(2.0f, 2.3f, 1.3f)
        )
    )
)
```

### Super Vocoder

```kotlin
val result = MediaFxKit.applyChain(
    inputPath = input,
    outputPath = output,
    effects = listOf(
        FxEffect.PitchShift(semitones = 3.0),
        FxEffect.Flanger(),
        FxEffect.Tremolo(frequency = 8.0, depth = 0.6),
        FxEffect.Echo(delayMs = 20, decay = 0.3f)
    )
)
```

### Clean

```kotlin
val result = MediaFxKit.applyChain(
    inputPath = input,
    outputPath = output,
    effects = listOf(
        FxEffect.HighPass(cutoffHz = 100),
        FxEffect.Compressor(thresholdDb = -20f, ratio = 2f),
        FxEffect.LoudnessNorm(integratedLoudness = -14f)
    )
)
```

---

## Preset Mapping Reference

| Preset | Effects Chain (in order) |
|--------|--------------------------|
| **Hard Tune** | `PitchShift(0–2 semi)` → `Chorus(single, delay=55)` → `Compressor(-15dB, 3:1)` |
| **Natural Tune** | `PitchShift(0–0.5 semi)` → `Echo(delay=60, decay=0.4)` |
| **Big Chorus** | `Chorus(3 voices, delays=[20–80]ms)` |
| **Super Vocoder** | `PitchShift(1–5 semi)` → `Flanger` → `Tremolo(4–12Hz)` → `Echo(delay=20)` |
| **Clean** | `HighPass(60–140Hz)` → `Compressor(-20dB, 2:1)` → `LoudnessNorm(-14 LUFS)` |

---

## Access Patterns

The FX API can be accessed in two ways:

### Via MediaFxKit (recommended facade)

```kotlin
// Individual effects
MediaFxKit.pitchShiftFx(input, output, semitones = 2.0)
MediaFxKit.addChorusFx(input, output, delays = listOf(55), ...)
MediaFxKit.highPassFx(input, output, cutoffHz = 100)

// Chain (primary usage)
MediaFxKit.applyChain(input, output, effects)
MediaFxKit.applyChainAsync(input, output, effects) { result -> ... }

// Cancellation
MediaFxKit.cancel(session.sessionId)
MediaFxKit.cancelAll()
```

### Via FxApi directly

```kotlin
FxApi.pitchShift(input, output, semitones = 2.0)
FxApi.applyChain(input, output, effects)
FxApi.cancel(sessionId)
```

---

## AudioEffect Subclasses (Also Added)

In addition to the FX API, 6 new `AudioEffect` subclasses were added for use with the existing `MediaFxKit.applyEffect()` / `applyEffects()` system:

| Class | Filter | Presets |
|-------|--------|---------|
| `Chorus` | `chorus` | `single()`, `threeVoice()`, `fiveVoice()` |
| `Echo` | `aecho` | `slapback()`, `subtle()`, `tight()`, `long()` |
| `Flanger` | `flanger` | `default()`, `subtle()`, `intense()` |
| `Tremolo` | `tremolo` | `slow()`, `medium()`, `fast()`, `vocoder()` |
| `HighPass` | `highpass` | `rumbleRemoval()`, `vocal()`, `aggressive()` |
| `LoudnessNorm` | `loudnorm` | `streaming()`, `broadcast()`, `podcast()` |

---

## Testing

**37 unit tests** verify all filter string generation and chain building:

- Individual `AudioEffect` subclass filter strings (Chorus, Echo, Flanger, Tremolo, HighPass, LoudnessNorm)
- `FxEffect` sealed class filter strings (all 8 types)
- `FxEffect.buildFilterChain()` for all 5 preset configurations
- `OutputFormat` enum properties
- Input validation (parameter ranges, list size matching)
- Regression tests for existing effects (PitchShift, Compressor, Reverb)

Run tests:
```bash
./gradlew :app:testReleaseUnitTest
```

---

## Integration Notes

1. **The app should call `applyChain()` for all presets** — individual effect APIs are for simple single-effect cases
2. **Output format is always M4A** (AAC 192kbps, `-movflags +faststart`) by default
3. **File overwrite** is handled internally (`-y` flag)
4. **Input validation** returns error if input file doesn't exist or is empty
5. **Thread safety** — all `suspend` methods dispatch to `Dispatchers.IO`
6. **No external dependencies** — uses the bundled FFmpeg-Kit 6.0 AAR
