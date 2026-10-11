# NEXUS Bodycam

> 🤖 **AI disclosure:** this project contains AI-generated content — see the
> [AI transparency statement](AI-TRANSPARENCY.md) at the end of this document.

**Turn Minecraft into a first-person bodycam experience.** Mount the camera on your chest and feel every step, every breath, every hit — with professional-grade lens effects, depth of field, and full weapon realism integration.

NEXUS Bodycam is a **client-side** camera overhaul for Minecraft Java Edition **1.20.1 (Fabric)**. It rebuilds the first-person camera from the ground up using a real-time temporal effects engine: footsteps synced to the actual in-game audio, physically-modelled springs, Perlin-driven idle micro-tremor, realistic breathing with sound, damage and landing shakes, and a complete post-processing lens pipeline (fisheye, chromatic aberration, vignette, lens dirt, film grain, depth of field).

Everything is fully configurable in-game through a scrollable config screen with **eight presets** — from a subtle documentary feel to full EXTREME shake, plus dedicated looks for documentary, horror, tactical and raw GoPro footage.

---

## Camera Effects — feel the ground

- **Audio-synced footsteps** — the camera bounces exactly once per footstep, locked to Minecraft's own step sounds (not a guess-timer). Walk, sprint, strafe: the cadence always matches what you hear.
- **Step smoothing & landing damping** — a vertical spring absorbs stair steps, slab edges and jumps so the horizon never jerks.
- **Rotational landing jolt** — land from a cliff and the camera kicks in pitch and rolls side-to-side, proportional to the fall. Damping is configurable.
- **Realistic head bob** — smooth cosine-based bobbing with phase locking (no visual pops), vertical + roll + yaw components. Vanilla bobbing can be fully disabled to avoid double motion.
- **Idle micro-tremor** — a subtle Perlin noise shake plus an 8 Hz GoPro-style micro-vibration that scales with movement, so the camera never feels dead.
- **Sprint lean & strafe lean** — the camera tilts into turns (up to 1.8°) and leans when strafing, with yaw inertia: the view lags your mouse slightly, like a device hanging on a vest.
- **Real roll** — true banking injected into the world render matrix (not a fake rotation), clamped ±10°.
- **Chest camera mode** — offset the camera down to chest height with a pitch tilt and wide-FOV multiplier for a true vest-mounted perspective.

## Breathing System — you can hear it

- **Synchronized breath audio** — a 3.06 s breath cycle with the exhale starting exactly at the midpoint, matched to the included `breath-in-out` sound (mono OGG). Visuals and audio stay locked together.
- **Asymmetric breathing shape** — quick inhale, smooth exhale, plus Perlin irregularity (real breathing isn't a perfect sine).
- **Exhaustion from sprinting** — keep running and your breathing rate ramps: +35% after 5 seconds, up to +65% after 20 seconds (progressive). Sound volume and pitch rise with exhaustion; it resets when you stop.
- **Smart gating** — you only hear and feel heavy breathing when it makes sense: standing still = calm, walking = mostly gated, exhausted = heaving.

## Damage & Impact

- **Damage shake** — any drop in health triggers a short Perlin burst on yaw/pitch, scaled by the damage amount (clamped so one hit doesn't nauseate you).
- **Landing impact** — vertical sink + rotational spring (pitch and alternating per-foot roll): bigger fall, bigger jolt.

## Post-Processing Lens Pipeline (Post-FX)

A custom core shader runs at the tail of the world render, with every parameter exposed in the config:

- **Fisheye / barrel distortion** (K1, K2 coefficients) — the signature bodycam look.
- **Wide FOV multiplier** — push the field of view beyond vanilla limits.
- **Radial chromatic aberration** — red/blue fringing at the frame edges, like a cheap wide lens.
- **Dual vignette** — cinematic cosⁿ falloff plus a mechanical (rounded-rectangle) vignette with adjustable floor and center gain.
- **Lens dirt / veiling glare** — a real grime overlay texture modulates light bloom, so bright scenes reveal the smudges on your lens.
- **Film grain** — sensor-style grain with configurable strength and cell size, quantized at ~30 fps for an authentic recording feel.
- **True depth of field** — a 17-tap Gaussian blur pass (per-screen copy, no feedback loops) used for the config menu background; the menu UI renders sharp on top of the blurred scene.

## Realistic Water

- **Surface waves** with height, speed and frequency controls.
- **Procedural caustics** and foam on the surface.
- **Refraction** through the water plane.
- **Underwater fog + tint** — RGB tint and density controls for that murky first-person dive.

## Full TACZ Integration (optional)

If [TACZ - Timeless and Classics Zero](https://modrinth.com/mod/tacz) is installed, NEXUS Bodycam adds **visual-only** gun realism (never touches raycasts, bullets or sights):

- **Gun spring** — a second-order spring on the weapon model: flick 90° and it overshoots (~102°) before settling. Tunable intensity, speed and overshoot.
- **Weapon lowering spring** — the gun eases down when idle and rises smoothly.
- **Run sway** — the weapon lags and sways while sprinting.
- **Recoil kick** — a decaying kick on the gun model when firing (sprint-fire triggers a light kick too).
- **Global view smoothing** — the rendered camera follows your mouse with exponential smoothing (visual only; your real aim is untouched).

Without TACZ, all TACZ mixins are disabled automatically — the mod loads normally.

## Shader Compatibility (Iris / OptiFine)

- Designed to work alongside bodycam-style shaderpacks: the mod owns the **camera motion**, the shader owns the lens look — no double-processing.
- **Sync with Shader** (default ON): detects an active shaderpack and reduces shake/bob amplitude to avoid double motion sickness, and reports `motionBlurBoost` / `swayIntensity` to the shader through an Iris compatibility bridge.
- Shadow passes are detected and skipped, so post-FX never corrupts shadow rendering.

## Configuration

Open the config screen from **Mod Menu** (or the in-game controls). The screen is **scrollable** with a compact adaptive layout and a blurred background, organized in categories:

- **Smoothing** — step smoothing, landing damping, view smoothing
- **Shaking** — idle shake amount & frequency
- **Bobbing** — bobbing amount, frequency, roll lean
- **Weapon & View** — TACZ spring, lowering, run sway, recoil, view smooth
- **Camera** — chest camera, height, pitch, wide FOV
- **Post-FX** — fisheye K1/K2, vignette, chromatic aberration, lens dirt, grain
- **Water** — waves, caustics, refraction, underwater fog & tint
- **Compatibility** — sync with shader, disable vanilla bobbing

**Presets:** `Subtle` · `Normal` · `Intense` · `EXTREME` (with a motion-sickness warning) · `Documentary` · `Horror` · `Tactical` · `Raw GoPro`.

Each preset tunes motion, optics, exposure, glow, lens dust and the critical overlay in one click:

- **Documentary** — steady handheld: mild bob/inertia, FOV 1.10, light chromatic aberration.
- **Horror** — unstable sensor: heavy idle shake (1.2 Hz), deep vignette, grain and dust pushed up.
- **Tactical** — readable sight picture: minimal wobble (idle 0.12), flat lens, effects kept out of the way.
- **Raw GoPro** — widest lens: FOV 1.40, strong fisheye (K1 0.32), visible chromatic aberration.

Settings save to `config/bodycam.json` with validation — invalid or missing values fall back to safe defaults.

## Technical Notes

- **Fully frame-based**: all effects run every rendered frame with real delta time (`System.nanoTime`, clamped), so behavior is identical at 20 or 200 FPS — even with heavy shaders.
- **Client-side only** — safe on any server, no server installation needed, no network traffic.
- **Diagnostics**: optional `[diag]` log line every 60 ticks (steps, cadence, breath rate, spring values) for debugging and tuning.
- Lightweight: a single singleton state object and a handful of mixins; no per-tick world scans.

## Tested on (v1.4.3+1.20.1)

Measured in-game with **every Post-Effect enabled**:

| | |
|---|---|
| CPU | **AMD Ryzen 5 5600X** |
| RAM | **16 GB DDR4 — 3200 MHz** |
| GPU | **AMD Radeon RX 580 2048 SP — 8 GB VRAM** |
| Render distance | **32 chunks** |
| Simulation distance | **32 chunks** |
| Post-FX | **all post-effects ON** (fisheye, chromatic aberration, vignette, lens dirt, grain, DOF…) |
| Result | **75 – 83 FPS average** |

## Installation

1. Install **Fabric Loader** for Minecraft 1.20.1.
2. Drop `NEXUS-bodycam-1.4.3+1.20.1.jar` into your `mods` folder.
3. (Optional) Install **Mod Menu** to access the config screen from the mods list, and **TACZ** for weapon realism.

**Requirements:** Fabric Loader ≥ 0.15.11 · Minecraft 1.20.1 · Fabric API. Mod Menu and TACZ are optional.

## Compatibility

| Mod | Status |
|---|---|
| Fabric API | Required |
| Mod Menu | Optional — config screen integration |
| TACZ | Optional — full weapon realism |
| Iris / Sodium | Compatible (shadow-pass guarded) |
| Bodycam-style shaderpacks | Compatible (Sync with Shader) |
| Distant Horizons | Compatible (rotation copy, no shared-state mutation) |

## Scope of the mod

NEXUS Bodycam changes only how the camera **feels and looks**. It does not modify movement speed, hitboxes, raycasts, server logic, or any gameplay balance — it is a pure immersion/visual mod.

---

## 🤖 AI disclosure

**☑ Contains AI-generated content** — *Yes, this project contains AI-generated content.*

| Generated with AI | |
|---|---|
| ☑ **Code** | |
| ☑ **Assets** | |
| ☑ **Text** | |

**Explanation:**
> Mostly AI-generated code and assets, with some user-made code! Some of the
> code was also modified by hand by the author.

*(Full statement: [AI-TRANSPARENCY.md](AI-TRANSPARENCY.md) in the repository.)*

---

**License:** GPL-3.0
**Source code:** https://github.com/jhfuheuiwfh/nexus-bodycam-mod
**Made for:** Minecraft Java 1.20.1 · Fabric · client-side
