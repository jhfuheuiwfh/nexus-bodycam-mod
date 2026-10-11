package dev.bodycam.mixin;

import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.ChestCam;
import dev.bodycam.camera.PostEffects;
import dev.bodycam.config.BodycamConfig;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Shadow
    private void bobView(MatrixStack matrices, float tickDelta) {
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void bodycam$onRenderWorld(float tickDelta, long startTime, MatrixStack matrices, CallbackInfo ci) {
        BodycamConfig config = BodycamConfig.loadCached();
        BodycamEffects.updateConfig(config);
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setInverseViewRotationMatrix(Lorg/joml/Matrix3f;)V",
            shift = At.Shift.AFTER))
    private void bodycam$applyRoll(float tickDelta, long startTime, MatrixStack matrices, CallbackInfo ci) {
        if (!BodycamEffects.getConfig().enabled) return;
        if (!BodycamEffects.hookRollLogado) {
            BodycamEffects.hookRollLogado = true;
            dev.bodycam.BodycamClient.LOGGER.info("[bodycam] hook de ROLL em renderWorld ATIVO");
        }
        float roll = (float) BodycamEffects.getRollTotal();
        if (Math.abs(roll) < 0.05f) return;
        matrices.multiply(new Quaternionf().rotationZ((float) Math.toRadians(roll)));
    }

    @Redirect(method = "renderWorld", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V"))
    private void bodycam$vanillaBob(GameRenderer self, MatrixStack matrices, float tickDelta) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (cfg.enabled && cfg.disableVanillaBobbing) return;
        this.bobView(matrices, tickDelta);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bodycam$wideFov(net.minecraft.client.render.Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double mult = ChestCam.getFovMultiplier();
        if (mult != 1.0) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void bodycam$postFX(float tickDelta, long startTime, MatrixStack matrices, CallbackInfo ci) {
        if (!PostEffects.isRegistered()) return;
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.postFXEnabled) return;
        if (dev.bodycam.compat.IrisCompat.isRenderingShadowPass()) return;

        PostEffects.render();
    }
}
