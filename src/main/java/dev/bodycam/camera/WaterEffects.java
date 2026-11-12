package dev.bodycam.camera;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.bodycam.BodycamClient;
import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class WaterEffects {
    private static final CameraState state = CameraState.INSTANCE;
    private static long lastNanos = 0;
    private static double waveTime = 0.0;
    private static boolean wasInWater = false;
    private static double waterEntryTime = 0.0;

    public static void update() {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.waterEnabled) return;

        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.0 : (now - lastNanos) / 1.0e9;
        lastNanos = now;
        dt = Mth.clamp((float) dt, 0.0f, 0.1f);

        waveTime += dt * cfg.waveSpeed;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean inWater = mc.player.isUnderWater() || mc.player.isInWater();
        if (inWater && !wasInWater) {
            waterEntryTime = now / 1.0e9;
        }
        wasInWater = inWater;
    }

    /**
     * Forge equivalent of Fabric's WorldRenderEvents.AFTER_ENTITIES: called
     * from the RenderLevelStageEvent listener in BodycamClient with the event
     * pose stack and the current camera position.
     */
    public static void render(PoseStack matrices, Vec3 camPos) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.waterEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean inWater = mc.player.isUnderWater() || mc.player.isInWater();
        if (!inWater) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        matrices.pushPose();
        matrices.translate(-camPos.x, -camPos.y, -camPos.z);

        renderUnderwaterFog(matrices, cfg);
        renderCaustics(matrices, cfg, camPos);
        renderWaterSurface(matrices, cfg, camPos);

        matrices.popPose();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderUnderwaterFog(PoseStack matrices, BodycamConfig cfg) {
        if (!cfg.underwaterFogEnabled) return;

        float fogStart = 0.0f;
        float fogEnd = (float) (10.0 * cfg.underwaterFogDensity * 10.0);

        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        RenderSystem.setShaderFogShape(RenderSystem.getShaderFogShape());
    }

    private static void renderCaustics(PoseStack matrices, BodycamConfig cfg, Vec3 camPos) {
        if (cfg.causticsStrength <= 0.01) return;

        double time = waveTime;
        double strength = cfg.causticsStrength;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int radius = 8;
        int px = (int) camPos.x;
        int pz = (int) camPos.z;
        int py = (int) camPos.y;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double worldX = px + dx;
                double worldZ = pz + dz;

                double wave1 = Math.sin(worldX * 0.5 + time * 2.0) * Math.cos(worldZ * 0.5 + time * 1.5);
                double wave2 = Math.sin(worldX * 0.3 - time * 1.2) * Math.cos(worldZ * 0.7 + time * 0.8);
                double caustic = (wave1 + wave2) * 0.5;

                if (caustic > 0.3) {
                    float alpha = (float) ((caustic - 0.3) * strength * 0.3);
                    float r = 0.7f;
                    float g = 0.9f;
                    float b = 1.0f;

                    float y = py + 0.1f;
                    buffer.vertex(worldX, y, worldZ).color(r, g, b, alpha).endVertex();
                    buffer.vertex(worldX + 1, y, worldZ).color(r, g, b, alpha).endVertex();
                    buffer.vertex(worldX + 1, y, worldZ + 1).color(r, g, b, alpha).endVertex();
                    buffer.vertex(worldX, y, worldZ + 1).color(r, g, b, alpha).endVertex();
                }
            }
        }

        tesselator.end();
    }

    private static void renderWaterSurface(PoseStack matrices, BodycamConfig cfg, Vec3 camPos) {
        if (cfg.waterOpacity <= 0.01) return;

        double time = waveTime;
        double height = cfg.waveHeight;
        double freq = cfg.waveFrequency;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        int radius = 12;
        int px = (int) camPos.x;
        int pz = (int) camPos.z;
        int py = (int) camPos.y;

        float alpha = (float) cfg.waterOpacity;
        float r = (float) cfg.underwaterTintR;
        float g = (float) cfg.underwaterTintG;
        float b = (float) cfg.underwaterTintB;
        float foamK = (float) cfg.foamIntensity;

        for (int dx = -radius; dx < radius; dx++) {
            for (int dz = -radius; dz < radius; dz++) {
                double worldX = px + dx;
                double worldZ = pz + dz;

                double wave = Math.sin(worldX * freq + time) * Math.cos(worldZ * freq + time * 0.7) * height;
                float y = (float) (py + 0.5 + wave);

                float fr = r;
                float fg = g;
                float fb = b;
                float fa = alpha;

                if (foamK > 0.01f && height > 1.0e-4) {
                    // crest = how close to the wave peak (0..1); foam only on the crests
                    float crest = (float) ((wave / height) * 0.5 + 0.5);
                    float foam = foamK * (float) Math.max(0.0, (crest - 0.70) * 3.3);
                    foam = Math.min(foam, 1.0f);
                    fr = fr + (1.0f - fr) * foam;
                    fg = fg + (1.0f - fg) * foam;
                    fb = fb + (1.0f - fb) * foam;
                    fa = Math.min(1.0f, fa + foam * 0.35f);
                }

                buffer.vertex(worldX, y, worldZ).color(fr, fg, fb, fa).endVertex();
                buffer.vertex(worldX + 1, y, worldZ).color(fr, fg, fb, fa).endVertex();
                buffer.vertex(worldX + 1, y, worldZ + 1).color(fr, fg, fb, fa).endVertex();
                buffer.vertex(worldX, y, worldZ + 1).color(fr, fg, fb, fa).endVertex();
            }
        }

        tesselator.end();
    }

    public static boolean isInWater() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return false;
        return player.isUnderWater() || player.isInWater();
    }

    public static double getWaveTime() {
        return waveTime;
    }
}
