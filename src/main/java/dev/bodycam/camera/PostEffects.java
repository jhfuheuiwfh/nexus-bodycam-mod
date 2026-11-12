package dev.bodycam.camera;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.bodycam.BodycamClient;
import dev.bodycam.compat.IrisCompat;
import dev.bodycam.config.BodycamConfig;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;

/**
 * Optics module — the post-processing pass. On Forge the shaders are
 * registered from the @Mod class (RegisterShadersEvent): the ShaderInstance is
 * constructed there with the event's resource provider and every (re)load
 * (resource pack change, F3+T) hands the fresh instance back through the
 * load callbacks below, so the static refs always track the live program.
 */
public class PostEffects {
    private static ShaderInstance postShader;
    private static ShaderInstance blurShader;

    private static RenderTarget scratch;
    private static AbstractTexture dirtTex;
    private static boolean dirtFailed = false;
    private static boolean logged = false;
    private static boolean loggedBlur = false;
    private static boolean probeLogged = false;

    private static long fpsLastNanos = 0;
    private static double fpsEma = 60.0;
    private static boolean degraded = false;
    private static boolean lastLoggedDegraded = false;

    /**
     * Auto-quality governor (A5): while the post pass is active, keep an EMA of the
     * real frame rate. Below 40 FPS the expensive extras are scaled down (the
     * underwater fbm refraction is skipped entirely — it is the costliest part of
     * the pass); above 55 FPS everything is restored. Hysteresis prevents flicker.
     */
    private static double qualityScale(BodycamConfig cfg) {
        long now = System.nanoTime();
        if (fpsLastNanos != 0) {
            double dt = Math.min(Math.max((now - fpsLastNanos) / 1.0e9, 1.0 / 240.0), 1.0 / 10.0);
            fpsEma = fpsEma * 0.95 + (1.0 / dt) * 0.05;
        }
        fpsLastNanos = now;

        if (!cfg.autoQuality) {
            degraded = false;
        } else if (!degraded && fpsEma < 40.0) {
            degraded = true;
        } else if (degraded && fpsEma > 55.0) {
            degraded = false;
        }

        if (degraded != lastLoggedDegraded) {
            lastLoggedDegraded = degraded;
            BodycamClient.LOGGER.info("[bodycam] auto-quality: {} (fpsEma={})",
                degraded ? "DEGRADADO" : "restaurado", String.format("%.1f", fpsEma));
        }

        return degraded ? 0.5 : 1.0;
    }

    /**
     * Load callback for the "bodycam:post" core shader. Called once at client
     * init and again on every resource reload — just re-point the static ref.
     */
    public static void onLoadShader(ShaderInstance shader) {
        postShader = shader;
        BodycamClient.LOGGER.info("[bodycam] Core shader bodycam:post registado.");
    }

    /** Load callback for the "bodycam:dofblur" core shader (config-screen backdrop). */
    public static void onLoadBlurShader(ShaderInstance shader) {
        blurShader = shader;
        BodycamClient.LOGGER.info("[bodycam] Core shader bodycam:dofblur registado.");
    }

    public static boolean copyScreen() {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (width <= 0 || height <= 0) return false;

        if (scratch == null) {
            scratch = new TextureTarget(width, height, true, false);
        } else if (scratch.width != width || scratch.height != height) {
            scratch.resize(width, height, false);
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scratch.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
            GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.bindWrite(false);
        return true;
    }

    private static void beginPass() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void endPass() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    private static void drawQuad() {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.vertex(-1, -1, 0).endVertex();
        buffer.vertex(1, -1, 0).endVertex();
        buffer.vertex(1, 1, 0).endVertex();
        buffer.vertex(-1, 1, 0).endVertex();
        tesselator.end();
    }

    private static void ensureDirt() {
        if (dirtTex != null || dirtFailed) return;
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation id = new ResourceLocation("bodycam", "textures/dirty_lens.png");
        try {
            InputStream in = mc.getResourceManager().getResource(id).get().open();
            AbstractTexture t = new DynamicTexture(NativeImage.read(in));
            mc.getTextureManager().register(id, t);
            dirtTex = t;
        } catch (Exception e) {
            dirtFailed = true;
            BodycamClient.LOGGER.warn("[bodycam] Falha ao carregar dirty_lens.png: {}", e.getMessage());
        }
    }

    public static void render() {
        if (postShader == null) return;
        BodycamConfig cfg = BodycamConfig.loadCached();
        if (!cfg.enabled || !cfg.postFXEnabled) return;
        if (IrisCompat.isRenderingShadowPass()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();
        if (width <= 0 || height <= 0) return;

        if (!copyScreen()) return;
        ensureDirt();

        double qs = qualityScale(cfg);

        if (!logged) {
            logged = true;
            BodycamClient.LOGGER.info(
                "[bodycam] postFX render ATIVO: fisheyeK1={} ca={} vignette={} dirt={} grain={} shaderOK={}",
                cfg.fisheyeK1, cfg.chromaticAberration, cfg.vignetteCosPow,
                cfg.lensDirtIntensity, cfg.grainStrength, postShader != null);
        }

        beginPass();
        RenderSystem.setShader(() -> postShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        postShader.setSampler("ScreenTex", scratch);
        if (dirtTex != null) {
            postShader.setSampler("DirtyLensTex", dirtTex);
        }

        var u = postShader.getUniform("PostFXEnabled");
        if (u != null) u.set(cfg.postFXEnabled ? 1f : 0f);

        u = postShader.getUniform("FisheyeK1");
        if (u != null) u.set((float) cfg.fisheyeK1);
        u = postShader.getUniform("FisheyeK2");
        if (u != null) u.set((float) cfg.fisheyeK2);

        u = postShader.getUniform("VignetteCosPow");
        if (u != null) u.set((float) cfg.vignetteCosPow);
        u = postShader.getUniform("VignetteMech");
        if (u != null) u.set((float) cfg.vignetteMech);
        u = postShader.getUniform("VignetteFloor");
        if (u != null) u.set((float) cfg.vignetteFloor);
        u = postShader.getUniform("VignetteCenterGain");
        if (u != null) u.set((float) cfg.vignetteCenterGain);
        u = postShader.getUniform("VignetteBlur");
        if (u != null) u.set((float) cfg.vignetteBlur);
        if (!loggedBlur) {
            loggedBlur = true;
            BodycamClient.LOGGER.info("[bodycam] VignetteBlur: uniform={} value={}",
                u != null ? "OK" : "NULL!", cfg.vignetteBlur);
        }

        u = postShader.getUniform("CAStrength");
        if (u != null) u.set((float) (cfg.chromaticAberration * qs));

        u = postShader.getUniform("LensDirtIntensity");
        if (u != null) u.set((float) (cfg.lensDirtIntensity * qs));
        u = postShader.getUniform("LensDirtVeiling");
        if (u != null) u.set((float) (cfg.lensDirtVeiling * qs));

        u = postShader.getUniform("GrainStrength");
        if (u != null) u.set((float) (BodycamEffects.getGrainStrength() * qs));
        u = postShader.getUniform("GrainCellSize");
        if (u != null) u.set((float) cfg.grainCellSize);

        u = postShader.getUniform("Time");
        if (u != null) u.set(RenderSystem.getShaderGameTime());

        u = postShader.getUniform("ScreenSize");
        if (u != null) u.set((float) width, (float) height);

        u = postShader.getUniform("FovWideMult");
        if (u != null) u.set((float) cfg.fovWideMult);

        u = postShader.getUniform("WaterRefraction");
        if (u != null) u.set(qs >= 1.0 ? (float) (cfg.waterRefraction * BodycamEffects.getUnderwaterT()) : 0f);
        u = postShader.getUniform("Underwater");
        if (u != null) u.set((float) BodycamEffects.getUnderwaterT());

        u = postShader.getUniform("Exposure");
        if (u != null) u.set((float) BodycamEffects.getExposure());

        // roadmap 09 — lens contamination envelopes
        u = postShader.getUniform("LensDrops");
        if (u != null) u.set((float) BodycamEffects.getLensDrops());
        u = postShader.getUniform("LensCond");
        if (u != null) u.set((float) BodycamEffects.getLensCond());
        u = postShader.getUniform("LensSmudge");
        if (u != null) u.set((float) BodycamEffects.getLensSmudge());

        // roadmap 07 — lens ghosting
        u = postShader.getUniform("Ghost");
        if (u != null) u.set((float) BodycamEffects.getGhostAmt());

        // roadmap 12 — dust motes (skipped while auto-quality is degraded)
        u = postShader.getUniform("Dust");
        if (u != null) u.set(qs >= 1.0 ? (float) BodycamEffects.getDustAmt() : 0f);

        // roadmap 10 — lens profile scales with how wide the FOV is pushed
        u = postShader.getUniform("EdgeProfile");
        if (u != null) {
            double fovFactor = Mth.clamp((cfg.fovWideMult - 1.0) / 0.5, 0.0, 1.0);
            u.set(cfg.lensProfileEnabled ? (float) (cfg.lensProfileStrength * fovFactor) : 0f);
        }

        // roadmap 02 — angular motion blur vector (0 while still; 0 when degraded)
        u = postShader.getUniform("BlurVec");
        if (u != null) {
            if (!cfg.motionBlurEnabled || degraded) {
                u.set(0f, 0f);
            } else {
                CameraState st = CameraState.INSTANCE;
                double yawN = Mth.clamp(st.yawVel / 1200.0, -1.0, 1.0);
                double pitchN = Mth.clamp(st.pitchVel / 1200.0, -1.0, 1.0);
                double mag = Math.hypot(yawN, pitchN);
                if (mag < 0.02) {
                    u.set(0f, 0f);
                } else {
                    double maxBlur = 0.05 * cfg.motionBlurStrength;
                    u.set((float) (yawN * maxBlur), (float) (pitchN * maxBlur));
                }
            }
        }

        drawQuad();

        // GPU probe: the shader paints a tiny marker pixel encoding whether the
        // VignetteBlur uniform and the edge-mask branch are alive ON THE GPU.
        // Read back once on the first frame and log it — this is the only way
        // to tell "uniform never arrived" from "arrived but invisible".
        if (!probeLogged) {
            probeLogged = true;
            try {
                RenderTarget main = mc.getMainRenderTarget();
                int px = (int) (0.105 * width);
                int py = (int) (0.905 * height);
                java.nio.ByteBuffer bb = org.lwjgl.BufferUtils.createByteBuffer(4);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
                GL11.glReadPixels(px, py, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bb);
                main.bindWrite(false);
                int pr = bb.get(0) & 255, pg = bb.get(1) & 255, pb = bb.get(2) & 255, pa = bb.get(3) & 255;
                BodycamClient.LOGGER.info(
                    "[bodycam] GPU probe: VignetteBlurAlive={} edgeMaskAlive={} rgba={},{},{},{}",
                    pr >= 128, pg >= 128, pr, pg, pb, pa);
            } catch (Throwable t) {
                BodycamClient.LOGGER.warn("[bodycam] GPU probe falhou: {}", t.toString());
            }
        }

        postShader.clear();
        endPass();
    }

    public static boolean renderBlur(float radius) {
        if (blurShader == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null && mc.player == null && mc.screen == null) return false;
        if (!copyScreen()) return false;

        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        beginPass();
        RenderSystem.setShader(() -> blurShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        blurShader.setSampler("ScreenTex", scratch);
        var u = blurShader.getUniform("BlurRadius");
        if (u != null) u.set(radius);
        u = blurShader.getUniform("ScreenSize");
        if (u != null) u.set((float) width, (float) height);
        drawQuad();
        blurShader.clear();
        endPass();
        return true;
    }

    public static boolean isRegistered() {
        return postShader != null;
    }
}
