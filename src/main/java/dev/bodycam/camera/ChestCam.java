package dev.bodycam.camera;

import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public class ChestCam {
    private static final CameraState state = CameraState.INSTANCE;

    public static Vec3 applyChestCam(Vec3 pos) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.chestCamEnabled) return pos;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return pos;

        double eyeY = pos.y;
        double chestY = eyeY + cfg.chestCamHeight;
        return new Vec3(pos.x, chestY, pos.z);
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
