# Media FX Kit

[![](https://jitpack.io/v/Swassow/media-fx-kit.svg)](https://jitpack.io/#Swassow/media-fx-kit)

A standalone Android library that wraps FFmpeg-Kit to provide audio/video effects processing. Designed for easy integration via JitPack.

## Features

- **Audio Effects**: Reverb, Equalizer, Compressor, Pitch Shift, Tempo Change
- **Audio Operations**: Mixing, Format Conversion, Volume Normalization, Waveform Extraction
- **Fluent API**: Builder pattern for custom FFmpeg commands
- **Coroutines Support**: Async operations with progress callbacks
- **Presets**: Ready-to-use effect presets for common use cases

## Installation

### Step 1: Add JitPack Repository

In your project's **`settings.gradle.kts`** (root level), add JitPack to the repositories:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

<details>
<summary>Groovy (settings.gradle)</summary>

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
</details>

### Step 2: Add the Dependency

In your app module's **`build.gradle.kts`** (`app/build.gradle.kts`):

```kotlin
dependencies {
    implementation("com.github.Swassow:media-fx-kit:v1.0.1")
}
```

<details>
<summary>Groovy (build.gradle)</summary>

```groovy
dependencies {
    implementation 'com.github.Swassow:media-fx-kit:v1.0.1'
}
```
</details>

### Step 3: Sync & Build

Click **"Sync Now"** in Android Studio. The library and its bundled FFmpeg-Kit will be downloaded automatically — no additional dependencies needed.

## Quick Start

### Apply Effects

```kotlin
import com.neuralsound.mediafxkit.MediaFxKit
import com.neuralsound.mediafxkit.effects.*

// Apply reverb
MediaFxKit.addReverb(inputPath, outputPath, Reverb.mediumHall())

// Apply equalizer with bass boost
MediaFxKit.boostBass(inputPath, outputPath, amount = 6f)

// Apply compression
MediaFxKit.compress(inputPath, outputPath, Compressor.vocal())

// Pitch shift by semitones
MediaFxKit.shiftPitch(inputPath, outputPath, semitones = 2f)

// Change tempo (1.5 = 150% speed)
MediaFxKit.changeTempo(inputPath, outputPath, factor = 1.5f)
```

### Chain Multiple Effects

```kotlin
MediaFxKit.applyEffects(inputPath, outputPath, listOf(
    Equalizer.bassBoost(4f),
    Compressor.medium(),
    Reverb.lightRoom()
))
```

### Async with Progress

```kotlin
lifecycleScope.launch {
    val result = MediaFxKit.applyEffectAsync(
        inputPath, 
        outputPath, 
        Reverb.largeHall()
    ) { progress ->
        updateProgressBar(progress)
    }
    
    result.onSuccess { success ->
        Log.d("MediaFxKit", "Processed in ${success.durationMs}ms")
    }.onFailure { failure ->
        Log.e("MediaFxKit", "Error: ${failure.errorMessage}")
    }
}
```

### Audio Mixing

```kotlin
// Mix two tracks
MediaFxKit.mixTwo(track1, track2, outputPath, volume1 = 1.0f, volume2 = 0.8f)

// Overlay (voice over music)
MediaFxKit.overlay(
    background = musicPath,
    foreground = voicePath,
    outputPath = outputPath,
    backgroundVolume = 0.3f
)

// Advanced mixing with custom tracks
MediaFxKit.mix(
    tracks = listOf(
        AudioMixer.Track(path1, volume = 1.0f),
        AudioMixer.Track(path2, volume = 0.5f, delay = 2.0f)
    ),
    outputPath = outputPath,
    normalize = true
)
```

### Format Conversion

```kotlin
// Convert to MP3
MediaFxKit.toMp3(inputPath, outputPath, quality = AudioConverter.Quality.HIGH)

// Convert to WAV
MediaFxKit.toWav(inputPath, outputPath)

// Extract audio from video
MediaFxKit.extractAudio(videoPath, audioPath, format = AudioConverter.Format.MP3)
```

### Volume Normalization

```kotlin
// Normalize to streaming standard (-14 LUFS)
MediaFxKit.normalize(inputPath, outputPath, targetLufs = -14f)

// Normalize to broadcast standard
MediaFxKit.normalizeTo(inputPath, outputPath, VolumeNormalizer.LoudnessTarget.BROADCAST)

// Adjust volume by dB
MediaFxKit.adjustVolume(inputPath, outputPath, volumeDb = 3f)
```

### Waveform Extraction

```kotlin
lifecycleScope.launch {
    val waveform = MediaFxKit.extractWaveform(inputPath, samplesPerSecond = 10)
    waveform?.let {
        val normalizedSamples = it.getNormalizedSamples()
        drawWaveform(normalizedSamples)
    }
}
```

## Custom Commands

For advanced use cases, use the command builder:

```kotlin
val result = MediaFxKit.command()
    .input(inputPath)
    .effect(Reverb.cathedral())
    .effect(Compressor.heavy())
    .filter("volume=0.8")
    .audioCodec("libmp3lame")
    .audioBitrate("320k")
    .output(outputPath)
    .executeSync()
```

## Effect Presets

### Reverb
- `Reverb.lightRoom()` - Subtle room ambiance
- `Reverb.mediumHall()` - Concert hall
- `Reverb.largeHall()` - Large venue
- `Reverb.cathedral()` - Cathedral reverb

### Equalizer
- `Equalizer.bassBoost(amount)` - Boost low frequencies
- `Equalizer.trebleBoost(amount)` - Boost high frequencies
- `Equalizer.vocalEnhance()` - Clarity for vocals
- `Equalizer.tenBand(gains)` - 10-band graphic EQ

### Compressor
- `Compressor.light()` - Gentle compression
- `Compressor.medium()` - Balanced dynamics
- `Compressor.heavy()` - Aggressive limiting
- `Compressor.vocal()` - Optimized for voice

### Pitch & Tempo
- `PitchShift.octaveUp()` / `octaveDown()`
- `PitchShift.fifthUp()` / `fifthDown()`
- `TempoChange.halfSpeed()` / `doubleSpeed()`
- `TempoChange.fromBpm(original, target)`

## Requirements

- Android API 24+ (Android 7.0)
- Kotlin 1.9+

## License

MIT License - see [LICENSE](LICENSE)

This library includes FFmpeg-Kit which is licensed under LGPL v3.0.
