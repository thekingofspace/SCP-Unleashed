package net.rai.scp_unleashed.scp001;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scp001Returns extends SavedData {
    public static final String DataName = "scp_unleashed_scp001_returns";
    public static final int EscapeDelayMin = 1200;
    public static final int EscapeDelayMax = 2400;
    public static final int EscapeNear = 16;
    public static final int EscapeFar = 40;
    public static final int Attempts = 40;
    public static final int Vertical = 6;
    public static final long RetryTicks = 200L;
    private static final SavedData.Factory<Scp001Returns> DataFactory = new SavedData.Factory<>(Scp001Returns::new, Scp001Returns::load);

    private final List<Pending> Waiting = new ArrayList<>();

    public record Pending(BlockPos Origin, long Due, Optional<Component> Name) {
    }

    public static Scp001Returns get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public List<Pending> pending() {
        return List.copyOf(Waiting);
    }

    public void clear() {
        Waiting.clear();
        setDirty();
    }

    public void schedule(ServerLevel World, Scp001Prototype Creature) {
        schedule(World, Creature, Creature.blockPosition());
    }

    public void schedule(ServerLevel World, Scp001Prototype Creature, BlockPos Origin) {
        RandomSource Random = World.getRandom();
        int Delay = Mth.nextInt(Random, EscapeDelayMin, EscapeDelayMax);
        Waiting.add(new Pending(Origin, World.getGameTime() + Delay, Optional.ofNullable(Creature.getCustomName())));
        setDirty();
    }

    public void tick(ServerLevel World) {
        if (Waiting.isEmpty() || World.getGameTime() % 20L != 0L) {
            return;
        }
        long Now = World.getGameTime();
        List<Pending> Due = new ArrayList<>();
        for (Pending Entry : Waiting) {
            if (Entry.Due() <= Now) {
                Due.add(Entry);
            }
        }
        for (Pending Entry : Due) {
            Waiting.remove(Entry);
            if (!arrive(World, Entry)) {
                Waiting.add(new Pending(Entry.Origin(), Now + RetryTicks, Entry.Name()));
            }
            setDirty();
        }
    }

    public static boolean arrive(ServerLevel World, Pending Entry) {
        Scp001Prototype Creature = Scp001Registry.Prototype.get().create(World);
        if (Creature == null) {
            return false;
        }
        Optional<BlockPos> Spot = spot(World, Creature, Entry.Origin(), EscapeNear, EscapeFar, World.getRandom());
        if (Spot.isEmpty()) {
            return false;
        }
        Entry.Name().ifPresent(Creature::setCustomName);
        return emerge(World, Creature, Spot.get(), World.getRandom().nextFloat() * 360.0f);
    }

    public static boolean emerge(ServerLevel World, Scp001Prototype Creature, BlockPos Pos, float Yaw) {
        Vec3 Bottom = Vec3.atBottomCenterOf(Pos);
        Creature.moveTo(Bottom.x, Bottom.y, Bottom.z, Yaw, 0.0f);
        Creature.setYHeadRot(Yaw);
        Creature.setYBodyRot(Yaw);
        Creature.finalizeSpawn(World, World.getCurrentDifficultyAt(Pos), MobSpawnType.EVENT, null);
        Creature.setHealth(Creature.getMaxHealth());
        Creature.beginEmerging();
        if (!World.addFreshEntity(Creature)) {
            return false;
        }
        Vec3 Back = Vec3.directionFromRotation(0.0f, Yaw).scale(-0.9);
        Scp001Portal.open(World, Bottom.add(Back), Yaw, Scp001Prototype.EmergeTicks + 20);
        return true;
    }

    public static Optional<BlockPos> spot(ServerLevel World, Scp001Prototype Creature, BlockPos Origin, int Near, int Far, RandomSource Random) {
        for (int Attempt = 0; Attempt < Attempts; Attempt++) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Radius = Near + Random.nextDouble() * Math.max(0, Far - Near);
            BlockPos Column = Origin.offset((int) Math.round(Math.cos(Angle) * Radius), 0, (int) Math.round(Math.sin(Angle) * Radius));
            if (!World.isLoaded(Column)) {
                continue;
            }
            for (int Dy = Vertical; Dy >= -Vertical; Dy--) {
                BlockPos Candidate = Column.above(Dy);
                if (Scp001Warp.fits(World, Creature, Candidate)) {
                    return Optional.of(Candidate);
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
            Saved.add(Item);
        }
        Root.put("Pending", Saved);
        return Root;
    }

    private static Scp001Returns load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp001Returns Data = new Scp001Returns();
        ListTag Saved = Root.getList("Pending", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag Item = Saved.getCompound(Index);
            Optional<BlockPos> Origin = NbtUtils.readBlockPos(Item, "Origin");
            if (Origin.isEmpty()) {
                continue;
            }
            Optional<Component> Name = Item.contains("Name") ? Optional.ofNullable(Component.Serializer.fromJson(Item.getString("Name"), Registries)) : Optional.empty();
            Data.Waiting.add(new Pending(Origin.get(), Item.getLong("Due"), Name.map(Component.class::cast)));
        }
        return Data;
    }
}
