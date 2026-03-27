# Media FX Kit - Standalone JitPack Library Plan

## Overview

Create a standalone Android library (`media-fx-kit`) that wraps FFmpeg-Kit to provide audio/video effects (reverb, EQ, compressor, pitch, tempo). This library will be:
1. **Created as empty project** - standalone Git repository
2. **Published to JitPack** for cross-project sharing
3. **Tested in a new project** before integrating with music-separation

---

## JitPack Platform Support

**JitPack supports multiple Git platforms:**

| Platform | Public Repos | Private Repos | Dependency Format |
|----------|--------------|---------------|-------------------|
| **GitHub** | ✅ Free | ✅ With subscription | `com.github.User:Repo:Tag` |
| **GitLab** | ✅ Free | ✅ With subscription + token | `com.gitlab.User:Repo:Tag` |
| **Bitbucket** | ✅ Free | ✅ With subscription + token | `com.bitbucket.User:Repo:Tag` |

### For GitLab (Your Current Platform)
Since your music-separation repo is on GitLab (`gitlab.com/neuralsound/sound-android/music-separation`):

**Public GitLab Repo:**
- Dependency: `implementation("com.gitlab.neuralsound:media-fx-kit:1.0.0")`
- JitPack URL: `https://jitpack.io/#gitlab.com/neuralsound/media-fx-kit`

**Private GitLab Repo:**
1. Create Personal Access Token on GitLab (scopes: `read_repository`, `read_api`)
2. Add token to JitPack account (jitpack.io → Settings → GitLab)
3. Configure authentication in consuming project's `gradle.properties`

### For GitHub
**Public GitHub Repo:**
- Dependency: `implementation("com.github.neuralsound:media-fx-kit:1.0.0")`
- JitPack URL: `https://jitpack.io/#neuralsound/media-fx-kit`

---

## Current FFmpeg-Kit Setup in music-separation

### Location
- `Music-Separation/app/libs/ffmpeg-kit.aar` (~34MB)

### How It's Currently Wired
- `build.gradle.kts`: Uses `implementation(files("libs/ffmpeg-kit.aar"))`
- `settings.gradle.kts`: Has `flatDir { dirs("libs") }` for local AAR
- Also requires `com.arthenica:smart-exception-java:0.2.1` companion library

### Why Local AAR?
The official FFmpeg-Kit Maven package is **no longer actively maintained**. Using local AAR gives:
- Production-ready, tested build
- Control over codec configurations
- Stability guarantees

---

## Using Local ffmpeg-kit.aar with JitPack

**Key Insight:** JitPack builds from your git repository and only sees files committed to git.

**Solution:** Commit `ffmpeg-kit.aar` to the library's repository.

### Considerations
- **Git LFS (Optional):** For large AAR files, consider Git LFS to avoid repo bloat
- **License:** FFmpeg-Kit uses LGPL/GPL - ensure redistribution is permitted
- The local AAR approach works exactly like in your current project

---

## Step-by-Step Guide

### Part 1: Create Empty Library Project

#### 1.1 Create New Android Project in Android Studio
- Select **"No Activity"** template
- Name: `media-fx-kit`
- Package: `com.neuralsound.mediafxkit`
- Language: Kotlin
- Min SDK: 24 (Android 7.0)
- Build configuration: Kotlin DSL (build.gradle.kts)

#### 1.2 Convert to Library Module
- Change plugin from `com.android.application` to `com.android.library`
- Remove `applicationId` from defaultConfig
- Add `maven-publish` plugin for JitPack

#### 1.3 Add FFmpeg-Kit AAR
- Create `libs/` folder inside the module
- Copy `ffmpeg-kit.aar` from music-separation to this folder
- Add dependency: `implementation(files("libs/ffmpeg-kit.aar"))`
- Add companion: `implementation("com.arthenica:smart-exception-java:0.2.1")`

#### 1.4 Configure for Publishing
- Add `publishing` block to build.gradle.kts
- Configure `singleVariant("release")` with sources and javadoc

#### 1.5 Create jitpack.yml
- Specify JDK 17
- Configure build command

---

### Part 2: Repository Structure

```
media-fx-kit/                        # Root (Git repo)
├── .gitignore
├── README.md
├── LICENSE                          # MIT or Apache 2.0
├── jitpack.yml                      # JitPack config
├── build.gradle.kts                 # Root build
├── settings.gradle.kts
├── gradle.properties
│
└── media-fx-kit/                    # Library module
    ├── libs/
    │   └── ffmpeg-kit.aar           # MUST BE COMMITTED TO GIT
    ├── build.gradle.kts             # com.android.library + maven-publish
    ├── consumer-rules.pro
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        └── java/com/neuralsound/mediafxkit/
            ├── MediaFxKit.kt            # Main facade
            ├── core/
            │   ├── FfmpegExecutor.kt
            │   ├── FfmpegCommandBuilder.kt
            │   └── ProcessingResult.kt
            ├── effects/
            │   ├── AudioEffect.kt       # Sealed base
            │   ├── Reverb.kt
            │   ├── Equalizer.kt
            │   ├── Compressor.kt
            │   ├── PitchShift.kt
            │   └── TempoChange.kt
            ├── operations/
            │   ├── AudioMixer.kt
            │   ├── AudioConverter.kt
            │   ├── VolumeNormalizer.kt
            │   └── WaveformExtractor.kt
            └── util/
                ├── FilterEscapeUtils.kt
                └── PathUtils.kt
```

---

### Part 3: Publish to JitPack

#### 3.1 Initialize Git Repository
- `git init`
- Add all files including `ffmpeg-kit.aar`
- `git add .` (AAR must be staged!)
- `git commit -m "Initial commit: Media FX Kit library"`

#### 3.2 Push to Git Platform

**Option A: GitHub (Recommended for public libraries)**
- Create repository: `github.com/neuralsound/media-fx-kit`
- `git remote add origin https://github.com/neuralsound/media-fx-kit.git`
- Dependency format: `com.github.neuralsound:media-fx-kit:1.0.0`
- JitPack URL: `https://jitpack.io/#neuralsound/media-fx-kit`

**Option B: GitLab (Your existing platform)**
- Create repository: `gitlab.com/neuralsound/media-fx-kit`
- `git remote add origin https://gitlab.com/neuralsound/media-fx-kit.git`
- Dependency format: `com.gitlab.neuralsound:media-fx-kit:1.0.0`
- JitPack URL: `https://jitpack.io/#gitlab.com/neuralsound/media-fx-kit`

**Option C: Bitbucket**
- Dependency format: `com.bitbucket.neuralsound:media-fx-kit:1.0.0`
- JitPack URL: `https://jitpack.io/#bitbucket.com/neuralsound/media-fx-kit`

#### 3.3 Tag Release
- `git tag -a v1.0.0 -m "Release 1.0.0"`
- `git push origin v1.0.0`

#### 3.4 Trigger JitPack Build
- Visit JitPack URL for your platform (see above)
- JitPack auto-builds when URL is accessed
- First build takes 5-10 minutes
- Check build logs if issues occur

---

### Part 4: Test in New Android Project

#### 4.1 Create Test Project
- Open Android Studio → New Project
- Name: `MediaFxKitTest`
- Package: `com.neuralsound.mediafxkittest`
- Template: Empty Activity (Compose)
- Min SDK: 24

#### 4.2 Add JitPack Repository
In `settings.gradle.kts`, add to `repositories`:
- `maven("https://jitpack.io")`

#### 4.3 Add Library Dependency
In `app/build.gradle.kts` (use format for your platform):
- GitHub: `implementation("com.github.neuralsound:media-fx-kit:1.0.0")`
- GitLab: `implementation("com.gitlab.neuralsound:media-fx-kit:1.0.0")`
- Bitbucket: `implementation("com.bitbucket.neuralsound:media-fx-kit:1.0.0")`

#### 4.4 Sync and Test
- Verify library imports correctly
- Test basic operations (reverb, EQ, mix)
- Verify FFmpeg commands execute successfully

---

### Part 5: Integrate with music-separation

#### 5.1 Add JitPack to music-separation
In `settings.gradle.kts`, add `maven("https://jitpack.io")` to repositories

#### 5.2 Replace Local FFmpeg
In `app/build.gradle.kts`:
- Remove: `implementation(files("libs/ffmpeg-kit.aar"))`
- Add: `implementation("com.gitlab.neuralsound:media-fx-kit:1.0.0")` (or github/bitbucket)

#### 5.3 Migrate Existing Code
Replace direct FFmpegKit calls with library API

---

## Library Features to Implement

### Effects (FFmpeg Filters)
| Effect | FFmpeg Filter | Description |
|--------|---------------|-------------|
| Reverb | `aecho` | Echo/reverb with configurable delay/decay |
| Equalizer | `equalizer` | Multi-band EQ with frequency/gain/width |
| Compressor | `acompressor` | Dynamic range with threshold/ratio |
| Pitch | `asetrate`+`aresample` | Pitch shift by semitones |
| Tempo | `atempo` | Speed change (chain for >2x) |
| Volume | `volume` | Level adjustment |
| Limiter | `alimiter` | Peak limiting |
| Normalize | `loudnorm` | EBU R128 normalization |

### Operations
- **AudioMixer:** Multi-track mixing with per-track volume
- **WaveformExtractor:** Generate waveform data for visualization
- **AudioConverter:** Format conversion (MP3, AAC, WAV, FLAC)
- **VolumeNormalizer:** Loudness normalization and limiting

### API Design
- Fluent builder pattern for chaining effects
- Sealed class hierarchy for type-safe effects
- Suspend functions with coroutines for async
- ProcessingResult wrapper for success/error handling

---

## Implementation Phases

### Phase 1: Repository Setup
1. Create empty Android library project
2. Add ffmpeg-kit.aar and commit to git
3. Configure build.gradle.kts for publishing
4. Add jitpack.yml configuration
5. Push to GitHub

### Phase 2: Core Implementation
6. Implement FfmpegExecutor (sync/async wrapper)
7. Create FfmpegCommandBuilder (fluent API)
8. Create ProcessingResult sealed class
9. Implement MediaFxKit main facade

### Phase 3: Effects
10. Create AudioEffect sealed base class
11. Implement Reverb effect
12. Implement Equalizer effect
13. Implement Compressor effect
14. Implement PitchShift/TempoChange

### Phase 4: Operations
15. Implement AudioMixer
16. Implement WaveformExtractor
17. Implement AudioConverter
18. Implement VolumeNormalizer

### Phase 5: Testing & Publishing
19. Add unit tests
20. Tag v1.0.0 release
21. Test in new Android project
22. Integrate with music-separation

---

## Notes

- FFmpeg-Kit `full` variant is ~50MB, includes all codecs
- JitPack builds may fail initially - check logs at jitpack.io
- First JitPack build takes 5-10 minutes
- Consumers of the library don't need to add ffmpeg-kit separately - it's bundled
