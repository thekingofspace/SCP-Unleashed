package net.rai.scp_unleashed.scp3199;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Scp3199EggBlockEntity extends BlockEntity {
    private int Hatch = Scp3199Egg.HatchTicks;

    public Scp3199EggBlockEntity(BlockPos Pos, BlockState State) {
        super(Scp3199Registry.NestEntity.get(), Pos, State);
    }

    public int hatchLeft() {
        return Hatch;
    }

    public void setHatch(int Ticks) {
        Hatch = Math.max(0, Ticks);
        setChanged();
    }

    public static void serverTick(Level World, BlockPos Pos, BlockState State, Scp3199EggBlockEntity Old) {
        if (World instanceof ServerLevel Server) {
            convert(Server, Pos, State, Old.Hatch);
        }
    }

    public static Scp3199Egg convert(ServerLevel World, BlockPos Pos, BlockState State, int Hatch) {
        World.setBlock(Pos, Scp3199EggBlock.leftover(State), 3);
        Scp3199Egg Egg = Scp3199Egg.laid(World, Vec3.atBottomCenterOf(Pos), Vec3.ZERO, World.getRandom().nextFloat() * 360.0f, -1);
        if (Egg != null) {
            Egg.setHatch(Hatch);
            Egg.settle(World.getGameTime() - Scp3199Egg.SettleTicks - 1);
        }
        return Egg;
    }

    @Override
    protected void saveAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.saveAdditional(Tag, Registries);
        Tag.putInt("Hatch", Hatch);
    }

    @Override
    protected void loadAdditional(CompoundTag Tag, HolderLookup.Provider Registries) {
        super.loadAdditional(Tag, Registries);
        Hatch = Tag.contains("Hatch") ? Math.max(0, Tag.getInt("Hatch")) : Scp3199Egg.HatchTicks;
    }
}
