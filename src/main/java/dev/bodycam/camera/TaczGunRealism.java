package dev.bodycam.camera;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

public class TaczGunRealism {
    private static final CameraState state = CameraState.INSTANCE;

    private static final Spring lowerSpring = new Spring();
    private static final Spring recoilPitchSpring = new Spring();
    private static final Spring recoilYawSpring = new Spring();
    // Phase D — own masses: reload nod, landing lag, wall pullback
    private static final Spring reloadSpring = new Spring();
    private static final Spring landLagSpring = new Spring();
    private static double wallPullSmooth = 0.0;
    private static double runSwayT = 0.0;
    private static long lastNanos = 0;

    private static double frameDelta() {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        return MathHelper.clamp((float) dt, 0.0f, 0.1f);
    }

    public static void update() {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled) {
            lowerSpring.reset();
            reloadSpring.reset();
            landLagSpring.reset();
            wallPullSmooth = 0.0;
            runSwayT = 0.0;
            return;
        }

        double dt = frameDelta();
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;

        double speed = Math.sqrt(player.getVelocity().x * player.getVelocity().x + player.getVelocity().z * player.getVelocity().z);
        boolean sprinting = player.isSprinting() && speed > 0.2;

        if (cfg.taczLoweringEnabled) {
            double targetLower = sprinting ? 1.0 : 0.0;
            lowerSpring.step(dt, 2.0 * Math.PI * cfg.taczLoweringSpeed, 0.7, targetLower);
            if (sprinting && cfg.taczRunSwayEnabled) {
                runSwayT += dt * 6.0;
            }
        } else {
            lowerSpring.reset();
            runSwayT = 0.0;
        }

        if (cfg.taczRecoilKickEnabled) {
            double omegaR = Math.sqrt(120.0);
            double zetaR = 14.0 / (2.0 * omegaR);
            recoilPitchSpring.step(dt, omegaR, zetaR, 0.0);
            recoilYawSpring.step(dt, omegaR, zetaR, 0.0);
        } else {
            recoilPitchSpring.reset();
            recoilYawSpring.reset();
        }

        // Phase D — reload nod: slow bounce-back (period ~1.2s, light overshoot)
        double omegaN = Math.sqrt(40.0);
        reloadSpring.step(dt, omegaN, 0.55, 0.0);
        // Phase D — landing lag: the weapon keeps dropping after the body lands
        double omegaL = Math.sqrt(50.0);
        landLagSpring.step(dt, omegaL, 0.6, 0.0);

        TaczBridge.updateWallPull(player, dt);
        wallPullSmooth = TaczBridge.getWallPull();
    }

    public static void triggerRecoil(double pitchKick, double yawKick) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.taczRecoilKickEnabled) return;
        recoilPitchSpring.impulse(pitchKick * cfg.taczRecoilKickStrength);
        recoilYawSpring.impulse(yawKick * cfg.taczRecoilKickStrength);
    }

    /** Phase D — GunReloadEvent: the gun nods down and settles back. */
    public static void triggerReload() {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.taczReloadNodEnabled) return;
        reloadSpring.impulse(-3.0);
    }

    /** Phase D — landing: the weapon mass lags behind the body drop. */
    public static void onLanding(double impact) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.taczLandingLagEnabled) return;
        landLagSpring.impulse(-impact * 6.0);
    }

    public static double getLowerAmount() { return lowerSpring.pos; }
    public static double getRunSwayT() { return runSwayT; }
    public static double getRecoilPitch() { return recoilPitchSpring.pos; }
    public static double getRecoilYaw() { return recoilYawSpring.pos; }
    public static double getReloadNod() { return reloadSpring.pos; }
    public static double getLandLag() { return landLagSpring.pos; }
    public static double getWallPull() { return wallPullSmooth; }
}
