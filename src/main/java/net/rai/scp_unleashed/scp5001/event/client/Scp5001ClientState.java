package net.rai.scp_unleashed.scp5001.event.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001StatePayload;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp5001ClientState {
    @Nullable
    private static Scp5001StatePayload Latest;
    private static int Exposure;
    private static int ExposureMax = 100;

    private Scp5001ClientState() {
    }

    public static void accept(Scp5001StatePayload Payload) {
        Latest = Payload;
    }

    public static void exposure(int Stacks, int Max) {
        Exposure = Stacks;
        ExposureMax = Math.max(1, Max);
    }

    public static float exposure() {
        return Math.min(1.0f, Exposure / (float) ExposureMax);
    }

    public static int stacks() {
        return Exposure;
    }

    public static Scp5001State.Phase phase() {
        if (Latest == null) {
            return Scp5001State.Phase.Dormant;
        }
        Scp5001State.Phase[] All = Scp5001State.Phase.values();
        return Latest.Phase() >= 0 && Latest.Phase() < All.length ? All[Latest.Phase()] : Scp5001State.Phase.Dormant;
    }

    @Nullable
    public static Scp5001StatePayload latest() {
        return Latest;
    }

    public static long now() {
        Minecraft Client = Minecraft.getInstance();
        return Client.level == null ? 0L : Client.level.getGameTime();
    }

    public static float lift() {
        return Latest == null ? 0.0f : Latest.Lift();
    }

    public static boolean rodsSpent() {
        return Latest != null && Latest.RodsSpent();
    }

    public static float rodExtension(float Partial) {
        if (Latest == null) {
            return 0.0f;
        }
        Scp5001State.Phase Phase = phase();
        if (Latest.RodsSpent() || Phase == Scp5001State.Phase.Resetting) {
            return 0.0f;
        }
        if (Phase == Scp5001State.Phase.Overdrive || Phase == Scp5001State.Phase.Purging) {
            return 1.0f;
        }
        if (Phase == Scp5001State.Phase.Unlocked) {
            return 0.04f;
        }
        if (Phase != Scp5001State.Phase.Rising) {
            return 0.0f;
        }
        float Elapsed = now() - Latest.RiseStart() + Partial;
        return Math.max(0.0f, Math.min(1.0f, Elapsed / Math.max(1.0f, Latest.LatchTicks())));
    }

    public static boolean overdriven(BlockPos Generator) {
        if (Latest == null) {
            return false;
        }
        List<BlockPos> Hot = Latest.Hot();
        return Hot.contains(Generator);
    }

    public static boolean cooling() {
        return Latest != null && Latest.CooldownEnd() > now();
    }

    public static void reset() {
        Latest = null;
        Exposure = 0;
    }
}
