package net.rai.scp_unleashed.scp106;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp106ReturnLedger extends SavedData {
    public static final String DataName = "scp_unleashed_scp106_returns";
    public static final int CheckEvery = 20;
    public static final int StaleTicks = 600;
    public static final int RetryTicks = 200;
    private static final SavedData.Factory<Scp106ReturnLedger> DataFactory = new SavedData.Factory<>(Scp106ReturnLedger::new, Scp106ReturnLedger::load);
    private final Map<UUID, Pending> Entries = new HashMap<>();
    private final Set<UUID> Superseded = new HashSet<>();

    public record Pending(ResourceKey<Level> Dimension, Vec3 Spot, long Due, @Nullable UUID Quarry) {
        public CompoundTag save() {
            CompoundTag Row = new CompoundTag();
            Row.putString("Dimension", Dimension.location().toString());
            Row.putDouble("X", Spot.x);
            Row.putDouble("Y", Spot.y);
            Row.putDouble("Z", Spot.z);
            Row.putLong("Due", Due);
            if (Quarry != null) {
                Row.putUUID("Quarry", Quarry);
            }
            return Row;
        }

        @Nullable
        public static Pending load(CompoundTag Row) {
            ResourceLocation Where = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (Where == null) {
                return null;
            }
            return new Pending(ResourceKey.create(Registries.DIMENSION, Where), new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")), Row.getLong("Due"),
                    Row.hasUUID("Quarry") ? Row.getUUID("Quarry") : null);
        }

        public Pending later(long Ticks) {
            return new Pending(Dimension, Spot, Due + Ticks, Quarry);
        }
    }

    public static Scp106ReturnLedger get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static long now(MinecraftServer Server) {
        return Server.overworld().getGameTime();
    }

    public void schedule(UUID Id, ResourceKey<Level> Dimension, Vec3 Spot, long Due, @Nullable UUID Quarry) {
        Entries.put(Id, new Pending(Dimension, Spot, Due, Quarry));
        setDirty();
    }

    public boolean pending(UUID Id) {
        return Entries.containsKey(Id);
    }

    public void complete(UUID Id) {
        if (Entries.remove(Id) != null) {
            setDirty();
        }
    }

    public boolean consumeSuperseded(UUID Id) {
        if (Superseded.remove(Id)) {
            setDirty();
            return true;
        }
        return false;
    }

    public int size() {
        return Entries.size();
    }

    public void tick(MinecraftServer Server) {
        if (Entries.isEmpty() || Server.getTickCount() % CheckEvery != 0) {
            return;
        }
        long Now = now(Server);
        for (Map.Entry<UUID, Pending> Row : new ArrayList<>(Entries.entrySet())) {
            Pending Entry = Row.getValue();
            if (Now < Entry.Due()) {
                continue;
            }
            ServerLevel World = Server.getLevel(Entry.Dimension());
            if (World == null) {
                continue;
            }
            Entity Found = World.getEntity(Row.getKey());
            boolean Loaded = false;
            if (Found instanceof Scp106OldMan Old && Old.isAlive()) {
                if (World.isPositionEntityTicking(Old.blockPosition())) {
                    if (Now >= Entry.Due() + StaleTicks) {
                        Old.hurryReturn();
                        Entries.put(Row.getKey(), Entry.later(StaleTicks));
                        setDirty();
                    }
                    continue;
                }
                Old.erase();
                Loaded = true;
            }
            if (replace(World, Row.getKey(), Entry)) {
                Entries.remove(Row.getKey());
                if (!Loaded) {
                    Superseded.add(Row.getKey());
                }
            } else {
                Entries.put(Row.getKey(), Entry.later(RetryTicks));
            }
            setDirty();
        }
    }

    private static boolean replace(ServerLevel World, UUID Old, Pending Entry) {
        LivingEntity Quarry = quarry(World, Entry);
        if (Quarry == null) {
            return false;
        }
        Vec3 Spot = Scp106OldMan.emergenceNear(World, Quarry, World.getRandom());
        if (Spot == null) {
            return false;
        }
        float Yaw = (float) (Mth.atan2(Quarry.getZ() - Spot.z, Quarry.getX() - Spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        Scp106OldMan Fresh = Scp106Spawning.emerge(World, Spot, Yaw, MobSpawnType.EVENT);
        if (Fresh == null) {
            return false;
        }
        Fresh.huntAfterReturn(Quarry);
        return true;
    }

    @Nullable
    private static LivingEntity quarry(ServerLevel World, Pending Entry) {
        LivingEntity Last = Entry.Quarry() != null && World.getEntity(Entry.Quarry()) instanceof LivingEntity Found ? Found : null;
        if (Scp106Targeting.rank(World, Last) == Scp106Targeting.RankPerson) {
            return Last;
        }
        LivingEntity Near = Scp106Targeting.choose(World, Entry.Spot(), Scp106OldMan.ReturnRange, Scp106OldMan.ReturnRange, 0.0, null, 1.0, false, true);
        if (Near != null) {
            return Near;
        }
        Player Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (Player Candidate : World.players()) {
            if (Scp106Targeting.rank(World, Candidate) != Scp106Targeting.RankPerson) {
                continue;
            }
            double Distance = Candidate.distanceToSqr(Entry.Spot());
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Rows = new ListTag();
        for (Map.Entry<UUID, Pending> Row : Entries.entrySet()) {
            CompoundTag Line = Row.getValue().save();
            Line.putUUID("Id", Row.getKey());
            Rows.add(Line);
        }
        Saved.put("Pending", Rows);
        ListTag Gone = new ListTag();
        for (UUID Id : Superseded) {
            CompoundTag Line = new CompoundTag();
            Line.putUUID("Id", Id);
            Gone.add(Line);
        }
        Saved.put("Superseded", Gone);
        return Saved;
    }

    public static Scp106ReturnLedger load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp106ReturnLedger Ledger = new Scp106ReturnLedger();
        for (Tag Raw : Saved.getList("Pending", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Raw;
            Pending Entry = Pending.load(Row);
            if (Entry != null && Row.hasUUID("Id")) {
                Ledger.Entries.put(Row.getUUID("Id"), Entry);
            }
        }
        for (Tag Raw : Saved.getList("Superseded", Tag.TAG_COMPOUND)) {
            CompoundTag Row = (CompoundTag) Raw;
            if (Row.hasUUID("Id")) {
                Ledger.Superseded.add(Row.getUUID("Id"));
            }
        }
        return Ledger;
    }
}
