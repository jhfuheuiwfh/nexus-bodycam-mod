package dev.bodycam.camera;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.bodycam.BodycamClient;
import dev.bodycam.compat.IrisCompat;
import dev.bodycam.config.BodycamConfig;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.WindowFramebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;

public class PostEffects {
    private static ShaderProgram postShader;
    private static ShaderProgram blurShader;
    private static boolean registered = false;

    private static Framebuffer scratch;
    private static AbstractTexture dirtTex;
    private static boolean dirtFailed = false;
    private static boolean logged = false;

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

    public static void register() {
        if (registered) return;
        registered = true;
        CoreShaderRegistrationCallback.EVENT.register(ctx -> {
            try {
                ctx.register(new Identifier("bodycam", "post"), VertexFormats.POSITION, PostEffects::setShader);
            } catch (Exception e) {
                BodycamClient.LOGGER.warn("[bodycam] Falha ao registar core shader post: {}", e.getMessage());
            }
            try {
                ctx.register(new Identifier("bodycam", "dofblur"), VertexFormats.POSITION, PostEffects::setBlurShader);
            } catch (Exception e) {
                BodycamClient.LOGGER.warn("[bodycam] Falha ao registar core shader dofblur: {}", e.getMessage());
            }
        });
    }

    private static void setShader(ShaderProgram shader) {
        postShader = shader;
        BodycamClient.LOGGER.info("[bodycam] Core shader bodycam:post registado.");
    }

    private static void setBlurShader(ShaderProgram shader) {
        blurShader = shader;
        BodycamClient.LOGGER.info("[bodycam] Core shader bodycam:dofblur registado.");
    }

    public static boolean copyScreen() {
        MinecraftClient mc = MinecraftClient.getInstance();
        Framebuffer main = mc.getFramebuffer();
        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
        if (width <= 0 || height <= 0) return false;

        if (scratch == null) {
            scratch = new WindowFramebuffer(width, height);
        } else if (scratch.textureWidth != width || scratch.textureHeight != height) {
            scratch.resize(width, height, false);
        }

        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.fbo);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scratch.fbo);
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
            GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        main.beginWrite(false);
        return true;
    }

    private static void beginPass() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
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
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        buffer.vertex(-1, -1, 0).next();
        buffer.vertex(1, -1, 0).next();
        buffer.vertex(1, 1, 0).next();
        buffer.vertex(-1, 1, 0).next();
        tessellator.draw();
    }

    private static void ensureDirt() {
        if (dirtTex != null || dirtFailed) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier id = new Identifier("bodycam", "textures/dirty_lens.png");
        try {
            AbstractTexture t = mc.getTextureManager().getTexture(id);
            if (t == null) {
                InputStream in = mc.getResourceManager().getResource(id).orElseThrow().getInputStream();
                t = new NativeImageBackedTexture(NativeImage.read(in));
                mc.getTextureManager().registerTexture(id, t);
            }
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

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();
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
        postShader.addSampler("ScreenTex", scratch);
        if (dirtTex != null) {
            postShader.addSampler("DirtyLensTex", dirtTex);
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
            double fovFactor = MathHelper.clamp((cfg.fovWideMult - 1.0) / 0.5, 0.0, 1.0);
            u.set(cfg.lensProfileEnabled ? (float) (cfg.lensProfileStrength * fovFactor) : 0f);
        }

        // roadmap 02 — angular motion blur vector (0 while still; 0 when degraded)
        u = postShader.getUniform("BlurVec");
        if (u != null) {
            if (!cfg.motionBlurEnabled || degraded) {
                u.set(0f, 0f);
            } else {
                CameraState st = CameraState.INSTANCE;
                double yawN = MathHelper.clamp(st.yawVel / 1200.0, -1.0, 1.0);
                double pitchN = MathHelper.clamp(st.pitchVel / 1200.0, -1.0, 1.0);
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
        postShader.unbind();
        endPass();
    }

    public static boolean renderBlur(float radius) {
        if (blurShader == null) return false;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null && mc.player == null && mc.currentScreen == null) return false;
        if (!copyScreen()) return false;

        int width = mc.getWindow().getFramebufferWidth();
        int height = mc.getWindow().getFramebufferHeight();

        beginPass();
        RenderSystem.setShader(() -> blurShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        blurShader.addSampler("ScreenTex", scratch);
        var u = blurShader.getUniform("BlurRadius");
        if (u != null) u.set(radius);
        u = blurShader.getUniform("ScreenSize");
        if (u != null) u.set((float) width, (float) height);
        drawQuad();
        blurShader.unbind();
        endPass();
        return true;
    }

    public static boolean isRegistered() {
        return postShader != null;
    }
}
