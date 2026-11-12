package dev.bodycam.mixin;

import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.ChestCam;
import dev.bodycam.camera.PostEffects;
import dev.bodycam.config.BodycamConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
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
    private void bobView(PoseStack poseStack, float partialTick) {
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void bodycam$onRenderLevel(float partialTick, long startTime, PoseStack poseStack, CallbackInfo ci) {
        BodycamConfig config = BodycamConfig.loadCached();
        BodycamEffects.updateConfig(config);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setInverseViewRotationMatrix(Lorg/joml/Matrix3f;)V",
            shift = At.Shift.AFTER))
    private void bodycam$applyRoll(float partialTick, long startTime, PoseStack poseStack, CallbackInfo ci) {
        if (!BodycamEffects.getConfig().enabled) return;
        if (!BodycamEffects.hookRollLogado) {
            BodycamEffects.hookRollLogado = true;
            dev.bodycam.BodycamClient.LOGGER.info("[bodycam] hook de ROLL em renderLevel ATIVO");
        }
        float roll = (float) BodycamEffects.getRollTotal();
        if (Math.abs(roll) < 0.05f) return;
        poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(roll)));
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void bodycam$vanillaBob(GameRenderer self, PoseStack poseStack, float partialTick) {
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (cfg.enabled && cfg.disableVanillaBobbing) return;
        this.bobView(poseStack, partialTick);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void bodycam$wideFov(Camera camera, float partialTick, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        double mult = ChestCam.getFovMultiplier();
        if (mult != 1.0) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void bodycam$postFX(float partialTick, long startTime, PoseStack poseStack, CallbackInfo ci) {
        if (!PostEffects.isRegistered()) return;
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (!cfg.enabled || !cfg.postFXEnabled) return;
        if (dev.bodycam.compat.IrisCompat.isRenderingShadowPass()) return;

        PostEffects.render();
    }
}
