# Mirage On-Device VLM & Eko Vision Architecture

## Overview
Technical documentation for **Eko Vision**, an offline on-device mobile vision-language model (VLM) application using Cactus Compute's **Mirage** (21.6 MB).

---

## 1. Subsystem Architecture

```
+-----------------------------------------------------------------------------------+
|                           Native Android Application Layer                        |
|                                                                                   |
|  +------------------------------+       +--------------------------------------+  |
|  |     CameraX Hardware HAL     |       |       Jetpack Compose HUD & Canvas   |  |
|  | - TextureView (COMPATIBLE)   | ----> | - BoundingBoxOverlay (Paths, Arrows) |  |
|  | - 60 FPS Viewfinder          |       | - PerceptionHud (Capture States)     |  |
|  | - previewView.bitmap capture |       | - MirageShowcaseSheet (5-Tab M3)     |  |
|  +------------------------------+       +--------------------------------------+  |
|                 |                                          ^                      |
|                 v (Exact Still Bitmap)                     | (Parsed JSON)        |
|  +-----------------------------------------------------------------------------+  |
|  |             CameraViewModel (Capture State Machine Controller)              |  |
|  | - FRAMING:   0% CPU, 60 FPS preview, prominent shutter button               |  |
|  | - ANALYZING: Still frame frozen, radar progress pulse, single forward pass  |  |
|  | - RESULT:    Frozen photo + crisp Compose bounding boxes/masks/relations/OCR|  |
|  +-----------------------------------------------------------------------------+  |
|                                         |                                         |
|                                         v                                         |
|  +-----------------------------------------------------------------------------+  |
|  |                      Headless Mirage WebAssembly Runtime                    |  |
|  | - androidx.webkit.WebViewAssetLoader (Local offline sandbox)                |  |
|  | - Window-attached 1.dp host (maintains Chromium renderer pump)              |  |
|  | - Pre-allocated 16MB C memory scratchpad + 256x256x3 RGB buffer             |  |
|  | - cactus.wasm (1.2 MB) + mirage.cact (21.6 MB)                              |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 2. Solutions to Key Challenges

### Reliable Camera Viewfinder
- Use `PreviewView.ImplementationMode.COMPATIBLE` (`TextureView`) inside `AndroidView(factory = { ctx -> ... })` with `MATCH_PARENT`.
- Avoid instantiating `PreviewView` inside `remember { ... }` prior to window attachment.
- Removed continuous background analyzers to prevent frame buffer contention and CPU throttling.

### Pure Tap-to-Scan Flow
- **FRAMING:** 0% idle CPU, silky smooth 60 FPS framing, prominent camera shutter button.
- **ANALYZING:** On shutter click, captures `previewView.bitmap`, freezes the still frame immediately, and runs Mirage forward pass (~4.5s - 5.4s on CPU).
- **RESULT:** Captured still frame displayed with aligned bounding boxes, sub-pixel polygon masks, directional spatial arrows, and OCR highlight boxes.
- **New Scan:** Tapping "New Scan" clears the frozen bitmap and returns directly to live camera preview.

### Unambiguous Toggle Highlighting
- Custom `HudTogglePill`:
  - **Active:** Glowing colored border, tinted fill (`alpha = 0.22`), vibrant icon, bold white text, active dot indicator.
  - **Inactive:** Thin subtle border (`alpha = 0.20`), dark background (`alpha = 0.65`), dimmed grey icon and text (`alpha = 0.45`).
  - Colors: Cyan (`#00E5FF`), Lime (`#00E676`), Violet (`#D500F9`), Amber (`#FFD600`).

### Top Label Visibility
- When objects are located near the upper edge of the frame, label pill badges render inside the bounding box so they remain visible below the system status bar.

### Strict Design Guidelines
- Zero emojis anywhere in the UI or codebase. Strictly Material `ImageVector`s.
