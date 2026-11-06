package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class Scp1730CrackBlock extends BaseEntityBlock {
    public static final MapCodec<Scp1730CrackBlock> CrackCodec = simpleCodec(Scp1730CrackBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;

    public Scp1730CrackBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CrackCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1730CrackBlockEntity(Pos, State);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    public static net.minecraft.world.phys.Vec3 mouth(BlockPos Pos, Direction Side) {
        return net.minecraft.world.phys.Vec3.atCenterOf(Pos).add(Side.getStepX() * 0.62, 0.0, Side.getStepZ() * 0.62);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (Random.nextInt(4) != 0) {
            return;
        }
        Direction Side = State.getValue(Facing);
        double X = Pos.getX() + 0.5 + Side.getStepX() * 0.55 + (Random.nextDouble() - 0.5) * 0.7 * Math.abs(Side.getStepZ());
        double Y = Pos.getY() + 0.2 + Random.nextDouble() * 0.6;
        double Z = Pos.getZ() + 0.5 + Side.getStepZ() * 0.55 + (Random.nextDouble() - 0.5) * 0.7 * Math.abs(Side.getStepX());
        World.addParticle(ParticleTypes.SQUID_INK, X, Y, Z, Side.getStepX() * 0.01, -0.01, Side.getStepZ() * 0.01);
    }
}
