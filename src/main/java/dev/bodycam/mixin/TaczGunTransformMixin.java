package dev.bodycam.mixin;

import com.tacz.guns.client.model.BedrockGunModel;
import dev.bodycam.camera.TaczGunRealism;
import dev.bodycam.camera.TaczSway;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(com.tacz.guns.client.event.FirstPersonRenderGunEvent.class)
public class TaczGunTransformMixin {
    @Inject(method = "applyFirstPersonGunTransform", at = @At("HEAD"))
    private static void bodycam$applySway(ClientPlayerEntity player, ItemStack stack, MatrixStack matrices,
                                           BedrockGunModel model, float partialTick, CallbackInfo ci) {
        if (!TaczSway.hookLogado) {
            TaczSway.hookLogado = true;
            dev.bodycam.BodycamClient.LOGGER.info("[bodycam] hook TACZ applyFirstPersonGunTransform ATIVO (TACZ detectado: sim)");
        }
        double yaw = TaczSway.getYawOffset();
        double pitch = TaczSway.getPitchOffset();
        if (Math.abs(yaw) > 1.0e-4 || Math.abs(pitch) > 1.0e-4) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) pitch));
        }

        double lower = TaczGunRealism.getLowerAmount();
        if (Math.abs(lower) > 0.001) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) (lower * 15.0)));
            matrices.translate(0.0f, (float) (-lower * 0.08), (float) (lower * 0.05));
        }

        double swayT = TaczGunRealism.getRunSwayT();
        if (swayT > 0.001) {
            double swayYaw = Math.sin(swayT) * 2.0;
            double swayPitch = Math.cos(swayT * 0.7) * 1.5;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) swayYaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) swayPitch));
        }

        double recoilP = TaczGunRealism.getRecoilPitch();
        double recoilY = TaczGunRealism.getRecoilYaw();
        if (Math.abs(recoilP) > 0.001 || Math.abs(recoilY) > 0.001) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) recoilP));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) recoilY));
        }

        // Phase D — reload nod (own spring, fired by GunReloadEvent)
        double reloadNod = TaczGunRealism.getReloadNod();
        if (Math.abs(reloadNod) > 0.001) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) reloadNod));
            matrices.translate(0.0f, (float) (reloadNod * 0.006), 0.0f);
        }

        // Phase D — landing lag: the weapon keeps dropping after the body lands
        double landLag = TaczGunRealism.getLandLag();
        if (Math.abs(landLag) > 0.001) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) landLag));
            matrices.translate(0.0f, (float) (landLag * 0.008), 0.0f);
        }

        // Phase D — wall pullback: muzzle too close to a block -> draw the gun in
        double wallPull = TaczGunRealism.getWallPull();
        if (wallPull > 0.01) {
            matrices.translate(0.0f, (float) (-wallPull * 0.03), (float) (wallPull * 0.07));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) (wallPull * 6.0)));
        }
    }
}
