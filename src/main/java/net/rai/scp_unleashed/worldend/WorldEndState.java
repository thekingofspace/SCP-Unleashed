package net.rai.scp_unleashed.worldend;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public final class WorldEndState extends SavedData {
    public enum Stage { Idle, Freezing, Released, Final }

    private static final String Name = "scp_unleashed_worldend";
    private Stage Current = Stage.Idle;
    private int Elapsed;
    private String Reason = "";
    private String Source = "";
    private boolean Focused;
    private double FocusX;
    private double FocusY;
    private double FocusZ;

    public static WorldEndState get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldEndState::new, WorldEndState::load, null), Name);
    }

    private static WorldEndState load(CompoundTag Tag, HolderLookup.Provider Registries) {
        WorldEndState State = new WorldEndState();
        String Stored = Tag.getString("Stage");
        for (Stage Each : Stage.values()) {
            if (Each.name().equals(Stored)) {
                State.Current = Each;
            }
        }
        State.Elapsed = Tag.getInt("Elapsed");
        State.Reason = Tag.getString("Reason");
        State.Source = Tag.getString("Source");
        State.Focused = Tag.getBoolean("Focused");
        State.FocusX = Tag.getDouble("FocusX");
        State.FocusY = Tag.getDouble("FocusY");
        State.FocusZ = Tag.getDouble("FocusZ");
        return State;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putString("Stage", Current.name());
        Tag.putInt("Elapsed", Elapsed);
        Tag.putString("Reason", Reason);
        Tag.putString("Source", Source);
        Tag.putBoolean("Focused", Focused);
        Tag.putDouble("FocusX", FocusX);
        Tag.putDouble("FocusY", FocusY);
        Tag.putDouble("FocusZ", FocusZ);
        return Tag;
    }

    public Stage stage() {
        return Current;
    }

    public void stage(Stage Next) {
        Current = Next;
        Elapsed = 0;
        setDirty();
    }

    public int elapsed() {
        return Elapsed;
    }

    public void advance() {
        Elapsed++;
        if (Elapsed % 20 == 0) {
            setDirty();
        }
    }

    public String reason() {
        return Reason;
    }

    public String source() {
        return Source;
    }

    public void begin(String ReasonJson, String SourceId) {
        Reason = ReasonJson;
        Source = SourceId;
        stage(Stage.Freezing);
    }

    public boolean focused() {
        return Focused;
    }

    public double focusX() {
        return FocusX;
    }

    public double focusY() {
        return FocusY;
    }

    public double focusZ() {
        return FocusZ;
    }

    public void focus(boolean Has, double X, double Y, double Z) {
        Focused = Has;
        FocusX = X;
        FocusY = Y;
        FocusZ = Z;
        setDirty();
    }

    public Component reasonComponent(HolderLookup.Provider Registries) {
        if (Reason.isEmpty()) {
            return Component.empty();
        }
        try {
            Component Parsed = Component.Serializer.fromJson(Reason, Registries);
            return Parsed == null ? Component.empty() : Parsed;
        } catch (RuntimeException Problem) {
            return Component.empty();
        }
    }
}
