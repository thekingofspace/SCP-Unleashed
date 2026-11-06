package net.rai.scp_unleashed.z0003.client;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import net.rai.scp_unleashed.integration.punchy.client.PunchyCompat;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.Method;

public final class Z0003HandSync {
    public static final String EquipClass = "punchy.client.state.HandEquipStateMachine";
    public static final String SyncName = "requestRenderedSync";

    private static final Logger Log = LogUtils.getLogger();
    private static boolean Checked;
    @Nullable
    private static Method Sync;

    private Z0003HandSync() {
    }

    public static void emptied() {
        if (!Checked) {
            Checked = true;
            Sync = ModList.get().isLoaded(PunchyCompat.ModId) ? resolve() : null;
        }
        if (Sync == null) {
            return;
        }
        try {
            Sync.invoke(null);
        } catch (Throwable Problem) {
            Sync = null;
            Log.warn("Could not ask Punchy to drop a thrown Z-0003 device from the hand, leaving it alone from now on", Problem);
        }
    }

    @Nullable
    private static Method resolve() {
        try {
            return Class.forName(EquipClass, false, Z0003HandSync.class.getClassLoader()).getMethod(SyncName);
        } catch (Throwable Problem) {
            Log.warn("Punchy is present but its hand state could not be reached, a thrown Z-0003 device may stay drawn in the hand for a moment", Problem);
            return null;
        }
    }
}
