package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class XkState extends SavedData {
    public enum Stage { Idle, Xk1, Xk2, Ended, Clearing }

    private static final String Name = "scp_unleashed_scp5001_xk";
    private Stage Current = Stage.Idle;
    private long FirstElapsed;
    private long SecondElapsed;
    private long LastDayTime = -1L;
    private float Sky;
    private float ClearFrom;
    private int ClearElapsed;
    private int ClearTicks = 1200;
    private boolean Announced;
    private boolean EndingBegun;
    private final YaldabaothFlight Flight = new YaldabaothFlight();

    public static XkState get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(XkState::new, XkState::load, null), Name);
    }

    private static XkState load(CompoundTag Tag, HolderLookup.Provider Registries) {
        XkState State = new XkState();
        String Stored = Tag.getString("Stage");
        for (Stage Each : Stage.values()) {
            if (Each.name().equals(Stored)) {
                State.Current = Each;
            }
        }
        State.FirstElapsed = Tag.getLong("FirstElapsed");
        State.SecondElapsed = Tag.getLong("SecondElapsed");
        State.LastDayTime = Tag.contains("LastDayTime") ? Tag.getLong("LastDayTime") : -1L;
        State.Sky = Tag.getFloat("Sky");
        State.ClearFrom = Tag.getFloat("ClearFrom");
        State.ClearElapsed = Tag.getInt("ClearElapsed");
        State.ClearTicks = Tag.contains("ClearTicks") ? Tag.getInt("ClearTicks") : 1200;
        State.Announced = Tag.getBoolean("Announced");
        State.EndingBegun = Tag.getBoolean("EndingBegun");
        State.Flight.load(Tag.getCompound("Flight"));
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putString("Stage", Current.name());
        Tag.putLong("FirstElapsed", FirstElapsed);
        Tag.putLong("SecondElapsed", SecondElapsed);
        Tag.putLong("LastDayTime", LastDayTime);
        Tag.putFloat("Sky", Sky);
        Tag.putFloat("ClearFrom", ClearFrom);
        Tag.putInt("ClearElapsed", ClearElapsed);
        Tag.putInt("ClearTicks", ClearTicks);
        Tag.putBoolean("Announced", Announced);
        Tag.putBoolean("EndingBegun", EndingBegun);
        Tag.put("Flight", Flight.save());
        return Tag;
    }

    @Override
    public boolean isDirty() {
        return true;
    }

    public Stage stage() {
        return Current;
    }

    public void stage(Stage Next) {
        Current = Next;
    }

    public long firstElapsed() {
        return FirstElapsed;
    }

    public void firstElapsed(long Value) {
        FirstElapsed = Value;
    }

    public long secondElapsed() {
        return SecondElapsed;
    }

    public void secondElapsed(long Value) {
        SecondElapsed = Value;
    }

    public long lastDayTime() {
        return LastDayTime;
    }

    public void lastDayTime(long Value) {
        LastDayTime = Value;
    }

    public float sky() {
        return Sky;
    }

    public void sky(float Value) {
        Sky = Math.max(0.0f, Math.min(1.0f, Value));
    }

    public float clearFrom() {
        return ClearFrom;
    }

    public int clearElapsed() {
        return ClearElapsed;
    }

    public int clearTicks() {
        return ClearTicks;
    }

    public void clearing(float From, int Ticks) {
        ClearFrom = From;
        ClearElapsed = 0;
        ClearTicks = Math.max(1, Ticks);
    }

    public void clearTick() {
        ClearElapsed++;
    }

    public boolean announced() {
        return Announced;
    }

    public void announced(boolean Value) {
        Announced = Value;
    }

    public boolean endingBegun() {
        return EndingBegun;
    }

    public void endingBegun(boolean Value) {
        EndingBegun = Value;
    }

    public YaldabaothFlight flight() {
        return Flight;
    }

    public void wipe() {
        Current = Stage.Idle;
        FirstElapsed = 0L;
        SecondElapsed = 0L;
        LastDayTime = -1L;
        Sky = 0.0f;
        ClearFrom = 0.0f;
        ClearElapsed = 0;
        Announced = false;
        EndingBegun = false;
        Flight.clear();
    }
}
