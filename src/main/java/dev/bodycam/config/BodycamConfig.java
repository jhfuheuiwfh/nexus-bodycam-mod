package dev.bodycam.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class BodycamConfig {
    public boolean enabled = true;
    public double stepSmoothing = 0.65;
    public double idleShake = 0.35;
    public double idleShakeFrequency = 0.8;
    public double bobbing = 0.55;
    public double bobbingFrequency = 1.0;
    public double rollLean = 0.5;
    public double landingDamping = 0.8;
    public double sprintMultiplier = 1.35;
    public double sneakMultiplier = 0.4;
    public boolean syncWithShader = true;
    public boolean disableVanillaBobbing = true;
    public String preset = "NORMAL";
    public double motionBlurBoost = 0.0;
    public double swayIntensity = 0.5;
    public boolean taczSwayEnabled = true;
    public double taczSwayIntensity = 1.0;
    public double taczSwaySpeed = 1.5;
    public double taczSwayOvershoot = 0.13;
    public double viewSmoothSpeed = 14.0;

    // roadmap 01 — Camera Inertia / Mass Lag (2nd-order follow, body turns first)
    public boolean inertiaEnabled = true;
    public double inertiaDelay = 0.05;      // perceived angular lag in seconds (30-70ms range)
    public double inertiaOvershoot = 0.5;   // 0 = critically damped, 1 = loosest
    // roadmap 04 — Gait Transition (cadence blend, sprint weight, decel settle)
    public boolean gaitTransitionEnabled = true;
    public double gaitSprintWeight = 0.35;  // extra footfall weight while sprinting
    public double gaitIrregularity = 0.12;  // step irregularity while speed changes fast

    public boolean chestCamEnabled = true;
    public double chestCamHeight = -0.30;
    public double chestCamPitch = 10.0;
    public double fovWideMult = 1.15;

    public boolean taczLoweringEnabled = true;
    public double taczLoweringSpeed = 2.0;
    public boolean taczRunSwayEnabled = true;
    public double taczRunSwayIntensity = 0.5;
    public boolean taczRecoilKickEnabled = true;
    public double taczRecoilKickStrength = 1.0;
    // Phase D — TACZ realism (each behaviour has its own mass; camera untouched)
    public boolean taczAdsStabilization = true;   // aiming tightens the weapon spring
    public boolean taczMovePenaltyEnabled = true; // walking while ADS makes the sight wander
    public boolean taczLandingLagEnabled = true;  // weapon drops after the body lands
    public boolean taczWallPullbackEnabled = true;// muzzle near a block -> gun drawn in
    public boolean taczReloadNodEnabled = true;   // reload nods the gun down and back

    public boolean postFXEnabled = true;
    public double fisheyeK1 = 0.18;
    public double fisheyeK2 = -0.08;
    public double vignetteCosPow = 2.2;
    public double vignetteMech = 0.35;
    public double vignetteFloor = 0.12;
    public double vignetteCenterGain = 1.15;
    public double chromaticAberration = 0.003;
    public double lensDirtIntensity = 1.0;
    public double lensDirtVeiling = 0.02;
    public double grainStrength = 0.05;
    public double grainCellSize = 2.0;
    public boolean autoQuality = true;

    // roadmap 06 — Auto Exposure: asymmetric luminance adaptation (temporal state, no histogram)
    public boolean autoExposureEnabled = true;
    public double autoExposureStrength = 0.35;   // gain added at full darkness
    public double autoExposureSpeedDark = 1.2;   // claro→escuro: recuperação lenta
    public double autoExposureSpeedBright = 4.0; // escuro→claro: adaptação rápida
    // roadmap 13 — Flash / Brightness Shock: brief exposure spike on sudden light jumps
    public boolean flashShockEnabled = true;
    public double flashShockStrength = 0.5;

    // roadmap 03 — Impact Recovery: shared sequence (vertical compress →
    // forward tilt → small overshoot → damped) triggered by falls, combat
    // hits and horizontal collisions with separate temporal envelopes
    public boolean impactRecoveryEnabled = true;
    public double impactStrength = 1.0;

    // roadmap 08 — Low-Light Noise: sensor grain gain scales with darkness
    public boolean lowLightNoiseEnabled = true;
    public double lowLightNoiseGain = 0.6;
    // roadmap 09 — Lens Contamination: drops (water/rain), condensation
    // (light shock), smudge (impacts); lensCleanSpeed 0 = persistent
    public boolean lensContaminationEnabled = true;
    public double lensCondStrength = 0.6;
    public double lensCleanSpeed = 1.0;
    // roadmap 05 — Surface Footsteps: block material shapes the footfall impulse
    public boolean surfaceFootstepsEnabled = true;
    public double surfaceFootstepGain = 1.0;
    // roadmap 07 — Lens Ghosting: veiling flare when the sun lines up with the view
    public boolean ghostFlareEnabled = true;
    public double ghostFlareStrength = 0.7;
    // roadmap 10 — Lens Profile: wide-FOV edge character (CA growth + peripheral softness)
    public boolean lensProfileEnabled = true;
    public double lensProfileStrength = 0.6;
    // roadmap 12 — Dust: drifting motes visible in shafts of light
    public boolean dustEnabled = true;
    public double dustStrength = 0.6;
    // roadmap 14 — Critical Damage Camera: low-HP instability (grain, flicker, tilt)
    public boolean criticalCamEnabled = true;
    public double criticalStrength = 1.0;
    // roadmap 02 — Angular Motion Blur: screen-space smear driven by camera angular velocity
    public boolean motionBlurEnabled = true;
    public double motionBlurStrength = 0.6;

    public boolean waterEnabled = true;
    public double waveHeight = 0.15;
    public double waveSpeed = 1.0;
    public double waveFrequency = 0.5;
    public double waterOpacity = 0.7;
    public double waterRefraction = 0.05;
    public double foamIntensity = 0.5;
    public double causticsStrength = 0.8;
    public boolean underwaterFogEnabled = true;
    public double underwaterFogDensity = 0.08;
    public double underwaterTintR = 0.1;
    public double underwaterTintG = 0.4;
    public double underwaterTintB = 0.6;

    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("bodycam.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static BodycamConfig load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                BodycamConfig config = GSON.fromJson(reader, BodycamConfig.class);
                if (config != null) {
                    config.sanitize();
                    return config;
                }
            } catch (Exception ignored) {}
        }
        return new BodycamConfig();
    }

    private static BodycamConfig cachedConfig;
    private static long cachedMtime = -1L;
    private static long cachedSize = -1L;

    /**
     * Hot-path variant of {@link #load()}: avoids re-reading + re-parsing the JSON
     * every frame. The cache is invalidated by file mtime/size, so external edits
     * (or the in-game GUI, which goes through save()) are still picked up.
     * The GUI itself should keep calling load() for a guaranteed-fresh instance.
     */
    public static BodycamConfig loadCached() {
        try {
            BasicFileAttributes attrs = Files.readAttributes(CONFIG_FILE, BasicFileAttributes.class);
            long mtime = attrs.lastModifiedTime().toMillis();
            long size = attrs.size();
            if (cachedConfig != null && mtime == cachedMtime && size == cachedSize) {
                return cachedConfig;
            }
            BodycamConfig config = load();
            cachedConfig = config;
            cachedMtime = mtime;
            cachedSize = size;
            return config;
        } catch (Exception e) {
            if (cachedConfig != null) return cachedConfig;
            cachedConfig = load();
            return cachedConfig;
        }
    }

    public static void invalidateCache() {
        cachedConfig = null;
        cachedMtime = -1L;
        cachedSize = -1L;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
            }
            invalidateCache();
        } catch (IOException ignored) {}
    }

    public void applyPreset(String presetName) {
        this.preset = presetName;
        switch (presetName.toUpperCase()) {
            case "SUBTLE" -> {
                stepSmoothing = 0.30; idleShake = 0.15; idleShakeFrequency = 0.5;
                bobbing = 0.25; bobbingFrequency = 0.8; rollLean = 0.25;
                landingDamping = 0.5; sprintMultiplier = 1.2; sneakMultiplier = 0.4;
                motionBlurBoost = 0.0; swayIntensity = 0.2;
                postFXEnabled = true; fisheyeK1 = 0.10; chromaticAberration = 0.0015;
                lensDirtIntensity = 1.0; lensDirtVeiling = 0.02; grainStrength = 0.03;
            }
            case "NORMAL" -> {
                stepSmoothing = 0.65; idleShake = 0.35; idleShakeFrequency = 0.8;
                bobbing = 0.55; bobbingFrequency = 1.0; rollLean = 0.5;
                landingDamping = 0.8; sprintMultiplier = 1.35; sneakMultiplier = 0.4;
                motionBlurBoost = 0.0; swayIntensity = 0.5;
                postFXEnabled = true; fisheyeK1 = 0.18; chromaticAberration = 0.003;
                lensDirtIntensity = 1.0; lensDirtVeiling = 0.02; grainStrength = 0.05;
            }
            case "INTENSE" -> {
                stepSmoothing = 0.85; idleShake = 0.60; idleShakeFrequency = 1.4;
                bobbing = 0.85; bobbingFrequency = 1.3; rollLean = 0.85;
                landingDamping = 1.0; sprintMultiplier = 1.6; sneakMultiplier = 0.35;
                motionBlurBoost = 0.0; swayIntensity = 0.8;
                postFXEnabled = true; fisheyeK1 = 0.25; chromaticAberration = 0.005;
                lensDirtIntensity = 1.0; lensDirtVeiling = 0.02; grainStrength = 0.07;
            }
            case "EXTREME" -> {
                stepSmoothing = 1.00; idleShake = 1.00; idleShakeFrequency = 2.6;
                bobbing = 1.20; bobbingFrequency = 1.8; rollLean = 1.30;
                landingDamping = 1.40; sprintMultiplier = 2.0; sneakMultiplier = 0.25;
                motionBlurBoost = 0.0; swayIntensity = 1.0;
                postFXEnabled = true; fisheyeK1 = 0.30; chromaticAberration = 0.006;
                lensDirtIntensity = 1.0; lensDirtVeiling = 0.02; grainStrength = 0.08;
            }
            // Phase E — documentary run-and-gun: steady handheld, honest optics
            case "DOCUMENTARY" -> {
                stepSmoothing = 0.50; idleShake = 0.20; idleShakeFrequency = 0.6;
                bobbing = 0.40; bobbingFrequency = 0.9; rollLean = 0.40;
                landingDamping = 0.70; sprintMultiplier = 1.3; sneakMultiplier = 0.4;
                motionBlurBoost = 0.0; swayIntensity = 0.35;
                postFXEnabled = true; fisheyeK1 = 0.14; chromaticAberration = 0.002;
                lensDirtIntensity = 1.0; lensDirtVeiling = 0.02; grainStrength = 0.04;
                fovWideMult = 1.10; motionBlurStrength = 0.35; vignetteMech = 0.30;
                autoExposureStrength = 0.35; lowLightNoiseGain = 0.5;
                ghostFlareStrength = 0.6; dustStrength = 0.5; criticalStrength = 1.0;
            }
            // Phase E — horror: unstable sensor, dirty glass, deep vignette
            case "HORROR" -> {
                stepSmoothing = 0.70; idleShake = 0.55; idleShakeFrequency = 1.2;
                bobbing = 0.60; bobbingFrequency = 1.0; rollLean = 0.60;
                landingDamping = 1.00; sprintMultiplier = 1.4; sneakMultiplier = 0.5;
                motionBlurBoost = 0.0; swayIntensity = 0.70;
                postFXEnabled = true; fisheyeK1 = 0.20; chromaticAberration = 0.004;
                lensDirtIntensity = 1.2; lensDirtVeiling = 0.05; grainStrength = 0.10;
                fovWideMult = 1.15; motionBlurStrength = 0.40; vignetteMech = 0.60;
                autoExposureStrength = 0.60; lowLightNoiseGain = 1.1;
                ghostFlareStrength = 1.2; dustStrength = 0.9; criticalStrength = 1.5;
            }
            // Phase E — tactical: tight, readable sight picture, minimal wobble
            case "TACTICAL" -> {
                stepSmoothing = 0.35; idleShake = 0.12; idleShakeFrequency = 0.4;
                bobbing = 0.30; bobbingFrequency = 0.9; rollLean = 0.30;
                landingDamping = 0.60; sprintMultiplier = 1.25; sneakMultiplier = 0.3;
                motionBlurBoost = 0.0; swayIntensity = 0.25;
                postFXEnabled = true; fisheyeK1 = 0.12; chromaticAberration = 0.0015;
                lensDirtIntensity = 0.8; lensDirtVeiling = 0.01; grainStrength = 0.03;
                fovWideMult = 1.05; motionBlurStrength = 0.30; vignetteMech = 0.25;
                autoExposureStrength = 0.25; lowLightNoiseGain = 0.4;
                ghostFlareStrength = 0.4; dustStrength = 0.4; criticalStrength = 0.8;
            }
            // Phase E — raw GoPro: widest lens, heavy CA/grain/motion smear
            case "RAW GOPRO" -> {
                stepSmoothing = 0.80; idleShake = 0.50; idleShakeFrequency = 1.5;
                bobbing = 0.90; bobbingFrequency = 1.4; rollLean = 0.90;
                landingDamping = 1.10; sprintMultiplier = 1.7; sneakMultiplier = 0.35;
                motionBlurBoost = 0.0; swayIntensity = 0.85;
                postFXEnabled = true; fisheyeK1 = 0.32; chromaticAberration = 0.006;
                lensDirtIntensity = 1.4; lensDirtVeiling = 0.06; grainStrength = 0.08;
                fovWideMult = 1.40; motionBlurStrength = 0.80; vignetteMech = 0.50;
                autoExposureStrength = 0.45; lowLightNoiseGain = 0.8;
                ghostFlareStrength = 0.9; dustStrength = 0.7; criticalStrength = 1.2;
            }
        }
        sanitize();
    }

    public void sanitize() {
        enabled = enabled;
        stepSmoothing = clamp(stepSmoothing, 0.0, 1.5);
        idleShake = clamp(idleShake, 0.0, 1.5);
        idleShakeFrequency = clamp(idleShakeFrequency, 0.1, 5.0);
        bobbing = clamp(bobbing, 0.0, 2.0);
        bobbingFrequency = clamp(bobbingFrequency, 0.1, 3.0);
        rollLean = clamp(rollLean, 0.0, 2.0);
        landingDamping = clamp(landingDamping, 0.0, 2.0);
        sprintMultiplier = clamp(sprintMultiplier, 0.5, 3.0);
        sneakMultiplier = clamp(sneakMultiplier, 0.1, 1.0);
        motionBlurBoost = clamp(motionBlurBoost, 0.0, 1.0);
        swayIntensity = clamp(swayIntensity, 0.0, 2.0);
        taczSwayIntensity = clamp(taczSwayIntensity, 0.0, 2.0);
        taczSwaySpeed = clamp(taczSwaySpeed, 0.3, 4.0);
        taczSwayOvershoot = clamp(taczSwayOvershoot, 0.0, 0.6);
        viewSmoothSpeed = clamp(viewSmoothSpeed, 0.0, 40.0);
        inertiaDelay = clamp(inertiaDelay, 0.02, 0.15);
        inertiaOvershoot = clamp(inertiaOvershoot, 0.0, 1.0);
        gaitSprintWeight = clamp(gaitSprintWeight, 0.0, 1.0);
        gaitIrregularity = clamp(gaitIrregularity, 0.0, 0.4);
        chestCamHeight = clamp(chestCamHeight, -1.0, 0.0);
        chestCamPitch = clamp(chestCamPitch, 0.0, 30.0);
        fovWideMult = clamp(fovWideMult, 1.0, 1.5);
        taczLoweringSpeed = clamp(taczLoweringSpeed, 0.5, 5.0);
        taczRunSwayIntensity = clamp(taczRunSwayIntensity, 0.0, 2.0);
        taczRecoilKickStrength = clamp(taczRecoilKickStrength, 0.0, 3.0);
        fisheyeK1 = clamp(fisheyeK1, 0.0, 0.5);
        fisheyeK2 = clamp(fisheyeK2, -0.3, 0.0);
        vignetteCosPow = clamp(vignetteCosPow, 1.0, 5.0);
        vignetteMech = clamp(vignetteMech, 0.0, 1.0);
        vignetteFloor = clamp(vignetteFloor, 0.0, 0.5);
        vignetteCenterGain = clamp(vignetteCenterGain, 1.0, 2.0);
        chromaticAberration = clamp(chromaticAberration, 0.0, 0.01);
        lensDirtIntensity = clamp(lensDirtIntensity, 0.0, 5.0);
        lensDirtVeiling = clamp(lensDirtVeiling, 0.0, 0.3);
        grainStrength = clamp(grainStrength, 0.0, 0.15);
        grainCellSize = clamp(grainCellSize, 1.0, 5.0);
        autoExposureStrength = clamp(autoExposureStrength, 0.0, 1.0);
        autoExposureSpeedDark = clamp(autoExposureSpeedDark, 0.2, 10.0);
        autoExposureSpeedBright = clamp(autoExposureSpeedBright, 0.2, 10.0);
        flashShockStrength = clamp(flashShockStrength, 0.0, 1.5);
        impactStrength = clamp(impactStrength, 0.0, 2.0);
        lowLightNoiseGain = clamp(lowLightNoiseGain, 0.0, 1.5);
        lensCondStrength = clamp(lensCondStrength, 0.0, 1.5);
        lensCleanSpeed = clamp(lensCleanSpeed, 0.0, 3.0);
        surfaceFootstepGain = clamp(surfaceFootstepGain, 0.25, 2.0);
        ghostFlareStrength = clamp(ghostFlareStrength, 0.0, 2.0);
        lensProfileStrength = clamp(lensProfileStrength, 0.0, 1.5);
        dustStrength = clamp(dustStrength, 0.0, 1.5);
        criticalStrength = clamp(criticalStrength, 0.0, 2.0);
        motionBlurStrength = clamp(motionBlurStrength, 0.0, 1.5);
        waveHeight = clamp(waveHeight, 0.0, 0.5);
        waveSpeed = clamp(waveSpeed, 0.1, 5.0);
        waveFrequency = clamp(waveFrequency, 0.1, 2.0);
        waterOpacity = clamp(waterOpacity, 0.0, 1.0);
        waterRefraction = clamp(waterRefraction, 0.0, 0.2);
        foamIntensity = clamp(foamIntensity, 0.0, 2.0);
        causticsStrength = clamp(causticsStrength, 0.0, 2.0);
        underwaterFogDensity = clamp(underwaterFogDensity, 0.0, 0.3);
        underwaterTintR = clamp(underwaterTintR, 0.0, 1.0);
        underwaterTintG = clamp(underwaterTintG, 0.0, 1.0);
        underwaterTintB = clamp(underwaterTintB, 0.0, 1.0);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
