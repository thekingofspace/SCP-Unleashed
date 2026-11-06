package net.rai.scp_unleashed.scp5001.mekhane.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import org.jetbrains.annotations.Nullable;

public class Scp5001SkyEmitterBlock extends BaseEntityBlock {
    public static final MapCodec<Scp5001SkyEmitterBlock> BlockCodec = simpleCodec(Scp5001SkyEmitterBlock::new);
    public static final BooleanProperty Lit = BooleanProperty.create("lit");

    public Scp5001SkyEmitterBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Lit);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp5001SkyEmitterBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp5001MekhaneRegistry.EmitterEntity.get(), Scp5001SkyEmitterBlockEntity::serverTick);
    }
}
