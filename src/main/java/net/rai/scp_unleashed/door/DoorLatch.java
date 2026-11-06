package net.rai.scp_unleashed.door;

import net.minecraft.nbt.CompoundTag;

public final class DoorLatch {
    public static final int HoldTicks = 10;

    private boolean Powered;
    private boolean Latched;
    private boolean Held;
    private long LastBlocked = Long.MIN_VALUE / 2;

    public boolean isPowered() {
        return Powered;
    }

    public boolean isLatched() {
        return Latched;
    }

    public boolean isHeld() {
        return Held;
    }

    public boolean hold(boolean Holding) {
        if (Held == Holding) {
            return false;
        }
        Held = Holding;
        return true;
    }

    public boolean power(boolean Signal, boolean Manual) {
        if (Signal == Powered) {
            return false;
        }
        Powered = Signal;
        if (Manual) {
            Latched = Signal;
        }
        return true;
    }

    public boolean toggle(boolean Manual) {
        if (!Manual || Powered) {
            return false;
        }
        if (Held) {
            Held = false;
            Latched = false;
            return true;
        }
        Latched = !Latched;
        return true;
    }

    public boolean holdsOpen() {
        return Powered || Latched || Held;
    }

    public boolean needsClearance(float Progress) {
        return !holdsOpen() && Progress > 0.0f;
    }

    public boolean wanted(long Now, float Progress, boolean Obstructed) {
        if (Obstructed) {
            LastBlocked = Now;
        }
        return holdsOpen() || Progress > 0.0f && Now - LastBlocked < HoldTicks;
    }

    public void save(CompoundTag Tag) {
        Tag.putBoolean("Powered", Powered);
        Tag.putBoolean("Latched", Latched);
        Tag.putBoolean("Held", Held);
    }

    public void load(CompoundTag Tag) {
        Powered = Tag.getBoolean("Powered");
        Latched = Tag.getBoolean("Latched");
        Held = Tag.getBoolean("Held");
    }
}
