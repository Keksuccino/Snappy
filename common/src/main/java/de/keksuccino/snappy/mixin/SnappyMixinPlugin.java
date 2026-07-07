package de.keksuccino.snappy.mixin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

public class SnappyMixinPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SODIUM_MIXIN_PACKAGE = ".compat.sodium.";
    private static Boolean konkreteLoaded;
    private static Boolean sodiumLoaded;
    private static boolean loggedSodiumCompatPatches;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!isKonkreteLoaded()) {
            return false;
        }
        if (mixinClassName.contains(SODIUM_MIXIN_PACKAGE)) {
            boolean applySodiumMixin = isSodiumLoaded();
            if (applySodiumMixin) {
                logSodiumCompatPatchesOnce();
            }
            return applySodiumMixin;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    private static boolean isKonkreteLoaded() {
        if (konkreteLoaded != null) {
            return konkreteLoaded;
        }
        konkreteLoaded = isModLoaded("konkrete") || isClassAvailable("de.keksuccino.konkrete.Konkrete");
        return konkreteLoaded;
    }

    private static boolean isSodiumLoaded() {
        if (sodiumLoaded != null) {
            return sodiumLoaded;
        }
        sodiumLoaded = isModLoaded("sodium");
        return sodiumLoaded;
    }

    private static void logSodiumCompatPatchesOnce() {
        if (loggedSodiumCompatPatches) {
            return;
        }
        loggedSodiumCompatPatches = true;
        LOGGER.info("[SNAPPY] Detected Sodium, applying Sodium compatibility patches.");
    }

    private static boolean isModLoaded(String modId) {
        return isFabricModLoaded(modId) || isNeoForgeModLoaded(modId);
    }

    private static boolean isFabricModLoaded(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", false, SnappyMixinPlugin.class.getClassLoader());
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            return (Boolean) loaderClass.getMethod("isModLoaded", String.class).invoke(loader, modId);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    private static boolean isNeoForgeModLoaded(String modId) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList", false, SnappyMixinPlugin.class.getClassLoader());
            Object modList = modListClass.getMethod("get").invoke(null);
            return (Boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, modId);
        } catch (ReflectiveOperationException | LinkageError e) {
            return false;
        }
    }

    private static boolean isClassAvailable(String className) {
        try {
            Class.forName(className, false, SnappyMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

}
