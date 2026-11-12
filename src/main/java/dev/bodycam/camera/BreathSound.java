package dev.bodycam.camera;

import dev.bodycam.BodycamForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge equivalent of the Fabric BreathSound registry hook: the "bodycam:breath"
 * SoundEvent is registered through a DeferredRegister attached to the mod event
 * bus by {@link #registerSounds(IEventBus)} (called from the @Mod constructor).
 */
public final class BreathSound {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BodycamForge.MOD_ID);

    public static final RegistryObject<SoundEvent> BREATH =
            SOUNDS.register("breath", () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(BodycamForge.MOD_ID, "breath")));

    private BreathSound() {}

    public static void registerSounds(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
