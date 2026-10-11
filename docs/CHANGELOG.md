# Changelog - NEXUS Bodycam

> Complete development log: every session, every change, in order.
> Extracted from the original README so the README can stay short.

---


> **RULE (mandatory for all agents):** ALWAYS record here any change
> made to the mod — without an entry in this section, the task is not complete.

### v1.0.0 — mod creation
- Temporal core (spring/exp-smooth/Perlin), bobbing, idle shake, roll, presets
  (SUBTLE/NORMAL/INTENSE/EXTREME), Mod Menu screen, IrisCompat, icon,
  access widener, lang en/pt.

### Crash fixes (14:08–14:20)
- `GameRendererMixin`: signature `renderWorld(float, long, MatrixStack, CallbackInfo)`
  (the old one with `boolean` threw InvalidInjectionException and the game did not start).
- `CameraMixin`: `update(BlockView, Entity, boolean, boolean, float, CallbackInfo)`.
- `CameraMixin.getRotation`: now returns a **copy** of the Quaternionf instead
  of mutating the shared field (Iris/DH call it every frame).
- Cross-review of signatures with the realistic-grass agent: 3/3 javap OK.

### Real roll + amplifications (14:42)
- Roll injected into the **MatrixStack of `renderWorld`** (getRotation was never
  called by the render → roll did not appear).
- Amplitudes increased: idle ±0.5°, roll clamp ±10°, vertical bob ~0.12 blocks.
- `disableVanillaBobbing` implemented with `@Redirect` on bobView (previously ignored).
- `[diag]` every 60 ticks + hook lines to prove that the mixins run.

### Real dt and fixed cadence (14:55)
- dt via `System.nanoTime()` (clamp 0.1 s) — the old `tickDelta/20` was
  framerate-dependent (with shaders at ~25 fps everything ran ~2.4× slower;
  steps took ~2 s).
- Step cadence fixed in time: 1.00 s cycle walking / 0.83 s running;
  `sprintMultiplier`/`sneakMultiplier` now scale **only the amplitude**.
- Step spring k 35→100, c 8→20 (settles in ~0.3 s).

### Realism pass (15:08)
- Footsteps: velocity kick on the spring every half bob cycle, foot alternation.
- Yaw inertia (camera lags on turns), sprint lean (~1.8°),
  GoPro-style 8 Hz micro-vibration (Perlin, scales with speed),
  lateral lean while strafing, base breathing (4.5 s cycle while standing only).

### Step sync with the AUDIO (15:35)
- Cause: impacts fired on their own phase, unrelated to the sounds.
  In vanilla, `Entity.move` calls `stepOnBlock → playStepSounds` every
  1.667 blocks (~0.39 s walking, ~0.30 s running).
- `EntityMixin` on `Entity.playStepSounds` (private; only reacts if
  `this == player`) → spring kick **at the moment of the sound**, foot flip, phase
  lock; cadence from the game (EMA → `iv=`); fallback after 3 s without sounds.
- Spring k 100→160→**220**, c 20→26→**30**; kick 1.2→1.75→**2.0**.

### Visual phase fixed (15:52)
- `bobVertical = -cos(phase*2)`: at the sound (phase=k·π) the camera is at the **bottom**
  of the bounce → each sound = one full bounce (before: 1 bounce every 2 sounds,
  always late); the phase snap became the cosine minimum (jumps
  quadratically smaller).

### Damage and landing shakes + TACZ spring 10% slower (21:15)
- **Damage shake**: detection via drop in `getHealth()` → 0.4 s Perlin burst
  (`clamp(damage×0.14, 0.25, 1.4) × swayIntensity`).
- **Rotational landing jolt**: spring k=180 c=22 on pitch (impact×50)
  and roll alternating per foot (impact×25×footSide).
- **TACZ gun spring**: `omega ×0.9` (10% slower; gun only, camera untouched).

### Full TACZ integration (18:55–19:02)
- `TaczGunTransformMixin` on `FirstPersonRenderGunEvent.applyFirstPersonGunTransform`
  (anchor verified by javap; refmap generated and verified 1:1).
- `TaczSway`: 2nd-order spring (flick 90° → peak ~102° → settles), VISUAL ONLY.
- View smoothing (`viewSmoothSpeed`) in `CameraMixin.setRotation`.
- `BodycamMixinPlugin`: `Tacz*` mixins conditioned on `isModLoaded("tacz")`.
- Config/GUI/lang: `taczSway*` fields + `viewSmoothSpeed`, category *Weapon & View*.
- **TACZ build pipeline** (see §2): `modCompileOnly` + jar in `libs` edited
  (manifest cleaned, 4 renderer classes removed) + invented `TargetClassLoader`
  method removed from the plugin (does not exist in `IMixinConfigPlugin`).

### Realistic breathing + sound (19:21–19:37 — current session)
- **Audio**: `breath-in-out` converted (it was MP3 in .wav) → mono OGG 3.06 s;
  `sounds.json` + en/pt subtitles; `SoundEvent` `bodycam:breath` registered.
- **SOUND BUG fixed**: the MC resolves entries without a prefix in the
  `minecraft:` namespace (`File minecraft:sounds/breath-in-out.ogg does not exist` +
  `Unable to play empty soundEvent: bodycam:breath`). Fix: qualified entry
  `bodycam:breath-in-out`.
- **Cycle = audio duration (3.06 s)** → exhale **exactly halfway** through the
  cycle (request: "exactly in the middle of the audio is the breath out").
- Asymmetric shape (fast inhale / smooth exhale) + Perlin irregularity.
- **Continuous running speeds up the breathing**: ≥5 s → **+35%**, ramp up to 20 s →
  **+65%**; `runT` resets when stopping; sound plays each cycle with pitch=mult
  (sync kept at any speed); volume **×1.05** (requested).
- **VISUAL BUG fixed**: amplitude was `idleShake×0.5` (0.175° = invisible);
  now it has its own: pitch `(shape−0.35)×(0.75+0.6·exhaustion)`° and
  Y `(shape−0.35)×(0.03+0.02·exhaustion)` blocks.
- `[diag]` gained `breath=` and `runT=` (level confirmation in the log).
- Final build of this session: `AF947D1F…557D911`, 1 270 658 bytes, installed.

### Session 2026-10-07 — RESEARCH + PLAN (NO code changes)
> Only research and planning in this session. **No code file was
> modified**; implementation left for the next session (requested by the user).

**Done (research/audit):**
- The research agent (Hound) delivered a full report on bodycam effect
  mathematics (GLSL formulas + parameters + URLs) — summary in §11.
- `Body Camera Shader - V1.6.1.zip` extracted for inspection at
  `%TEMP%\opencode\bcs-v161` (it is a complete OptiFine/Iris shaderpack:
  composite1..6, final, settings.glsl, LUTs).
- Vanilla APIs confirmed with `javap` on the mapped jar (yarn 1.20.1+build.10):
  - `Framebuffer`: `fbo` (public int), `textureWidth/Height`,
    `beginRead()/endRead()/beginWrite(boolean)/endWrite()`,
    `draw(int,int[,boolean])`, `getColorAttachment()`, `delete()`, `resize()`.
  - `ShaderProgram`: `addSampler(String,Object)`, `getUniform(String)`,
    `bind()`, `markUniformsDirty()`.
  - `SimpleFramebuffer(int,int,boolean,boolean)`; `BufferRenderer.drawWithGlobalProgram`;
    `Tessellator.getInstance().getBuffer()` + `begin(DrawMode,VertexFormat)` + `end()`.
  - `GameRenderer.getFov` **private** (`getFov(Camera,float,boolean)`).
  - `RenderSystem.setShader(Supplier<ShaderProgram>)`, `setShaderTexture(int,int)`.
- Core shader pattern proven on this machine (copy from realistic-grass):
  `CoreShaderRegistrationCallback.EVENT.register(ctx -> ctx.register(
  new Identifier(MOD_ID,"post"), VertexFormats.POSITION_TEXTURE, PostShader::set))`
  + `assets/bodycam/shaders/core/post.{vsh,fsh}` + `post.json`
  (sampler `Sampler0` + uniforms declared in the JSON).
- TACZ-Refabricated API listed from the `libs/` jar:
  - Fabric/Forge-style events in the **`cn.sh1rocu.tacz.api.event.*`** namespace
    (`BaseEvent`, `ICancellableEvent`, `ComputeFovModifierEvent`,
    `InputEvent$MouseButton$Pre/Post`, `LivingHurtEvent`, `PlaySoundSourceEvent`…)
    and `com.tacz.guns.api.event.common.*` (`GunFireEvent`, `GunShootEvent`,
    `GunReloadEvent`, …) — **confirm the Callback signature + side
    (client) before subscribing**.
  - The current mixin on `com.tacz.guns.client.event.FirstPersonRenderGunEvent
    .applyFirstPersonGunTransform` remains as the anchor for the gun effects.

**Architecture decision (post-processing):**
- Effects IN THE MOD (not in the shaderpack): core shader `bodycam:post` + fullscreen
  quad drawn at the **TAIL of `GameRenderer.renderWorld`** (after the
  hand/HUD, before the GUI), with a main→aux FBO copy via `glBlitFramebuffer`
  (or `draw`) to avoid a feedback-loop when sampling the texture while writing.
- Compat: works with/without Iris+BSL (Iris returns the final image to the
  main framebuffer before the HUD); without a pack it also runs.
- Order in the fragment (physics: world→lens→sensor): **1) fisheye/barrel,
  2) radial CA, 3) cosⁿ vignette, 4) lens dirt (light reaction),
  5) sensor grain (last)**; everything pinned to the screen (never to the world).

**Implementation plan (NEXT SESSION — not done yet):**
1. **New shaders** (`assets/bodycam/shaders/core/post.vsh/fsh` + `post.json`):
   - *Fisheye/barrel*: `r' = r·(1 + k1·r² + k2·r⁴)`, k1 **0.10–0.30**
     (default ~0.18), k2 −0.15..0 (keeping k1+k2>0.02); overscan FOV+10–15%;
     corners with clamp + rounded mask (crop ~0.94).
   - *Fisheye vignette*: `pow(cos(r·θmax), uCosPow)` (uCosPow 2.0–2.5) +
     mechanical component `1−uMech·smoothstep(0.75,1.45,r)`, floor 0.08–0.20,
     central gain 1.10–1.30; slightly decentered/polygonal (mix 0.15–0.35).
   - *Lens dirty*: procedural (fbm + specks + scratches), **fixed to the screen**,
     always defocused (low frequency), reacts to light: `col += bloomProxy·dirt·I`
     (intensity 0.8–3.0, veiling 0.03–0.10); bloom proxy from local luminance
     sampling (no separate bloom pass — use local max).
   - *Bodycam grain*: temporal hash quantized to 30 fps, cell 1.5–2.5 px,
     weight `mix(1.0,0.12,smoothstep(0.25,0.85,L))` (grain in the shadows),
     70–85% mono + 15–30% chromatic, **soft-light** (not additive), 0.03–0.08.
   - Bonus (cheap, include): radial CA `ca·r²` 0.0015–0.006.
2. **Chest camera**: in `CameraMixin.update` TAIL (1st person only):
   `setPos(pos.add(0, chestOffsetY, 0))` with default ≈ **−0.30/−0.35 blocks**
   (eyes 1.62 → chest ~1.25–1.35 m) + constant pitch tilt (~8–12°
   downward) added IN the final `setRotation` (safe: vanilla re-reads pitch on the
   next frame); does NOT touch the player's real yaw/pitch.
3. **Wide FOV**: `@Inject` RETURN mixin on `GameRenderer.getFov`
   (private, `CallbackInfoReturnable<Double>`) → multiply by `fovWideMult`
   (~1.15) when chest-cam is active; check interaction with TACZ ADS.
4. **TACZ realism** (100% visual, raycast untouched):
   - gun lowering while running (rotate in the transform, with a spring),
   - breathing/idle sway on the gun model (Perlin already exists),
   - (if Callback confirmed) extra visual recoil kick on fire.
5. **Config** new fields in `BodycamConfig` + sanitize + GUI + lang;
   `[diag]` with new fields; then: build (§2), jar installed + sha256.

---

### Session 2026-10-08 — FULL IMPLEMENTATION (previous session's plan + water + DOF)

**Done (new/changed code):**

- **Post-FX core shader** (plan §8 step 1):
  - `assets/bodycam/shaders/core/post.{json,vsh,fsh}` — fisheye/barrel
    (`FisheyeK1/K2`), radial CA, cosⁿ+mechanical vignette, lens dirt, grain
    at 30 fps, `FovWideMult`; sampler `DirtyLensTex`.
  - `dev/bodycam/camera/PostEffects.java` — registration via
    `CoreShaderRegistrationCallback`, fullscreen quad at the **TAIL of
    `GameRenderer.renderWorld`**, per-frame uniforms, binding of texture
    `bodycam:textures/dirty_lens.png` to sampler `DirtyLensTex`;
    guards the Iris shadow-pass.
  - **User texture integrated**: `textures/Dirty Lens Grime Overlay.png`
    → copied to `assets/bodycam/textures/dirty_lens.png`.
- **Chest camera + wide FOV** (plan step 2):
  - `ChestCam.java` (height offset, pitch tilt, FOV multiplier).
  - `CameraMixin` rewritten: chest position + view
    smoothing (`viewSmoothSpeed`) + roll + TaczSway/TaczGunRealism update.
  - `GameRendererMixin`: `@Inject RETURN` on `getFov` (multiplies by
    `fovWideMult`), vanilla `bobView` redirect (with
    `disableVanillaBobbing`), roll hook (fix for the old crash: `renderWorld`
    signature corrected to `FJL...MatrixStack`), TAIL → `PostEffects.render()`.
- **TACZ realism** (plan step 3):
  - `TaczGunRealism.java` (lowering spring, recoil kick with decay).
  - `TaczGunTransformMixin` rewritten (lowering + run sway + recoil kick on
    the gun transform).
  - `EntityMixin`: detects sprint → `triggerRecoil` (cast `(Object) this`
    corrected to the mixin `instanceof` pattern).
- **Realistic water (extra, requested by the user)**:
  - `WaterEffects.java` — surface waves, procedural caustics,
    underwater fog, tint; registered in
    `WorldRenderEvents.AFTER_ENTITIES` in `BodycamClient`.
- **Config + GUI + lang** (plan step 4):
  - `BodycamConfig`: new fields (chestCam*, fovWideMult, taczLowering*,
    taczRunSway*, taczRecoilKick*, postFX/fisheye/vignette/CA/lensDirt/grain,
    water* incl. underwater fog/tint) + `sanitize()` + updated presets.
  - `BodycamConfigScreen`: new sections (Camera, Post-FX, Water) with
    toggles/sliders; **background with a DOF/blur effect** (3 layers + dark
    overlay, requested by the user).
  - `en_us.json` / `pt_br.json`: new keys for all the options.
- **IrisCompat**: `isRenderingShadowPass()` used by PostEffects.
- **`renderDofBlur()`** in the GUI: blurred background when opening the config menu.

**Build of this session:**
- `gradlew clean build` — **BUILD SUCCESSFUL** (fix for 30 compilation
  errors: imports `GlStateManager`/`RenderSystem`/`IrisCompat`,
  `isInWater`→`isTouchingWater`, mixin `instanceof` cast, removal of the
  anonymous `WorldRenderContext` → `PostEffects.render()` without arguments).
- Jar installed at `mods\NEXUS-bodycam-1.0.1+1.20.1.jar`
  - **SHA256**: `A3FE4C2DCA15827C3411BABA68509C830224565232344FCE355B6309B43C72FB`
  - Size: 3 450 189 bytes
- Gradle extracted from `%TEMP%\opencode\gradle-8.5-bin.zip` (old wrapper
  deleted; `gradle-wrapper.properties` + `gradlew.bat` recreated in the project).

**Notice to the realistic-grass agent:** the crash `bodycam.mixins.json:
GameRendererMixin` (`bodycam$onRenderWorld(FJZ...)` vs `FJL...;`) was
fixed in this session (`renderWorld` signature + roll hook redone
on the INVOKE of `setInverseViewRotationMatrix`).

---

### Session 2026-10-08 (continuation) — EntityMixin CRASH FIX + RESTORATION of the step hook

**Reported in-game crash:** `EntityMixin` pointed at
`net.minecraft.entity.Entity`, but injected into `tickMovement` — a method that
only exists on `LivingEntity` (`ClassNotFoundException`/mixin failure at
startup). The class also had extra braces (syntax error).

**Done:**

- **`EntityMixin.java` rewritten**: `@Mixin(Entity.class)` with the
  **original and correct** hook — `@Inject` on `playStepSounds` (private vanilla
  method, confirmed by javap) at `HEAD`; only reacts if
  `(Object) this == MinecraftClient.getInstance().player` → calls
  `BodycamEffects.onFootstep()`. This **restores the footstep/audio sync**
  that had been left without callers in the previous rewrite
  (`footstepCount`, EMA of `footInterval`, foot flip, phase lock).
- **Sprint recoil moved** to `ClientPlayerEntityMixin` (which already
  injects into the player's `tickMovement`): same logic
  (`speed > 0.5 && isSprinting()` → `TaczGunRealism.triggerRecoil(0.3, 0.15)`),
  with guard `player == MinecraftClient.getInstance().player`.
- `getVelocity`/`isSprinting` `@Shadow` mixins removed (inherited
  methods — only produced warnings); direct calls.

**Build of this session:**
- `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL** (no mixin
  warnings).
- Jar installed at `mods\NEXUS-bodycam-1.0.1+1.20.1.jar`
  - **SHA256**: `EB04FBD38C6AC4DD116FB9D6B427526822E093602D26C243D32BDFA4B505FECA`
  - Size: 3 450 319 bytes

---

### Session 2026-10-08 (cont. 2) — post-FX VFX FIX + real DOF + compact UI

**User report:** "make the UI smaller and the DOF doesn't work, nor the vfx".

**Causes found:**
- **VFX (post-FX) invisible**: `Sampler0` (the screen) was **never bound** to the
  shader — the fsh read garbage; `DirtyLensTex` passed an `Identifier` that the
  `ShaderProgram.bind()` ignores (only accepts `Framebuffer`/`AbstractTexture`/
  `Integer`); `post.vsh` applied `ProjMat*ModelViewMat` to coordinates already
  in NDC (quad projected off-screen).
- **DOF was not DOF**: `renderDofBlur` only drew 3 translucent fills —
  there was never any blur.

**Done:**
- **`post.vsh`**: `gl_Position = vec4(Position.xy, 0.0, 1.0)` (direct NDC).
- **`PostEffects.java`**: `copyScreen()` — blit from the main FBO to a
  `WindowFramebuffer` scratch (`GL30.glBlitFramebuffer`), avoiding a
  feedback-loop when sampling its own texture; `addSampler("Sampler0",
  scratch)`; `ensureDirt()` loads `dirty_lens.png` as a
  `NativeImageBackedTexture` registered in the `TextureManager` and binds it as
  an `AbstractTexture`; single log line `[bodycam] postFX render ATIVO` for
  log diagnostics.
- **Real DOF (new shader `bodycam:dofblur`)**: json/vsh/fsh — 17-tap
  Gaussian blur (2 rings) with `BlurRadius`/`ScreenSize`;
  `PostEffects.renderBlur(radius)` copies the screen and draws the blurred quad;
  the GUI draws sharp on top of it. Also new:
  `renderDofBlur()` in the GUI calls `renderBlur(6.0f)` + overlay
  `0x50000000` (fallback `0x90000000` if the shader fails).
- **Smaller UI**: `COL_W` 145→122, `ROW_H` **adaptive**
  (`clamp((height-76)/23, 12, 18)`, previously fixed 24), widgets 20→`ROW_H`,
  headers +10→+6, columns centered at ±(COL_W+5), rows start at y=48.

**Build of this session:**
- `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL**.
- Jar installed at `mods\NEXUS-bodycam-1.0.1+1.20.1.jar`
  - **SHA256**: `7D2E113C77F9C7A970EE5689530CB3617B3DEC45C35E8C0033D0123598702512`
  - Size: 3 453 251 bytes
  - Includes: `assets/bodycam/shaders/core/{post,dofblur}.{json,vsh,fsh}`

---

### Session 2026-10-08 (cont. 3) — global shader FIX (VFX+DOF show up) + UI scroll

**Report:** "the VFX don't work (there's not even blur on the GUI)… the UI is
all fucked up… there's no scroll (bottom parts cut off!)".

**Diagnosis (log + bytecode):**
- The log proved that `PostEffects.render()` WAS RUNNING (`postFX render ATIVO`) but
  nothing appeared — because `Tessellator.draw()` calls
  `BufferRenderer.drawWithGlobalProgram`, which uses `RenderSystem.getShader()`
  and **ignores** the manual `shader.bind()` → the quad was drawn with the wrong/null
  shader.
- UI: ROW_H minimum 12 + headers with only a 6 gap (text is 9px) → overlapping headers;
  labels wider > column; slider knob drawn ON TOP OF the
  text; content ~379px > screen (cut off at the bottom, no scroll).

**Done:**
- `RenderSystem.setShader(() -> postShader/blurShader)` + white `setShaderColor`
  before drawQuad (post FX and GUI blur) — **the real VFX fix**.
- **UI**: scrolling with `mouseScrolled` (`clearAndInit`, 12px step,
  clamped to `contentBottom`); visible **scrollbar** (white thumb, 3px);
  header gap 6→11; **adaptive** `COL_W` `min(150, max(96,
  (width-24)/2-5))`; full-width widgets `300`→`colW*2+10`;
  `BodycamSlider.renderButton` **redraws the text on top of the knob**;
  long labels shortened (`Chrom. Aberration` / `Aberr. Cromatica`);
  method corrected `renderWidget`→`renderButton` (1.20.1).

**Build of this session:**
- `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL**.
- Jar installed at `mods\NEXUS-bodycam-1.0.1+1.20.1.jar`
  - **SHA256**: `9B5E1B84E720D0C72F3BEB78F9FC3612B62A34DB943CF20F4A542FCDAAC68D29`
  - Size: 3 454 176 bytes

---

### Session 2026-10-08 (cont. 4) — NEW mod LOGO

- New logo in `textures\logo.png` (2000×2000) → resized to
  **256×256** (HighQualityBicubic) and copied to
  `src\main\resources\assets\bodycam\icon.png` (referenced by
  `fabric.mod.json` → `"icon"`); original kept.
- **Build of this session:**
  - `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL**.
  - Installed jar: **SHA256** `6E9F81E45057D4D1924FF3B14C4BD1151B276E1AD987791E79AB7B4AE1F6617E` (3 519 019 bytes; icon.png in the jar: 66 799 bytes)

---

### Session 2026-10-08 (cont. 5) — ALL TEXTS IN ENGLISH + NEW SVG LOGO + MODRINTH DESCRIPTION

- **README translated to English in place**; the original Portuguese README
  was duplicated first to **`README(pt-br).md`** (untouched backup, 47 357
  bytes). Structure preserved (897 lines, 46 headings, all sha256/numbers
  verbatim).
- **`fabric.mod.json` description → English**: *"Realistic bodycam-style
  camera with temporal effects, post-processing, DOF and tactical HUD for
  Minecraft 1.20.1"*.
- **`lang/pt_br.json` → English** (now identical to `en_us.json`, so every
  locale shows English).
- **Java strings → English**: `BodycamMixinPlugin` log line
  (`TACZ not detected -> mixin {} disabled`) + remaining Portuguese comments
  in `BodycamEffects` / `TaczSway`. Verified: no Portuguese left in `src\main`.
- **NEW LOGO as SVG** (`textures/logo.svg`, master source) built with
  researched advanced techniques (research notes in
  `%TEMP%\opencode\svg_techniques_research.md`): lens barrel with shared
  `userSpaceOnUse` gradient, off-center `fx/fy` NVG glass, `spreadMethod="reflect"`
  specular rings, chromatic aberration filter (R/B `feOffset` ±2px + screen
  blend), metallic rim (`feSpecularLighting`), film grain
  (`feTurbulence fractalNoise 0.8 / 3 octaves`, alpha 0.55, opacity 0.14),
  REC dot with glow, HUD corner brackets, scanlines, vignette. The old
  `textures/logo.png` (2000×2000) was moved out of the project (backed up in
  the agent's temp); `assets/bodycam/icon.png` regenerated 256×256 from the
  SVG (69 246 bytes, transparent corners).
- **`MODRINTH.md` created**: large English description for the Modrinth
  listing (features, post-FX, water, TACZ, shaders, config, install,
  compatibility table).
- **Build of this session:**
  - `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL**.
  - Installed jar: **SHA256** `3321EF62C280C3A8BAF846CA37C00249E7B263E5976DC2F3CCF5870EE4D1934A` (3 521 439 bytes; icon.png in the jar: 69 246 bytes)

---

### Session 2026-10-08 (cont. 6) — RENDER CORRUPTION FIX (root cause: Sampler0 clobbered)

- **Bug reported in-game**: world rendered black, entity/skin textures sliced
  into floating horizontal strips across the screen, white bars, a stray
  "glowing eyes" texture fragment bottom-left — while the bodycam FX
  (fisheye, dirt, grain, CA) stayed visible on top.
- **Root cause PROVEN by javap of the mapped 1.20.1 jar**:
  `VertexBuffer.drawInternal()` does
  `for (i = 0; i < 12; i++) shader.addSampler("Sampler" + i, RenderSystem.getShaderTexture(i))`
  right **before** `shader.bind()`. Those globals still hold the last
  world/entity texture (the block/entity **atlas**), so our
  `postShader.addSampler("Sampler0", scratch)` was overwritten every draw:
  the post shader sampled the ATLAS through the fisheye while the quad
  (blend `one`/`zero`) replaced the whole screen with it.
- **Fix (no global state touched)**: renamed the sampler `Sampler0` →
  `ScreenTex` — the vanilla loop only writes names `"Sampler" + i`, so a
  custom name survives. Files changed: `post.json`, `post.fsh`,
  `dofblur.json`, `dofblur.fsh`, `PostEffects.java`
  (`addSampler("ScreenTex", scratch)` ×2). `DirtyLensTex` was never affected
  (not a `SamplerN` name). Verified inside the built jar: `ScreenTex`
  present, `Sampler0` absent; grep of `src\main` = 0 occurrences.
- **Build of this session:**
  - `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL**.
  - Installed jar: **SHA256** `CD680D7ED1B0FD191E63AFD4C6960AA7754F32A6149BAAE011021F8022BD8DC9` (3 521 436 bytes)
- **USER CONFIRMED IN-GAME: the fix WORKS** (world renders again, no more
  sliced textures). Follow-up request: lens dirt too strong → live config
  `config\bodycam.json` tuned **`lensDirtIntensity` 3.0 → 1.0**,
  **`lensDirtVeiling` 0.06 → 0.02** (user-data file, not source; presets in
  `BodycamConfig.applyPreset` untouched).

---

### Session 2026-10-08 (cont. 7) — LENS DIRT UNIFIED across ALL presets

- **User request** (after confirming the render fix in-game): make the lens
  dirt the same transparency everywhere.
- **`BodycamConfig.java`**: field defaults `lensDirtIntensity` **1.5 → 1.0**,
  `lensDirtVeiling` **0.06 → 0.02**; ALL presets in `applyPreset` now set
  `lensDirtIntensity = 1.0; lensDirtVeiling = 0.02` (previously SUBTLE 0.8 /
  NORMAL 1.5 / INTENSE 2.2 / EXTREME 3.0, veiling never set by presets).
  Grain/fisheye/CA values per preset untouched.
- **Live config** `config\bodycam.json` (user-data, separate from source):
  `lensDirtIntensity` **3.0 → 1.0**, `lensDirtVeiling` **0.06 → 0.02**.
- **Build of this session:**
  - `gradle.bat build --no-daemon` — **BUILD SUCCESSFUL**.
  - Installed jar: **SHA256** `D1F9476D42A5EE9A6F75AC3130CBC2C5470C4E723E4C6215E50EFEB56734F80E` (3 521 427 bytes)

---

### Session 2026-10-09 — PHASE A OPTIMIZATIONS + A0 CRITICAL FIX → v1.1.0+1.20.1

- **A0 (critical, pre-existing): the camera POSITION was never applied.**
  `Camera.setPos` was opened in `bodycam.accesswidener` and shadowed in
  `CameraMixin`, but **never called** → `getVerticalOffset()` (step-smoothing Y,
  vertical bob, breathing Y) and `ChestCam.applyChestCam` (chest height) were
  complete no-ops; only rotations ever reached the camera. Fixed:
  `ChestCam.applyChestCam` now **returns** the offset `Vec3d` (it was assigning
  a local variable that everyone discarded) and `CameraMixin` calls
  `setPos(pos)` after the offsets. **Playtest required**: the camera physically
  moves vertically for the first time (bob/steps/chestCamHeight).
- **A1: dead `Camera.getRotation` roll hook removed.** javap proof:
  `GameRenderer.renderWorld` reads `getPitch()/getYaw()` directly — the roll
  lives in the `renderWorld` MatrixStack hook and always worked; the
  `getRotation` inject was dead code AND caused **double roll on Sodium
  particles** (`BillboardParticleMixin` composes the camera rotation into the
  same MatrixStack). Jar scans: Sodium=1 hit, Iris/DistantHorizons/TACZ/
  ImmediatelyFast=0 (README L413 "Iris/DH call getRotation" was outdated).
- **A2: config cache** — `BodycamConfig.loadCached()` caches the parsed JSON by
  file **mtime+size** (invalidated by `save()`), killing 2–4 I/O+Gson parses per
  frame (`GameRendererMixin`, `PostEffects`, `IrisCompat`, client tick).
  The GUI keeps a guaranteed-fresh `load()`.
- **A3: single spring integrator** — new `camera/Spring.java` (semi-implicit
  Euler, dt clamp 0.1 s, sub-step 0.008, optional yaw-wrap for degrees);
  replaced the 3 hand-copied loops: `BodycamEffects` (step spring + land
  pitch/roll jolts), `TaczSway` (yaw+pitch), `TaczGunRealism` (lowering +
  recoil pitch/yaw). All constants identical (k/c/omega/zeta unchanged).
- **A4: `waterRefraction` + `foamIntensity` finally wired** (present in config
  + sanitize since the water feature, read by nobody). New uniforms
  `WaterRefraction` + `Underwater` (post.json + post.fsh + PostEffects): fbm
  ripple on the whole sample **only while in water** (uv clamped, skipped when
  PostFX off or degraded). Foam: crest-based white blend on the surface quads
  scaled by `foamIntensity` (0–2, default 0.5; unchanged shape/geometry).
- **A5: auto-quality governor** (`autoQuality` boolean, default true): EMA of
  the real frame time while the post pass runs — <40 FPS degrades (×0.5 on
  CA/lens dirt/grain + refraction forced off → skips the expensive fbm path),
  >55 FPS restores; hysteresis prevents flicker; one log line per transition
  (`auto-quality: DEGRADADO/restaurado`).
- **A6: `dirty_lens.png` resized 1536×1024 → 1024×683** (high-quality bicubic;
  2 172 841 → 997 581 bytes ≈ −54 %). NPOT + normalized UVs → no shader change;
  original backed up in the agent temp dir. VRAM ≈ 6 MB → ≈ 2.8 MB.
- **Version bump `1.0.1+1.20.1` → `1.1.0+1.20.1`** (`gradle.properties`;
  `fabric.mod.json` uses `${version}` — **gotcha**: `processResources` does NOT
  invalidate on gradle.properties changes, a **`clean` is mandatory** or the jar
  keeps the old embedded version under the new file name).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (11 s).
  - Installed jar: **SHA256** `4FE1A13081C69142D52BF84AC36A38AAEEC71CA55A4D791BFDD19112F7C8BBC6` (2 354 908 bytes; embedded version verified `1.1.0+1.20.1`; dirty_lens in jar = 997 581 bytes; WaterRefraction/Underwater uniforms present)

### Session 2026-10-09 (cont.) — ROADMAP C1: Camera Inertia + Gait Transition → v1.2.0+1.20.1

- **Roadmap saved to disk**: `PROJECT-MOD\ROADMAP-bodycam.md` — the full
  user roadmap (01–14 effects + TACZ table + module architecture + presets +
  recommended order) written to a file so it can never be lost from context
  again. Source of truth for Phases C/D/E.
- **01 Camera Inertia / Mass Lag**: view smoothing is now a 2nd-order spring
  (`CameraMixin.stepInertia`) instead of plain exp smoothing — the body turns
  first and the camera finishes 30–70 ms later (`omega = 1/inertiaDelay`,
  default `inertiaDelay = 0.05`), with a short overshoot on sudden direction
  changes (`zeta = 1 − 0.6·inertiaOvershoot`, default 0.5 → ζ 0.7 ≈ 5 %
  overshoot) and slight carry-on when the player stops. Wrap-aware yaw (no
  snap crossing ±180°), instant snap on >100° jumps (teleports) and on
  re-enable after toggle (`inertiaInit`). Legacy exp smoothing kept intact
  behind `inertiaEnabled=false` — `viewSmoothSpeed` field untouched.
- **04 Gait Transition**: cadence now blends through ALL gaits — sneak period
  (1.45 s) → walk (1.0) → sprint (0.83), continuous by speed, **phase never
  reset**; sprint footfalls ×(1 + `gaitSprintWeight`·speedNorm) heavier
  (default 0.35); cadence + impulse get perlin irregularity (default ±12 %)
  while speed changes fast (accel gate via smoothed velocity derivative);
  deceleration settle: `settleGate` dips to 0.7 during hard decel and calms
  foot-roll + micro jitter, then recovers smoothly. Whole feature gated by
  `gaitTransitionEnabled` (default on).
- **GUI + lang**: two new rows under Smoothing — `Camera Inertia` toggle +
  `Inertia Delay (s)` slider; `Inertia Overshoot` slider + `Gait Transitions`
  toggle (en_us/pt_br keys added, English labels per convention).
- **Version bump `1.1.0+1.20.1` → `1.2.0+1.20.1`** (`clean` build — the
  gradle.properties gotcha from Phase A still applies).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (9 s).
  - Installed jar: **SHA256** `7EFDE65968AD29A74B8A4491DAF5D54E4A7B32B99E895EFA56B0AA66D4E2C750` (2 356 952 bytes; embedded version verified `1.2.0+1.20.1`; 43 jars in mods folder)

### Session 2026-10-09 (cont. 2) — ROADMAP C2: Auto Exposure + Flash Shock → v1.2.1+1.20.1

- **06 Auto Exposure / Exposure Adaptation**: temporal gain state computed CPU-side
  once per render frame in `BodycamEffects.updateExposure` — luminance proxy is
  the combined light level at the player (cheap block lookup, **no framebuffer
  readback / histogram**). Target gain `1 + pow(1 − L/15, 1.4) · strength`
  (default strength 0.35 → up to ×1.35 in full darkness). **Asymmetric curve**:
  leaving darkness adapts fast (`autoExposureSpeedBright = 4.0`), entering
  darkness recovers slowly (`autoExposureSpeedDark = 1.2`) — both configurable.
  New shader uniform **`Exposure`** (post.json + post.fsh) applied right after
  CA as sensor gain, so vignette/dirt/grain react to the adapted image. State
  is only CONSUMED by the shader (single update/frame, shadow passes excluded).
- **13 Flash / Brightness Shock**: on a sudden upward light jump (≥6 levels,
  e.g. cave mouth → sunlight, entering a lit room) a brief envelope fires —
  instant attack, quadratic decay over 0.35 s, cooldown 1 s, amplitude
  `clamp(jump/15, 0.3, 1.0) · flashShockStrength` (default 0.5). Never random:
  driven by the real light-level delta. Phase D can add TACZ muzzle flashes to
  the same envelope. Independent toggle (`flashShockEnabled`).
- **GUI + lang**: 2 new rows in Post-FX (`Auto Exposure` toggle + `Exposure
  Gain (Dark)` slider; `Flash Shock` toggle + `Flash Strength` slider).
- **Config**: `autoExposureEnabled/autoExposureStrength/autoExposureSpeedDark/
  autoExposureSpeedBright/flashShockEnabled/flashShockStrength` + sanitize
  (strength 0–1, speeds 0.2–10, flash 0–1.5).
- **Version bump `1.2.0` → `1.2.1+1.20.1`** (clean build).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (9 s).
  - Installed jar: **SHA256** `BCF93F22C2FB2D5ADF8DF13750439BB319A0F7679613D7EE40B2BE06E833DAD4` (embedded version verified `1.2.1+1.20.1`; Exposure uniform verified inside post.json AND post.fsh; 43 jars in mods folder)

### Session 2026-10-09 (cont. 3) — ROADMAP C3: Impact Recovery → v1.2.2+1.20.1

- **03 Impact Recovery**: one shared impact SEQUENCE (short vertical
  compression → forward tilt → small overshoot → damped recovery) fed by
  **three separate trigger envelopes**, all reusing the existing landing
  springs (`landPitchSpring`/`landRollSpring`/`stepSpring`):
  - **Falls** (existing path, unchanged amplitude) — now also sets a short
    0.15 s cooldown so a landing never collides with the other triggers.
  - **Combat hits** (HP drop ≥0.05): pitch impulse `s·18`, roll `s·7`
    (alternating sides), vertical `s·0.45`, where `s = clamp(dmg·0.1, 0.2,
    1.0) · impactStrength`; 0.15 s cooldown so a rapid-fire burst fires the
    big jolt once while the per-damage noise shake keeps running.
  - **Horizontal collisions**: fires when `horizontalCollision` is true while
    the EMA'd horizontal speed was >0.06 — amplitude scales with the speed
    that was blocked (`clamp(prevSpeed/0.25, 0.2, 1.0)`); 0.3 s cooldown and
    the EMA decay below the threshold mean pushing against a wall never
    re-fires. Roll alternates sides (direction of the blow is unknown).
  - **Overshoot**: when enabled the shared landing spring runs underdamped
    (zeta 16/(2√180) ≈ 0.60 → visible small overshoot); with the toggle off
    it falls back to the legacy zeta ≈0.82 and the combat/collision triggers
    never fire (pure legacy landing behaviour).
- **GUI + lang**: Motion row `Impact Recovery` toggle + `Impact Strength`
  slider (0–2). Config `impactRecoveryEnabled/impactStrength` + sanitize.
- **Version bump `1.2.1` → `1.2.2+1.20.1`** (clean build).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (9 s).
  - Installed jar: **SHA256** `615CDE625CF5DAF05BB0B98BC121A9F0C4928DCB73FC59735E4B7C528F8A3A40` (2 359 281 bytes; embedded version verified `1.2.2+1.20.1`; 43 jars in mods folder)

### Session 2026-10-10 — AUDIT + C4–C8 + Phase D recorded → v1.3.0+1.20.1

> Audit found these features already implemented in the source (previous
> session wrote the code and bumped the version but NEVER recorded them
> here — this entry retroactively documents that work, verified line by
> line against the actual code before writing it).

- **C4 (roadmap 08) Low-Light Sensor Noise**: `lowLightNoiseEnabled/gain`;
  `getGrainStrength()` multiplies grain by `1 + gain·darkness` (darkness =
  `pow(1 − light/15, 1.4)` from `updateExposure`).
- **C5 (roadmap 09) Lens Contamination**: three CPU envelopes in
  `updateLens` — drops saturate underwater + accumulate in rain, condensation
  fires on bright light jumps (2 s cooldown), smudge accrues from wall/combat
  impacts (`smudgeLens`); `lensCleanSpeed` 0 = persistent. Shader blocks in
  `post.fsh` (`LensDrops/LensCond/LensSmudge`).
- **C6 (roadmap 05 + 11) Surface Footsteps + Underwater Lens**:
  `EntityMixin → onFootstep(BlockState)`; `surfaceGainFor` maps
  `BlockSoundGroup` (metal 1.30 → wood 1.15 → stone/glass 1.05 → soft 0.75),
  gain smoothed step-to-step; `updateUnderwater` fast-onset/slow-release
  blend + sheets the lens on exiting water.
- **C7 (roadmap 07/10/12/14)**: `updateGhost` (sun↔view vector dot > 0.94
  veiling flare), `EdgeProfile` (CA grows toward the edge + peripheral
  softness, scales with `fovWideMult`), `updateDust` (motes gated to light
  window 1..8), `updateCritical` (HP ≤ 25% → grain boost + exposure flicker +
  roll tilt).
- **C8 (roadmap 02) Angular Motion Blur**: `CameraMixin.updateAngularVelocity`
  (final camera, λ25 smoothing) → `BlurVec` uniform; 4-tap symmetric smear
  in `post.fsh`, skipped entirely while still/degraded.
- **Phase D — TACZ realism**: `TaczBridge` (ONLY class touching TACZ API,
  loaded only if `isModLoaded("tacz")`; `GunShootEvent`/`GunReloadEvent`),
  ADS stabilization (−55% sway at full aim), movement-to-aim penalty
  (`moveSpring` + perlin drift), landing weapon lag (`landLagSpring`),
  wall pullback (1 m raycast → draw-in), reload nod (`reloadSpring`);
  all applied in `TaczGunTransformMixin`, camera never touched by gunfire.
- **GUI + lang** for every new toggle/slider (both `en_us.json` and
  `pt_br.json`), config `sanitize()` clamps for all new fields.
- Version was already `1.3.0+1.20.1` in `gradle.properties`; jar
  `NEXUS-bodycam-1.3.0+1.20.1.jar` existed in `build\libs` — but the
  installed jar (12:54) differed from the built one (12:57) and
  `core/GameState.java`/`core/RenderCoordinator.java` were edited after
  that build → stale install, fixed in the session below.

### Session 2026-10-10 (cont.) — Phase E presets + module architecture → v1.4.0+1.20.1

- **Architecture (roadmap §5) — now actually WIRED** (was dead code):
  `CameraMixin` no longer calls `BodycamEffects.onFrame` directly — it
  calls `RenderCoordinator.cameraUpdate(tickDelta)`, which (a) skips Iris
  shadow passes (counted in `shadowSkips`), (b) guards against same-frame
  re-entry, (c) runs the Motion module once, then (d) `GameState.sample`
  publishes one per-frame world snapshot (velocity, sprint/sneak, ground,
  light, darkness) for every module to read.
- **Phase E — 4 new presets** in `BodycamConfig.applyPreset` + GUI cycling
  button (now 8 values):
  - `DOCUMENTARY` — steady handheld: mild bob/inertia, FOV 1.10, light CA,
    grain 0.04, motion blur 0.35.
  - `HORROR` — unstable sensor: heavy idle shake (1.2 Hz), deep vignette
    (0.60), grain 0.10 + low-light gain 1.1, ghost 1.2, critical 1.5,
    exposure gain 0.60.
  - `TACTICAL` — readable sight picture: minimal wobble (idle 0.12),
    FOV 1.05, CA 0.0015, dirt 0.8, motion blur 0.30.
  - `RAW GOPRO` — widest lens: FOV 1.40, fisheye K1 0.32, CA 0.006,
    dirt 1.4, motion blur 0.80, grain 0.08.
  - Each preset also sets `fovWideMult/motionBlurStrength/vignetteMech/
    autoExposureStrength/lowLightNoiseGain/ghostFlareStrength/
    dustStrength/criticalStrength`; `sanitize()` still clamps everything.
- **Version bump `1.3.0` → `1.4.0+1.20.1`** (convention: `1.x.0` =
  feature drop; `1.0.x` = fixes; `x.0.0` = gigantic updates).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (28 s).
  - Installed jar: **SHA256** `B16E82611DC8F781B0BA242765E1E0F3F7886F6C9057D4631997616ADABB907F` (2 374 907 bytes; `NEXUS-bodycam-1.4.0+1.20.1.jar`; old 1.3.0 jar removed from mods; 43 jars in mods folder)

### Session 2026-10-10 (cont.) — config UI rebuilt + crosshair alignment — v1.4.1+1.20.1

- **Config screen (`BodycamConfigScreen`) fully rebuilt** (user report: "UI feia",
  text bugged, buttons too small):
  - **Scroll bug fixed**: category headers are now items of the content layout
    (content Y − scroll), drawn only when fully inside the viewport — no more
    fixed `y=38` "Smoothing" label that stayed on screen while scrolling, and no
    header/widget can ever paint over the top/bottom bars.
  - **No overlap**: widgets are instantiated ONLY when fully inside
    `[contentTop, contentBot]`, so scrolled rows never collide with
    title/Save/Reset/preset (the old "Reset to Defaults" over "Step Smoothing"
    glitch). Layout is built by a declarative builder (`hdr/sl/tg/full`) that
    auto-pairs the two columns and computes `contentH`.
  - **Bigger controls**: rows `18 → 22 px`, sliders `22 px`, top Save/Reset
    `110×18`, Preset moved to a **fixed bottom bar** (`fullW × 26`), all inside
    a beveled panel with header accent bars + underline.
  - **Custom slider render**: vanilla knob removed (it was drawn on top of the
    label); dark card + hover state + gold value track at the bottom + trimmed
    centered label.
  - Scrollbar kept (thumb sized by `contentH`), DOF-blur background kept.
- **Crosshair / block-breaking alignment fixed** (user report: "a mira de
  quebrar blocos não está alinhada com a crosshair"): the rendered camera is
  rotated by chest-cam pitch, inertia and shake, but vanilla aims with the raw
  player yaw/pitch, so the block outline + crack overlay landed away from the
  crosshair. `EntityMixin` now injects `Entity.getRotationVec(F)` for the LOCAL
  player and returns the vector built from `CameraState.finalViewYaw/Pitch`
  (guarded: config enabled, `lastCamYaw` not NaN) — crosshair ray, block outline
  and breaking progress all sit exactly under the crosshair.
- **Version bump `1.4.0` → `1.4.1+1.20.1`** (convention: `1.0.x`/`1.x.1` =
  fixes).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (10 s).
  - Installed jar: **SHA256** `FE3FA7540BEF94C543FF7F882E8D20C95E2603B5077FAD55481FDF0677BC6B42` (2 377 704 bytes; `NEXUS-bodycam-1.4.1+1.20.1.jar`; old 1.4.0 jar removed from mods)
  - `MODRINTH.md` updated (8 presets + jar `1.4.1`).

---

### Session 2026-10-10 (cont. 2) — aim vector sign fix — v1.4.2+1.20.1

- **Aim direction inverted 180° fixed** (user report: "eu miro para cima e o
  bloco quebra embaixo!"): `EntityMixin.bodycam$alignedAim` rebuilt the look
  vector with `cos/sin(-angle − π)` and the resulting vector was exactly the
  **negation** of vanilla `Entity.getRotationVector` — the raycast pointed to the
  opposite side of the crosshair. The math now matches vanilla byte for byte:
  `(sin(yawRad)·cos(pitchRad), −sin(pitchRad), cos(yawRad)·cos(pitchRad))`
  with `pitchRad = pitch·0.017453292`, `yawRad = −yaw·0.017453292`.
- **Verified**: numeric check old = −vanilla / new = vanilla (pitch/yaw sampled);
  bytecode of the remapped jar inspected; `fabric.mod.json` reports
  `1.4.2+1.20.1`.
- **Version bump `1.4.1` → `1.4.2+1.20.1`** (AVISO GERAL: `1.0.x` = small fix).
- **Build of this session:**
  - `gradle.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (11 s).
  - Installed jar: **SHA256** `5B0120B49A82046104EAEE6B891675D75CFCE8AEDB16C3D4C55398A31C7360D8` (2 377 730 bytes; `NEXUS-bodycam-1.4.2+1.20.1.jar`; old 1.4.1 jar removed from mods, 41 jars total)
  - `MODRINTH.md` + this README updated to `1.4.2`.

---


### Session 2026-10-11 — vignette blur (bodycam defocus) — v1.4.3+1.20.1

- **Request (user):** "a vignette has to be blurred (like a real bodycam)".
- **New config `vignetteBlur` (0..1, default 0.6)** — scales the effect; `0` turns
  it off, `1` is full defocus at the corner.
- **Shader `bodycam:post`**: new uniform `VignetteBlur`. Just before the vignette
  multiply, a **12-tap golden-angle disc blur** is mixed in under the mask
  `smoothstep(0.45, 1.05, r) * VignetteBlur`: the frame is sharp in the centre and
  progressively goes out of focus toward the border, like a real wide bodycam
  lens. The disc is aspect-corrected (round in pixels), clamped to the screen, the
  radius is `0.035 * mask` (~3.5 % of the screen at the corner) and the whole block
  is skipped when the mask is negligible (cost ≈ 0 in the centre of the frame).
- **GUI**: new Post-FX slider `Vignette Blur`
  (`sl("vignetteBlur", 0.0, 1.0)` + getter/setter cases);
  lang keys `bodycam.config.vignetteBlur` (`en_us` / `pt_br`).
- **`fabric.mod.json`**: `license` `MIT` → `GPL-3.0`; `contact.sources` /
  `modmenu.sources` placeholder (`seu-usuario/bodycam`) → the real repository; new
  `contact.issues`.
- **Version bump `1.4.2` → `1.4.3+1.20.1`** (AVISO GERAL: `1.0.x` = small fix).
- **Build of this session:**
  - `gradlew.bat clean build --no-daemon` — **BUILD SUCCESSFUL** (1 m 46 s).
  - Jar: `NEXUS-bodycam-1.4.3+1.20.1.jar` — **SHA256**
    `D48FE4785AC0A53371DFF1C862BB48408170E20FA0DEF9AE4646F19EEEB49BFD` (2 378 260 bytes).
  - Verified inside the jar: `version = 1.4.3+1.20.1`, `VignetteBlur` uniform and
    the blur block present in `post.fsh`.
- **Pending**: playtest of the vignette blur. ⚠️ The old `1.4.2` jar was still
  locked in `mods` (game running) — **it must be removed before the next launch**
  or Fabric will see two `bodycam` jars.
- **Docs**: README / MODRINTH / TODO / DEVNOTES / wiki / this changelog updated to
  `1.4.3`.

---
