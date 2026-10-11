package dev.bodycam.camera;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ChestCam {
    private static final CameraState state = CameraState.INSTANCE;

    public static Vec3d applyChestCam(Vec3d pos) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.chestCamEnabled) return pos;

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return pos;

        double eyeY = pos.y;
        double chestY = eyeY + cfg.chestCamHeight;
        return new Vec3d(pos.x, chestY, pos.z);
    }

    public static float applyPitch(float pitch) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.chestCamEnabled) return pitch;
        return pitch + (float) cfg.chestCamPitch;
    }

    public static double getFovMultiplier() {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.chestCamEnabled) return 1.0;
        return cfg.fovWideMult;
    }
}
