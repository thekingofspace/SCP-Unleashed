package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Chores;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.Map;

public class Scp4666WindowBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp4666WindowBlock> BlockCodec = simpleCodec(Scp4666WindowBlock::new);
    public static final BooleanProperty Boarded = BooleanProperty.create("boarded");
    private static final Map<Direction, VoxelShape> Cracked = Scp4666Shapes.table(new double[][]{{0, 0, 7, 16, 16, 9}});
    private static final Map<Direction, VoxelShape> Planked = Scp4666Shapes.table(new double[][]{{0, 0, 7, 16, 16, 9}, {0, 0, 5.5, 16, 16, 7}});

    public Scp4666WindowBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Boarded, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Boarded);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Boarded) ? Planked : Cracked).get(State.getValue(FACING));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp4666Registry.Boards.get()) || State.getValue(Boarded) || !Scp4666World.inside(World)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
            Scp4666Chores.board(Server, Visitor, Pos);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Boarded) || Random.nextInt(3) != 0) {
            return;
        }
        Direction Inward = State.getValue(FACING);
        double X = Pos.getX() + 0.5 + (Random.nextDouble() - 0.5) * 0.8 * Math.abs(Inward.getStepZ());
        double Z = Pos.getZ() + 0.5 + (Random.nextDouble() - 0.5) * 0.8 * Math.abs(Inward.getStepX());
        World.addParticle(ParticleTypes.SNOWFLAKE, X, Pos.getY() + Random.nextDouble(), Z, Inward.getStepX() * 0.05, -0.01, Inward.getStepZ() * 0.05);
    }
}
