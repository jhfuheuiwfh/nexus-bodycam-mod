package dev.bodycam.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.BedrockGunModel;
import dev.bodycam.camera.TaczGunRealism;
import dev.bodycam.camera.TaczSway;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge/mojmap port of the Fabric TACZ sway hook. The target class lives in
 * TACZ (compileOnly, see libs/); the whole mixin is gated by
 * BodycamMixinPlugin so it never applies when TACZ is absent.
 */
@Mixin(com.tacz.guns.client.event.FirstPersonRenderGunEvent.class)
public class TaczGunTransformMixin {
    @Inject(method = "applyFirstPersonGunTransform", at = @At("HEAD"), remap = false)
    private static void bodycam$applySway(LocalPlayer player, ItemStack stack, PoseStack matrices,
                                           BedrockGunModel model, float partialTick, CallbackInfo ci) {
        if (!TaczSway.hookLogado) {
            TaczSway.hookLogado = true;
            dev.bodycam.BodycamClient.LOGGER.info("[bodycam] hook TACZ applyFirstPersonGunTransform ATIVO (TACZ detectado: sim)");
        }
        double yaw = TaczSway.getYawOffset();
        double pitch = TaczSway.getPitchOffset();
        if (Math.abs(yaw) > 1.0e-4 || Math.abs(pitch) > 1.0e-4) {
            matrices.mulPose(Axis.YP.rotationDegrees((float) yaw));
            matrices.mulPose(Axis.XP.rotationDegrees((float) pitch));
        }

        double lower = TaczGunRealism.getLowerAmount();
        if (Math.abs(lower) > 0.001) {
            matrices.mulPose(Axis.XP.rotationDegrees((float) (lower * 15.0)));
            matrices.translate(0.0f, (float) (-lower * 0.08), (float) (lower * 0.05));
        }

        double swayT = TaczGunRealism.getRunSwayT();
        if (swayT > 0.001) {
            double swayYaw = Math.sin(swayT) * 2.0;
            double swayPitch = Math.cos(swayT * 0.7) * 1.5;
            matrices.mulPose(Axis.YP.rotationDegrees((float) swayYaw));
            matrices.mulPose(Axis.XP.rotationDegrees((float) swayPitch));
        }

        double recoilP = TaczGunRealism.getRecoilPitch();
        double recoilY = TaczGunRealism.getRecoilYaw();
        if (Math.abs(recoilP) > 0.001 || Math.abs(recoilY) > 0.001) {
            matrices.mulPose(Axis.XP.rotationDegrees((float) recoilP));
            matrices.mulPose(Axis.YP.rotationDegrees((float) recoilY));
        }

        // Phase D — reload nod (own spring, fired by GunReloadEvent)
        double reloadNod = TaczGunRealism.getReloadNod();
        if (Math.abs(reloadNod) > 0.001) {
            matrices.mulPose(Axis.XP.rotationDegrees((float) reloadNod));
            matrices.translate(0.0f, (float) (reloadNod * 0.006), 0.0f);
        }

        // Phase D — landing lag: the weapon keeps dropping after the body lands
        double landLag = TaczGunRealism.getLandLag();
        if (Math.abs(landLag) > 0.001) {
            matrices.mulPose(Axis.XP.rotationDegrees((float) landLag));
            matrices.translate(0.0f, (float) (landLag * 0.008), 0.0f);
        }

        // Phase D — wall pullback: muzzle too close to a block -> draw the gun in
        double wallPull = TaczGunRealism.getWallPull();
        if (wallPull > 0.01) {
            matrices.translate(0.0f, (float) (-wallPull * 0.03), (float) (wallPull * 0.07));
            matrices.mulPose(Axis.ZP.rotationDegrees((float) (wallPull * 6.0)));
        }
    }
}
