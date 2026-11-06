package net.rai.scp_unleashed.scp5001.mekhane;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MekhaneProtocol extends SavedData {
    public enum Stage {
        Idle,
        Engaged,
        Summon,
        Duel,
        Clear
    }

    private static final String Name = "scp_unleashed_scp5001_mekhane";
    private Stage Current = Stage.Idle;
    private long StageTick;
    private long Iron;
    private long Electronics;
    private BlockPos Emitter = BlockPos.ZERO;
    private boolean HasEmitter;
    private BlockPos Console = BlockPos.ZERO;
    private boolean HasConsole;
    @Nullable
    private UUID MekhaneId;
    @Nullable
    private UUID SerpentId;
    private int Cursor = -1;
    private int Stall;
    private int Restarts;
    private boolean Lone;
    private boolean Banished;
    private float Yaw;
    private Vec3 Arena = Vec3.ZERO;
    private boolean Forced;
    private int ForcedX;
    private int ForcedZ;

    public static MekhaneProtocol get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(MekhaneProtocol::new, MekhaneProtocol::load, null), Name);
    }

    private static MekhaneProtocol load(CompoundTag Tag, HolderLookup.Provider Registries) {
        MekhaneProtocol Made = new MekhaneProtocol();
        String Stored = Tag.getString("Stage");
        for (Stage Each : Stage.values()) {
            if (Each.name().equals(Stored)) {
                Made.Current = Each;
            }
        }
        Made.StageTick = Tag.getLong("StageTick");
        Made.Iron = Tag.getLong("Iron");
        Made.Electronics = Tag.getLong("Electronics");
        Made.HasEmitter = Tag.getBoolean("HasEmitter");
        Made.Emitter = BlockPos.of(Tag.getLong("Emitter"));
        Made.HasConsole = Tag.getBoolean("HasConsole");
        Made.Console = BlockPos.of(Tag.getLong("Console"));
        Made.MekhaneId = Tag.hasUUID("Mekhane") ? Tag.getUUID("Mekhane") : null;
        Made.SerpentId = Tag.hasUUID("Serpent") ? Tag.getUUID("Serpent") : null;
        Made.Cursor = Tag.contains("Cursor") ? Tag.getInt("Cursor") : -1;
        Made.Stall = Tag.getInt("Stall");
        Made.Restarts = Tag.getInt("Restarts");
        Made.Lone = Tag.getBoolean("Lone");
        Made.Banished = Tag.getBoolean("Banished");
        Made.Yaw = Tag.getFloat("Yaw");
        Made.Arena = new Vec3(Tag.getDouble("ArenaX"), Tag.getDouble("ArenaY"), Tag.getDouble("ArenaZ"));
        Made.Forced = Tag.getBoolean("Forced");
        Made.ForcedX = Tag.getInt("ForcedX");
        Made.ForcedZ = Tag.getInt("ForcedZ");
        return Made;
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putString("Stage", Current.name());
        Tag.putLong("StageTick", StageTick);
        Tag.putLong("Iron", Iron);
        Tag.putLong("Electronics", Electronics);
        Tag.putBoolean("HasEmitter", HasEmitter);
        Tag.putLong("Emitter", Emitter.asLong());
        Tag.putBoolean("HasConsole", HasConsole);
        Tag.putLong("Console", Console.asLong());
        if (MekhaneId != null) {
            Tag.putUUID("Mekhane", MekhaneId);
        }
        if (SerpentId != null) {
            Tag.putUUID("Serpent", SerpentId);
        }
        Tag.putInt("Cursor", Cursor);
        Tag.putInt("Stall", Stall);
        Tag.putInt("Restarts", Restarts);
        Tag.putBoolean("Lone", Lone);
        Tag.putBoolean("Banished", Banished);
        Tag.putFloat("Yaw", Yaw);
        Tag.putDouble("ArenaX", Arena.x);
        Tag.putDouble("ArenaY", Arena.y);
        Tag.putDouble("ArenaZ", Arena.z);
        Tag.putBoolean("Forced", Forced);
        Tag.putInt("ForcedX", ForcedX);
        Tag.putInt("ForcedZ", ForcedZ);
        return Tag;
    }

    public Stage stage() {
        return Current;
    }

    public long stageTick() {
        return StageTick;
    }

    public void stage(Stage Next, long GameTime) {
        Current = Next;
        StageTick = GameTime;
        Cursor = -1;
        Stall = 0;
        setDirty();
    }

    public void stageTick(long Value) {
        StageTick = Value;
        setDirty();
    }

    public long iron() {
        return Iron;
    }

    public long electronics() {
        return Electronics;
    }

    public void add(MekhaneValues.Kind Sort, long Units) {
        if (Sort == MekhaneValues.Kind.Iron) {
            Iron += Units;
        } else {
            Electronics += Units;
        }
        setDirty();
    }

    public long amount(MekhaneValues.Kind Sort) {
        return Sort == MekhaneValues.Kind.Iron ? Iron : Electronics;
    }

    public long need(MekhaneValues.Kind Sort) {
        return Sort == MekhaneValues.Kind.Iron ? MekhaneValues.ironNeed() : MekhaneValues.electronicsNeed();
    }

    public boolean met() {
        return Iron >= MekhaneValues.ironNeed() && Electronics >= MekhaneValues.electronicsNeed();
    }

    public void fill() {
        Iron = Math.max(Iron, MekhaneValues.ironNeed());
        Electronics = Math.max(Electronics, MekhaneValues.electronicsNeed());
        setDirty();
    }

    public boolean hasEmitter() {
        return HasEmitter;
    }

    public BlockPos emitter() {
        return Emitter;
    }

    public void emitter(BlockPos Pos) {
        if (!HasEmitter || !Emitter.equals(Pos)) {
            Emitter = Pos.immutable();
            HasEmitter = true;
            setDirty();
        }
    }

    public boolean hasConsole() {
        return HasConsole;
    }

    public BlockPos console() {
        return Console;
    }

    public void console(BlockPos Pos) {
        if (!HasConsole || !Console.equals(Pos)) {
            Console = Pos.immutable();
            HasConsole = true;
            setDirty();
        }
    }

    @Nullable
    public UUID mekhaneId() {
        return MekhaneId;
    }

    public void mekhaneId(@Nullable UUID Id) {
        MekhaneId = Id;
        setDirty();
    }

    @Nullable
    public UUID serpentId() {
        return SerpentId;
    }

    public void serpentId(@Nullable UUID Id) {
        SerpentId = Id;
        setDirty();
    }

    public int cursor() {
        return Cursor;
    }

    public void cursor(int Value) {
        Cursor = Value;
        setDirty();
    }

    public int stall() {
        return Stall;
    }

    public void stall(int Value) {
        Stall = Value;
        setDirty();
    }

    public int restarts() {
        return Restarts;
    }

    public void restarts(int Value) {
        Restarts = Value;
        setDirty();
    }

    public boolean lone() {
        return Lone;
    }

    public void lone(boolean Value) {
        Lone = Value;
        setDirty();
    }

    public boolean banished() {
        return Banished;
    }

    public void banished(boolean Value) {
        Banished = Value;
        setDirty();
    }

    public float yaw() {
        return Yaw;
    }

    public Vec3 arena() {
        return Arena;
    }

    public void arena(Vec3 Point, float Facing) {
        Arena = Point;
        Yaw = Facing;
        setDirty();
    }

    public boolean forced() {
        return Forced;
    }

    public int forcedX() {
        return ForcedX;
    }

    public int forcedZ() {
        return ForcedZ;
    }

    public void forced(boolean Value, int X, int Z) {
        Forced = Value;
        ForcedX = X;
        ForcedZ = Z;
        setDirty();
    }

    public void reset() {
        Current = Stage.Idle;
        Iron = 0;
        Electronics = 0;
        MekhaneId = null;
        SerpentId = null;
        Cursor = -1;
        Stall = 0;
        Restarts = 0;
        Lone = false;
        Banished = false;
        setDirty();
    }
}
