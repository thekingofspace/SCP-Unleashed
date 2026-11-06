package net.rai.scp_unleashed.scp001scarlet;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class ScarletWorld extends SavedData {
    private static final String Name = "scp_unleashed_scarlet_world";
    private long LastDayTime = -1L;
    private long ClockElapsed;
    private float Sky;
    private float ClearFrom;
    private int ClearElapsed;
    private boolean Clearing;
    private boolean EndingBegun;
    private boolean Announced;
    private int Told;
    private long LastRite = Long.MIN_VALUE;

    public static ScarletWorld get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ScarletWorld::new, ScarletWorld::load, null), Name);
    }

    private static ScarletWorld load(CompoundTag Tag, HolderLookup.Provider Registries) {
        ScarletWorld Data = new ScarletWorld();
        Data.LastDayTime = Tag.contains("LastDayTime") ? Tag.getLong("LastDayTime") : -1L;
        Data.ClockElapsed = Tag.getLong("Clock");
        Data.Sky = Tag.getFloat("Sky");
        Data.ClearFrom = Tag.getFloat("ClearFrom");
        Data.ClearElapsed = Tag.getInt("ClearElapsed");
        Data.Clearing = Tag.getBoolean("Clearing");
        Data.EndingBegun = Tag.getBoolean("EndingBegun");
        Data.Announced = Tag.getBoolean("Announced");
        Data.Told = Tag.getInt("Told");
        Data.LastRite = Tag.contains("LastRite") ? Tag.getLong("LastRite") : Long.MIN_VALUE;
        return Data;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putLong("LastDayTime", LastDayTime);
        Tag.putLong("Clock", ClockElapsed);
        Tag.putFloat("Sky", Sky);
        Tag.putFloat("ClearFrom", ClearFrom);
        Tag.putInt("ClearElapsed", ClearElapsed);
        Tag.putBoolean("Clearing", Clearing);
        Tag.putBoolean("EndingBegun", EndingBegun);
        Tag.putBoolean("Announced", Announced);
        Tag.putInt("Told", Told);
        Tag.putLong("LastRite", LastRite);
        return Tag;
    }

    public long advance(long Now) {
        long Last = LastDayTime;
        LastDayTime = Now;
        setDirty();
        if (Last < 0L) {
            return 0L;
        }
        long Delta = Now - Last;
        return Delta >= 1L && Delta <= 24000L ? Delta : (Delta > 24000L ? 24000L : 0L);
    }

    public void forgetDayTime() {
        LastDayTime = -1L;
        setDirty();
    }

    public long clock() {
        return ClockElapsed;
    }

    public void clock(long Value) {
        ClockElapsed = Math.max(0L, Value);
        setDirty();
    }

    public float sky() {
        return Sky;
    }

    public void sky(float Value) {
        Sky = Math.max(0.0f, Math.min(1.0f, Value));
        setDirty();
    }

    public boolean clearing() {
        return Clearing;
    }

    public void beginClearing() {
        Clearing = true;
        ClearFrom = Sky;
        ClearElapsed = 0;
        setDirty();
    }

    public float clearFrom() {
        return ClearFrom;
    }

    public int clearTick() {
        ClearElapsed++;
        setDirty();
        return ClearElapsed;
    }

    public void stopClearing() {
        Clearing = false;
        setDirty();
    }

    public boolean endingBegun() {
        return EndingBegun;
    }

    public void endingBegun(boolean Value) {
        EndingBegun = Value;
        setDirty();
    }

    public boolean announced() {
        return Announced;
    }

    public void announced(boolean Value) {
        Announced = Value;
        setDirty();
    }

    public int told() {
        return Told;
    }

    public void told(int Value) {
        Told = Value;
        setDirty();
    }

    public long lastRite() {
        return LastRite;
    }

    public void lastRite(long Value) {
        LastRite = Value;
        setDirty();
    }

    public void wipe() {
        LastDayTime = -1L;
        ClockElapsed = 0L;
        Sky = 0.0f;
        ClearFrom = 0.0f;
        ClearElapsed = 0;
        Clearing = false;
        EndingBegun = false;
        Announced = false;
        Told = 0;
        LastRite = Long.MIN_VALUE;
        setDirty();
    }
}
