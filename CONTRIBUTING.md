# Contributing to Eko Vision

Thank you for your interest in contributing to **Eko Vision**! 🚀  
We are building a scalable, community-driven on-device vision playground for Android powered by micro-VLMs like **Mirage (22MB)**.

---

## 🎯 How Can You Contribute?

You can contribute in many ways:
* **New Vision Modes:** Add specialized analyzers (e.g. Document scanner, Nutrition facts extractor, Accessibility voice reader).
* **UI/UX Enhancements:** Improve the Compose Canvas bounding box animations, telemetry HUD, or gesture interactions.
* **Performance & NDK:** Optimize image buffer conversions (`ImageProxy` -> RGB) using RenderScript replacement, Libyuv, or Vulkan.
* **Integrations:** Connect Mirage's perception JSON to local text SLMs (e.g., MediaPipe LLM / llama.cpp).
* **Documentation & Tutorials:** Help make the documentation and setup instructions crystal clear.

---

## 🛠️ Development Workflow

### 1. Fork & Clone
```bash
git clone https://github.com/<your-username>/eko-vision.git
cd eko-vision
git checkout -b feature/my-new-feature
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
* Open a PR against the `main` branch.
* Describe what your PR adds or fixes, including screenshots/recordings if there are UI changes.
* Link any related issues.

---

## 📜 License
By contributing to Eko Vision, you agree that your contributions will be licensed under its **Apache License 2.0**.
