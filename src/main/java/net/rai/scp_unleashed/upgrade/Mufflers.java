package net.rai.scp_unleashed.upgrade;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Mufflers {
    public static final int CheckInterval = 100;

    private Mufflers() {
    }

    public static boolean isMuffled(ServerLevel World, BlockPos Pos) {
        MufflerData Data = MufflerData.peek(World);
        return Data != null && !Data.isEmpty() && Data.has(MufflerTargets.coreOf(World, Pos));
    }

    @Nullable
    public static MufflerData.Entry install(ServerLevel World, BlockPos Clicked) {
        BlockState State = World.getBlockState(Clicked);
        if (!MufflerTargets.muffleable(State)) {
            return null;
        }
        BlockPos Core = MufflerTargets.coreOf(World, Clicked);
        MufflerData Data = MufflerData.get(World);
        if (Data.has(Core)) {
            return null;
        }
        Block Kind = World.getBlockState(Core).getBlock();
        Set<BlockPos> Footprint = MufflerTargets.footprint(World, Core);
        MufflerData.Entry Added = Data.add(Core, BuiltInRegistries.BLOCK.getKey(Kind), Footprint);
        PacketDistributor.sendToPlayersInDimension(World, MufflerPayload.added(Added));
        return Added;
    }

    public static boolean remove(ServerLevel World, long Core, boolean Drop) {
        MufflerData Data = MufflerData.peek(World);
        if (Data == null) {
            return false;
        }
        MufflerData.Entry Gone = Data.remove(Core);
        if (Gone == null) {
            return false;
        }
        if (Drop) {
            BlockPos Pos = BlockPos.of(Core);
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, new ItemStack(UpgradeType.Muffler.getItem()));
        }
        PacketDistributor.sendToPlayersInDimension(World, MufflerPayload.removed(Core));
        return true;
    }

    public static boolean intact(ServerLevel World, MufflerData.Entry Machine) {
        BlockState State = World.getBlockState(BlockPos.of(Machine.Core()));
        return BuiltInRegistries.BLOCK.getKey(State.getBlock()).equals(Machine.BlockId());
    }

    public static void changed(ServerLevel World, BlockPos Pos) {
        MufflerData Data = MufflerData.peek(World);
        if (Data == null || Data.isEmpty()) {
            return;
        }
        MufflerData.Entry Machine = Data.entry(Pos.asLong());
        if (Machine != null && !intact(World, Machine)) {
            remove(World, Machine.Core(), true);
        }
    }

    public static void audit(ServerLevel World) {
        MufflerData Data = MufflerData.peek(World);
        if (Data == null || Data.isEmpty()) {
            return;
        }
        List<Long> Broken = new ArrayList<>();
        for (MufflerData.Entry Machine : Data.entries()) {
            BlockPos Core = BlockPos.of(Machine.Core());
            if (World.isLoaded(Core) && !intact(World, Machine)) {
                Broken.add(Machine.Core());
            }
        }
        for (long Core : Broken) {
            remove(World, Core, true);
        }
    }

    public static void sync(ServerPlayer User) {
        MufflerData Data = MufflerData.peek(User.serverLevel());
        List<MufflerData.Entry> Entries = Data == null ? List.of() : List.copyOf(Data.entries());
        PacketDistributor.sendToPlayer(User, MufflerPayload.replace(Entries));
    }
}
