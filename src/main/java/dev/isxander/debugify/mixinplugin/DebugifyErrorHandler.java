package dev.isxander.debugify.mixinplugin;

import dev.isxander.debugify.Debugify;
import dev.isxander.debugify.fixes.BugFixData;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.extensibility.IMixinErrorHandler;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class DebugifyErrorHandler implements IMixinErrorHandler {
    private static final Set<BugFixData> ERRORED_FIXES = new HashSet<>();
    private static final Set<BugFixData> ALREADY_FIXED = new HashSet<>();

    @Override
    public ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action) {
        return handleError(action, mixin, th);
    }

    @Override
    public ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action) {
        return handleError(action, mixin, th);
    }

    private ErrorAction handleError(ErrorAction usualAction, IMixinInfo mixin, Throwable th) {
        Optional<BugFixData> bugFix = BugFixDataCache.getIfResolved(mixin.getClassName());
        if (bugFix.isEmpty())
            return usualAction;

        BugFixData fix = bugFix.get();

        // Walk the exception chain to find the root cause message —
        // Mixin often wraps the real InjectionError inside a RuntimeException.
        String message = extractDeepestMessage(th);

        // If the failure is because the injection point / target was not found,
        // the bug was most likely already fixed by Forge or another mod.
        // We suppress the crash and mark it as "already fixed" instead.
        if (isTargetNotFound(message)) {
            ALREADY_FIXED.add(fix);
            Debugify.LOGGER.info("Bug fix {} appears to already be fixed by Forge or another mod (injection target not found), skipping.", fix.bugId());
            return ErrorAction.NONE;
        }

        ERRORED_FIXES.add(fix);
        Debugify.LOGGER.error("Failed to fully apply bug fix {}, mixin class {} will not be applied! This may cause runtime errors if a partial injection occurs.", fix.bugId(), mixin.getName());
        return ErrorAction.WARN;
    }

    private static String extractDeepestMessage(Throwable th) {
        if (th == null) return "";
        String last = th.getMessage() == null ? "" : th.getMessage();
        Throwable cur = th.getCause();
        while (cur != null) {
            String m = cur.getMessage();
            if (m != null && !m.isEmpty()) last = m;
            cur = cur.getCause();
        }
        return last == null ? "" : last;
    }

    private static boolean isTargetNotFound(String message) {
        if (message == null || message.isEmpty()) return false;
        // Common Mixin error phrases when an injection point or target is absent.
        return message.contains("could not find any targets")
                || message.contains("0/1 succeeded")
                || message.contains("0/2 succeeded")
                || message.contains("0/3 succeeded")
                || message.contains("failed injection check")
                || message.contains("InjectionError")
                || message.contains("not found in target")
                || message.contains("target method not found")
                || message.contains("target field not found");
    }

    public static boolean hasErrored(BugFixData fix) {
        return ERRORED_FIXES.contains(fix);
    }

    public static void markAlreadyFixed(BugFixData fix) {
        ALREADY_FIXED.add(fix);
    }

    public static boolean isAlreadyFixed(BugFixData fix) {
        return ALREADY_FIXED.contains(fix);
    }

    public static Set<BugFixData> getAlreadyFixed() {
        return ALREADY_FIXED;
    }

    public static Set<BugFixData> getErrored() {
        return ERRORED_FIXES;
    }
}
