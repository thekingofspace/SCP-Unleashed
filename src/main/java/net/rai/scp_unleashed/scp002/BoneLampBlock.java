package net.rai.scp_unleashed.scp002;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BoneLampBlock extends FleshBlock {
    public static final MapCodec<BoneLampBlock> LampCodec = simpleCodec(BoneLampBlock::new);
    public static final BooleanProperty Lit = BooleanProperty.create("lit");
    public static final int LitLevel = 10;
    public static final int DimLevel = 4;
    private static final VoxelShape Outline = FleshFixtureBlock.shapesFor(Fixture.Lamp).get(Direction.NORTH);

    public BoneLampBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Lit, true));
    }

    @Override
    protected MapCodec<? extends BoneLampBlock> codec() {
        return LampCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Lit);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Mode) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable(Fixture.Lamp.flavourKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
