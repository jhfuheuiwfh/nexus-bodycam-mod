package dev.bodycam.camera;

import cn.sh1rocu.tacz.api.LogicalSide;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import dev.bodycam.BodycamClient;
import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;

/**
 * Phase D — the ONLY class that touches the TACZ API. It is loaded solely
 * after BodycamClient has verified FabricLoader.isModLoaded("tacz"), so no
 * other class in the mod ever resolves a com.tacz/cn.sh1rocu symbol.
 *
 * Separation of masses (roadmap rule): real shots drive the WEAPON recoil
 * springs here; the camera itself is never kicked by gunfire.
 */
public final class TaczBridge {
    private static boolean inited = false;
    private static float aimAmount = 0.0f;
    private static double wallPull = 0.0;

    private TaczBridge() {}

    /** Called once at client init, only when TACZ is present. */
    public static void init() {
        if (inited) return;
        inited = true;
        try {
            GunShootEvent.CALLBACK.register(TaczBridge::onShoot);
            GunReloadEvent.CALLBACK.register(TaczBridge::onReload);
            BodycamClient.LOGGER.info("[bodycam] TACZ API ligada: GunShootEvent + GunReloadEvent registados");
        } catch (Throwable t) {
            BodycamClient.LOGGER.warn("[bodycam] Falha ao registar eventos TACZ: {}", t.toString());
        }
    }

    private static void onShoot(GunShootEvent event) {
        try {
            if (event.getLogicalSide() != LogicalSide.CLIENT) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || event.getShooter() != mc.player) return;
            TaczGunRealism.triggerRecoil(0.5, 0.08);
        } catch (Throwable ignored) {}
    }

    private static void onReload(GunReloadEvent event) {
        try {
            if (event.getLogicalSide() != LogicalSide.CLIENT) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || event.getEntity() != mc.player) return;
            TaczGunRealism.triggerReload();
        } catch (Throwable ignored) {}
    }

    /** Sampled once per render frame (main camera only — see TaczSway.update). */
    public static void tickFrame() {
        if (!inited) return;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            aimAmount = 0.0f;
            return;
        }
        try {
            IClientPlayerGunOperator op = IClientPlayerGunOperator.fromLocalPlayer(player);
            aimAmount = MathHelper.clamp(op.getClientAimingProgress(1.0f), 0.0f, 1.0f);
        } catch (Throwable t) {
            aimAmount = 0.0f;
        }
    }

    /** 0..1 ADS progress (0 when TACZ absent or not aiming). */
    public static float getAimAmount() {
        return inited ? aimAmount : 0.0f;
    }

    /** 0..1 — how close something solid is in front of the muzzle (0 = clear). */
    public static double getWallPull() {
        return wallPull;
    }

    /** Wall pullback probe — called once per frame from TaczGunRealism. */
    public static void updateWallPull(ClientPlayerEntity player, double dt) {
        if (!inited) {
            wallPull = 0.0;
            return;
        }
        BodycamConfig cfg = BodycamEffects.getConfig();
        double target = 0.0;
        if (cfg.taczWallPullbackEnabled && cfg.enabled) {
            try {
                HitResult hit = player.raycast(1.0, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    double dist = hit.getPos().distanceTo(player.getEyePos());
                    target = MathHelper.clamp(1.0 - dist, 0.0, 1.0);
                }
            } catch (Throwable ignored) {}
        }
        double lambda = target > wallPull ? 14.0 : 7.0;
        wallPull = TemporalSmoother.expSmooth(wallPull, target, lambda, dt);
    }
}
