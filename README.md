<div align="center">

# 👁️ Eko Vision

**Ultra-Lightweight On-Device Vision Intelligence for Android**  
*Powered by Mirage (~22 MB Vision-Language Model by Cactus Compute)*

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2025.02-green.svg?logo=android)](https://developer.android.com/jetpack/compose)
[![CameraX](https://img.shields.io/badge/CameraX-1.4.1-orange.svg)](https://developer.android.com/training/camerax)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Model Footprint](https://img.shields.io/badge/Model%20Size-22%20MB-blueviolet.svg)](#about-mirage)

<p align="center">
  <b>100% On-Device</b> • <b>Sub-100ms Latency</b> • <b>Zero Cloud Calls</b> • <b>Full Hardware Privacy</b>
</p>

</div>

---

## 🌟 Overview

**Eko Vision** is an open-source, modular Android application and reference demo showcasing **Mirage**—an ultra-compact ~22 MB on-device Vision-Language Model (VLM) developed by [Cactus Compute](https://cactuscompute.com). 

While traditional multi-modal VLMs require 2 GB to 8 GB+ of RAM and heavyweight servers, Mirage compresses 51.3M parameters into just **~22 MB of 2-bit weights**, executing comprehensive visual perception in under **80 ms on mobile NPU / ~110 ms on CPU**.

Eko Vision turns your smartphone into a real-time visual perception HUD with normalized bounding boxes, instant OCR, spatial reasoning, and scene understanding—running entirely offline without sending a single pixel over the network.

---

## 🎯 Architecture & Data Flow

```mermaid
flowchart TD
    subgraph Capture [Android Camera Pipeline]
        Cam[CameraX PreviewView] -->|ImageAnalysis Frame| Analyzer[CameraFrameAnalyzer]
        Analyzer -->|Throttled ~8 FPS / Portrait Bitmap| Bridge[Bitmap to Packed RGB]
    end

    subgraph CoreEngine [On-Device Inference]
        Bridge -->|Raw RGB Buffer| Engine[libcactus / Mirage VLM]
        Weights[(mirage.cact\n~22 MB Weights)] -.->|Loaded on Init| Engine
        Engine -->|Sub-100ms Single-Pass| JsonOut[Structured JSON Perception]
    end

    subgraph Presentation [Jetpack Compose UI]
        JsonOut --> State[CameraViewModel Flow]
        State --> BoxOverlay[BoundingBox & OCR Overlay]
        State --> Hud[Telemetry & Mode HUD]
        State -.->|Optional| LocalLLM[Local 0.5B Text SLM\nMediaPipe / llama.cpp]
    end
```

---

## 🚀 Key Features

* **⚡ Real-Time Camera HUD:** Live CameraX pipeline with real-time bounding box tracking and OCR text recognition rendered directly onto Compose Canvas.
* **🔒 100% Private & Offline:** Camera frames never leave device memory. Telemetry is opt-in and disabled by default.
* **🔋 Battery-Conscious Throttling:** Smart frame sampling (8–10 FPS) prevents thermal throttling and minimizes battery drain.
* **🧩 Modular & Extensible:** Built on clean MVI/MVVM architecture with pluggable `VisionMode` strategies for community contributions.

---

## 🗺️ Roadmap & Contribution Areas

We welcome contributions of all levels! Here is our current roadmap:

- [x] **Phase 1: Foundation (Current MVP)**
  - [x] CameraX integration with thread-safe `CameraFrameAnalyzer`
  - [x] Mirage engine contract & RGB buffer serializer
  - [x] Compose Canvas bounding box overlay with confidence scores
  - [x] Performance telemetry HUD (FPS, inference latency in ms)
  - [x] Freeze/Pause frame inspector
- [ ] **Phase 2: Smart Extraction & Forms**
  - [ ] Structured receipt, invoice, and business card schema parsing
  - [ ] One-tap export to JSON / Room DB / CSV
  - [ ] Document boundary detection & perspective correction
- [ ] **Phase 3: Assistive & Voice Mode**
  - [ ] Android Text-to-Speech (TTS) integration for spatial narration
  - [ ] Haptic feedback when objects enter center focus
  - [ ] Offline audio cues for visually impaired navigation
- [ ] **Phase 4: On-Device Multimodal Copilot**
  - [ ] Pipe Mirage JSON perceptions into local SLMs (e.g. Qwen-2.5-0.5B via MediaPipe or llama.cpp)
  - [ ] Conversational Q&A about what the camera sees (*"What type of cable is this?"*)
- [ ] **Phase 5: Background Gallery Indexing**
  - [ ] WorkManager batch indexing for device photos
  - [ ] Local semantic search (find photos by natural language description)

---

## 🛠️ Project Structure

```
eko-vision/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/eko/vision/
│   │       ├── EkoVisionApp.kt               # App class (Engine initialization)
│   │       ├── MainActivity.kt               # Edge-to-edge entry point
│   │       ├── core/
│   │       │   ├── camera/
│   │       │   │   └── CameraFrameAnalyzer.kt # CameraX frame analyzer & throttling
│   │       │   ├── engine/
│   │       │   │   └── MirageEngine.kt       # Bridge to Cactus Mirage weights
│   │       │   └── model/
│   │       │       └── MiragePerception.kt   # Perception data classes & JSON models
│   │       └── presentation/
│   │           ├── camera/
│   │           │   ├── CameraScreen.kt       # Main camera compose screen
│   │           │   ├── CameraViewModel.kt    # UI state & frame coordinator
│   │           │   └── components/
│   │           │       ├── BoundingBoxOverlay.kt # Compose Canvas overlays
│   │           │       └── PerceptionHud.kt      # Telemetry & controls HUD
│   │           └── theme/
│   │               └── Theme.kt              # Sleek HUD Material3 dark theme
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml                   # Gradle Version Catalog
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE                                  # Apache 2.0 License
├── CONTRIBUTING.md                          # Contribution guidelines
└── README.md
```

---

## 💻 Getting Started

### Prerequisites
* **Android Studio Ladybug (2024.2+)** or newer
* **JDK 17** configured in Gradle
* Android Device with **API 26+ (Android 8.0+)** and ARM64 architecture (physical device recommended for CameraX and NPU acceleration)

### Build & Run
1. **Clone the repository:**
   ```bash
   git clone https://github.com/sohamdalwadi/eko-vision.git
   cd eko-vision
   ```
2. **Obtain Mirage Model Weights (`mirage.cact`):**
   * Download the official 22MB Mirage weights from [Cactus Compute Hugging Face](https://huggingface.co/Cactus-Compute/mirage).
   * Place `mirage.cact` in `app/src/main/assets/` (or let the app download it dynamically).
3. **Open in Android Studio:**
   * Sync Gradle with Project Files.
   * Connect an Android device with USB debugging enabled.
   * Click **Run (Shift + F10)**.

---

## 🤝 Contributing

We love contributions! Whether you want to fix a bug, improve UI performance, add new vision modes, or write documentation:

1. Read our [Contribution Guide](CONTRIBUTING.md).
2. Fork the repo and create your feature branch:
   ```bash
   git checkout -b feature/awesome-vision-mode
   ```
3. Commit your changes and open a Pull Request.

---

## 📄 License

This project is licensed under the **Apache License 2.0** — matching the upstream Cactus Compute license. See the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <sub>Built with ❤️ by the open-source community. Special thanks to the Cactus Compute team for pioneering micro-VLM edge intelligence.</sub>
</div>
