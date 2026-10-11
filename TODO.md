<div align="center">

# 🗺️ NEXUS Bodycam — TODO & Roadmap

**Current version:** `1.4.2+1.20.1` · [Repository](../../) · [Issues](../../issues) · [Wiki](../../wiki)

</div>

---

## ✅ Done

<details>
<summary><b>Core engine</b> — click to expand</summary>

- [x] Temporal effects architecture (frame-based, real delta time)
- [x] Real roll injected into the world render matrix
- [x] Steps synchronized with the vanilla audio (`EntityMixin` + `-cos` phase)
- [x] Step smoothing, landing damping, rotational landing jolt
- [x] Idle micro-tremor (Perlin + 8 Hz GoPro vibration)
- [x] Realistic breathing + synchronized OGG + sprint exhaustion (5 s / 20 s)
- [x] Damage shake + impact recovery sequence
- [x] Camera inertia (2nd-order view spring) + gait transition
- [x] Chest camera mode + wide FOV

</details>

<details>
<summary><b>Post-FX & optics</b> — click to expand</summary>

- [x] Core shader `bodycam:post` — fisheye, chromatic aberration, vignette, lens dirt, grain
- [x] True depth of field (`bodycam:dofblur`, 17 taps) on the config menu background
- [x] Auto exposure + flash/brightness shock
- [x] Low-light sensor noise (C4)
- [x] Lens contamination — drops/condensation/impact smudges (C5)
- [x] Surface-aware footsteps + underwater lens response (C6)
- [x] Lens ghosting, lens profile, volumetric dust, critical damage camera (C7)
- [x] Angular motion blur (C8)
- [x] Realistic water: waves, caustics, refraction, fog/tint

</details>

<details>
<summary><b>TACZ integration</b> — click to expand</summary>

- [x] Visual gun spring + view smoothing + plugin gate
- [x] Weapon lowering, run sway, recoil kick
- [x] Phase D — weapon inertia, ADS stabilization, movement-to-aim, wall proximity, reload response

</details>

<details>
<summary><b>UI, config & polish</b> — click to expand</summary>

- [x] Config screen rebuilt (scroll + viewport clipping + custom sliders + preset bar)
- [x] 8 presets incl. `DOCUMENTARY` / `HORROR` / `TACTICAL` / `RAW GOPRO`
- [x] Crosshair / block-break alignment fixed (`getRotationVec`)
- [x] v1.4.2 aim vector sign fix
- [x] Render corruption fixed (sampler renamed to `ScreenTex`)
- [x] Logo (SVG → `icon.png 256`), texts in EN, Modrinth description
- [x] Build pipeline + `gradlew` wrapper + `.gitignore`

</details>

<details>
<summary><b>Repo & publishing</b> — click to expand</summary>

- [x] GitHub repository (`nexus-bodycam-mod`, public)
- [x] Proper `LICENSE` (GPL-3.0) + clean `README.md`
- [x] `.gitignore` — no build junk, caches or third-party jars
- [x] Changelog & dev notes moved to `docs/`
- [x] AI disclosure (`AI-TRANSPARENCY.md`, README, Modrinth listing)
- [x] Wiki (install / build / configuration / FAQ)
- [x] Demo GIFs in `media/`
- [x] GitHub Release `v1.4.2+1.20.1` with the jar attached

</details>

---

## 🧪 Playtesting — high priority

The features below are **implemented but not fully verified in-game**:

- [ ] **v1.4.2** — scroll the config screen top→bottom; aim UP and mine: the cracking block must be the one under the crosshair (chest cam ON *and* OFF)
- [ ] Breathing (sound + visual), sprint exhaustion at 5 s / 20 s
- [ ] TACZ feel: spring, lowering, sway, recoil
- [ ] Damage shake + fall landing (incl. wall-collision nod)
- [ ] Post-FX + GUI DOF + UI scroll (world not black, no sliced textures)
- [ ] Mod Menu screen opens and saves config
- [ ] Presets: `HORROR` and `RAW GOPRO` read clearly different; GUI saves + reloads
- [ ] Auto exposure: cave → surface fast, surface → cave slow, no weirdness at night / under Iris
- [ ] Motion blur only while turning; grain rises in the dark; lens drops in rain/underwater
- [ ] Low-HP instability, sun ghost flare, volumetric dust in light shafts

---

## 🛠 Maintenance

- [ ] Keep `docs/CHANGELOG.md` updated on **every** change (task without a changelog entry = task not done)
- [ ] Keep version in `gradle.properties`, `fabric.mod.json` and README in sync
- [ ] Consider CI (GitHub Actions) to build the jar automatically
- [ ] Issue templates / `CONTRIBUTING.md`
- [ ] Verify the wiki after each release
- [ ] ~~Fix rainbow ring (chromatic aberration too strong) in `textures/logo.svg`~~ — **cancelled** at the author's request, keep as-is

---

## 💡 Ideas (unscheduled)

- [ ] More presets / community preset sharing (`config/bodycam.json` import)
- [ ] Bodycam REC overlay (timestamp, battery, blinking dot)
- [ ] Optional `libs/` automation (script that fetches the TACZ jar)
- [ ] Localization pass beyond `en_us` / `pt_br`

---

<div align="center">

`[x]` done · `[ ]` pending · ~~struck-through~~ cancelled

</div>
