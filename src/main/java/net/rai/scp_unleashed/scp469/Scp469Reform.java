package net.rai.scp_unleashed.scp469;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Scp469Reform extends SavedData {
    public static final String DataName = "scp_unleashed_scp469_reform";
    public static final int DelayMin = 3600;
    public static final int DelayMax = 6000;
    public static final int Near = 10;
    public static final int Far = 36;
    public static final int Attempts = 48;
    public static final int Vertical = 6;
    public static final long RetryTicks = 200L;
    public static final double Watchers = 64.0;
    public static final double TooClose = 12.0;
    private static final SavedData.Factory<Scp469Reform> DataFactory = new SavedData.Factory<>(Scp469Reform::new, Scp469Reform::load);

    private final List<Pending> Waiting = new ArrayList<>();

    public record Pending(BlockPos Origin, long Due, Optional<Component> Name, Optional<Vec3> Seen, Optional<UUID> Who) {
    }

    public static Scp469Reform get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<Pending> pending() {
        return List.copyOf(Waiting);
    }

    public void clear() {
        Waiting.clear();
        setDirty();
    }

    public void schedule(ServerLevel World, Scp469Angel Creature) {
        BlockPos Origin = Creature.footing() != null ? Creature.footing() : Creature.blockPosition();
        int Delay = Mth.nextInt(World.getRandom(), DelayMin, DelayMax);
        Waiting.add(new Pending(Origin, World.getGameTime() + Delay, Optional.ofNullable(Creature.getCustomName()), Optional.ofNullable(Creature.lastSeen()),
                Optional.ofNullable(Creature.remembered())));
        setDirty();
    }

    public void hurry(ServerLevel World) {
        List<Pending> Moved = new ArrayList<>();
        for (Pending Entry : Waiting) {
            Moved.add(new Pending(Entry.Origin(), World.getGameTime() - 1L, Entry.Name(), Entry.Seen(), Entry.Who()));
        }
        Waiting.clear();
        Waiting.addAll(Moved);
        setDirty();
    }

    public void tick(ServerLevel World) {
        if (Waiting.isEmpty() || World.getGameTime() % 20L != 0L) {
            return;
        }
        process(World);
    }

    public void process(ServerLevel World) {
        process(World, Near, Far);
    }

    public void process(ServerLevel World, int Inner, int Outer) {
        long Now = World.getGameTime();
        List<Pending> Due = new ArrayList<>();
        for (Pending Entry : Waiting) {
            if (Entry.Due() <= Now) {
                Due.add(Entry);
            }
        }
        for (Pending Entry : Due) {
            Waiting.remove(Entry);
            if (!arrive(World, Entry, Inner, Outer)) {
                Waiting.add(new Pending(Entry.Origin(), Now + RetryTicks, Entry.Name(), Entry.Seen(), Entry.Who()));
            }
            setDirty();
        }
    }

    public static boolean arrive(ServerLevel World, Pending Entry, int Inner, int Outer) {
        if (!World.isLoaded(Entry.Origin())) {
            return false;
        }
        Optional<BlockPos> Spot = spot(World, Entry.Origin(), Inner, Outer, World.getRandom());
        if (Spot.isEmpty()) {
            return false;
        }
        Scp469Angel Creature = Scp469Registry.Angel.get().create(World);
        if (Creature == null) {
            return false;
        }
        Vec3 Bottom = Vec3.atBottomCenterOf(Spot.get());
        float Yaw = World.getRandom().nextFloat() * 360.0f;
        Creature.moveTo(Bottom.x, Bottom.y, Bottom.z, Yaw, 0.0f);
        Creature.setYHeadRot(Yaw);
        Creature.setYBodyRot(Yaw);
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(Spot.get()), MobSpawnType.EVENT, null);
        Entry.Name().ifPresent(Creature::setCustomName);
        Creature.remember(Entry.Seen().orElse(null), Entry.Who().orElse(null));
        Creature.beginReform();
        if (!World.noCollision(Creature) || !World.addFreshEntity(Creature)) {
            return false;
        }
        return true;
    }

    public static AABB spreadRoom(Vec3 Bottom) {
        return Scp469Angel.Spread.makeBoundingBox(Bottom);
    }

    public static boolean fits(ServerLevel World, BlockPos Pos) {
        return LeadPlanner.isDryStandable(World, Pos) && !World.getBlockState(Pos.below()).is(Blocks.BARRIER) && World.noCollision(spreadRoom(Vec3.atBottomCenterOf(Pos))) && !World.containsAnyLiquid(spreadRoom(Vec3.atBottomCenterOf(Pos)));
    }

    public static boolean watched(ServerLevel World, Vec3 Bottom) {
        Vec3 Middle = Bottom.add(0.0, 1.1, 0.0);
        for (ServerPlayer Watcher : World.players()) {
            if (Watcher.isSpectator() || !Watcher.isAlive() || Watcher.distanceToSqr(Middle) > Watchers * Watchers) {
                continue;
            }
            if (Watcher.distanceToSqr(Middle) < TooClose * TooClose) {
                return true;
            }
            Vec3 Eye = Watcher.getEyePosition();
            for (Vec3 Probe : new Vec3[]{Middle, Bottom.add(0.0, 2.1, 0.0)}) {
                HitResult Hit = World.clip(new ClipContext(Eye, Probe, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Watcher));
                if (Hit.getType() == HitResult.Type.MISS) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Optional<BlockPos> spot(ServerLevel World, BlockPos Origin, int Inner, int Outer, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Inner + Random.nextDouble() * Math.max(0, Outer - Inner);
            BlockPos Column = Origin.offset((int) Math.round(Math.cos(Angle) * Radius), 0, (int) Math.round(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column) || !World.isPositionEntityTicking(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (fits(World, Candidate)) {
                    if (!watched(World, Vec3.atBottomCenterOf(Candidate))) {
                        return Optional.of(Candidate);
                    }
                    break;
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        ListTag Saved = new ListTag();
        for (Pending Entry : Waiting) {
            CompoundTag Item = new CompoundTag();
            Item.put("Origin", NbtUtils.writeBlockPos(Entry.Origin()));
            Item.putLong("Due", Entry.Due());
            Entry.Name().ifPresent(Name -> Item.putString("Name", Component.Serializer.toJson(Name, Registries)));
            Entry.Seen().ifPresent(Seen -> {
                Item.putDouble("SeenX", Seen.x);
                Item.putDouble("SeenY", Seen.y);
                Item.putDouble("SeenZ", Seen.z);
            });
            Entry.Who().ifPresent(Who -> Item.putUUID("Who", Who));
            Saved.add(Item);
        }
        Root.put("Pending", Saved);
        return Root;
    }

    public static Scp469Reform load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp469Reform Data = new Scp469Reform();
        ListTag Saved = Root.getList("Pending", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Item = Saved.getCompound(Index);
            Optional<BlockPos> Origin = NbtUtils.readBlockPos(Item, "Origin");
            if (Origin.isEmpty()) {
                continue;
            }
            Optional<Component> Name = Item.contains("Name") ? Optional.ofNullable(Component.Serializer.fromJson(Item.getString("Name"), Registries)) : Optional.empty();
            Optional<Vec3> Seen = Item.contains("SeenX") ? Optional.of(new Vec3(Item.getDouble("SeenX"), Item.getDouble("SeenY"), Item.getDouble("SeenZ"))) : Optional.empty();
            Optional<UUID> Who = Item.hasUUID("Who") ? Optional.of(Item.getUUID("Who")) : Optional.empty();
            Data.Waiting.add(new Pending(Origin.get(), Item.getLong("Due"), Name.map(Component.class::cast), Seen, Who));
        }
        return Data;
    }
}
