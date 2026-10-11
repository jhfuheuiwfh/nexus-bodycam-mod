package dev.bodycam.camera;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.util.math.MathHelper;

public class TaczSway {
    private static final CameraState state = CameraState.INSTANCE;

    private static boolean inited = false;
    private static long lastNanos = 0;
    private static final Spring yawSpring = new Spring();
    private static final Spring pitchSpring = new Spring();
    // Phase D — movement-to-aim penalty: its own slow mass (spring on speed*ADS)
    private static final Spring moveSpring = new Spring();

    public static double lastYawOffset = 0.0;
    public static double lastPitchOffset = 0.0;
    public static boolean hookLogado = false;

    private static double frameDelta() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        return MathHelper.clamp((float) dt, 0.0f, 0.1f);
    }

    public static void update() {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.taczSwayEnabled) {
            inited = false;
            lastYawOffset = 0.0;
            lastPitchOffset = 0.0;
            moveSpring.reset();
            return;
        }

        double dt = frameDelta();
        TaczBridge.tickFrame();
        float aim = TaczBridge.getAimAmount();

        float targetYaw = state.finalViewYaw;
        float targetPitch = state.finalViewPitch;

        if (!inited) {
            yawSpring.snap(targetYaw);
            pitchSpring.snap(targetPitch);
            inited = true;
        }

        if (Math.abs(MathHelper.wrapDegrees(targetYaw - yawSpring.pos)) > 100.0) {
            yawSpring.snap(targetYaw);
        }
        if (Math.abs(targetPitch - pitchSpring.pos) > 100.0) {
            pitchSpring.snap(targetPitch);
        }

        // 0.9 = weapon spring 10% slower (returns more slowly; camera NOT moved)
        double omega = 2.0 * Math.PI * cfg.taczSwaySpeed * 0.9;
        double zeta = dampingRatio(cfg.taczSwayOvershoot);

        yawSpring.stepWrapped(dt, omega, zeta, targetYaw);
        pitchSpring.step(dt, omega, zeta, targetPitch);

        double yawOff = MathHelper.wrapDegrees((float) (yawSpring.pos - targetYaw)) * cfg.taczSwayIntensity;
        double pitchOff = (pitchSpring.pos - targetPitch) * cfg.taczSwayIntensity;

        // Phase D — ADS stabilization: aiming tightens the weapon spring
        if (cfg.taczAdsStabilization && aim > 0.0f) {
            double stab = 1.0 - 0.55 * aim;
            yawOff *= stab;
            pitchOff *= stab;
        }

        // Phase D — movement-to-aim penalty: walking while aimed makes the
        // sight wander on its own slow mass (perlin drift, not a static offset)
        double moveTarget = 0.0;
        if (cfg.taczMovePenaltyEnabled && aim > 0.5f) {
            net.minecraft.client.network.ClientPlayerEntity p =
                    net.minecraft.client.MinecraftClient.getInstance().player;
            if (p != null) {
                double spd = Math.sqrt(p.getVelocity().x * p.getVelocity().x + p.getVelocity().z * p.getVelocity().z);
                moveTarget = MathHelper.clamp(spd / 0.281, 0.0, 1.0) * 2.5 * aim;
            }
        }
        moveSpring.step(dt, 2.0 * Math.PI * 2.5, 0.9, moveTarget);
        if (moveSpring.pos > 0.01) {
            double driftY = TemporalSmoother.perlin2D(state.microT * 0.5, 13.7);
            double driftP = TemporalSmoother.perlin2D(17.3, state.microT * 0.45);
            yawOff += driftY * moveSpring.pos;
            pitchOff += driftP * moveSpring.pos * 0.6;
        }

        lastYawOffset = yawOff;
        lastPitchOffset = pitchOff;
    }

    private static double dampingRatio(double overshoot) {
        if (overshoot <= 0.0005) return 1.0;
        double ln = Math.log(1.0 / overshoot);
        double zeta = ln / Math.sqrt(Math.PI * Math.PI + ln * ln);
        return MathHelper.clamp((float) zeta, 0.1f, 1.0f);
    }

    public static double getYawOffset() { return lastYawOffset; }
    public static double getPitchOffset() { return lastPitchOffset; }
}
