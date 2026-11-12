package dev.bodycam.mixin;

import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.CameraState;
import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "playStepSound", at = @At("HEAD"))
    private void bodycam$onFootstep(BlockPos pos, BlockState blockState, CallbackInfo ci) {
        if ((Object) this != Minecraft.getInstance().player) return;
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
    @Inject(method = "getViewVector(F)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
    private void bodycam$alignedAim(float partialTick, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this != Minecraft.getInstance().player) return;
        BodycamConfig cfg = BodycamEffects.getConfig();
        if (cfg == null || !cfg.enabled) return;
        CameraState st = CameraState.INSTANCE;
        if (Double.isNaN(st.lastCamYaw)) return;
        float yaw = st.finalViewYaw;
        float pitch = st.finalViewPitch;
        if (Float.isNaN(yaw) || Float.isNaN(pitch)) return;
        // Vanilla math (Entity.calculateViewVector): (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))
        float pitchRad = pitch * 0.017453292F;
        float yawRad = -yaw * 0.017453292F;
        float cy = Mth.cos(yawRad);
        float sy = Mth.sin(yawRad);
        float cp = Mth.cos(pitchRad);
        float sp = Mth.sin(pitchRad);
        cir.setReturnValue(new Vec3(sy * cp, -sp, cy * cp));
    }
}
