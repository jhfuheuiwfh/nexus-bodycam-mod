package dev.bodycam.mixin;

import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.CameraState;
import dev.bodycam.camera.ChestCam;
import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void setPos(Vec3d pos);

    @Shadow public abstract float getYaw();
    @Shadow public abstract float getPitch();
    @Shadow public abstract Vec3d getPos();

    private static final CameraState state = CameraState.INSTANCE;

    @Inject(method = "update", at = @At("TAIL"))
    private void bodycam$onUpdate(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!BodycamEffects.hookCameraLogado) {
            BodycamEffects.hookCameraLogado = true;
            dev.bodycam.BodycamClient.LOGGER.info("[bodycam] hook Camera.update ATIVO");
        }
        if (!BodycamEffects.getConfig().enabled) return;

        dev.bodycam.core.RenderCoordinator.cameraUpdate(tickDelta);

        double offsetY = BodycamEffects.getVerticalOffset();
        Vec3d pos = getPos();
        if (Math.abs(offsetY) > 0.0001) {
            pos = pos.add(0, offsetY, 0);
        }

        pos = ChestCam.applyChestCam(pos);
        setPos(pos);

        float rawYaw = getYaw() + (float) BodycamEffects.getYawOffset();
        float rawPitch = getPitch() + (float) BodycamEffects.getPitchOffset();
        rawPitch = ChestCam.applyPitch(rawPitch);

        BodycamConfig cfg = BodycamEffects.getConfig();
        double dt = Math.max(BodycamEffects.lastDt, 1.0e-4);
        if (cfg.inertiaEnabled) {
            stepInertia(rawYaw, rawPitch, cfg, dt);
        } else {
            state.viewYawSmoothed = smoothView(state.viewYawSmoothed, rawYaw, cfg.viewSmoothSpeed, dt, true);
            state.viewPitchSmoothed = smoothView(state.viewPitchSmoothed, rawPitch, cfg.viewSmoothSpeed, dt, false);
        }
        state.viewDelta = MathHelper.wrapDegrees(rawYaw - state.viewYawSmoothed);

        setRotation(state.viewYawSmoothed, state.viewPitchSmoothed);
        state.finalViewYaw = state.viewYawSmoothed;
        state.finalViewPitch = state.viewPitchSmoothed;
        updateAngularVelocity(dt);
        dev.bodycam.camera.TaczSway.update();
        dev.bodycam.camera.TaczGunRealism.update();
    }

    /**
     * Roadmap 02 — angular velocity of the FINAL camera (post-inertia) in
     * deg/s, heavily smoothed (lambda 25) so the motion-blur vector dies the
     * instant the player stops turning instead of smearing one frame longer.
     */
    private static void updateAngularVelocity(double dt) {
        double yaw = state.finalViewYaw;
        double pitch = state.finalViewPitch;
        if (Double.isNaN(state.lastCamYaw)) {
            state.lastCamYaw = yaw;
            state.lastCamPitch = pitch;
            return;
        }
        double dyaw = MathHelper.wrapDegrees((float) (yaw - state.lastCamYaw));
        double dpitch = pitch - state.lastCamPitch;
        state.lastCamYaw = yaw;
        state.lastCamPitch = pitch;
        state.yawVel = dev.bodycam.camera.TemporalSmoother.expSmooth(
                state.yawVel, MathHelper.clamp(dyaw / dt, -1800.0, 1800.0), 25.0, dt);
        state.pitchVel = dev.bodycam.camera.TemporalSmoother.expSmooth(
                state.pitchVel, MathHelper.clamp(dpitch / dt, -1800.0, 1800.0), 25.0, dt);
    }

    private static float smoothView(float current, float target, double lambda, double dt, boolean wrap) {
        if (Float.isNaN(current) || lambda <= 0.0) return target;
        double delta = wrap ? MathHelper.wrapDegrees(target - current) : (target - current);
        if (Math.abs(delta) > 100.0) return target;
        return (float) (current + delta * (1.0 - Math.exp(-lambda * dt)));
    }

    /**
     * Roadmap 01 — Camera Inertia / Mass Lag: the body turns first, the camera
     * finishes the movement a few ms later. A 2nd-order spring (not exp smoothing)
     * gives the short overshoot on sudden direction changes and the slight carry-on
     * when the player stops — a chest-mounted camera, not a floating head.
     * omega = 1/inertiaDelay maps the delay knob (30-70ms) to bandwidth;
     * zeta = 1 - 0.6*overshoot maps 0..1 to critical..loose damping.
     */
    private static void stepInertia(float rawYaw, float rawPitch, BodycamConfig cfg, double dt) {
        if (!state.inertiaInit || Float.isNaN(state.viewYawSmoothed)) {
            state.viewYawSmoothed = rawYaw;
            state.viewPitchSmoothed = rawPitch;
            state.inertiaYawSpring.snap(rawYaw);
            state.inertiaPitchSpring.snap(rawPitch);
            state.inertiaInit = true;
            return;
        }
        double yawErr = MathHelper.wrapDegrees(rawYaw - (float) state.inertiaYawSpring.pos);
        double pitchErr = rawPitch - state.inertiaPitchSpring.pos;
        if (Math.abs(yawErr) > 100.0 || Math.abs(pitchErr) > 100.0) {
            state.viewYawSmoothed = rawYaw;
            state.viewPitchSmoothed = rawPitch;
            state.inertiaYawSpring.snap(rawYaw);
            state.inertiaPitchSpring.snap(rawPitch);
            return;
        }
        double omega = 1.0 / MathHelper.clamp(cfg.inertiaDelay, 0.02, 0.15);
        double zeta = 1.0 - 0.6 * MathHelper.clamp(cfg.inertiaOvershoot, 0.0, 1.0);
        state.inertiaYawSpring.stepWrapped(dt, omega, zeta, rawYaw);
        state.inertiaPitchSpring.step(dt, omega, zeta, rawPitch);
        state.viewYawSmoothed = (float) state.inertiaYawSpring.pos;
        state.viewPitchSmoothed = (float) state.inertiaPitchSpring.pos;
    }
}
