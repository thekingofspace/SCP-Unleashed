package net.rai.scp_unleashed.scp5001.event.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp5001.event.Scp5001Event;
import net.rai.scp_unleashed.scp5001.event.network.Scp5001OpenPayload;

import java.util.Map;

public class Scp5001ConsoleBlock extends Scp5001DeviceBlock {
    public static final Map<Direction, VoxelShape> Shapes0 = Scp5001Shapes.rotations(Shapes.or(Block.box(-14.0, 0.0, 3.0, 30.0, 10.0, 15.0),
            Block.box(0.0, 10.0, 13.0, 16.0, 28.0, 15.0)));

    public Scp5001ConsoleBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected Map<Direction, VoxelShape> shapes() {
        return Shapes0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User instanceof ServerPlayer Operator) {
            Scp5001Event.openConsole(Operator, Pos, Scp5001OpenPayload.MainScreen);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
