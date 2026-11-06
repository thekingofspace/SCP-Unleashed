package net.rai.scp_unleashed.scp352;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class Scp352Rest {
    public static final long Day = 24000L;
    public static final long Shortest = 2L * Day;
    public static final long Longest = 8L * Day;
    public static final long Deep = 5L * Day;
    public static final long Sound = 2L * Day;
    public static final long Thin = Day / 2L;

    private long StartGame;
    private long StartDay;
    private long Length;
    private long SavedGame;
    private long SavedDay;
    private boolean Active;

    public static long roll(RandomSource Random) {
        return Shortest + (long) Math.floor(Random.nextDouble() * (Longest - Shortest + 1L));
    }

    public static long clampLength(long Ticks) {
        return Math.max(0L, Math.min(Longest, Ticks));
    }

    public void begin(Level World, long Ticks) {
        StartGame = World.getGameTime();
        StartDay = World.getDayTime();
        Length = clampLength(Ticks);
        Active = true;
    }

    public void clear() {
        Active = false;
        Length = 0L;
    }

    public boolean active() {
        return Active;
    }

    public long length() {
        return Length;
    }

    public long elapsed(Level World) {
        if (!Active) {
            return 0L;
        }
        return Math.max(0L, Math.max(World.getGameTime() - StartGame, World.getDayTime() - StartDay));
    }

    public long left(Level World) {
        return Active ? Math.max(0L, Length - elapsed(World)) : 0L;
    }

    public boolean due(Level World) {
        return Active && elapsed(World) >= Length;
    }

    public void freeze(Level World) {
        if (!Active) {
            return;
        }
        StartGame += Math.max(0L, World.getGameTime() - SavedGame);
        StartDay += Math.max(0L, World.getDayTime() - SavedDay);
    }

    public void save(CompoundTag Tag, Level World) {
        Tag.putBoolean("Active", Active);
        Tag.putLong("StartGame", StartGame);
        Tag.putLong("StartDay", StartDay);
        Tag.putLong("Length", Length);
        Tag.putLong("SavedGame", World.getGameTime());
        Tag.putLong("SavedDay", World.getDayTime());
    }

    public void load(CompoundTag Tag) {
        Active = Tag.getBoolean("Active");
        StartGame = Tag.getLong("StartGame");
        StartDay = Tag.getLong("StartDay");
        Length = clampLength(Tag.getLong("Length"));
        SavedGame = Tag.getLong("SavedGame");
        SavedDay = Tag.getLong("SavedDay");
    }

    public static String depth(long Left) {
        if (Left > Deep) {
            return "deep";
        }
        if (Left > Sound) {
            return "sound";
        }
        if (Left > Thin) {
            return "thin";
        }
        return "stirring";
    }
}
