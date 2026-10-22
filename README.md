<div align="center">

---

<img width="992" height="397" alt="Nexus-Bodycam Cyberpunk Recording HUD (2)" src="https://github.com/user-attachments/assets/94e9e55f-62d6-4bae-8eb5-b9d1071057c3" />

---
# 📹 NEXUS Bodycam

**Turn Minecraft into a first-person bodycam experience.**
Chest-mounted camera, breathing you can hear, lens effects and full weapon realism.

`Minecraft 1.20.1` · `Fabric` · `Client-side` · `v1.4.3+1.20.1`

[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen.svg)](https://fabricmc.net/game/version/1.20.1/)
[![Loader: Fabric](https://img.shields.io/badge/loader-Fabric-DBD2A8.svg)](https://fabricmc.net/use/)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.org/)
[![AI disclosure](https://img.shields.io/badge/AI-disclosed-yellow.svg)](AI-TRANSPARENCY.md)

[Install](#installation) · [Build](#building-from-source) · [Configuration](#configuration) · [Wiki](../../wiki) · [Issues](../../issues) · [AI disclosure](AI-TRANSPARENCY.md) · [Site](https://jhfuheuiwfh.github.io/nexus-bodycam-mod/)

</div>

---

## Preview

| | |
|---|---|
| ![Smooth steps](gameplay/smooth-steps.gif) | ![Flying camera shake](gameplay/flying.gif) |
| **Smooth steps** — audio-synced bob + step damping | **Flying shake** — the camera shakes hard while you fly |
| ![Roll & motion blur](gameplay/roll&motion-blur.gif) | ![Gun realism](gameplay/gun.gif) |
| **Real roll** — true banking, angular motion blur | **TACZ gun spring** — recoil, sway, lowering |

---

## What it does

NEXUS Bodycam is a **pure client-side camera overhaul**: it changes how the camera *feels and looks* — never movement speed, hitboxes, raycasts or server logic.

### 🦶 Camera motion
- **Audio-synced footsteps** — one bounce per footstep, locked to Minecraft's own step sounds (no guess-timer).
- **Step smoothing & landing damping** — a vertical spring eats stairs, slabs and jumps.
- **Rotational landing jolt** — big falls kick pitch and roll proportionally.
- **Realistic head bob** — cosine-based, phase-locked (vanilla bobbing can be disabled).
- **Idle micro-tremor** — Perlin noise + 8 Hz GoPro-style vibration that scales with movement.
- **Sprint/strafe lean & yaw inertia** — the view lags your mouse slightly, like a device on a vest.
- **True roll** — injected into the world render matrix (±10°), not a fake rotation.
- **Chest camera mode** — camera dropped to chest height, pitch tilt, wide-FOV multiplier.

### 🫁 Breathing system
- **Synchronized breath audio** — 3.06 s cycle, exhale at the exact midpoint, matched to the included OGG.
- **Asymmetric shape** — quick inhale, smooth exhale, Perlin irregularity.
- **Sprint exhaustion** — +35 % after 5 s, up to +65 % after 20 s; volume and pitch rise, resets when you stop.

### 💥 Damage & impact
- Damage shake scaled to the hit, landing spring (pitch + alternating per-foot roll), impact recovery sequence.

### 🔧 Post-processing lens pipeline
Fisheye (K1/K2) · wide-FOV multiplier · radial chromatic aberration · dual vignette · lens dirt/veiling glare · film grain (quantized at ~30 fps) · 17-tap depth of field for the config menu · auto exposure · flash shock · lens contamination · angular motion blur · volumetric dust · low-light sensor noise.

### 🌊 Realistic water
Surface waves, procedural caustics, foam, refraction, underwater fog + RGB tint.

### 🔫 TACZ integration *(optional)*
Visual-only gun realism — **never** touches raycasts, bullets or sights: second-order gun spring with overshoot, weapon lowering, run sway, recoil kick, global view smoothing. Without TACZ the mixins disable themselves automatically.

### 🎨 Shader compatibility
Designed to work *with* bodycam shaderpacks: the mod owns the **camera motion**, the shader owns the **lens look**. "Sync with Shader" reduces shake/bob when Iris detects a shaderpack, and shadow passes are skipped so post-FX never corrupts them.

### 🎛 8 presets
`Subtle` · `Normal` · `Intense` · `EXTREME` · `Documentary` · `Horror` · `Tactical` · `Raw GoPro`

---

## Installation

📥 **Download:** [**latest release**](../../releases/latest) — the buttons and links always resolve automatically to the newest published JAR (`NEXUS-bodycam-*+1.20.1.jar`); no per-version edits needed.

1. Install **[Fabric Loader](https://fabricmc.net/use/installer/)** for Minecraft **1.20.1**.
2. Drop `NEXUS-bodycam-1.4.3+1.20.1.jar` into your `mods` folder.
3. *(Optional)* **[Mod Menu](https://modrinth.com/mod/modmenu)** for the in-game config screen, **[TACZ](https://modrinth.com/mod/tacz)** for weapon realism, **[Iris](https://modrinth.com/mod/iris)** for shaders.

**Requirements:** Fabric Loader ≥ 0.15.11 · Fabric API · Java 17+.
Full walkthrough (with troubleshooting): **[Installation Wiki page](../../wiki/Installation)**.

---

## Building from source

```bash
git clone https://github.com/jhfuheuiwfh/nexus-bodycam-mod.git
cd nexus-bodycam-mod
```

1. **JDK 17 or 21** on `JAVA_HOME` (any modern JDK works — it compiles targeting Java 17).
2. Put the **TACZ jar in `libs/`** — it is a third-party binary, so it is *not* in this repo (see [`libs/README.md`](libs/README.md)).
3. Build:

```bash
./gradlew build          # Windows: gradlew.bat build
```

The result is `build/libs/NEXUS-bodycam-1.4.3+1.20.1.jar`.
More detail: **[Building Wiki page](../../wiki/Building)**.

---

## Configuration

Open **Mod Menu → NEXUS Bodycam → Settings**. The screen scrolls and is organized in categories:

| Category | Options |
|---|---|
| Smoothing | step smoothing, landing damping, view smoothing |
| Shaking | idle shake amount & frequency |
| Bobbing | amount, frequency, roll lean |
| Weapon & View | TACZ spring, lowering, run sway, recoil, view smooth |
| Camera | chest camera, height, pitch, wide FOV |
| Post-FX | fisheye K1/K2, vignette, chromatic aberration, lens dirt, grain |
| Water | waves, caustics, refraction, underwater fog & tint |
| Compatibility | sync with shader, disable vanilla bobbing |

Settings are saved to `config/bodycam.json` with validation — invalid or missing values fall back to safe defaults.

---

## Compatibility

| Mod | Status |
|---|---|
| Fabric API | **Required** |
| Mod Menu | Optional — config screen |
| TACZ | Optional — weapon realism |
| Iris / Sodium | Compatible (shadow-pass guarded) |
| Bodycam-style shaderpacks | Compatible (*Sync with Shader*) |
| Distant Horizons | Compatible |

**Performance** (measured with every Post-FX enabled, 32-chunk render/simulation distance): **75–83 FPS avg** on a Ryzen 5 5600X + RX 580 8 GB + 16 GB DDR4.

---

## Third-Party Credits & Acknowledgements

NEXUS Bodycam is built for the Minecraft modding ecosystem and acknowledges the developers and contributors behind the third-party projects listed below. These projects are independent of NEXUS Bodycam and retain their own authorship, licenses, and distribution terms.
Dependencies & Integrations

- **[Fabric Loader](https://github.com/FabricMC/fabric-loader)** — FabricMC contributors. Mod-loading platform.

- **[Fabric API](https://github.com/FabricMC/fabric-api)** — FabricMC contributors. Required modding library.

- **[Mod Menu](https://github.com/TerraformersMC/ModMenu)** — Mod Menu contributors. Optional configuration-screen integration.

- **[TaCZ](https://github.com/MCModderAnchor/TACZ)** — Timeless and Classics Zero — TaCZ developers and contributors. Optional weapon-animation and visual integration.
Rendering & Compatibility

- **[Iris Shaders](https://github.com/IrisShaders/Iris)** — Iris developers and contributors. Shader compatibility and integration support. 

- **[Sodium](https://github.com/CaffeineMC/sodium)** — JellySquid and Sodium contributors. Rendering optimization and compatibility within the supported mod ecosystem.

- **[Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons)** — Distant Horizons developers and contributors. Compatibility support.

Attribution & Licensing. All third-party projects remain the property of their respective authors and are governed by their own licenses and distribution terms. Please consult each project's official page or source repository for full licensing information.
NEXUS Bodycam does not claim authorship of these projects. Compatibility, integration, or acknowledgement does not imply endorsement by their developers. Unless explicitly stated otherwise, these mods are separate downloads and are not included in the NEXUS Bodycam distribution.
Any third-party code, assets, textures, sounds, or other materials distributed with NEXUS Bodycam must retain the applicable permissions, notices, and license requirements. Shaderpacks used alongside NEXUS Bodycam are independent projects and should be credited to their respective creators.
**[Minecraft](https://www.minecraft.net/en-us)** is a trademark of **Mojang Studios** and **Microsoft**. NEXUS Bodycam is an independent project and is not affiliated with, endorsed by, or sponsored by Mojang Studios or Microsoft.

---
## Project layout

```
├── src/main/java/dev/bodycam/   # camera, effects, config, GUI, mixins
├── src/main/resources/          # shaders (post, dofblur), sounds, textures, lang
├── sounds/  textures/           # source assets (WAV master, art originals)
├── libs/                        # local third-party jars (git-ignored)
├── docs/                        # CHANGELOG.md · DEVNOTES.md
├── TODO.md                      # roadmap & pending work
├── AI-TRANSPARENCY.md           # how AI was used in this project
└── MODRINTH.md                  # Modrinth store description
```

---

## Disclosures

This project is **substantially AI-generated** and says so openly: a large share of the
**code, shaders, assets and documentation** was produced by AI coding agents
(OpenCode sessions), with the human author directing the work, testing in-game and
making the final calls. Some of the code was written from scratch and **modified by
hand** by the author, so the codebase is a mix of AI and human work.

Full details — what AI did, what the human did — are in **[AI-TRANSPARENCY.md](AI-TRANSPARENCY.md)**.
The same disclosure is used on the Modrinth page (content rating → *Contains AI-generated content*).

## **PHOTOSENSITIVITY WARNING:** This project may contain flashing lights.

---

## License

Released under the **[GNU General Public License v3.0](LICENSE)**.

## Support

Feedback and bug reports welcome:

- **Email:** [stableuser1223@gmail.com](mailto:stableuser1223@gmail.com?subject=NEXUS-bodycam-mod%20feedback)

**Links:** [Repository](../../) · [Issue tracker](../../issues) · [Wiki](../../wiki) · [Changelog](docs/CHANGELOG.md) · [Dev notes](docs/DEVNOTES.md)
