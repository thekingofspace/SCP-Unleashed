package net.rai.scp_unleashed.scp001scarlet.king.loot;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletKingRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ScarletPodBlock extends BaseEntityBlock {
    public static final MapCodec<ScarletPodBlock> Codec = simpleCodec(ScarletPodBlock::new);
    public static final IntegerProperty Stage = IntegerProperty.create("stage", 0, 3);
    public static final VoxelShape[] Shapes = {Block.box(4.0, 0.0, 4.0, 12.0, 9.0, 12.0), Block.box(4.0, 0.0, 4.0, 12.0, 11.0, 12.0),
            Block.box(3.5, 0.0, 3.5, 12.5, 12.5, 12.5), Block.box(3.0, 0.0, 3.0, 13.0, 14.0, 13.0)};

    public ScarletPodBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Stage, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Stage);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes[State.getValue(Stage)];
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new ScarletPodBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ScarletKingRegistry.PodEntity.get(), ScarletPodBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        long Beat = (World.getGameTime() + Pos.asLong()) % 30L;
        if (Beat == 0L || Beat == 6L) {
            int Grown = State.getValue(Stage);
            for (int Index = 0; Index < 3 + Grown; Index++) {
                World.addParticle(new DustParticleOptions(new Vector3f(1.0f, 0.55f, 0.35f), 0.8f), Pos.getX() + 0.3 + Random.nextDouble() * 0.4,
                        Pos.getY() + 0.2 + Random.nextDouble() * (0.4 + Grown * 0.1), Pos.getZ() + 0.3 + Random.nextDouble() * 0.4, 0.0, 0.01, 0.0);
            }
        }
    }
}
