package net.rai.scp_unleashed.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Raid {
    private final int Id;
    private final ResourceLocation Faction;
    private final int Era;
    private final ResourceKey<Level> Dimension;
    private final String Group;
    private final BlockPos Estimate;
    private final double Error;
    private final double Footprint;
    private final double Confidence;
    private final double Heading;
    private final boolean Forced;
    private final long Created;
    private RaidStage Stage = RaidStage.Warning;
    private RaidOutcome Outcome = RaidOutcome.None;
    private long StageAt;
    private long Deadline;
    private long LastPresence;
    private BlockPos Target;
    private final Set<UUID> Raiders = new LinkedHashSet<>();
    @Nullable
    private UUID Caster;
    private int Spawned;
    private int Lost;
    private int Stolen;
    private CompoundTag Extra = new CompoundTag();
    private final Set<Long> Claims = new HashSet<>();
    private Runnable Dirty = () -> {
    };

    public Raid(int Id, ResourceLocation Faction, int Era, ResourceKey<Level> Dimension, String Group, BlockPos Estimate, double Error, double Footprint,
                double Confidence, double Heading, boolean Forced, long Created, BlockPos Target) {
        this.Id = Id;
        this.Faction = Faction;
        this.Era = Era;
        this.Dimension = Dimension;
        this.Group = Group;
        this.Estimate = Estimate.immutable();
        this.Error = Error;
        this.Footprint = Footprint;
        this.Confidence = Confidence;
        this.Heading = Heading;
        this.Forced = Forced;
        this.Created = Created;
        this.StageAt = Created;
        this.LastPresence = Created;
        this.Target = Target.immutable();
    }

    void bind(Runnable Marker) {
        Dirty = Marker;
    }

    public void changed() {
        Dirty.run();
    }

    public int id() {
        return Id;
    }

    public ResourceLocation faction() {
        return Faction;
    }

    public int era() {
        return Era;
    }

    public ResourceKey<Level> dimension() {
        return Dimension;
    }

    public String group() {
        return Group;
    }

    public BlockPos estimate() {
        return Estimate;
    }

    public double error() {
        return Error;
    }

    public double footprint() {
        return Footprint;
    }

    public double confidence() {
        return Confidence;
    }

    public double heading() {
        return Heading;
    }

    public boolean forced() {
        return Forced;
    }

    public long created() {
        return Created;
    }

    public RaidStage stage() {
        return Stage;
    }

    public void setStage(RaidStage Value, long Now) {
        Stage = Value;
        StageAt = Now;
        changed();
    }

    public long stageAt() {
        return StageAt;
    }

    public RaidOutcome outcome() {
        return Outcome;
    }

    public void setOutcome(RaidOutcome Value) {
        Outcome = Value;
        changed();
    }

    public long deadline() {
        return Deadline;
    }

    public void setDeadline(long Value) {
        Deadline = Value;
        changed();
    }

    public long lastPresence() {
        return LastPresence;
    }

    public void setLastPresence(long Value) {
        LastPresence = Value;
    }

    public BlockPos target() {
        return Target;
    }

    public Vec3 targetCentre() {
        return Vec3.atBottomCenterOf(Target);
    }

    public void setTarget(BlockPos Value) {
        Target = Value.immutable();
        changed();
    }

    public boolean live() {
        return Stage != RaidStage.Ended;
    }

    public Set<UUID> raiders() {
        return Collections.unmodifiableSet(Raiders);
    }

    public int remaining() {
        return Raiders.size();
    }

    public void enlist(UUID Raider, boolean IsCaster) {
        Raiders.add(Raider);
        Spawned++;
        if (IsCaster) {
            Caster = Raider;
        }
        changed();
    }

    public boolean release(UUID Raider, boolean Killed) {
        if (!Raiders.remove(Raider)) {
            return false;
        }
        if (Killed) {
            Lost++;
        }
        changed();
        return true;
    }

    @Nullable
    public UUID caster() {
        return Caster;
    }

    public boolean isCaster(UUID Raider) {
        return Raider.equals(Caster);
    }

    public int spawned() {
        return Spawned;
    }

    public int lost() {
        return Lost;
    }

    public int stolen() {
        return Stolen;
    }

    public void addStolen() {
        Stolen++;
        changed();
    }

    public CompoundTag extra() {
        return Extra;
    }

    public boolean claim(long Key) {
        return Claims.add(Key);
    }

    public void unclaim(long Key) {
        Claims.remove(Key);
    }

    public boolean claimed(long Key) {
        return Claims.contains(Key);
    }

    public CompoundTag save() {
        CompoundTag Saved = new CompoundTag();
        Saved.putInt("Id", Id);
        Saved.putString("Faction", Faction.toString());
        Saved.putInt("Era", Era);
        Saved.putString("Dimension", Dimension.location().toString());
        Saved.putString("Group", Group);
        Saved.putLong("Estimate", Estimate.asLong());
        Saved.putDouble("Error", Error);
        Saved.putDouble("Footprint", Footprint);
        Saved.putDouble("Confidence", Confidence);
        Saved.putDouble("Heading", Heading);
        Saved.putBoolean("Forced", Forced);
        Saved.putLong("Created", Created);
        Saved.putString("Stage", Stage.serialName());
        Saved.putString("Outcome", Outcome.serialName());
        Saved.putLong("StageAt", StageAt);
        Saved.putLong("Deadline", Deadline);
        Saved.putLong("LastPresence", LastPresence);
        Saved.putLong("Target", Target.asLong());
        ListTag Names = new ListTag();
        for (UUID Raider : Raiders) {
            Names.add(StringTag.valueOf(Raider.toString()));
        }
        Saved.put("Raiders", Names);
        if (Caster != null) {
            Saved.putUUID("Caster", Caster);
        }
        Saved.putInt("Spawned", Spawned);
        Saved.putInt("Lost", Lost);
        Saved.putInt("Stolen", Stolen);
        Saved.put("Extra", Extra);
        return Saved;
    }

    @Nullable
    public static Raid load(CompoundTag Saved) {
        ResourceLocation Faction = ResourceLocation.tryParse(Saved.getString("Faction"));
        ResourceLocation Place = ResourceLocation.tryParse(Saved.getString("Dimension"));
        if (Faction == null || Place == null) {
            return null;
        }
        Raid Loaded = new Raid(Saved.getInt("Id"), Faction, Saved.getInt("Era"), ResourceKey.create(Registries.DIMENSION, Place), Saved.getString("Group"),
                BlockPos.of(Saved.getLong("Estimate")), Saved.getDouble("Error"), Saved.getDouble("Footprint"), Saved.getDouble("Confidence"),
                Saved.getDouble("Heading"), Saved.getBoolean("Forced"), Saved.getLong("Created"), BlockPos.of(Saved.getLong("Target")));
        Loaded.Stage = RaidStage.byName(Saved.getString("Stage"));
        Loaded.Outcome = RaidOutcome.byName(Saved.getString("Outcome"));
        Loaded.StageAt = Saved.getLong("StageAt");
        Loaded.Deadline = Saved.getLong("Deadline");
        Loaded.LastPresence = Saved.getLong("LastPresence");
        ListTag Names = Saved.getList("Raiders", Tag.TAG_STRING);
        for (int Index = 0; Index < Names.size(); Index++) {
            UUID Raider = uuid(Names.getString(Index));
            if (Raider != null) {
                Loaded.Raiders.add(Raider);
            }
        }
        if (Saved.hasUUID("Caster")) {
            Loaded.Caster = Saved.getUUID("Caster");
        }
        Loaded.Spawned = Saved.getInt("Spawned");
        Loaded.Lost = Saved.getInt("Lost");
        Loaded.Stolen = Saved.getInt("Stolen");
        Loaded.Extra = Saved.getCompound("Extra");
        return Loaded;
    }

    @Nullable
    public static UUID uuid(String Text) {
        try {
            return UUID.fromString(Text);
        } catch (IllegalArgumentException Problem) {
            return null;
        }
    }
}
