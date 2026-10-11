# NEXUS Bodycam - Development Notes

> Historical working document (original README). It describes the build
> environment, the internal design, the status checklist and the effects
> research. The changelog lives in [CHANGELOG.md](CHANGELOG.md).

---


> "Bodycam"-style camera mod (GoPro/police vest) for Minecraft Java **1.20.1**,
> with temporal effects (step smoothing, idle shaking, realistic bobbing),
> **breathing with synchronized sound**, **damage/landing shakes**,
> **full TACZ integration** (gun spring + view smoothing),
> compatible with your **Bodycam Shaders**, and with **its own menu in Mod Menu**.
>
> **Current version: `1.4.2+1.20.1`** — installed build:
> `sha256 5B0120B49A82046104EAEE6B891675D75CFCE8AEDB16C3D4C55398A31C7360D8` (2 377 730 bytes)

---

## ⚠️ MANDATORY RULE FOR AGENTS

> **ANY agent modifying the mod MUST ALWAYS (MANDATORY) record
> ALL CHANGES in section 8 (CHANGELOG — ALL CHANGES) of this README,
> BEFORE considering the task complete.**
>
> - No exceptions: every change to code, config, sounds, mixins, GUI,
>   build or assets goes into the CHANGELOG (what + why + result).
> - Task without a CHANGELOG entry = **task not done**.
> - Also update the checklist (section 9) and the #TODO block (section 10)
>   when applicable.

---

## 1. User request (summary)

1. Create the **Bodycam** mod for **Minecraft Java 1.20.1** (Java 17.2+ / modern toolchain).
2. Effects based on **temporal technology via code/script**:
   - Step smoothing (step smoothing / landing damping)
   - Temporal idle shaking (idle micro-tremor)
   - Realistic bobbing (head bob with smooth curves, not a raw sine wave)
   - **Realistic breathing with audio** (`breath-in-out` sound synchronized with the visuals)
   - **Damage shake** and **rotational landing jolt**
3. Integration with **TACZ-Refabricated**: visual gun spring (overshoot) +
   view smoothing, without touching raycast/bullet/sights.
4. Bodycam visual effects **compatible with the already-installed Bodycam shader**
   (the mod only provides the camera movement; the shader provides lens/CA/blur/grain).
5. **Own menu in Mod Menu** (the "Settings" button opens the mod's config screen).

---

## 2. Build environment (verified on this machine)

| Item | Value |
|---|---|
| Base project (template) | `C:\Users\Adriano\Downloads\realistic-grass\realistic-grass` |
| Project (bodycam) | `C:\Users\Adriano\Downloads\bodycam` |
| README (this one) | `C:\Users\Adriano\Downloads\NEXUS-bodycam-mod\README.md` |
| JDK used | `D:\jdk-21.0.11+10` (JDK 21 — compiles targeting Java 17) |
| Java on PATH | `1.8.0_471` — **DO NOT USE** |
| Gradle | no `gradle` on PATH and no wrapper in the project → Gradle 8.5 extracted at `C:\Users\Adriano\AppData\Local\Temp\opencode\gradle-8.5` (`bin\gradle.bat`) |
| git | `D:\Git\cmd\git.exe` (git 2.55.0 on PATH) |
| Loom | `fabric-loom` 1.5-SNAPSHOT (resolves → 1.5.8) |
| ffmpeg | `C:\Users\Adriano\AppData\Local\Microsoft\WinGet\Links\ffmpeg.exe` (winget, used for WAV→OGG) |

### Build command (PowerShell, run at the project root)

```powershell
$env:JAVA_HOME='D:\jdk-21.0.11+10'
& 'C:\Users\Adriano\AppData\Local\Temp\opencode\gradle-8.5\bin\gradle.bat' clean build --no-daemon --console=plain
```

Expected output: `build\libs\NEXUS-bodycam-1.4.2+1.20.1.jar`

Installation: copy to
`C:\Users\Adriano\AppData\Roaming\FreesmLauncher\instances\1.20.1\minecraft\mods\`
(replace only the `NEXUS-bodycam-*.jar`; never touch third-party jars)
and verify with `Get-FileHash -Algorithm SHA256`.

**Cautions:**
- Never use the Java 1.8.0_471 from PATH.
- The 1st attempt may fail due to transient DNS on `repo.maven.apache.org` → rerun the build.
- **TACZ copy in `bodycam\libs\`** — JAR EDITED BY HAND, never restore from the original:
  - MANIFEST: lines `Fabric-Loom-Version:` and `Fabric-Gradle-Version:` removed
    (otherwise Loom 1.5.8 errors "Mod was built with a newer version of Loom (1.11.8)").
  - Removed the classes `com/tacz/guns/client/renderer/item/{Ammo,AnimateGeo,GunSmithTable,Attachment}ItemRenderer.class`
    (otherwise tiny-remapper reports "unfixable conflicts" render vs `class_756`/`method_3166`).
  - Dependency: `modCompileOnly fileTree(dir: 'libs', include: '*.jar')`
    (with plain `compileOnly` the Mixin AP reports "Unable to locate obfuscation mapping").
- Loom stores the named MC at:
  `%USERPROFILE%\.gradle\caches\fabric-loom\minecraftMaven\...1.20.1+build.10-v2\*.jar`
  → useful for `javap -p -c` (this is how APIs are proven).

---

## 3. Versions (gradle.properties)

```properties
org.gradle.jvmargs=-Xmx2G
minecraft_version=1.20.1
yarn_mappings=1.20.1+build.10
loader_version=0.15.11
fabric_version=0.92.2+1.20.1
modmenu_version=7.2.2
mod_version=1.4.2+1.20.1
maven_group=dev.bodycam
archives_base_name=NEXUS-bodycam
```

Mods present in the test `mods\` folder: TACZ-Refabricated 0.7.1-forge1.1.8-hotfix2 (id `tacz`),
Iris 1.7.6, Sodium 0.5.13, DistantHorizons 3.3.3, realistic-grass 1.0.0.

---

## 4. Mod Menu dependency

Source: <https://github.com/TerraformersMC/ModMenu> (official README, branch 1.20.1).

### build.gradle

```groovy
repositories {
    maven {
        name = "Terraformers"
        url = "https://maven.terraformersmc.com/"
    }
}

dependencies {
    modImplementation "com.terraformersmc:modmenu:${project.modmenu_version}"
}
```

> In projects with Loom for 1.20.1 the correct one is **`modImplementation`** (remaps to intermediary).

### fabric.mod.json — entrypoints

```json
{
  "entrypoints": {
    "client": ["dev.bodycam.BodycamClient"],
    "modmenu": ["dev.bodycam.BodycamModMenu"]
  }
}
```

### Integration class

```java
package dev.bodycam;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.bodycam.gui.BodycamConfigScreen;

public class BodycamModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> new BodycamConfigScreen(parentScreen);
    }
}
```

---

## 5. Mod design

### 5.1 Temporal architecture (the "core")

All effects run **per frame** (not per tick) with **real dt from `System.nanoTime()`
(clamped to 0.1s)** — FPS-independent (with shaders at ~20 fps nothing gets slower).

```
CameraState (singleton)
  ├─ stepOffsetY / stepVelocityY     // vertical spring for step/landing
  ├─ landKickPitch(+Vel) / landKickRoll(+Vel)   // ROTATIONAL fall jolt
  ├─ bobPhase / bobVertical / bobRoll / bobYaw
  ├─ footPhase / footSide / footInterval / lastFootstepT  // audio sync
  ├─ idleNoiseT / microT             // Perlin noise (idle + 8Hz micro-vib)
  ├─ rollSmooth / yawLag / leanPitch // roll, yaw inertia, lean
  ├─ breathT / breathGate / breathPitch / breathY  // breathing
  ├─ runTime / exhaustion / breathRateMult        // exhaustion from running
  ├─ prevHealth / damageShakeT / damageAmp / damageNoiseT  // damage shake
  ├─ viewDelta                       // view smoothing (TACZ)
  └─ lastYaw / lastPitch
```

**Per-frame loop (`BodycamEffects.onFrame`):**

1. Read the `Player` (horizontal speed, on ground, falling, yaw, health).
2. **Steps synchronized with the vanilla AUDIO**: mixin on
   `Entity.playStepSounds` (only reacts if `this == player`) → at the exact
   moment of the sound, kick the spring, flip the foot and lock the bob phase. Cadence comes
   from the game itself (EMA of the interval between sounds, `iv=` in the diag): 1.667 blocks/step
   ≈ 0.39 s walking / 0.30 s running. Temporal fallback (1.00 s walking /
   0.83 s running) only when there are 3 s without sounds (water, etc.).
3. `bobVertical = -cos(phase*2)` → each sound = exactly ONE full bounce
   at the bottom of the cosine (phase locks at k·π = minimum → no visual jumps).
4. Integrate springs: steps k=220 c=30 (kick 2.0), landing k=180 c=22.
5. Perlin idle shake (amplitude ~0.5°) + 8 Hz GoPro-style micro-vibration
   (scales with `swayIntensity` and speed).
6. Sprint lean (~1.8°), lateral lean while strafing, yaw
   inertia (camera lags −0.45·Δyaw, clamp ±5° → device "hung" on the body).
7. **Breathing** (see 5.6).
8. **Damage shake** (see 5.7).
9. Roll: lean when turning (clamp ±10°) + alternating foot roll.
10. Apply everything at render: position offset + rotations (roll injected into the
    MatrixStack of `renderWorld` — see 5.4).

### 5.2 Injection points (mixins)

| Mixin | Target class | Where | Function |
|---|---|---|---|
| `CameraMixin` | `net.minecraft.client.render.Camera` | `update(BlockView, Entity, boolean, boolean, float)` / `setRotation` / `getRotation` | applies idle shake + bobbing + roll; exp-smoothing of the view; `getRotation` returns a **copy** (does not mutate the shared quaternion) |
| `ClientPlayerEntityMixin` | `ClientPlayerEntity` | `tickMovement` | feeds the core per tick |
| `EntityMixin` | `net.minecraft.entity.Entity` | `playStepSounds` (private) | **footstep sound hook** → visual sync with audio |
| `GameRendererMixin` | `net.minecraft.client.render.GameRenderer` | `renderWorld(float, long, MatrixStack)` | roll on the view matrix + per-frame integration; `@Redirect` on bobView for `disableVanillaBobbing` |
| `TaczGunTransformMixin` | `FirstPersonRenderGunEvent` | `applyFirstPersonGunTransform(LocalPlayer, ItemStack, MatrixStack, BedrockGunModel, float)` HEAD | TACZ gun spring (visual only) |
| `BodycamMixinPlugin` | plugin | `shouldApplyMixin` | gate: `Tacz*` mixins only with `isModLoaded("tacz")` |

> Signatures verified with `javap` on the mapped jar (yarn 1.20.1+build.10) and
> jointly reviewed by the realistic-grass agent (3/3 + shadows OK).

### 5.3 Bodycam Shader compatibility

- The mod does **not** touch FOV, blur, grain, chromatic aberration, vignettes or
  lens distortion — that stays **100% up to the shader** (Iris/OptiFine).
- Option `syncWithShader` (default ON): with a shader active it reduces the
  shake/bob amplitude (avoids double motion sickness).
- `disableVanillaBobbing` (default ON): `@Redirect` on the vanilla bobView — no
  double bobbing; the mod takes full control.
- `motionBlurBoost` and `swayIntensity` are pushed to the shader via `IrisCompat`
  (EXTREME presets intensify camera + shader together).

### 5.4 Real roll (important fix)

`renderWorld` builds the view matrix with `Camera.getPitch()/getYaw()` and **never
calls `getRotation()`** — injecting roll only in getRotation rendered nothing.
Roll is now injected **into the MatrixStack of `renderWorld`**, after
`RenderSystem.setInverseViewRotationMatrix` (free ordinal). `getRotation`
returns a copy (`new Quaternionf(orig).mul(roll)`) because the field is final and
shared (Iris/DistantHorizons call it every frame).

### 5.5 Configuration (JSON in `config/bodycam.json`)

```json
{
  "enabled": true,
  "stepSmoothing": 1.5,
  "idleShake": 0.35,
  "idleShakeFrequency": 0.8,
  "bobbing": 0.55,
  "bobbingFrequency": 1.0,
  "rollLean": 0.5,
  "landingDamping": 0.8,
  "sprintMultiplier": 1.35,
  "sneakMultiplier": 0.4,
  "syncWithShader": true,
  "disableVanillaBobbing": true,
  "preset": "NORMAL",
  "motionBlurBoost": 0.0,
  "swayIntensity": 0.5,
  "taczSwayEnabled": true,
  "taczSwayIntensity": 1.0,
  "taczSwaySpeed": 1.5,
  "taczSwayOvershoot": 0.13,
  "viewSmoothSpeed": 14.0
}
```

Clamps/validation on load (same pattern as `realistic-grass`): any 0/missing
value → default, NPE guarded, ranges clamped.

### 5.6 Realistic breathing + sound (new)

- **Audio**: `assets/bodycam/sounds/breath-in-out.ogg` — the original file was
  an **MP3 disguised as .wav** (header `FF FB`), converted with ffmpeg to mono OGG
  3.06 s. Registered as `SoundEvent` `bodycam:breath`
  (`BreathSound.register()` in `onInitializeClient`).
- **`sounds.json` uses the QUALIFIED name `bodycam:breath-in-out`** — entries without
  a prefix are resolved by the MC in the **minecraft:** namespace (bug that caused
  `File minecraft:sounds/breath-in-out.ogg does not exist` +
  `Unable to play empty soundEvent`).
- **Visual cycle = 3.06 s = audio duration** → the **exhale starts exactly
  halfway** through the cycle (aligned with the middle of the sound, requested by the user).
- Asymmetric shape: inhale with a fast start (`sin`, peak in the middle),
  exhale with a smooth release (`cos`); **Perlin** irregularity (real
  breathing is not perfectly periodic).
- **Sound each cycle**: volume `(0.5 + 0.5·exhaustion) × gate × 1.05` (the ×1.05 is the
  requested boost), **pitch = speed multiplier** → the audio speeds up
  together with the visuals without losing sync.
- **Exhaustion from continuous running** (`runT`, resets when stopping):
  | Continuous running | Breathing rate |
  |---|---|
  | < 5 s | ×1.00 (normal) |
  | ≥ 5 s | ×1.35 (+35%) |
  | 5 → 20 s | progressive ramp |
  | ≥ 20 s | ×1.65 (+65%) |
- **Amplitudes independent of the preset** (old bug: it was
  `idleShake×0.5 = 0.175°` = invisible):
  - pitch: `(shape − 0.35) × (0.75 + 0.6·exhaustion)` degrees
  - vertical: `(shape − 0.35) × (0.03 + 0.02·exhaustion)` blocks
  - gate: standing still = 1.0; walking = `0.15 + 0.85·exhaustion` (you only
    hear/feel heavy breathing when tired).

### 5.7 Damage and landing shakes (new)

- **Damage**: drop in `getHealth()` detected in `onFrame` (real dt) → 0.4 s Perlin
  burst, amplitude `clamp(damage×0.14, 0.25, 1.4) × swayIntensity`,
  added to yaw/pitch. Absorption does not count (health does not change).
- **Landing**: in addition to the existing vertical sink, a **rotational**
  spring (k=180, c=22): pitch `impact×50` and roll alternating per foot
  `impact×25×footSide` — the camera tilts on the fall, the bigger the fall the bigger
  the jolt (scaled by `landingDamping`).

### 5.8 TACZ integration (new)

- **Gun spring (`TaczSway`)**: 2nd-order spring on the visual model — a flick of
  90° overshoots past 90 (peak ~102°) and settles aligned; **speed ×0.9 (10% slower**
  than the original, requested by the user). It ALWAYS applies (aiming or
  not) and **ONLY on the model** — game raycast/bullet/sights untouched.
- **Global view smoothing**: the rendered camera follows the mouse with
  exp-smoothing (`viewSmoothSpeed`, in `CameraMixin.setRotation`) — visual only;
  the player's real yaw/pitch do not change.
- Without TACZ installed, the `Tacz*` mixins do not apply (plugin) → the mod starts
  normally.
- Config: `taczSwayEnabled/taczSwayIntensity/taczSwaySpeed/taczSwayOvershoot`
  + `viewSmoothSpeed`, with category *Weapon & View* in the menu.

### 5.9 Diagnostics (`[diag]` every 60 ticks)

```
[diag] tick=… frames=… dt=… steps=… iv=… enabled=… bobY=… foot=… lean=…
       lag=… roll=… step=… swayY=… swayP=… viewD=… breath=… runT=…
```

- `steps>0` proves the footstep sound hook is live; `iv≈0.39` walking / `0.30` running.
- `breath=1.35` at 5 s of running / `1.65` at 20 s; `runT=` continuous seconds.
- `swayY/swayP` = TACZ spring; `viewD` = view smoothing.
- Hook lines: `hook Camera.update ATIVO`, `hook de ROLL em renderWorld ATIVO`,
  `hook TACZ applyFirstPersonGunTransform ATIVO`.
- Sound errors appear as `Missing sound for event:` /
  `Unable to play empty soundEvent:` / `File … does not exist` (that is how
  the namespace bug was diagnosed).

---

## 6. Own menu in Mod Menu

- **Class:** `dev.bodycam.gui.BodycamConfigScreen extends Screen`
  - Scrollable list: sliders + toggles; categories *Smoothing*, *Shaking*,
    *Bobbing*, *Compatibility*, **_Weapon & View_ (TACZ + view)**.
  - Buttons: **Reset defaults**, **Save**, **Cancel**; motion sickness
    tooltip on the EXTREME preset.
  - Saves to `config/bodycam.json` on confirm.
- **Registration:** `BodycamModMenu implements ModMenuApi`; `"modmenu"`
  entrypoint in `fabric.mod.json`.
- **Mod Menu is optional** (build-only dependency; without it the mod still loads).
- **Language:** `assets/bodycam/lang/{en_us,pt_br}.json` (UTF-8 without BOM) with
  all strings — includes the `subtitles.bodycam.breath` subtitle.

---

## 7. File structure (current)

```
bodycam/
├── build.gradle                    # modCompileOnly fileTree(libs/*.jar) for TACZ
├── gradle.properties               # mod_version=1.4.2+1.20.1
├── settings.gradle
├── libs/
│   └── TACZ-Refabricated-….jar     # EDITED COPY (manifest + 4 classes removed)
└── src/main/
    ├── java/dev/bodycam/
    │   ├── BodycamClient.java          # entrypoint + BreathSound registration + [diag]
    │   ├── BodycamModMenu.java         # ModMenuApi → config screen
    │   ├── camera/
    │   │   ├── CameraState.java        # all the temporal state
    │   │   ├── TemporalSmoother.java   # spring / exp-smooth / Perlin
    │   │   ├── BodycamEffects.java     # orchestrates all effects + breathing
    │   │   ├── BreathSound.java        # SoundEvent bodycam:breath
    │   │   └── TaczSway.java           # gun spring + view smoothing
    │   ├── config/BodycamConfig.java   # JSON + sanitize + presets + TACZ fields
    │   ├── gui/BodycamConfigScreen.java
    │   ├── compat/IrisCompat.java      # syncWithShader / overrides
    │   └── mixin/
    │       ├── BodycamMixinPlugin.java # gate isModLoaded("tacz")
    │       ├── CameraMixin.java
    │       ├── ClientPlayerEntityMixin.java
    │       ├── EntityMixin.java        # footstep sound hook
    │       ├── GameRendererMixin.java  # roll + frame hook + bobView redirect
    │       └── TaczGunTransformMixin.java
    └── resources/
        ├── fabric.mod.json
        ├── bodycam.mixins.json
        ├── bodycam.accesswidener
        └── assets/bodycam/
            ├── icon.png
            ├── sounds.json             # "bodycam:breath-in-out" (qualified!)
            ├── sounds/breath-in-out.ogg
            └── lang/{en_us,pt_br}.json
```

---


> **Changelog moved:** see [CHANGELOG.md](CHANGELOG.md).

## 9. Status checklist

- [x] Environment audited (JDK 21, Gradle 8.5, Loom 1.5.8, yarn 1.20.1+build.10)
- [x] Project created and builds green (v1.0.0 → **v1.0.1+1.20.1**)
- [x] Mixins with signatures validated by javap + cross-review
- [x] Real roll via the renderWorld MatrixStack
- [x] Real dt (nanoTime) — FPS-independent
- [x] Steps synchronized with the vanilla audio (EntityMixin + -cos phase)
- [x] Damage shakes and rotational landing jolt
- [x] TACZ integration (visual spring + smooth view + plugin gate + build pipeline)
- [x] Breathing with synchronized OGG sound + running exhaustion (5 s/20 s)
- [x] Qualified sounds.json (minecraft: namespace fix)
- [x] `[diag]` complete (steps/iv/sway/viewD/breath/runT)
- [x] README.md updated with ALL changes
- [x] FX effects research done (Agent A) + formulas/parameters saved in §11
- [x] vanilla/TACZ APIs verified by javap + core shader pattern confirmed
- [x] post-FX architecture decided and plan written in §8 (session 2026-10-07)
- [x] Implement post-FX (`bodycam:post`): fisheye, CA, vignette, lens dirt, grain
- [x] Implement chest camera + wide FOV (CameraMixin + GameRenderer.getFov)
- [x] TACZ realism: lowering while running + extra sway + recoil kick
- [x] `BodycamConfig` + sanitize + GUI + lang for the new fields
- [x] Realistic water (waves, caustics, underwater fog, tint)
- [x] User dirty lens texture integrated (`dirty_lens.png`)
- [x] DOF/blur on the config GUI background
- [x] Build + install jar + sha256 + README (CHANGELOG §8 recorded)
- [x] Fix crash `EntityMixin` (`tickMovement` does not exist on `Entity`) + restore `playStepSounds` hook
- [x] Fix VFX post-FX (Sampler0 bound via blit→scratch, dirt as AbstractTexture, vsh NDC)
- [x] Real GUI DOF (new shader `bodycam:dofblur`, 17 taps, `renderBlur(6.0f)`)
- [x] Compact UI (adaptive colW 96–150, adaptive ROW_H 12–18)
- [x] Fix global shader: `RenderSystem.setShader(() -> shader)` before the quad (Tessellator ignores manual bind)
- [x] UI: scroll (mouseScrolled + scrollbar) + header gap 11 + text above the slider knob
- [x] README translated to English (`README(pt-br).md` backup kept)
- [x] All texts in English: fabric.mod.json, pt_br.json, Java logs/comments
- [x] New SVG logo (`textures/logo.svg`, advanced filters) → icon.png 256
- [x] `MODRINTH.md`: large English Modrinth description
- [x] **Fix render corruption (root cause)**: `drawInternal` clobbers
      `Sampler0..11` with `RenderSystem.getShaderTexture(i)` → renamed our
      sampler to `ScreenTex` (post + dofblur, json/fsh/Java)
- [ ] Test in-game: breathing (sound + visual), running 5 s/20 s, TACZ, damage, fall
- [ ] Test the Mod Menu screen in-game
- [ ] Test post-FX + GUI DOF + UI scroll in-game (jar `D1F9476D...`) —
      **corrupted-world/sliced-textures bug CONFIRMED FIXED by the user** 🎉;
      lens dirt unified at 1.0/0.02 (live config + ALL presets)
- [x] **Phase A (v1.1.0+1.20.1)**: A0 `setPos` fix (vertical camera effects
      were dead), A1 dead `getRotation` hook removed (Sodium particle double
      roll), A2 config mtime cache, A3 unified `Spring`, A4
      `waterRefraction`/`foamIntensity` wired, A5 auto-quality governor,
      A6 `dirty_lens.png` 1536→1024 wide, version bump + README §8/§9/§10
- [ ] **Playtest Phase A** (jar `4FE1A130…`): camera NOW moves vertically for
      the first time — check bob/steps/chest height (no nausea/clipping),
      roll not doubled on Sodium particles, refraction only underwater,
      `auto-quality` log lines, GUI still saves config (cache invalidation)
- [x] **Phase C1 (v1.2.0+1.20.1)**: roadmap 01 Camera Inertia (2nd-order
      view spring: 30–70 ms lag + short overshoot) + 04 Gait Transition
      (sneak→walk→sprint cadence blend, sprint step weight, deceleration
      settle, speed-change irregularity) + GUI/lang, roadmap file saved
- [ ] **Playtest Phase C1** (jar `7EFDE659…`): inertia feel (mouse flicks
      give a short overshoot, stopping carries slightly, no nausea), gait
      blend while accelerating/decelerating (no phase pop), heavier sprint
      steps, new GUI sliders/toggles save and reload correctly
- [x] **Phase C2 (v1.2.1+1.20.1)**: roadmap 06 Auto Exposure (asymmetric
      light-level adaptation, `Exposure` uniform) + 13 Flash/Brightness
      Shock (upward light-jump envelope, never random) + GUI/lang
- [ ] **Playtest Phase C2** (jar `BCF93F22…`): cave → surface adapts fast,
      surface → cave recovers slowly, brightness flash on cave mouth/doorway,
      no exposure weirdness at night or under shaders (Iris)
- [x] **Phase C3 (v1.2.2+1.20.1)**: roadmap 03 Impact Recovery — shared
      compress → tilt → overshoot → recover sequence with separate trigger
      envelopes for falls / combat hits / wall collisions + GUI/lang
- [ ] **Playtest Phase C3** (jar `615CDE62…`): landing still feels right
      (now with slight overshoot), taking damage gives a clear jolt into the
      hit, sprinting into a wall nods the camera once (no repeated shaking
      while pushing), toggle off restores legacy landing
- [x] **Audit 2026-10-10**: found C4–C8 + Phase D already in the source but
      NEVER recorded in §8 (README said v1.2.2 while code was 1.3.0 — the
      "hallucinated changelog" complaint). Verified every feature line by
      line (config, `BodycamEffects`, `post.fsh` uniforms, GUI rows, lang
      keys, `TaczBridge`/`TaczGunRealism`/`TaczSway`/`TaczGunTransformMixin`)
      and wrote the missing §8 entries (session `C4–C8 + Phase D → v1.3.0`)
- [x] **Phase E (v1.4.0+1.20.1)**: 4 new presets `DOCUMENTARY`/`HORROR`/
      `TACTICAL`/`RAW GOPRO` (motion + optics + exposure/glow/dust/critical
      values each), GUI cycling button now 8 presets, `sanitize()` clamps
- [x] **Architecture wired (roadmap §5)**: `CameraMixin →
      RenderCoordinator.cameraUpdate` (shadow-pass skip + same-frame guard)
      + `GameState.sample` per-frame snapshot — the module entry point no
      longer bypasses the coordinator
- [x] **Build + install v1.4.0**: `BUILD SUCCESSFUL` (28 s), jar
      `B16E8261…` installed, old 1.3.0 removed, §8/§9/§10 + header updated
- [ ] **Playtest Phase C4–C8**: grain rises in the dark, lens drops in
      rain/underwater + smudge on impacts, metal vs soft footsteps, sun
      ghost flare, dust in light shafts, low-HP instability, motion blur
      only while turning
- [ ] **Playtest Phase D (TACZ)**: ADS tightens, walking while aimed drifts,
      landing/reload nod the weapon, wall pullback, shot never kicks camera
- [ ] **Playtest Phase E presets**: cycle all 4 new presets, HORROR and
      RAW GOPRO read clearly different, GUI saves + reloads config
- [x] **v1.4.1 config UI rebuilt**: headers scroll with content (old fixed
      "Smoothing" bug), widgets only instantiated inside the viewport (no
      overlap with title/Save/Reset), 22 px rows, custom sliders (gold value
      track, no knob over the label), preset in a fixed bottom bar
- [x] **v1.4.1 crosshair alignment**: `EntityMixin.getRotationVec(F)` returns
      the final camera rotation → block outline + breaking cracks sit exactly
      under the crosshair (was off with chest cam / inertia / shake)
- [x] **v1.4.2 aim sign fix**: `EntityMixin.getRotationVec(F)` now returns the
      vanilla-identical vector from the final camera rotation (the old trig was
      negated → raycast pointed to the opposite side)
- [ ] **Playtest v1.4.2** (jar `5B0120B4…`): scroll the config screen
      top→bottom, then aim UP and mine — the block that cracks must be the one
      under the crosshair, chest cam ON and OFF

---

## 10. SAVE IN #TODO(todowrite)

> Task block in the OpenCode `todowrite` list format — written by hand
> here in the README, without invoking the tool. Update this block every session.

```
todos:
  - [x] Environment audit (JDK 21, Gradle 8.5, Loom 1.5.8)          priority: high
  - [x] bodycam/ project created; green v1.0.0 jar                        priority: high
  - [x] Crash fixes: renderWorld + Camera.update signatures           priority: high
  - [x] Real roll in the renderWorld MatrixStack                          priority: high
  - [x] Real dt (nanoTime) + time-stable step cadence          priority: high
  - [x] Step sync with the audio (EntityMixin, -cos phase)         priority: high
  - [x] Damage shakes + rotational landing jolt                priority: high
  - [x] TACZ integration: visual spring + view smoothing + plugin gate priority: high
  - [x] Build pipeline with the edited TACZ jar in libs/                  priority: high
  - [x] Breathing: OGG sound + audio sync + 5s/20s exhaustion           priority: high
  - [x] Fix qualified sounds.json (minecraft: namespace)               priority: high
  - [x] Fix breathing amplitude (was 0.175° invisible)               priority: high
  - [x] [diag] complete (steps/iv/swayY/viewD/breath/runT)               priority: medium
  - [x] README.md with ALL changes                                  priority: medium
  - [x] FX research (formulas/parameters) + javap vanilla/TACZ + §11     priority: high
  - [x] post-FX/camera/TACZ plan written in §8 (no code yet)       priority: high
  - [x] IMPLEMENT: core shader bodycam:post + PostEffects (TAIL renderWorld)
                                                        priority: high
  - [x] IMPLEMENT: chest camera (CameraMixin) + wide FOV (getFov)
                                                        priority: high
  - [x] IMPLEMENT: TACZ realism (lowering, sway, kick)                priority: high
  - [x] IMPLEMENT: new fields in BodycamConfig + GUI + lang          priority: medium
  - [x] IMPLEMENT: realistic water (WaterEffects + AFTER_ENTITIES)       priority: medium
  - [x] IMPLEMENT: integrate the user's dirty_lens.png into the shader     priority: medium
  - [x] IMPLEMENT: DOF/blur on the config GUI background                  priority: low
  - [x] Build + install jar + sha256 + CHANGELOG §8                    priority: high
  - [x] Fix crash EntityMixin (tickMovement on Entity) + hook playStepSounds
                                                        priority: high
  - [x] Fix VFX: Sampler0/blit scratch + dirt AbstractTexture + vsh NDC   priority: high
  - [x] Real GUI DOF: dofblur shader + renderBlur(6px)                    priority: high
  - [x] Smaller UI: adaptive colW/ROW_H                                  priority: medium
  - [x] Fix global shader: RenderSystem.setShader before the quad           priority: high
  - [x] UI scroll: mouseScrolled + scrollbar + gap headers + knob/text   priority: high
  - [x] Translate README to English (README(pt-br).md backup)             priority: high
  - [x] All texts in English (fabric.mod.json / pt_br / Java logs)        priority: high
  - [x] New SVG logo (advanced filters) + icon.png 256 swapped            priority: high
  - [x] MODRINTH.md big English description                               priority: high
  - [x] Fix render corruption: Sampler0 clobbered by drawInternal ->
        renamed to ScreenTex (post + dofblur, json/fsh/Java)         priority: high
  - [x] Lens dirt unified: defaults + ALL presets = 1.0/0.02,
        live config adjusted (3.0 -> 1.0)                          priority: high
  - [x] Phase A optimizations: A0 setPos fix (vertical camera FX were
        dead code), A1 removed dead getRotation hook (Sodium double
        roll), A2 config mtime/size cache, A3 unified Spring helper,
        A4 waterRefraction+foamIntensity wired, A5 auto-quality
        governor, A6 dirty_lens 1536->1024, v1.1.0+1.20.1          priority: high
  - [ ] Playtest Phase A (jar 4FE1A130): vertical bob/steps/chest height
        first time active, no double roll with Sodium, refraction
        underwater only, auto-quality logs, config GUI save ok      priority: high
  - [x] Save ROADMAP to disk (PROJECT-MOD\ROADMAP-bodycam.md)             priority: high
  - [x] Phase C1: 01 Camera Inertia (2nd-order view spring,
        30-70ms lag + overshoot) + 04 Gait Transition (sneak/walk/sprint
        cadence blend, sprint step weight, decel settle, accel
        irregularity), GUI + lang, v1.2.0+1.20.1                        priority: high
  - [x] Playtest Phase C1+C2 (jars 7EFDE659 / BCF93F22): inertia feel, gait
        blend, exposure adaptation + flash, GUI sliders save ok           priority: high
  - [x] Phase C3: 03 Impact Recovery — shared compress→tilt→overshoot
        sequence, separate envelopes for falls/combat/collisions,
        v1.2.2+1.20.1                                                    priority: high
  - [x] Playtest Phase C3 (jar 615CDE62): landing + damage jolt + wall
        nod, toggle-off restores legacy landing                           priority: high
  - [x] Phase C4: 08 Low-Light Sensor Noise (grain ponderado por
        luminância - reutiliza a infra de grain existente)               priority: high
  - [x] Phase C4: 08 Low-Light Sensor Noise (luminance-weighted grain)   priority: high
  - [x] Phase C5: 09 Lens Contamination (water drops / condensation /
        impact smudges, separate masks + CPU decay)                      priority: high
  - [x] Phase C6: 05 Surface-Aware Footsteps (BlockState profiles) +
        11 Underwater Lens Response                                     priority: high
  - [x] Phase C7: 07 Lens Ghosting + 10 Lens Profile +
        12 Volumetric Dust + 14 Critical Damage Camera                  priority: medium
  - [x] Phase C8: 02 Angular Motion Blur (LAST - technique +
        Iris compatibility dependent)                                   priority: medium
  - [o] Phase D: TACZ table (weapon inertia, ADS stabilization,
        movement-to-aim, landing weapon lag, wall proximity,
        reload body response; separate springs per event)               priority: high
  - [x] Phase B/D: module architecture (GameState/Motion/Optics/
        Environment/Weapon solvers + RenderCoordinator)                 priority: medium
  - [x] Phase E: presets DOCUMENTARY/HORROR/TACTICAL/RAW GOPRO          priority: medium
  - [x] v1.4.1: config UI rebuilt (headers scroll with content, widgets
        clipped to viewport, 22px rows, custom sliders, preset bottom bar)
                                                                  priority: high
  - [x] v1.4.1: crosshair/block-break alignment (EntityMixin
        getRotationVec -> final camera yaw/pitch)                     priority: high
  - [x] v1.4.2: aim vector sign fix (EntityMixin matched vanilla EXCEPT for a
        global negation; ray hit the opposite side)                    priority: high
  - [ ] Playtest v1.4.2 (jar 5B0120B4…): config scroll clean, aiming UP breaks
        the block under the crosshair, chest cam ON/OFF                priority: high
  - [ ] Final in-game test (breathing, TACZ, damage, fall)              priority: high
  - [ ] Test the new jar in-game: post-FX, GUI DOF, UI scroll,
        world NOT black + no sliced textures (jar D1F9476D…)           priority: high
  - [ ] Test the Mod Menu screen in-game                                  priority: medium
  - [-] Fix rainbow ring (chromatic aberration too strong) in textures/logo.svg
                                        CANCELLED at user's request (keep as-is)
```

**Next step:** playtest **C1–C3** (jar `NEXUS-bodycam-1.2.2+1.20.1.jar`,
sha256 `615CDE62…` — includes C1 inertia/gait `7EFDE659…` and C2 exposure
`BCF93F22…`; only the latest jar stays in mods). Continue the roadmap in the
user's recommended order: **C4** 08 Low-Light Sensor Noise, **C5** 09 Lens
Contamination, **C6** 05 Surface Footsteps + 11 Underwater Response, **C7**
07/10/12/14, **C8** 02 Angular Motion Blur (last), then **Phase D** TACZ
table + module architecture, **Phase E** presets. Roadmap file:
`PROJECT-MOD\ROADMAP-bodycam.md`.

---

## 11. Effects research (Agent A) — executive summary

> Sources: Hound report (with URLs) + shaderpack extracted at
> `%TEMP%\opencode\bcs-v161` (Body Camera Shader V1.6.1).
> Any developer tomorrow uses THESE numbers as defaults.

### 11.1 Pipeline order (world → screen)
1. **Geometric distortion** (fisheye/barrel) — in the lens, real physics.
2. **Chromatic aberration** — radial shift of the channels.
3. **Vignette** — mechanical (lens rounding) + cosⁿ.
4. **Lens dirt** — only lit where the scene is bright (bloom/sun).
5. **Tone mapping / exposure** — already comes from the shaderpack (Iris/BSL) — DO NOT repeat.
6. **Sensor grain** — last in the world, before the HUD.
7. **HUD on top** (never distorted/grained) → overlay + compression
   artifacts (burning, if done at all).

### 11.2 GLSL formulas (default values in parentheses)
- **Fisheye (Brown-Conrady):** `r' = r·(1 + k1·r² + k2·r⁴)`;
  k1 **0.10–0.30** (default 0.18), k2 −0.15..0, keep k1+k2 > 0.02;
  overscan: render with FOV +10–15% and crop; corners: UV clamp +
  rounded mask (~0.94) so edge pixels do not stick.
- **Vignette:** `1 − uMech·smoothstep(a,b,r) + cos^n(r·θmax)` —
  cosPow **2.0–2.5**, floor 0.08–0.20, central gain 1.10–1.30;
  slightly decentered/polygonal (mix 0.15–0.35) so it does not look like a
  perfect circle.
- **Radial CA:** `ca·r²`, shift only G and R, amplitudes **0.0015–0.006**
  (R outward, B inward); zero at the center.
- **Lens dirt:** fbm (octaves ~3–4) + specks + scratches + fingerprint smudges;
  **always** defocused (low frequency), **fixed to the screen** (not the scene);
  light reaction: `col += bloomProxy·dirt·intensity`, intensity **0.8–3.0**,
  veiling 0.03–0.10; the "bloomProxy" is the local luminance max (no separate
  bloom pass in the mod).
- **Sensor grain:** **temporal** noise hash quantized to 30 fps
  (fract(t·30)), cell 1.5–2.5 px, weight
  `mix(1.0, 0.12, smoothstep(0.25, 0.85, L))` (grain in the shadows, almost none
  in the highlights), 70–85% monochromatic + 15–30% chromatic,
  total opacity **0.03–0.08**, **soft-light** blend (NEVER additive).

### 11.3 Mistakes to avoid (Agent A's list)
- Applying grain/HUD *before* the distortion (it turns into a mess at the edge).
- Multiplicative vignette without a floor → dead black corners.
- Distortion without overscan → stretched image and stuck corners.
- Grain on top of the HUD → illegible text; additive grain → "TV light".
- Lens dirt tied to the scene → flickers with the camera; turned off → dead dirt.
- Repeating tonemapping (Iris/BSL already does it) → double curve, washed-out shadows.

### 11.4 Saved references
- Extracted BCS zip: `%TEMP%\opencode\bcs-v161\shaders\` (composite*.fsh,
  `settings.glsl`, `lib/`, LUTs) — real parameters to copy.
- Hound report URLs: see the original session attachment (the reference
  pages are the OptiFine/Iris docs (code), a fisheye shadertoy,
  AAA lens dirt articles (vidar/Unreal) and r/Simulation
 /MC shader dev archive topics).

---
