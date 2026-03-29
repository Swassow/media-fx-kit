# media-fx-kit — Required High-Level APIs for FX Feature

## Overview

The NeuralSound karaoke app needs **8 individual effect APIs** + **1 chain API** from media-fx-kit.
The app composes these into 5 FX presets (Hard Tune, Natural Tune, Big Chorus, Super Vocoder, Clean).

All APIs should:
- Accept **input file path** and **output file path**
- Execute the FFmpeg command internally (caller never builds FFmpeg commands)
- Return a **Result** (success with output path, or failure with error message)
- Support **async execution** with cancellation via a session/job ID

---

## API Contract

### Common Types

```kotlin
// Result wrapper returned by all effect methods
data class FxResult(
    val success: Boolean,
    val outputPath: String?,   // non-null on success
    val error: String? = null  // non-null on failure
)

// For async variants
data class FxSession(
    val sessionId: Long        // can be passed to cancelSession()
)

// A single effect config for chaining
sealed class FxEffect {
    data class PitchShift(val semitones: Double, val sampleRate: Int = 44100) : FxEffect()
    data class Chorus(val inGain: Float = 0.5f, val outGain: Float = 0.9f, val delays: List<Int>, val decays: List<Float>, val speeds: List<Float>, val depths: List<Float>) : FxEffect()
    data class Echo(val inGain: Float = 0.8f, val outGain: Float = 0.88f, val delayMs: Int = 60, val decay: Float = 0.4f) : FxEffect()
    data class Flanger(val delayMs: Float = 1f, val depth: Float = 2f, val speed: Float = 10f, val width: Float = 80f) : FxEffect()
    data class Tremolo(val frequency: Double = 5.0, val depth: Double = 0.5) : FxEffect()
    data class Compressor(val thresholdDb: Float = -20f, val ratio: Float = 2f, val attackMs: Float = 5f, val releaseMs: Float = 100f, val makeupDb: Float = 0f) : FxEffect()
    data class HighPass(val cutoffHz: Int = 80) : FxEffect()
    data class LoudnessNorm(val integratedLoudness: Float = -14f, val truePeak: Float = -1f, val loudnessRange: Float = 11f) : FxEffect()
}
```

---

## Required APIs (8 individual + 1 chain)

### 1. `pitchShift`
Shift pitch by N semitones without changing tempo.

```kotlin
suspend fun pitchShift(
    inputPath: String,
    outputPath: String,
    semitones: Double,       // e.g. 0.5, 2.0, -1.0
    sampleRate: Int = 44100  // detect from input if possible
): FxResult
```

**FFmpeg filter internally:** `asetrate=SR*factor, aresample=SR, atempo=1/factor`

**Used by:** Hard Tune (0–2 semitones), Natural Tune (0–0.5), Super Vocoder (1–5)

---

### 2. `addChorus`
Add chorus effect with configurable voice count and depth.

```kotlin
suspend fun addChorus(
    inputPath: String,
    outputPath: String,
    inGain: Float = 0.5f,
    outGain: Float = 0.9f,
    delays: List<Int>,        // e.g. [40, 48, 32] (ms per voice)
    decays: List<Float>,      // e.g. [0.4, 0.32, 0.3]
    speeds: List<Float>,      // e.g. [0.25, 0.4, 0.3]
    depths: List<Float>       // e.g. [2.0, 2.3, 1.3]
): FxResult
```

**FFmpeg filter:** `chorus=inGain:outGain:d1|d2|d3:dec1|dec2|dec3:spd1|spd2|spd3:dep1|dep2|dep3`

**Used by:** Hard Tune (single voice), Big Chorus (3 voices)

---

### 3. `addEcho`
Add echo/delay effect.

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

**FFmpeg filter:** `aecho=inGain:outGain:delayMs:decay`

**Used by:** Natural Tune, Super Vocoder

---

### 4. `addFlanger`
Add flanger modulation effect.

```kotlin
suspend fun addFlanger(
    inputPath: String,
    outputPath: String,
    delayMs: Float = 1f,
    depth: Float = 2f,
    speed: Float = 10f,
    width: Float = 80f,
    shape: String = "sinusoidal"  // or "triangular"
): FxResult
```

**FFmpeg filter:** `flanger=delay=D:depth=DEP:speed=S:width=W:shape=SHAPE`

**Used by:** Super Vocoder

---

### 5. `addTremolo`
Add amplitude modulation (tremolo) effect.

```kotlin
suspend fun addTremolo(
    inputPath: String,
    outputPath: String,
    frequency: Double = 5.0,  // Hz (modulation rate)
    depth: Double = 0.5       // 0.0–1.0
): FxResult
```

**FFmpeg filter:** `tremolo=f=FREQ:d=DEPTH`

**Used by:** Super Vocoder

---

### 6. `compress`
Apply dynamic range compression.

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

**FFmpeg filter:** `acompressor=threshold=TdB:ratio=R:attack=A:release=REL:makeup=MdB`

**Used by:** Hard Tune, Clean

---

### 7. `highPass`
Apply high-pass filter to remove low-frequency rumble.

```kotlin
suspend fun highPass(
    inputPath: String,
    outputPath: String,
    cutoffHz: Int = 80       // frequency in Hz
): FxResult
```

**FFmpeg filter:** `highpass=f=CUTOFF`

**Used by:** Clean

---

### 8. `normalizeLoudness`
Apply EBU R128 loudness normalization.

```kotlin
suspend fun normalizeLoudness(
    inputPath: String,
    outputPath: String,
    integratedLoudness: Float = -14f,  // LUFS target
    truePeak: Float = -1f,             // dBTP ceiling
    loudnessRange: Float = 11f         // LU
): FxResult
```

**FFmpeg filter:** `loudnorm=I=IL:TP=TP:LRA=LRA`

**Used by:** Clean

---

### 9. `applyChain` ⭐ (Most Important)
Apply multiple effects in a **single FFmpeg pass** (one decode → filter → encode cycle).
This is what the app will primarily use — individual APIs above are for simple single-effect cases.

```kotlin
suspend fun applyChain(
    inputPath: String,
    outputPath: String,
    effects: List<FxEffect>,
    outputFormat: OutputFormat = OutputFormat.M4A
): FxResult
```

**Behavior:**
- Builds a single `-af` filter chain from all effects in order
- Executes one FFmpeg command (efficient — no intermediate files)
- Output codec: AAC 192kbps for M4A

```kotlin
enum class OutputFormat { M4A, MP3, WAV }
```

**Example usage from app:**
```kotlin
// Hard Tune preset at strength 54
val result = MediaFxKit.applyChain(
    inputPath = "/path/to/vocal.m4a",
    outputPath = "/path/to/output.m4a",
    effects = listOf(
        FxEffect.PitchShift(semitones = 1.08, sampleRate = 44100),
        FxEffect.Chorus(delays = listOf(55), decays = listOf(0.4f), speeds = listOf(0.25f), depths = listOf(2f)),
        FxEffect.Compressor(thresholdDb = -15f, ratio = 3f, attackMs = 5f, releaseMs = 50f)
    )
)
if (result.success) {
    // use result.outputPath
}
```

---

## Cancellation API

```kotlin
// Cancel a running FFmpeg session
fun cancelAll()

// Cancel specific session (if applyChain returns a session handle)
fun cancel(sessionId: Long)
```

---

## Async Variant (Optional but Recommended)

For non-suspend contexts, provide callback-based async variants:

```kotlin
fun applyChainAsync(
    inputPath: String,
    outputPath: String,
    effects: List<FxEffect>,
    outputFormat: OutputFormat = OutputFormat.M4A,
    onComplete: (FxResult) -> Unit
): FxSession   // returns session for cancellation
```

---

## Preset Mapping Reference

This shows how the app will compose kit APIs into presets:

| Preset | Effects Chain (in order) |
|--------|--------------------------|
| **Hard Tune** | `PitchShift(0–2 semi)` → `Chorus(single, delay=55)` → `Compressor(-15dB, 3:1)` |
| **Natural Tune** | `PitchShift(0–0.5 semi)` → `Echo(delay=60, decay=0.4)` |
| **Big Chorus** | `Chorus(3 voices, delays=[20–80]ms)` |
| **Super Vocoder** | `PitchShift(1–5 semi)` → `Flanger` → `Tremolo(4–12Hz)` → `Echo(delay=20)` |
| **Clean** | `HighPass(60–140Hz)` → `Compressor(-20dB, 2:1)` → `LoudnessNorm(-14 LUFS)` |

---

## Integration Notes

1. **The app will call `applyChain()` for all presets** — individual effect APIs are secondary
2. **Output format is always M4A** (AAC 192kbps, `-movflags +faststart`)
3. **The kit should handle file overwrite** (`-y` flag internally)
4. **Input validation**: return error if input file doesn't exist or is empty
5. **Thread safety**: methods should be safe to call from coroutines on `Dispatchers.IO`
6. **The kit should NOT depend on any external FFmpegKit** — it should bundle its own or use the one already in its dependencies
