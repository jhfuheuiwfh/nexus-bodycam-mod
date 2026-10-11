package dev.bodycam.mixin;

import dev.bodycam.camera.CameraState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void onTickMovement(CallbackInfo ci) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        if (player != MinecraftClient.getInstance().player) return;
        CameraState.INSTANCE.fallDistance = player.fallDistance;
        // Phase D: the sprint→recoil hack was removed — real shots now drive
        // the weapon recoil springs through TaczBridge (GunShootEvent), so the
        // camera is never kicked by running (roadmap rule: recoil ≠ sprint).
    }
}