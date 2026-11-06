package net.rai.scp_unleashed.intro.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class IntroHand {
    public static final String PunchyId = "punchy";
    public static final String PunchyRenderer = "punchy.client.render.PunchyArmRenderer";
    public static final String PunchyHide = "setFirstPersonHidden";
    private static final Logger Log = LogUtils.getLogger();
    private static boolean Hidden;
    private static boolean PunchyChecked;
    private static Method PunchyToggle;

    private IntroHand() {
    }

    public static void sync(boolean Hide) {
        if (Hide == Hidden) {
            return;
        }
        Hidden = Hide;
        Minecraft.getInstance().gameRenderer.setRenderHand(!Hide);
        punchy(Hide);
    }

    public static void release() {
        sync(false);
    }

    private static void punchy(boolean Hide) {
        if (!PunchyChecked) {
            PunchyChecked = true;
            if (ModList.get().isLoaded(PunchyId)) {
                try {
                    PunchyToggle = Class.forName(PunchyRenderer).getMethod(PunchyHide, boolean.class);
                } catch (ReflectiveOperationException | LinkageError Problem) {
                    Log.warn("The intro could not reach Punchy's first-person hide switch, relying on the skipped hand pass", Problem);
                }
            }
        }
        if (PunchyToggle == null) {
            return;
        }
        try {
            PunchyToggle.invoke(null, Hide);
        } catch (ReflectiveOperationException | RuntimeException Problem) {
            Log.warn("The intro could not toggle Punchy's first-person arm", Problem);
            PunchyToggle = null;
        }
    }
}
