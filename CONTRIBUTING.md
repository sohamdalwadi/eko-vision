# Contributing to Eko Vision

Thank you for your interest in contributing to **Eko Vision**! 🚀  
We are building a scalable, community-driven on-device vision playground for Android powered by micro-VLMs like **Mirage (22MB)**.

---

## 🌿 Branching Strategy & Contribution Flow

We use a two-branch workflow to maintain stability while enabling rapid community iteration:

```
feature/your-feature
        │
        ▼ (Pull Request)
      [ dev ]  ──────► Automated CI builds "Beta APK"
        │
        ▼ (Merge when tested & stable)
     [ main ]  ──────► Automated CI builds "Stable APK"
```

1. **`main` (Stable):** Production-ready code. Direct pushes are restricted.
2. **`dev` (Active Development):** Integration branch for ongoing work. **All pull requests must target `dev`.**
3. **`feature/<name>` or `fix/<name>`:** Create your branch from `dev`.

---

## 🤖 Automated CI Verification

When you submit a Pull Request targeting `dev` or `main`:
* **GitHub Actions** automatically checks out your PR, sets up JDK 17, and runs `./gradlew assembleDebug`.
* If your changes cause a compilation error or break dependencies, the PR check will fail.
* Reviewers can verify the temporary PR APK artifact directly before merging.
* Merging to `dev` automatically generates a new **Beta APK** under the `beta-latest` GitHub Release.
* Merging to `main` automatically updates the **Stable APK** under `stable-latest`.

> [!NOTE]
> **Zero Cost & Minimal Storage:** Build artifacts are retained for only 24 hours, and old workflow runs are pruned automatically. This keeps the project lightweight and well within free limits.

---

## 🎯 Contribution Areas

You can contribute in many ways:
* **New Vision Modes:** Add specialized analyzers (e.g. Document scanner, Nutrition facts extractor, Accessibility voice reader).
* **UI/UX Enhancements:** Improve the Compose Canvas bounding box animations, telemetry HUD, or gesture interactions.
* **Performance & NDK:** Optimize image buffer conversions (`ImageProxy` -> RGB) using RenderScript replacement, Libyuv, or Vulkan.
* **Integrations:** Connect Mirage's perception JSON to local text SLMs (e.g., MediaPipe LLM / llama.cpp).
* **Documentation & Tutorials:** Help make the documentation and setup instructions crystal clear.

---

## 🛠️ Step-by-Step Development Guide

### 1. Fork & Branch
```bash
# Clone your fork
git clone https://github.com/<your-username>/eko-vision.git
cd eko-vision

# Checkout the dev branch
git checkout dev

# Create a feature branch
git checkout -b feature/awesome-vision-mode
```

### 2. Code Standards
* **Kotlin Style:** Follow official [Android Kotlin Style Guidelines](https://developer.android.com/kotlin/style-guide).
* **Jetpack Compose:** Keep composables stateless where possible, hoist state to `CameraViewModel`, and avoid unnecessary recompositions in the high-frequency camera loop.
* **No Cloud Leaks:** All frame data and perception logic must remain strictly **100% on-device**. No cloud telemetry or image uploading.

### 3. Commit Guidelines
Write clear, conventional commit messages:
* `feat: add document perspective correction mode`
* `fix: prevent camera analyzer buffer leak during freeze`
* `docs: update setup instructions for NPU acceleration`
* `perf: optimize bitmap to rgb byte conversion`

### 4. Opening a Pull Request
1. Push your branch to your fork:
   ```bash
   git push -u origin feature/awesome-vision-mode
   ```
2. Open a Pull Request targeting the **`dev`** branch of `sohamdalwadi/eko-vision`.
3. Provide a clear description and any screenshots/recordings if UI was modified.
4. Verify that the GitHub Actions build check passes.

---

## 📜 License
By contributing to Eko Vision, you agree that your contributions will be licensed under its **Apache License 2.0**.
