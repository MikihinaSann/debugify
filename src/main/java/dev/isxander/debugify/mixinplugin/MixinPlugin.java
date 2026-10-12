package dev.isxander.debugify.mixinplugin;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.fixes.BugFixData;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.*;

public class MixinPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
        Debugify.onPreInitialize();

        // NeoForge equivalent of upstream's "debugify" Fabric entrypoint:
        // other mods declare conflicts through [modproperties] in their own
        // neoforge.mods.toml, which is already readable at mixin-load time.
        // See META-INF/neoforge.mods.toml for the documented format.
        ModList modList = ModList.get();
        if (modList == null)
            return;

        for (IModFileInfo modFile : modList.getModFiles()) {
            String providerModId = modFile.getMods().stream()
                    .findFirst()
                    .map(info -> info.getModId())
                    .orElse(modFile.moduleName());
            readModProperties(providerModId, modFile.getFileProperties(), modList);
        }
    }

    private void readModProperties(String providerModId, Map<String, Object> properties, ModList modList) {
        // [modproperties.<modid>] conventionally nests the mod's own id, so
        // scan every level of the table for our keys. Supported forms:
        //   debugify.disabled_fixes = ["MC-1"]            (flat or nested)
        //   debugify.provided_disabled_fixes = { modid = ["MC-2"] }
        //   debugify = { disabled_fixes = [...], provided_disabled_fixes = {...} }
        for (String bugId : asStringList(findKey(properties, "debugify.disabled_fixes"))) {
            BugFixData.registerApiConflict(providerModId, bugId);
        }
        if (findKey(properties, "debugify") instanceof Map<?, ?> debugifyTable) {
            asStringList(debugifyTable.get("disabled_fixes"))
                    .forEach(bugId -> BugFixData.registerApiConflict(providerModId, bugId));
        }

        Object provided = findKey(properties, "debugify.provided_disabled_fixes");
        if (provided == null && findKey(properties, "debugify") instanceof Map<?, ?> debugifyTable) {
            provided = debugifyTable.get("provided_disabled_fixes");
        }
        if (provided instanceof Map<?, ?> providedMap) {
            providedMap.forEach((modIdObj, bugs) -> {
                String modId = String.valueOf(modIdObj);
                if (modList.isLoaded(modId)) {
                    asStringList(bugs).forEach(bugId -> BugFixData.registerApiConflict(modId, bugId));
                }
            });
        } else {
            // String/list form: "modid:MC-123,MC-456"
            for (String entry : asStringList(provided)) {
                int colon = entry.indexOf(':');
                if (colon < 0) continue;
                String modId = entry.substring(0, colon);
                if (modList.isLoaded(modId)) {
                    for (String bugId : entry.substring(colon + 1).split(",")) {
                        if (!bugId.isBlank())
                            BugFixData.registerApiConflict(modId, bugId.trim());
                    }
                }
            }
        }
    }

    private static Object findKey(Map<String, Object> map, String key) {
        Object direct = map.get(key);
        if (direct != null)
            return direct;
        for (Object value : map.values()) {
            if (value instanceof Map<?, ?> nested) {
                @SuppressWarnings("unchecked")
                Object found = findKey((Map<String, Object>) nested, key);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private static List<String> asStringList(Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        if (value instanceof String string) {
            return Arrays.stream(string.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
        return List.of();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }


    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        Optional<BugFixData> bugFixOptional = BugFixDataCache.getOrResolve(mixinClassName);

        if (bugFixOptional.isEmpty())
            return true;

        BugFixData bugFix = bugFixOptional.get();
        var multipleMixins = Debugify.CONFIG.getBugFixes().containsKey(bugFix);
        Debugify.CONFIG.registerBugFix(bugFix);

        if (DebugifyErrorHandler.hasErrored(bugFix)) {
            Debugify.LOGGER.warn("Preventing loading of {} mixin, {} because another mixin for the same bug fix failed to apply.", bugFix.bugId(), mixinClassName);
            return false;
        }

        Set<String> conflicts = bugFix.getActiveConflicts();
        if (!conflicts.isEmpty()) {
            if (Debugify.CONFIG.isBugFixEnabled(bugFix) && !multipleMixins)
                Debugify.LOGGER.warn("Force disabled {} because it's conflicting with: {}", bugFix.bugId(), String.join(", ", conflicts));
            return false;
        } else if (!bugFix.satisfiesOSRequirement()) {
            if (Debugify.CONFIG.isBugFixEnabled(bugFix) && !multipleMixins)
                Debugify.LOGGER.warn("Force disabled {} because it only applies to OS: {}", bugFix.bugId(), bugFix.requiredOs().name());
            return false;
        }

        return Debugify.CONFIG.isBugFixEnabled(bugFix);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
