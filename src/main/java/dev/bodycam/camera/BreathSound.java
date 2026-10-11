package dev.bodycam.camera;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class BreathSound {
    public static SoundEvent EVENT;

    private BreathSound() {}

    public static void register() {
        Identifier id = new Identifier("bodycam", "breath");
        EVENT = Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
