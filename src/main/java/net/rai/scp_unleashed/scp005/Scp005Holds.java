package net.rai.scp_unleashed.scp005;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp005.network.HoldsPayload;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Scp005Holds extends SavedData {
    public static final String DataName = "scp_unleashed_scp005_holds";
    public static final long Forever = Long.MAX_VALUE;
    private static final SavedData.Factory<Scp005Holds> DataFactory = new SavedData.Factory<>(Scp005Holds::new, Scp005Holds::load);

    private final Map<BlockPos, Long> Until = new LinkedHashMap<>();

    public static Scp005Holds get(ServerLevel World) {
        return World.getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    public boolean isHolding(BlockPos Anchor) {
        return Until.containsKey(Anchor);
    }

    public void hold(ServerLevel World, BlockPos Anchor, HoldTime Time) {
        Until.put(Anchor.immutable(), Time.isForever() ? Forever : World.getGameTime() + Time.getTicks());
        setDirty();
        sync(World);
    }

    public void release(ServerLevel World, BlockPos Anchor, KeyHoldable Door) {
        if (Until.remove(Anchor) != null) {
            setDirty();
            sync(World);
        }
        Door.releaseHold(World, Anchor);
    }

    public void tick(ServerLevel World) {
        if (Until.isEmpty()) {
            return;
        }
        long Now = World.getGameTime();
        List<BlockPos> Expired = new ArrayList<>();
        boolean Changed = false;
        Iterator<Map.Entry<BlockPos, Long>> Cursor = Until.entrySet().iterator();
        while (Cursor.hasNext()) {
            Map.Entry<BlockPos, Long> Held = Cursor.next();
            BlockPos Anchor = Held.getKey();
            if (!World.isLoaded(Anchor)) {
                continue;
            }
            KeyHoldable Door = Scp005Doors.find(World.getBlockState(Anchor));
            if (Door == null || !Door.isHeldOpen(World, Anchor)) {
                Cursor.remove();
                Changed = true;
            } else if (Now >= Held.getValue()) {
                Cursor.remove();
                Expired.add(Anchor);
                Changed = true;
            }
        }
        for (BlockPos Anchor : Expired) {
            KeyHoldable Door = Scp005Doors.find(World.getBlockState(Anchor));
            if (Door != null) {
                Door.releaseHold(World, Anchor);
            }
        }
        if (Changed) {
            setDirty();
            sync(World);
        }
    }

    public HoldsPayload payload() {
        List<HoldsPayload.HeldDoor> Doors = new ArrayList<>(Until.size());
        for (Map.Entry<BlockPos, Long> Held : Until.entrySet()) {
            Doors.add(new HoldsPayload.HeldDoor(Held.getKey(), Held.getValue()));
        }
        return new HoldsPayload(Doors);
    }

    public void sync(ServerLevel World) {
        PacketDistributor.sendToPlayersInDimension(World, payload());
    }

    public void syncTo(ServerPlayer Viewer) {
        PacketDistributor.sendToPlayer(Viewer, payload());
    }

    @Override
    public CompoundTag save(CompoundTag Root, HolderLookup.Provider Registries) {
        ListTag Entries = new ListTag();
        for (Map.Entry<BlockPos, Long> Held : Until.entrySet()) {
            CompoundTag Saved = new CompoundTag();
            Saved.putLong("Pos", Held.getKey().asLong());
            Saved.putLong("Until", Held.getValue());
            Entries.add(Saved);
        }
        Root.put("Holds", Entries);
        return Root;
    }

    public static Scp005Holds load(CompoundTag Root, HolderLookup.Provider Registries) {
        Scp005Holds Data = new Scp005Holds();
        ListTag Entries = Root.getList("Holds", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Entries.size(); Index++) {
            CompoundTag Saved = Entries.getCompound(Index);
            Data.Until.put(BlockPos.of(Saved.getLong("Pos")), Saved.getLong("Until"));
        }
        return Data;
    }
}
