package net.rai.scp_unleashed.scp3008.furniture.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class Scp3008FurnitureEStorageBlock extends Scp3008FurnitureEBlock implements EntityBlock {
    public Scp3008FurnitureEStorageBlock(Properties Settings, Scp3008FurnitureEKind Type) {
        super(Settings, Type);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return partOf(State) == 0 ? new Scp3008FurnitureEStorageEntity(Pos, State) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!Type.holds(partOf(State))) {
            return super.useWithoutItem(State, World, Pos, User, Hit);
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (World.getBlockEntity(mainPos(Pos, State)) instanceof Scp3008FurnitureEStorageEntity Storage) {
            User.openMenu(Storage);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (World.getBlockEntity(Pos) instanceof Scp3008FurnitureEStorageEntity Storage) {
            Storage.recheckOpen();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        Containers.dropContentsOnDestroy(State, NewState, World, Pos);
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(World.getBlockEntity(mainPos(Pos, State)));
    }
}
