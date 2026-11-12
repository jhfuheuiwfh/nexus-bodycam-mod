package dev.bodycam;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.bodycam.camera.BreathSound;
import dev.bodycam.camera.PostEffects;
import dev.bodycam.hud.RecOverlay;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge entry point for NEXUS Bodycam. Client-only mod: every client-side
 * listener is registered inside a dist guard so the class never loads a
 * client-only event type on a dedicated server.
 */
@Mod(BodycamForge.MOD_ID)
public class BodycamForge {
    public static final String MOD_ID = "bodycam";
    public static final Logger LOGGER = LoggerFactory.getLogger("Bodycam");

    public BodycamForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BreathSound.registerSounds(modBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::registerKeys);
            modBus.addListener(this::registerOverlays);
            modBus.addListener(this::registerShaders);
            modBus.addListener(this::clientSetup);
            // enables the "Config" button in the mod list (Forge mod list screen)
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (minecraft, parent) -> new dev.bodycam.gui.BodycamConfigScreen(parent)));
        }
        LOGGER.info("Bodycam (Forge) construtor: dist={}", FMLEnvironment.dist);
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(BodycamClient.TOGGLE_REC);
    }

    private void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("rec_overlay",
                (gui, graphics, partialTick, width, height) -> RecOverlay.renderOverlay(graphics, partialTick));
    }

    private void registerShaders(RegisterShadersEvent event) {
        try {
            // Forge 1.20.1 API: construct the ShaderInstance here with the
            // event's resource provider; the consumer re-points PostEffects'
            // static refs on every (re)load (resource pack change, F3+T).
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    BodycamForge.MOD_ID + ":post", DefaultVertexFormat.POSITION),
                    PostEffects::onLoadShader);
        } catch (Exception e) {
            BodycamForge.LOGGER.warn("[bodycam] Falha ao registar core shader post: {}", e.getMessage());
        }
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    BodycamForge.MOD_ID + ":dofblur", DefaultVertexFormat.POSITION),
                    PostEffects::onLoadBlurShader);
        } catch (Exception e) {
            BodycamForge.LOGGER.warn("[bodycam] Falha ao registar core shader dofblur: {}", e.getMessage());
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        BodycamClient.init();
    }
}
