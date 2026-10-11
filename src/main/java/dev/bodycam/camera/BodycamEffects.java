package dev.bodycam.camera;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BodycamEffects {
    private static final CameraState state = CameraState.INSTANCE;
    private static BodycamConfig config = BodycamConfig.load();

    private static final double WALK_SPEED = 0.216;
    private static final double SPRINT_SPEED = 0.281;
    private static final double WALK_PERIOD = 1.0;
    private static final double SPRINT_PERIOD = 0.83;
    private static final double SNEAK_PERIOD = 1.45;

    private static long lastNanos = 0;

    public static long frames = 0;
    public static long footstepCount = 0;
    public static double lastDt = 0.0;
    public static boolean hookCameraLogado = false;
    public static boolean hookRollLogado = false;

    public static void updateConfig(BodycamConfig newConfig) {
        config = newConfig;
    }

    private static double frameDelta() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        dt = MathHelper.clamp(dt, 0.0, 0.1);
        lastDt = dt;
        return dt;
    }

    private static double nowSec() {
        return System.nanoTime() / 1.0e9;
    }

    public static void onFootstep() {
        onFootstep(null);
    }

    /**
     * Roadmap 05 — Surface Footsteps: the block under the player shapes the
     * footfall impulse (metal rings harder, soft ground damps). The gain is
     * smoothed step-to-step so moving across materials never pops.
     */
    public static void onFootstep(BlockState blockState) {
        if (!config.enabled) return;
        footstepCount++;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        double speed = Math.sqrt(player.getVelocity().x * player.getVelocity().x + player.getVelocity().z * player.getVelocity().z);
        double speedNorm = MathHelper.clamp(speed / SPRINT_SPEED, 0.0, 1.0);
        if (speedNorm < 0.10) return;

        double target = 1.0;
        if (config.surfaceFootstepsEnabled && blockState != null) {
            target = surfaceGainFor(blockState);
        }
        state.surfaceGain = state.surfaceGain + (target - state.surfaceGain) * 0.5;
        double gain = config.surfaceFootstepsEnabled
                ? state.surfaceGain * config.surfaceFootstepGain : 1.0;

        double t = nowSec();
        if (state.lastFootstepT > 0) {
            double e = t - state.lastFootstepT;
            if (e > 0.06 && e < 2.0) {
                state.footInterval = state.footInterval <= 0.05 ? e : state.footInterval * 0.8 + e * 0.2;
            }
        }
        state.lastFootstepT = t;

        state.bobPhase = Math.floor(state.bobPhase / Math.PI + 0.5) * Math.PI;
        state.lastFootPhase = state.bobPhase;

        state.footSide = -state.footSide;
        double sprintWeight = config.gaitTransitionEnabled && player.isSprinting()
                ? 1.0 + config.gaitSprintWeight * speedNorm : 1.0;
        state.stepSpring.impulse(-2.0 * speedNorm * speedNorm * config.bobbing * sprintWeight * gain);
    }

    private static double surfaceGainFor(BlockState blockState) {
        try {
            BlockSoundGroup g = blockState.getSoundGroup();
            if (g == BlockSoundGroup.METAL) return 1.30;
            if (g == BlockSoundGroup.WOOD || g == BlockSoundGroup.NETHER_WOOD
                    || g == BlockSoundGroup.CHERRY_WOOD || g == BlockSoundGroup.BAMBOO_WOOD) return 1.15;
            if (g == BlockSoundGroup.STONE || g == BlockSoundGroup.DEEPSLATE
                    || g == BlockSoundGroup.GLASS || g == BlockSoundGroup.AMETHYST_BLOCK) return 1.05;
            if (g == BlockSoundGroup.GRAVEL || g == BlockSoundGroup.SAND
                    || g == BlockSoundGroup.GRASS || g == BlockSoundGroup.SNOW
                    || g == BlockSoundGroup.WOOL || g == BlockSoundGroup.MOSS_CARPET
                    || g == BlockSoundGroup.ROOTED_DIRT || g == BlockSoundGroup.POWDER_SNOW) return 0.75;
        } catch (Exception ignored) {}
        return 1.0;
    }

    public static void onFrame(float tickDelta) {
        frames++;
        if (!config.enabled) {
            reset();
            return;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        double dt = frameDelta();
        double speed = Math.sqrt(player.getVelocity().x * player.getVelocity().x + player.getVelocity().z * player.getVelocity().z);
        double speedNorm = MathHelper.clamp(speed / SPRINT_SPEED, 0.0, 1.0);

        updateStepSmoothing(player, dt);
        updateBobbing(speed, speedNorm, dt);
        updateMicroJitter(dt, speedNorm);
        updateLean(dt, speedNorm);
        updateBreath(player, dt, speed);
        updateIdleShake(dt);
        updateDamage(player, dt);
        updateRoll(player, dt);
        updateEnvironment(player, dt);
    }

    // = roadmap 06 Auto Exposure + 13 Flash/Brightness Shock ===============

    private static final double FLASH_DUR = 0.35;

    /**
     * Temporal exposure state for the post shader. Luminance proxy = combined
     * light level at the player (cheap block lookup, no framebuffer readback /
     * histogram). Adaptation is asymmetric: leaving darkness adapts fast,
     * entering darkness recovers slowly. A sudden upward light jump (cave
     * mouth, lit room, later a muzzle flash via Phase D) fires a brief
     * brightness-shock envelope. The shader only CONSUMES the value — state
     * updates here once per render frame (shadow passes never call onFrame).
     */
    private static void updateExposure(ClientPlayerEntity player, double dt) {
        if (!config.autoExposureEnabled && !config.flashShockEnabled) {
            state.exposureMult = 1.0;
            state.flashT = 0.0;
            double l = MathHelper.clamp(player.getWorld().getLightLevel(player.getBlockPos()), 0, 15);
            state.darkness = Math.pow(1.0 - l / 15.0, 1.4);
            state.lightLevel = l;
            return;
        }
        double light = MathHelper.clamp(player.getWorld().getLightLevel(player.getBlockPos()), 0, 15);

        double darkness = Math.pow(1.0 - light / 15.0, 1.4);
        state.darkness = darkness;
        double target = config.autoExposureEnabled
                ? 1.0 + darkness * config.autoExposureStrength : 1.0;
        double rate = target > state.exposureMult
                ? config.autoExposureSpeedDark : config.autoExposureSpeedBright;
        state.exposureMult += (target - state.exposureMult) * (1.0 - Math.exp(-rate * dt));

        if (config.flashShockEnabled && state.lightLevel >= 0.0) {
            state.flashCooldown = Math.max(0.0, state.flashCooldown - dt);
            double jump = light - state.lightLevel;
            if (jump >= 6.0 && state.flashCooldown <= 0.0) {
                state.flashAmp = MathHelper.clamp(jump / 15.0, 0.3, 1.0) * config.flashShockStrength;
                state.flashT = FLASH_DUR;
                state.flashCooldown = 1.0;
            }
        }
        state.lightLevel = light;
        if (state.flashT > 0.0) state.flashT = Math.max(0.0, state.flashT - dt);
    }

    /** Combined exposure gain (auto adaptation + flash envelope) for the post shader. */
    public static double getExposure() {
        double mult = state.exposureMult;
        if (state.flashT > 0.0) {
            double x = state.flashT / FLASH_DUR;
            mult += state.flashAmp * x * x;
        }
        // roadmap 14 — critical damage: unstable sensor supply (flicker)
        if (config.criticalCamEnabled && state.criticalAmt > 0.01) {
            mult += 0.08 * state.criticalAmt * config.criticalStrength
                    * TemporalSmoother.perlin2D(state.microT * 1.7, 4.2);
        }
        return MathHelper.clamp(mult, 0.5, 2.5);
    }

    // = roadmap 08/09/11/07/12/14 — environment envelope pass =============

    /**
     * All state the post shader consumes but does not simulate itself. Runs
     * once per render frame from onFrame (shadow passes only consume
     * uniforms — they never update state).
     */
    private static void updateEnvironment(ClientPlayerEntity player, double dt) {
        updateExposure(player, dt);
        updateUnderwater(player, dt);
        updateLens(player, dt);
        updateGhost(player, dt);
        updateDust(player, dt);
        updateCritical(player, dt);
    }

    /** Roadmap 11 — underwater blend: fast onset (lambda 6), slow release (1.5). */
    private static void updateUnderwater(ClientPlayerEntity player, double dt) {
        boolean inWater = WaterEffects.isInWater() && config.waterEnabled;
        double target = inWater ? 1.0 : 0.0;
        double lambda = target > state.underwaterT ? 6.0 : 1.5;
        state.underwaterT = TemporalSmoother.expSmooth(state.underwaterT, target, lambda, dt);
        if (state.underwaterT < 0.001) state.underwaterT = 0.0;
        // leaving the water sheets the lens (feeds the drops envelope below)
        if (state.wasInWater && !inWater) state.lensDrops = 1.0;
        state.wasInWater = inWater;
    }

    /**
     * Roadmap 09 — Lens Contamination: three independent envelopes.
     * Drops saturate while submerged and accumulate in rain (open sky),
     * condensation snaps on a bright light shock, smudge builds on impacts.
     * lensCleanSpeed 0 = persistent (real bodycams are never cleaned).
     */
    private static void updateLens(ClientPlayerEntity player, double dt) {
        if (!config.lensContaminationEnabled) {
            state.lensDrops = state.lensCond = state.lensSmudge = 0.0;
            state.condCd = Math.max(0.0, state.condCd - dt);
            return;
        }
        state.condCd = Math.max(0.0, state.condCd - dt);

        if (state.underwaterT > 0.5) {
            state.lensDrops = 1.0;
        } else if (player.getWorld().isRaining() && state.lightLevel >= 9.0) {
            state.lensDrops = Math.min(1.0, state.lensDrops + dt * 0.35);
        }

        // bright light jump (cave mouth / lit room) condenses the cold lens
        if (state.flashT > 0.0 && state.condCd <= 0.0) {
            state.lensCond = 1.0;
            state.condCd = 2.0;
        }

        double clean = config.lensCleanSpeed * dt;
        state.lensDrops = Math.max(0.0, state.lensDrops - clean * 0.15);
        state.lensCond = Math.max(0.0, state.lensCond - clean * 0.5);
        state.lensSmudge = Math.max(0.0, state.lensSmudge - clean * 0.25);
    }

    /** Called from the impact triggers (collision / damage) — roadmap 09. */
    private static void smudgeLens(double amount) {
        if (config.lensContaminationEnabled) {
            state.lensSmudge = Math.min(1.0, state.lensSmudge + amount);
        }
    }

    /**
     * Roadmap 07 — Lens Ghosting: veiling flare when the sun sits almost
     * exactly along the view vector. Attack is slow (2.5), release slow-ish
     * (0.8) so the ghost blooms and fades like real glass reflections.
     */
    private static void updateGhost(ClientPlayerEntity player, double dt) {
        double target = 0.0;
        if (config.ghostFlareEnabled) {
            long ticks = player.getWorld().getTimeOfDay();
            double tod = (((ticks % 24000L) + 24000L) % 24000L) / 24000.0;
            double dayAmt = tod <= 0.5 ? Math.sin(Math.PI * (tod / 0.5)) : 0.0;
            if (dayAmt > 0.01 && state.lightLevel >= 12.0) {
                double a = Math.PI * (tod / 0.5);
                Vec3d sun = new Vec3d(Math.cos(a), Math.sin(a), Math.sin(a)).normalize();
                double yr = Math.toRadians(state.finalViewYaw);
                double pr = Math.toRadians(state.finalViewPitch);
                Vec3d look = new Vec3d(
                        -Math.sin(yr) * Math.cos(pr),
                        -Math.sin(pr),
                        Math.cos(yr) * Math.cos(pr));
                double d = look.dotProduct(sun);
                double t = MathHelper.clamp((d - 0.94) / (0.996 - 0.94), 0.0, 1.0);
                target = t * t * (3.0 - 2.0 * t) * dayAmt * config.ghostFlareStrength;
            }
        }
        double lambda = target > state.ghostAmt ? 2.5 : 0.8;
        state.ghostAmt = TemporalSmoother.expSmooth(state.ghostAmt, target, lambda, dt);
    }

    /** Roadmap 12 — dust motes only read inside a shaft of light (window 1..8). */
    private static void updateDust(ClientPlayerEntity player, double dt) {
        double target = 0.0;
        if (config.dustEnabled && state.lightLevel >= 0.0) {
            double l = state.lightLevel;
            double gate = smoothstep(1.0, 3.0, l) * (1.0 - smoothstep(7.0, 10.0, l));
            target = config.dustStrength * gate;
        }
        state.dustAmt = TemporalSmoother.expSmooth(state.dustAmt, target, 2.0, dt);
    }

    /** Roadmap 14 — critical damage: fast onset (3.0), slow settle (0.35). */
    private static void updateCritical(ClientPlayerEntity player, double dt) {
        double frac = player.getMaxHealth() > 0.0f ? player.getHealth() / player.getMaxHealth() : 1.0;
        double target = config.criticalCamEnabled && frac <= 0.25 ? 1.0 : 0.0;
        double lambda = target > state.criticalAmt ? 3.0 : 0.35;
        state.criticalAmt = TemporalSmoother.expSmooth(state.criticalAmt, target, lambda, dt);
    }

    private static double smoothstep(double a, double b, double x) {
        double t = MathHelper.clamp((x - a) / (b - a), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    // = uniform getters consumed by PostEffects ===========================

    /** Grain gain with low-light boost (08) and critical boost (14) baked in. */
    public static double getGrainStrength() {
        double g = config.grainStrength;
        if (config.lowLightNoiseEnabled) g *= 1.0 + config.lowLightNoiseGain * state.darkness;
        if (config.criticalCamEnabled) g *= 1.0 + 1.5 * state.criticalAmt * config.criticalStrength;
        return g;
    }

    public static double getLensDrops() { return config.lensContaminationEnabled ? state.lensDrops : 0.0; }
    public static double getLensCond() {
        return config.lensContaminationEnabled ? state.lensCond * config.lensCondStrength : 0.0;
    }
    public static double getLensSmudge() { return config.lensContaminationEnabled ? state.lensSmudge : 0.0; }
    public static double getGhostAmt() { return state.ghostAmt; }
    public static double getDustAmt() { return state.dustAmt; }
    public static double getCriticalAmt() { return state.criticalAmt; }
    public static double getUnderwaterT() { return state.underwaterT; }

    private static void updateStepSmoothing(ClientPlayerEntity player, double dt) {
        boolean onGround = player.isOnGround();
        state.impactCd = Math.max(0.0, state.impactCd - dt);
        if (!onGround) {
            state.airborneMaxFall = Math.max(state.airborneMaxFall, player.fallDistance);
        }
        if (!state.wasOnGround && onGround && state.airborneMaxFall > 0.0) {
            double impact = Math.min(state.airborneMaxFall * 0.15, 1.5) * config.landingDamping;
            state.stepSpring.impulse(-impact);
            // jolto rotacional de aterrissagem: mola amortecida em pitch + roll
            state.landPitchSpring.impulse(impact * 50.0);
            state.landRollSpring.impulse(impact * 25.0 * state.footSide);
            state.impactCd = 0.15;
            TaczGunRealism.onLanding(impact);
        }
        // roadmap 03 — collision impact: was moving, now blocked by a wall
        // (prevSpeed is the EMA'd horizontal speed — it decays below the
        // threshold while pushing, so a held-against-wall contact never re-fires)
        if (config.impactRecoveryEnabled && player.horizontalCollision
                && state.impactCd <= 0.0 && state.prevSpeed > 0.06) {
            double s = MathHelper.clamp(state.prevSpeed / 0.25, 0.2, 1.0) * config.impactStrength;
            state.landPitchSpring.impulse(s * 22.0);
            state.landRollSpring.impulse(s * 8.0 * state.impactSide);
            state.stepSpring.impulse(-s * 0.4);
            state.impactSide = -state.impactSide;
            state.impactCd = 0.3;
            smudgeLens(s * 0.5);
        }
        if (onGround) {
            state.airborneMaxFall = 0.0;
        }
        state.wasOnGround = onGround;
        state.fallDistance = player.fallDistance;

        double k = 220.0 * (1.0 + config.stepSmoothing);
        double c = 30.0 * (1.0 + config.stepSmoothing * 0.5);
        double omega = Math.sqrt(k);
        state.stepSpring.step(dt, omega, c / (2.0 * omega), 0.0);

        double omegaL = Math.sqrt(180.0);
        // roadmap 03 — underdamped recovery (zeta ~0.60, small overshoot) when
        // impact recovery is on; legacy zeta 0.82 when disabled
        double zetaL = (config.impactRecoveryEnabled ? 16.0 : 22.0) / (2.0 * omegaL);
        state.landPitchSpring.step(dt, omegaL, zetaL, 0.0);
        state.landRollSpring.step(dt, omegaL, zetaL, 0.0);
    }

    private static final double DAMAGE_SHAKE_DUR = 0.4;

    private static void updateDamage(ClientPlayerEntity player, double dt) {
        float hp = player.getHealth();
        if (state.prevHealth < 0.0f) {
            state.prevHealth = hp;
            return;
        }
        if (hp < state.prevHealth - 0.05f) {
            float dmg = state.prevHealth - hp;
            state.damageAmp = MathHelper.clamp(dmg * 0.14f, 0.25f, 1.4f) * config.swayIntensity;
            state.damageShakeT = DAMAGE_SHAKE_DUR;
            // roadmap 03 — combat impact: compress + tilt into the hit, own
            // envelope (short cooldown so a rapid-fire burst fires once,
            // while the per-damage noise shake keeps running)
            if (config.impactRecoveryEnabled && state.impactCd <= 0.0) {
                double s = MathHelper.clamp(dmg * 0.1, 0.2, 1.0) * config.impactStrength;
                state.landPitchSpring.impulse(s * 18.0);
                state.landRollSpring.impulse(s * 7.0 * state.impactSide);
                state.stepSpring.impulse(-s * 0.45);
                state.impactSide = -state.impactSide;
                state.impactCd = 0.15;
                smudgeLens(s * 0.6);
            }
        }
        state.prevHealth = hp;

        if (state.damageShakeT > 0.0) {
            state.damageShakeT = Math.max(0.0, state.damageShakeT - dt);
            state.damageNoiseT += dt * 28.0;
            if (state.damageShakeT <= 0.0) state.damageAmp = 0.0;
        }
    }

    private static double damageDecay() {
        return state.damageShakeT > 0.0 ? state.damageShakeT / DAMAGE_SHAKE_DUR : 0.0;
    }

    private static void updateBobbing(double speed, double speedNorm, double dt) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        boolean sprinting = p != null && p.isSprinting();
        boolean sneaking = p != null && p.isSneaking();

        // roadmap 04 — acceleration drives cadence irregularity + deceleration settle
        double rawAccel = dt > 1.0e-4 ? (speed - state.prevSpeed) / dt : 0.0;
        state.prevSpeed = speed;
        state.speedAccel = TemporalSmoother.expSmooth(state.speedAccel, MathHelper.clamp(rawAccel, -4.0, 4.0), 10.0, dt);
        double decel = config.gaitTransitionEnabled ? MathHelper.clamp(-state.speedAccel / 0.8, 0.0, 1.0) : 0.0;
        state.settleGate = TemporalSmoother.expSmooth(state.settleGate, 1.0 - 0.30 * decel, 6.0, dt);

        state.airGate = TemporalSmoother.expSmooth(state.airGate, p != null && p.isOnGround() ? 1.0 : 0.0, 8.0, dt);

        double mult = 1.0;
        if (sprinting) mult *= config.sprintMultiplier;
        if (sneaking) mult *= config.sneakMultiplier;

        boolean synced = state.footInterval > 0.05
                && state.lastFootstepT > 0 && (nowSec() - state.lastFootstepT) <= 3.0;

        double rate;
        if (synced) {
            rate = Math.PI / state.footInterval;
        } else {
            double period = WALK_PERIOD;
            if (speed > WALK_SPEED) {
                double t = MathHelper.clamp((speed - WALK_SPEED) / (SPRINT_SPEED - WALK_SPEED), 0.0, 1.0);
                period = WALK_PERIOD + (SPRINT_PERIOD - WALK_PERIOD) * t;
            } else if (config.gaitTransitionEnabled && speed > 0.02) {
                // slow cadence blends toward the sneak cycle instead of staying at walk
                double t = MathHelper.clamp(speed / WALK_SPEED, 0.0, 1.0);
                period = SNEAK_PERIOD + (WALK_PERIOD - SNEAK_PERIOD) * t;
            }
            rate = (Math.PI * 2.0 / period) * config.bobbingFrequency;
            if (config.gaitTransitionEnabled) {
                // irregular steps while speed changes fast — phase is never reset
                double accelFactor = Math.min(Math.abs(state.speedAccel) / 1.2, 1.0);
                rate *= 1.0 + TemporalSmoother.perlin2D(state.microT * 1.9, 6.4)
                        * config.gaitIrregularity * accelFactor;
            }
        }
        state.bobPhase += rate * dt;

        updateFootfall(speedNorm, dt, sprinting);

        double amp = config.bobbing * mult * speed;
        state.bobVertical = -Math.cos(state.bobPhase * 2.0) * amp * 0.55 * state.airGate;
        state.bobRoll = Math.sin(state.bobPhase * 0.5) * amp * 8.0;
        state.bobYaw = Math.sin(state.bobPhase * 0.25) * amp * 4.0;
    }

    private static void updateFootfall(double speedNorm, double dt, boolean sprinting) {
        boolean synced = state.lastFootstepT > 0 && (nowSec() - state.lastFootstepT) <= 3.0;
        double prev = state.lastFootPhase;
        double now = state.bobPhase;

        if (!synced && speedNorm > 0.15 && now > prev) {
            int crossed = (int) Math.min(Math.floor(now / Math.PI) - Math.floor(prev / Math.PI), 4);
            double accelFactor = config.gaitTransitionEnabled
                    ? Math.min(Math.abs(state.speedAccel) / 1.2, 1.0) : 0.0;
            double wobble = 1.0 + TemporalSmoother.perlin2D(state.microT * 2.3, 9.1)
                    * config.gaitIrregularity * accelFactor;
            double sprintWeight = config.gaitTransitionEnabled && sprinting
                    ? 1.0 + config.gaitSprintWeight * speedNorm : 1.0;
            for (int i = 0; i < crossed; i++) {
                state.footSide = -state.footSide;
                state.stepSpring.impulse(-2.0 * speedNorm * speedNorm * config.bobbing * sprintWeight * wobble);
            }
        }
        state.lastFootPhase = now;

        double footTarget = state.footSide * 1.2 * speedNorm * config.swayIntensity * state.settleGate;
        state.footRoll = TemporalSmoother.expSmooth(state.footRoll, footTarget, 9.0, dt);
    }

    private static void updateMicroJitter(double dt, double speedNorm) {
        state.microT += dt * 8.0;
        double vib = (0.18 + 0.82 * speedNorm) * config.swayIntensity * state.settleGate;
        state.jitterYaw = TemporalSmoother.perlin2D(state.microT, 3.7) * vib * 1.2;
        state.jitterPitch = TemporalSmoother.perlin2D(7.1, state.microT * 1.17) * vib * 0.9;
    }

    private static void updateLean(double dt, double speedNorm) {
        double target = 1.8 * speedNorm * speedNorm;
        state.leanPitch = TemporalSmoother.expSmooth(state.leanPitch, target, 4.0, dt);
    }

    // = duracao do breath-in-out.ogg (sync perfeito: o exalar comeca exatamente a meio do ciclo)
    private static final double BREATH_CYCLE = 3.06;

    private static void updateBreath(ClientPlayerEntity player, double dt, double speed) {
        // continuous timer while running (5s -> +35% breath speed, 20s -> +65%; resets when stopping)
        boolean running = player.isSprinting() || speed >= SPRINT_SPEED * 0.85;
        state.runTime = running ? state.runTime + dt : 0.0;

        double mult;
        if (state.runTime < 5.0) mult = 1.0;
        else if (state.runTime < 20.0) mult = 1.35 + 0.30 * (state.runTime - 5.0) / 15.0;
        else mult = 1.65;
        state.breathRateMult = mult;
        state.exhaustion = (mult - 1.0) / 0.65;

        state.breathT += dt * (Math.PI * 2.0 / BREATH_CYCLE) * mult;
        boolean cycled = state.breathT >= Math.PI * 2.0;
        if (cycled) state.breathT -= Math.PI * 2.0;

        double p = state.breathT / (Math.PI * 2.0);
        // peak EXACTLY halfway (= start of the exhale in the audio); inhale with fast onset,
        // exhale with smooth release
        double shape = p < 0.5 ? Math.sin(p * Math.PI) : Math.cos((p - 0.5) * Math.PI);
        // irregularity: real breathing is not perfectly periodic
        double irregular = 1.0 + TemporalSmoother.perlin2D(state.microT * 0.7, 11.7) * 0.22;

        double gateTarget = speed < 0.05 ? 1.0 : (0.15 + 0.85 * state.exhaustion);
        state.breathGate = TemporalSmoother.expSmooth(state.breathGate, gateTarget, 2.5, dt);

        // breathing has its own amplitudes (does not depend on the preset's idleShake)
        double basePitch = 0.75 + 0.6 * state.exhaustion;
        state.breathPitch = (shape - 0.35) * basePitch * state.breathGate * irregular;
        state.breathY = (shape - 0.35) * (0.03 + 0.02 * state.exhaustion) * state.breathGate * irregular;

        if (cycled && state.breathGate > 0.25 && BreathSound.EVENT != null) {
            float vol = (float) ((0.5 + 0.5 * state.exhaustion) * state.breathGate * 1.05);
            player.playSound(BreathSound.EVENT, vol, (float) mult);
        }
    }

    private static void updateIdleShake(double dt) {
        state.idleNoiseT += dt * config.idleShakeFrequency;
        double shake = config.idleShake;
        state.bobYaw += TemporalSmoother.perlin2D(state.idleNoiseT, 0) * shake * 2.0;
        state.bobPitch = TemporalSmoother.perlin2D(0, state.idleNoiseT * 1.3) * shake * 2.0;
    }

    private static void updateRoll(ClientPlayerEntity player, double dt) {
        float yawDiff = MathHelper.subtractAngles(player.getYaw(), state.lastYaw);
        state.lastYaw = player.getYaw();
        state.lastPitch = player.getPitch();

        double yawRad = Math.toRadians(player.getYaw());
        double fwdX = -Math.sin(yawRad), fwdZ = Math.cos(yawRad);
        double rightX = fwdZ, rightZ = -fwdX;
        double lateral = player.getVelocity().x * rightX + player.getVelocity().z * rightZ;

        double targetRoll = -yawDiff * 1.0 * config.rollLean
                + MathHelper.clamp(lateral * 5.0, -2.5, 2.5) * config.swayIntensity;
        targetRoll = MathHelper.clamp(targetRoll, -10.0, 10.0);
        state.rollSmooth = TemporalSmoother.expSmooth(state.rollSmooth, targetRoll, 12.0, dt);

        double lagTarget = MathHelper.clamp(-yawDiff * 0.45, -5.0, 5.0);
        state.yawLag = TemporalSmoother.expSmooth(state.yawLag, lagTarget, 12.0, dt);
    }

    public static double getStepOffsetY() { return state.stepSpring.pos; }
    public static double getBobVertical() { return state.bobVertical; }
    public static double getBobRoll() { return state.bobRoll; }
    public static double getBobYaw() { return state.bobYaw; }
    public static double getIdlePitch() { return state.bobPitch; }
    public static double getRoll() { return state.rollSmooth; }
    public static double getFootRoll() { return state.footRoll; }
    public static double getYawLag() { return state.yawLag; }
    public static double getLeanPitch() { return state.leanPitch; }
    public static double getFootInterval() { return state.footInterval; }
    public static double getBreathMult() { return state.breathRateMult; }
    public static double getRunTime() { return state.runTime; }

    public static double getVerticalOffset() {
        return state.stepSpring.pos + state.bobVertical + state.breathY;
    }

    public static double getYawOffset() {
        double dmg = TemporalSmoother.perlin2D(state.damageNoiseT, 5.3)
                * state.damageAmp * damageDecay() * 1.6;
        return state.bobYaw + state.jitterYaw + state.yawLag + dmg;
    }

    public static double getPitchOffset() {
        double dmg = TemporalSmoother.perlin2D(3.9, state.damageNoiseT * 1.3)
                * state.damageAmp * damageDecay() * 1.2;
        return state.bobPitch + state.jitterPitch + state.leanPitch + state.breathPitch
                + state.landPitchSpring.pos + dmg;
    }

    public static double getRollTotal() {
        double critTilt = config.criticalCamEnabled
                ? 2.0 * state.criticalAmt * config.criticalStrength : 0.0;
        return state.rollSmooth + state.bobRoll + state.footRoll + state.landRollSpring.pos + critTilt;
    }

    public static BodycamConfig getConfig() { return config; }

    private static void reset() {
        state.stepSpring.reset();
        state.landPitchSpring.reset();
        state.landRollSpring.reset();
        state.bobPhase = state.bobVertical = state.bobRoll = state.bobYaw = 0;
        state.bobPitch = 0;
        state.idleNoiseT = state.rollSmooth = 0;
        state.footRoll = state.yawLag = state.leanPitch = 0;
        state.breathPitch = state.breathY = state.breathGate = state.breathT = 0;
        state.runTime = state.exhaustion = 0;
        state.breathRateMult = 1.0;
        state.microT = state.jitterYaw = state.jitterPitch = 0;
        state.lastFootPhase = 0;
        state.footSide = 1;
        state.lastFootstepT = 0;
        state.footInterval = 0;
        state.airborneMaxFall = 0;
        state.airGate = 1;
        state.prevSpeed = 0;
        state.speedAccel = 0;
        state.settleGate = 1;
        state.inertiaYawSpring.reset();
        state.inertiaPitchSpring.reset();
        state.inertiaInit = false;
        state.exposureMult = 1.0;
        state.lightLevel = -1.0;
        state.darkness = 0;
        state.flashT = 0;
        state.flashAmp = 0;
        state.flashCooldown = 0;
        state.lensDrops = 0;
        state.lensCond = 0;
        state.lensSmudge = 0;
        state.condCd = 0;
        state.underwaterT = 0;
        state.wasInWater = false;
        state.ghostAmt = 0;
        state.dustAmt = 0;
        state.criticalAmt = 0;
        state.yawVel = 0;
        state.pitchVel = 0;
        state.lastCamYaw = Double.NaN;
        state.lastCamPitch = Double.NaN;
        state.surfaceGain = 1.0;
        state.prevHealth = -1.0f;
        state.impactCd = 0;
        state.impactSide = 1;
        state.damageShakeT = 0;
        state.damageAmp = 0;
        state.damageNoiseT = 0;
        lastNanos = 0;
    }
}