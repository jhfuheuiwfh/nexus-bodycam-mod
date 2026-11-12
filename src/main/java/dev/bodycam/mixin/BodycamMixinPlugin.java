package dev.bodycam.mixin;

import net.minecraftforge.fml.ModList;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class BodycamMixinPlugin implements IMixinConfigPlugin {
    // Own logger on purpose: mixin configs load very early, and touching
    // BodycamClient here would classload a client-side class too soon.
    private static final Logger LOGGER = LoggerFactory.getLogger("Bodycam");

    @Override
    public void onLoad(String mixinClassName) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains("Tacz")) {
            boolean tacz = isTaczLoaded();
            if (!tacz) {
                LOGGER.info("[bodycam] TACZ not detected -> mixin {} disabled", mixinClassName);
            }
            // The Tacz mixins target TACZ classes directly, so they can only be
            // compiled when the third-party TACZ jar is present at build time.
            // A jar built WITHOUT it does not contain them at all — skip instead
            // of asking Mixin to load a class that is not there.
            else if (!classIsInThisJar(mixinClassName)) {
                LOGGER.info(
                        "[bodycam] TACZ present but mixin {} was not compiled into this jar -> disabled", mixinClassName);
                return false;
            }
            return tacz;
        }
        return true;
    }

    private static boolean isTaczLoaded() {
        try {
            return ModList.get().isLoaded("tacz");
        } catch (Throwable t) {
            // ModList not ready yet at mixin-config load time -> treat as absent
            return false;
        }
    }

    /** True when the given class file is packaged inside this mod's own jar. */
    private static boolean classIsInThisJar(String className) {
        String resource = className.replace('.', '/') + ".class";
        return BodycamMixinPlugin.class.getClassLoader().getResource(resource) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
