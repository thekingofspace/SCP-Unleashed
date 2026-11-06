package net.rai.scp_unleashed.scp3008.econ;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carried;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008Carry;
import net.rai.scp_unleashed.scp3008.gear.carry.Scp3008CarryParts;

import java.util.ArrayList;
import java.util.List;

public final class Scp3008Disassembly {
    private static final int RemoveFlags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    private Scp3008Disassembly() {
    }

    public static boolean refused(Level World, Player User, BlockPos At, BlockState State) {
        return !Scp3008Carry.liftable(State) || Scp3008Carry.fixed(World, At, State) || !World.mayInteract(User, At);
    }

    public static boolean unpack(Player User, BlockPos Pos) {
        Level World = User.level();
        if (World.isClientSide) {
            return false;
        }
        BlockState State = World.getBlockState(Pos);
        if (refused(World, User, Pos, State)) {
            return false;
        }
        List<BlockPos> Positions = Scp3008Carry.partsOf(World, Pos, State);
        if (Positions.isEmpty() || Positions.size() > Scp3008CarryParts.MaxParts) {
            return false;
        }
        for (BlockPos At : Positions) {
            BlockState PartState = World.getBlockState(At);
            if (!PartState.is(State.getBlock()) || refused(World, User, At, PartState)) {
                return false;
            }
        }
        BlockPos Anchor = Positions.get(0);
        List<Scp3008Carried.Part> Parts = new ArrayList<>();
        for (BlockPos At : Positions) {
            BlockEntity Entity = World.getBlockEntity(At);
            CompoundTag Data = Entity == null ? new CompoundTag() : Entity.saveWithId(World.registryAccess());
            Parts.add(new Scp3008Carried.Part(At.subtract(Anchor), World.getBlockState(At), Data));
        }
        for (BlockPos At : Positions) {
            BlockEntity Entity = World.getBlockEntity(At);
            if (Entity != null) {
                Clearable.tryClear(Entity);
                World.removeBlockEntity(At);
            }
        }
        for (BlockPos At : Positions) {
            World.setBlock(At, Blocks.AIR.defaultBlockState(), RemoveFlags);
        }
        for (BlockPos At : Positions) {
            World.blockUpdated(At, Blocks.AIR);
            Blocks.AIR.defaultBlockState().updateNeighbourShapes(World, At, Block.UPDATE_ALL);
        }
        Vec3 Centre = Vec3.atCenterOf(Anchor);
        Scp3008Carry.drop(World, Centre, new Scp3008Carried(List.copyOf(Parts), Scp3008CarryParts.facingOf(Parts.get(0).State()), 0));
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp3008EconRegistry.Unscrew.get(), SoundSource.PLAYERS, 0.8f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        World.playSound(null, Centre.x, Centre.y, Centre.z, Scp3008EconRegistry.FlatPack.get(), SoundSource.BLOCKS, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        return true;
    }
}
