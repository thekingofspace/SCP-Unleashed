package net.rai.scp_unleashed.scp107;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class Scp107Rain extends SavedData {
    public static final String DataName = "scp_unleashed_scp107";
    public static final int RampTicks = 100;
    private static final SavedData.Factory<Scp107Rain> DataFactory = new SavedData.Factory<>(Scp107Rain::new, Scp107Rain::load);

    @Nullable
    private Scp107RainKind Kind;
    private int Elapsed;
    private int Total;
    private int Rumble = -1;

    public static Scp107Rain get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static float strength(float Elapsed, int Total) {
        if (Total <= 0) {
            return 0.0f;
        }
        float Rising = Elapsed / RampTicks;
        float Falling = (Total - Elapsed) / RampTicks;
        return Mth.clamp(Math.min(Rising, Falling), 0.0f, 1.0f);
    }

    public boolean active() {
        return Kind != null && Elapsed < Total;
    }

    @Nullable
    public Scp107RainKind kind() {
        return Kind;
    }

    public int elapsed() {
        return Elapsed;
    }

    public int total() {
        return Total;
    }

    public int remaining() {
        return Math.max(0, Total - Elapsed);
    }

    public int rumble() {
        return Rumble;
    }

    public float strength() {
        return Kind == null ? 0.0f : strength(Elapsed, Total);
    }

    public void set(Scp107RainKind Wanted, int Start, int End, int RumbleAt) {
        Kind = Wanted;
        Elapsed = Start;
        Total = Math.max(Start + 1, End);
        Rumble = RumbleAt;
        setDirty();
    }

    public void clear() {
        Kind = null;
        Elapsed = 0;
        Total = 0;
        Rumble = -1;
        setDirty();
    }

    public void advance() {
        Elapsed++;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        if (Kind != null) {
            Tag.putString("Kind", Kind.id());
            Tag.putInt("Elapsed", Elapsed);
            Tag.putInt("Total", Total);
            Tag.putInt("Rumble", Rumble);
        }
        return Tag;
    }

    public static Scp107Rain load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp107Rain Rain = new Scp107Rain();
        if (Tag.contains("Kind")) {
            Scp107RainKind.byId(Tag.getString("Kind")).ifPresent(Found -> {
                Rain.Kind = Found;
                Rain.Elapsed = Tag.getInt("Elapsed");
                Rain.Total = Tag.getInt("Total");
                Rain.Rumble = Tag.contains("Rumble") ? Tag.getInt("Rumble") : -1;
            });
        }
        return Rain;
    }
}
