package dev.bodycam;

import com.mojang.blaze3d.platform.InputConstants;
import dev.bodycam.camera.BodycamEffects;
import dev.bodycam.camera.WaterEffects;
import dev.bodycam.config.BodycamConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side hub for the bodycam pipeline. On Forge this is not an
 * entrypoint anymore: BodycamForge (the @Mod class) wires the mod-bus
 * events, and this class owns the FORGE-bus handlers plus the shared state.
 */
public final class BodycamClient {
    public static final Logger LOGGER = LoggerFactory.getLogger("Bodycam");
    public static final String MOD_ID = "bodycam";
    private static int tickCounter = 0;

    // v1.5 — unbound by default; rebind in Controls → NEXUS Bodycam
    public static final KeyMapping TOGGLE_REC = new KeyMapping(
            "key.bodycam.toggleRec",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.bodycam");

    private BodycamClient() {}

    public static void init() {
        BodycamConfig config = BodycamConfig.load();
        BodycamEffects.updateConfig(config);

        // Phase D — TACZ bridge only loads when the mod is present, so no
        // com.tacz symbol is ever resolved on a TACZ-less install
        if (ModList.get().isLoaded("tacz")) {
            dev.bodycam.camera.TaczBridge.init();
        }

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        // Water render hook (Fabric's WorldRenderEvents.AFTER_ENTITIES on Forge)
        forgeBus.addListener((RenderLevelStageEvent event) -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                WaterEffects.update();
                WaterEffects.render(event.getPoseStack(), event.getCamera().getPosition());
            }
        });
        // Fabric's ClientTickEvents.END_CLIENT_TICK on Forge
        forgeBus.addListener((TickEvent.ClientTickEvent event) -> {
            if (event.phase != TickEvent.Phase.END) return;
            onClientTick(Minecraft.getInstance());
        });

        LOGGER.info("Bodycam mod initialized! (Forge)");
    }

    private static void onClientTick(Minecraft client) {
        // v1.5 — keybind toggle with action-bar confirmation
        while (TOGGLE_REC.consumeClick()) {
            BodycamConfig cfg = BodycamConfig.load();
            cfg.recOverlayEnabled = !cfg.recOverlayEnabled;
            cfg.save();
            if (client.player != null) {
                client.player.displayClientMessage(Component.translatable(
                        cfg.recOverlayEnabled ? "bodycam.overlay.on" : "bodycam.overlay.off"), true);
            }
        }

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
    }
}
