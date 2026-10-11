package dev.bodycam;

import dev.bodycam.config.BodycamConfig;
import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.PostEffects;
import dev.bodycam.camera.WaterEffects;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BodycamClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Bodycam");
    public static final String MOD_ID = "bodycam";
    private static int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        dev.bodycam.camera.BreathSound.register();
        PostEffects.register();
        BodycamConfig config = BodycamConfig.load();
        BodycamEffects.updateConfig(config);

        // Phase D — TACZ bridge only loads when the mod is present, so no
        // com.tacz/cn.sh1rocu symbol is ever resolved on a TACZ-less install
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("tacz")) {
            dev.bodycam.camera.TaczBridge.init();
        }

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            WaterEffects.update();
            WaterEffects.render(ctx);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            BodycamConfig currentConfig = BodycamConfig.loadCached();
            BodycamEffects.updateConfig(currentConfig);
            if (++tickCounter % 60 == 0) {
                LOGGER.info("[diag] tick={} frames={} dt={} steps={} iv={} enabled={} bobY={} foot={} lean={} lag={} roll={} step={} swayY={} swayP={} viewD={} breath={} runT={} water={} waveT={}",
                        tickCounter, BodycamEffects.frames, BodycamEffects.lastDt,
                        BodycamEffects.footstepCount, BodycamEffects.getFootInterval(), currentConfig.enabled,
                        BodycamEffects.getBobVertical(), BodycamEffects.getFootRoll(), BodycamEffects.getLeanPitch(),
                        BodycamEffects.getYawLag(), BodycamEffects.getRoll(), BodycamEffects.getStepOffsetY(),
                        dev.bodycam.camera.TaczSway.getYawOffset(), dev.bodycam.camera.TaczSway.getPitchOffset(),
                        dev.bodycam.camera.CameraState.INSTANCE.viewDelta,
                        BodycamEffects.getBreathMult(), BodycamEffects.getRunTime(),
                        WaterEffects.isInWater(), String.format("%.2f", WaterEffects.getWaveTime()));
            }
        });

        LOGGER.info("Bodycam mod initialized!");
    }
}
