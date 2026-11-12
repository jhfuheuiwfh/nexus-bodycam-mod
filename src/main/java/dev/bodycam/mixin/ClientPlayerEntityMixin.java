package dev.bodycam.mixin;

import dev.bodycam.camera.CameraState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
    @Inject(method = "aiStep", at = @At("TAIL"))
    private void onTickMovement(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (player != Minecraft.getInstance().player) return;
        CameraState.INSTANCE.fallDistance = player.fallDistance;
        // Phase D: the sprint→recoil hack was removed — real shots now drive
        // the weapon recoil springs through TaczBridge (GunShootEvent), so the
        // camera is never kicked by running (roadmap rule: recoil ≠ sprint).
    }
}
