package dev.bodycam.core;

import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.compat.IrisCompat;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Phase: architecture (roadmap §5). Module map — this class owns the
 * single per-frame entry point for MOTION state (BodycamEffects), so:
 *
 *   dev.bodycam.core   -> RenderCoordinator (this) + GameState (sampling)
 *   dev.bodycam.camera.BodycamEffects      = Motion module
 *   dev.bodycam.camera.PostEffects         = Optics module (shader pass)
 *   dev.bodycam.camera.WaterEffects        = Environment module
 *   dev.bodycam.camera.Tacz* + TaczBridge  = Weapon module
 *
 * The coordinator guarantees state is updated ONCE per render frame:
 * shadow passes (Iris) re-enter Camera.update but must only CONSUME the
 * uniforms, never advance envelopes (advancing them at shadow-pass rate
 * would make adaptation/dt wrong).
 */
public final class RenderCoordinator {
    private static boolean frameActive = false;
    public static long frames = 0;
    public static long shadowSkips = 0;

    private RenderCoordinator() {}

    /** Called from CameraMixin at the end of Camera.update. */
    public static void cameraUpdate(float tickDelta) {
        if (IrisCompat.isRenderingShadowPass()) {
            shadowSkips++;
            return;
        }
        if (frameActive) {
            // same render frame re-entered (multi-camera setups): state already advanced
            return;
        }
        frameActive = true;
        frames++;
        try {
            BodycamEffects.onFrame(tickDelta);
            // one world snapshot per frame for every module to read
            GameState.sample(net.minecraft.client.MinecraftClient.getInstance().player,
                    BodycamEffects.lastDt);
        } finally {
            frameActive = false;
        }
    }

    /** Per-frame snapshot of what every module reads (sampling lives here). */
    public static void sample(ClientPlayerEntity player, double dt) {
        GameState.sample(player, dt);
    }
}
