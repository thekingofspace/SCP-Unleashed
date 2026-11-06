package net.rai.scp_unleashed.scp005.client;

import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.scp005.network.HoldsPayload;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HeldDoors {
    private static final Map<BlockPos, Long> Until = new HashMap<>();

    private HeldDoors() {
    }

    public static void replace(List<HoldsPayload.HeldDoor> Doors) {
        Until.clear();
        for (HoldsPayload.HeldDoor Door : Doors) {
            Until.put(Door.Anchor(), Door.Until());
        }
    }

    @Nullable
    public static Long untilAt(BlockPos Anchor) {
        return Until.get(Anchor);
    }

    public static void clear() {
        Until.clear();
    }
}
