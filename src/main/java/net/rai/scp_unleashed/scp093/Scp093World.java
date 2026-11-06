package net.rai.scp_unleashed.scp093;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp093World extends SavedData {
    public static final String DataName = "scp_unleashed_scp093";
    private static final SavedData.Factory<Scp093World> DataFactory = new SavedData.Factory<>(Scp093World::new, Scp093World::load);

    public record Origin(ResourceKey<Level> Dimension, @Nullable BlockPos Gateway, Vec3 Spot, float Yaw) {
    }

    private final Map<UUID, Origin> Origins = new HashMap<>();
    private final List<BlockPos> Gates = new ArrayList<>();

    public static Scp093World get(MinecraftServer Server) {
        return Server.overworld().getDataStorage().computeIfAbsent(DataFactory, DataName);
    }

    @Nullable
    public Origin originOf(UUID Traveller) {
        return Origins.get(Traveller);
    }

    public void setOrigin(UUID Traveller, Origin Where) {
        Origins.put(Traveller, Where);
        setDirty();
    }

    public void clearOrigin(UUID Traveller) {
        if (Origins.remove(Traveller) != null) {
            setDirty();
        }
    }

    public List<BlockPos> gates() {
        return List.copyOf(Gates);
    }

    public void addGate(BlockPos Lower) {
        Gates.add(Lower.immutable());
        setDirty();
    }

    public void removeGate(BlockPos Lower) {
        if (Gates.remove(Lower)) {
            setDirty();
        }
    }

    @Nullable
    public BlockPos nearestGate(int X, int Z, int Reach) {
        BlockPos Best = null;
        long BestGap = (long) Reach * Reach;
        for (BlockPos Spot : Gates) {
            long Dx = Spot.getX() - X;
            long Dz = Spot.getZ() - Z;
            long Gap = Dx * Dx + Dz * Dz;
            if (Gap <= BestGap) {
                BestGap = Gap;
                Best = Spot;
            }
        }
        return Best;
    }

    @Override
    public CompoundTag save(CompoundTag Saved, HolderLookup.Provider Lookup) {
        ListTag Rows = new ListTag();
        for (Map.Entry<UUID, Origin> Entry : Origins.entrySet()) {
            Origin Where = Entry.getValue();
            CompoundTag Row = new CompoundTag();
            Row.putUUID("Id", Entry.getKey());
            Row.putString("Dimension", Where.Dimension().location().toString());
            if (Where.Gateway() != null) {
                Row.put("Mirror", NbtUtils.writeBlockPos(Where.Gateway()));
            }
            Row.putDouble("X", Where.Spot().x);
            Row.putDouble("Y", Where.Spot().y);
            Row.putDouble("Z", Where.Spot().z);
            Row.putFloat("Yaw", Where.Yaw());
            Rows.add(Row);
        }
        Saved.put("Origins", Rows);
        ListTag GateRows = new ListTag();
        for (BlockPos Spot : Gates) {
            GateRows.add(NbtUtils.writeBlockPos(Spot));
        }
        Saved.put("Gates", GateRows);
        return Saved;
    }

    public static Scp093World load(CompoundTag Saved, HolderLookup.Provider Lookup) {
        Scp093World Data = new Scp093World();
        ListTag Rows = Saved.getList("Origins", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Rows.size(); Index++) {
            CompoundTag Row = Rows.getCompound(Index);
            ResourceLocation Dimension = ResourceLocation.tryParse(Row.getString("Dimension"));
            if (!Row.hasUUID("Id") || Dimension == null) {
                continue;
            }
            BlockPos Gateway = NbtUtils.readBlockPos(Row, "Mirror").orElse(null);
            Data.Origins.put(Row.getUUID("Id"), new Origin(ResourceKey.create(Registries.DIMENSION, Dimension), Gateway,
                    new Vec3(Row.getDouble("X"), Row.getDouble("Y"), Row.getDouble("Z")), Row.getFloat("Yaw")));
        }
        ListTag GateRows = Saved.getList("Gates", Tag.TAG_INT_ARRAY);
        for (int Index = 0; Index < GateRows.size(); Index++) {
            int[] Values = GateRows.getIntArray(Index);
            if (Values.length == 3) {
                Data.Gates.add(new BlockPos(Values[0], Values[1], Values[2]));
            }
        }
        return Data;
    }
}
