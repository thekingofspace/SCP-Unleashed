package net.rai.scp_unleashed.scp131;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.archive.investigation.LeadPlanner;
import net.rai.scp_unleashed.scp131.network.Scp131GriefPayload;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Scp131Grief extends SavedData {
    public static final String DataName = "scp_unleashed_scp131";
    public static final int GriefTicks = 6000;
    public static final int GriefCap = 12000;
    public static final int GatherTicks = 2400;
    public static final int KillerGrace = 1200;
    public static final int SpotLimit = 8;
    private static final SavedData.Factory<Scp131Grief> DataFactory = new SavedData.Factory<>(Scp131Grief::new, Scp131Grief::load);
    private static volatile long ClientUntil;

    private long Until;
    private int NextSpot;
    private final List<Spot> Spots = new ArrayList<>();
    private final Map<UUID, Long> Killers = new HashMap<>();

    public static Scp131Grief get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public static boolean isGrieving(Level World) {
        if (World.isClientSide) {
            return World.getGameTime() < ClientUntil;
        }
        MinecraftServer Server = World.getServer();
        return Server != null && get(Server).Until > World.getGameTime();
    }

    public static void setClientUntil(long Value) {
        ClientUntil = Value;
    }

    public static long clientUntil() {
        return ClientUntil;
    }

    public long until() {
        return Until;
    }

    public long remaining(long Now) {
        return Math.max(0L, Until - Now);
    }

    public void mourn(ServerLevel World, Vec3 Where, @Nullable Entity Killer) {
        long Now = World.getGameTime();
        Until = Math.min(Now + GriefCap, Math.max(Until, Now) + GriefTicks);
        Spots.removeIf(Entry -> Entry.Expires() <= Now);
        Spots.add(new Spot(NextSpot++, LeadPlanner.dimensionOf(World), Where, Now + GatherTicks));
        while (Spots.size() > SpotLimit) {
            Spots.remove(0);
        }
        if (Killer != null) {
            Killers.put(Killer.getUUID(), Until + KillerGrace);
        }
        setDirty();
        broadcast(World.getServer());
    }

    public void grieve(MinecraftServer Server, long Now, int Ticks, @Nullable ServerLevel World, @Nullable Vec3 Where) {
        Until = Now + Math.max(0, Ticks);
        if (World != null && Where != null) {
            Spots.add(new Spot(NextSpot++, LeadPlanner.dimensionOf(World), Where, Now + Math.min(GatherTicks, Math.max(200, Ticks))));
        }
        setDirty();
        broadcast(Server);
    }

    public void cheer(MinecraftServer Server) {
        Until = 0L;
        Spots.clear();
        Killers.clear();
        setDirty();
        broadcast(Server);
    }

    public boolean isKiller(UUID Who, long Now) {
        Long Expires = Killers.get(Who);
        return Expires != null && Expires > Now;
    }

    public Optional<Spot> nearestSpot(ServerLevel World, Vec3 From, double Range) {
        long Now = World.getGameTime();
        String Dimension = LeadPlanner.dimensionOf(World);
        Spot Best = null;
        double BestDistance = Range * Range;
        for (Spot Entry : Spots) {
            if (Entry.Expires() <= Now || !Entry.Dimension().equals(Dimension)) {
                continue;
            }
            double Distance = Entry.Where().distanceToSqr(From);
            if (Distance <= BestDistance) {
                BestDistance = Distance;
                Best = Entry;
            }
        }
        return Optional.ofNullable(Best);
    }

    public void broadcast(@Nullable MinecraftServer Server) {
        if (Server != null) {
            PacketDistributor.sendToAllPlayers(new Scp131GriefPayload(Until));
        }
    }

    public void sendTo(ServerPlayer User) {
        PacketDistributor.sendToPlayer(User, new Scp131GriefPayload(Until));
    }

    @Override
    public CompoundTag save(CompoundTag Tag, HolderLookup.Provider Registries) {
        Tag.putLong("Until", Until);
        Tag.putInt("NextSpot", NextSpot);
        ListTag Saved = new ListTag();
        for (Spot Entry : Spots) {
            CompoundTag One = new CompoundTag();
            One.putInt("Id", Entry.Id());
            One.putString("Dimension", Entry.Dimension());
            One.putDouble("X", Entry.Where().x);
            One.putDouble("Y", Entry.Where().y);
            One.putDouble("Z", Entry.Where().z);
            One.putLong("Expires", Entry.Expires());
            Saved.add(One);
        }
        Tag.put("Spots", Saved);
        ListTag Remembered = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Killers.entrySet()) {
            CompoundTag One = new CompoundTag();
            One.putUUID("Id", Entry.getKey());
            One.putLong("Until", Entry.getValue());
            Remembered.add(One);
        }
        Tag.put("Killers", Remembered);
        return Tag;
    }

    public static Scp131Grief load(CompoundTag Tag, HolderLookup.Provider Registries) {
        Scp131Grief Data = new Scp131Grief();
        Data.Until = Tag.getLong("Until");
        Data.NextSpot = Tag.getInt("NextSpot");
        ListTag Saved = Tag.getList("Spots", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Saved.size(); Index++) {
            CompoundTag One = Saved.getCompound(Index);
            Data.Spots.add(new Spot(One.getInt("Id"), One.getString("Dimension"), new Vec3(One.getDouble("X"), One.getDouble("Y"), One.getDouble("Z")), One.getLong("Expires")));
        }
        ListTag Remembered = Tag.getList("Killers", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Remembered.size(); Index++) {
            CompoundTag One = Remembered.getCompound(Index);
            if (One.hasUUID("Id")) {
                Data.Killers.put(One.getUUID("Id"), One.getLong("Until"));
            }
        }
        return Data;
    }

    public record Spot(int Id, String Dimension, Vec3 Where, long Expires) {
    }
}
