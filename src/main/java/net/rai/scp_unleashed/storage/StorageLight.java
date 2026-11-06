package net.rai.scp_unleashed.storage;

public final class StorageLight {
    public static final int LampLevel = 7;
    public static final int MaxLevel = 15;
    public static final int FullBright = 0xF000F0;

    private StorageLight() {
    }

    public static int pack(int BlockLight, int SkyLight) {
        return BlockLight << 4 | SkyLight << 20;
    }

    public static int block(int Packed) {
        return (Packed & 0xFFFF) >> 4;
    }

    public static int sky(int Packed) {
        return Packed >> 20 & 0xFFFF;
    }

    public static int clamp(int Value) {
        return Math.max(0, Math.min(MaxLevel, Value));
    }

    public static int interior(int Packed) {
        return atLeast(Packed, LampLevel);
    }

    public static int atLeast(int Packed, int BlockLight) {
        if (BlockLight >= MaxLevel) {
            return FullBright;
        }
        return pack(Math.max(clamp(block(Packed)), clamp(BlockLight)), clamp(sky(Packed)));
    }
}
