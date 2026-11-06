package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.scp4845.order.Scp4845Orders;

public class Scp4845CounterBlock extends Scp4845FurnitureBlock {
    public static final MapCodec<Scp4845CounterBlock> CounterCodec = simpleCodec(Settings -> new Scp4845CounterBlock(Settings, Scp4845Shapes.Counter));

    public Scp4845CounterBlock(Properties Settings, double[][] North) {
        super(Settings, North);
    }

    @Override
    protected MapCodec<? extends Scp4845CounterBlock> codec() {
        return CounterCodec;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Customer && Scp4845Orders.openAt(Customer, Pos)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
