package dev.bodycam.mixin;

import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.CameraState;
import dev.bodycam.config.BodycamConfig;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "playStepSounds", at = @At("HEAD"))
    private void bodycam$onFootstep(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        BodycamEffects.onFootstep(blockState);
    }

    /**
     * Crosshair / block-breaking alignment: the rendered camera is rotated by
     * chest-cam pitch, inertia and shake, but vanilla aims with the raw player
     * yaw/pitch — so the crack overlay lands away from the crosshair. Make the
     * LOCAL player's look direction equal the FINAL camera rotation, so the ray
     * (and therefore the block outline + breaking progress) sits exactly under
     * the crosshair.
     */
    @Inject(method = "getRotationVec(F)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
    private void bodycam$alignedAim(float tickDelta, CallbackInfoReturnable<Vec3d> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (cfg == null || !cfg.enabled) return;
        CameraState st = CameraState.INSTANCE;
        if (Double.isNaN(st.lastCamYaw)) return;
        float yaw = st.finalViewYaw;
        float pitch = st.finalViewPitch;
        if (Float.isNaN(yaw) || Float.isNaN(pitch)) return;
        // Vanilla math (Entity.getRotationVector): (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))
        float pitchRad = pitch * 0.017453292F;
        float yawRad = -yaw * 0.017453292F;
        float cy = MathHelper.cos(yawRad);
        float sy = MathHelper.sin(yawRad);
        float cp = MathHelper.cos(pitchRad);
        float sp = MathHelper.sin(pitchRad);
        cir.setReturnValue(new Vec3d(sy * cp, -sp, cy * cp));
    }
}
