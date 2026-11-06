package net.rai.scp_unleashed.raid.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containment.ContainmentBlock;
import net.rai.scp_unleashed.containment.OneWayGlassBlock;
import net.rai.scp_unleashed.containment.ReinforcedGlassBlock;
import net.rai.scp_unleashed.containment.ReinforcedPaneBlock;
import net.rai.scp_unleashed.storage.StorageContainerBlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BaseSignals {
    private record Sample(ResourceKey<Level> Dimension, double X, double Z) {
    }

    private static final Map<UUID, Sample> Last = new HashMap<>();

    private BaseSignals() {
    }

    public static boolean counts(ServerPlayer User) {
        return !User.isSpectator() && User.isAlive();
    }

    public static void sample(MinecraftServer Server) {
        long Now = Server.overworld().getGameTime();
        RaidBases Data = RaidBases.get(Server);
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            if (!counts(User)) {
                continue;
            }
            String Group = RaidBases.groupOf(User);
            ResourceKey<Level> Dimension = User.level().dimension();
            Sample Before = Last.get(User.getUUID());
            double Moved = Before != null && Before.Dimension().equals(Dimension) ? Math.hypot(User.getX() - Before.X(), User.getZ() - Before.Z()) : Double.MAX_VALUE;
            Data.signal(Group, Dimension, User.blockPosition(), BaseWeights.presence(Moved), Now, 0);
            Last.put(User.getUUID(), new Sample(Dimension, User.getX(), User.getZ()));
            BlockPos Respawn = User.getRespawnPosition();
            if (Respawn != null) {
                Data.signal(Group, User.getRespawnDimension(), Respawn, BaseWeights.RespawnPresence, Now, BaseHeatmap.Respawn);
            }
        }
    }

    public static void placed(ServerPlayer User, ServerLevel World, BlockPos Pos, BlockState Placed) {
        if (!counts(User)) {
            return;
        }
        Block Kind = Placed.getBlock();
        boolean Mod = Scp_unleashed.ModId.equals(BuiltInRegistries.BLOCK.getKey(Kind).getNamespace());
        BlockEntity Tile = Placed.hasBlockEntity() ? World.getBlockEntity(Pos) : null;
        boolean Bed = Placed.is(BlockTags.BEDS);
        boolean Locker = Tile instanceof StorageContainerBlockEntity;
        boolean Storage = Locker || Tile instanceof Container || Tile != null && World.getCapability(Capabilities.ItemHandler.BLOCK, Pos, null) != null;
        boolean Containment = Locker || Kind instanceof ContainmentBlock || Kind instanceof ReinforcedGlassBlock || Kind instanceof ReinforcedPaneBlock
                || Kind instanceof OneWayGlassBlock;
        int Marks = (Storage ? BaseHeatmap.Storage : 0) | (Mod && Tile != null ? BaseHeatmap.Machine : 0) | (Bed ? BaseHeatmap.Sleep : 0);
        long Now = World.getServer().overworld().getGameTime();
        RaidBases Data = RaidBases.get(World.getServer());
        String Group = RaidBases.groupOf(User);
        Data.signal(Group, World.dimension(), Pos, BaseWeights.placed(Mod, Tile != null, Bed, Storage, Containment), Now, Marks);
        if (Storage) {
            Data.noteStash(Group, World.dimension(), Pos, Now);
        }
    }

    public static void slept(ServerPlayer User, BlockPos Bed) {
        if (!counts(User)) {
            return;
        }
        long Now = User.server.overworld().getGameTime();
        RaidBases.get(User.server).signal(RaidBases.groupOf(User), User.level().dimension(), Bed, BaseWeights.Sleep, Now, BaseHeatmap.Sleep);
    }

    public static void spawnSet(ServerPlayer User, ResourceKey<Level> Dimension, BlockPos Spawn) {
        if (!counts(User)) {
            return;
        }
        long Now = User.server.overworld().getGameTime();
        RaidBases.get(User.server).signal(RaidBases.groupOf(User), Dimension, Spawn, BaseWeights.SpawnSet, Now, BaseHeatmap.Respawn);
    }

    public static void left(UUID User) {
        Last.remove(User);
    }

    public static void forget() {
        Last.clear();
    }
}
