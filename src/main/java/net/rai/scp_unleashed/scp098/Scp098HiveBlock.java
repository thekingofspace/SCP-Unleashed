package net.rai.scp_unleashed.scp098;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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
import org.jetbrains.annotations.Nullable;

public class Scp098HiveBlock extends BaseEntityBlock {
    public static final MapCodec<Scp098HiveBlock> BlockCodec = simpleCodec(Scp098HiveBlock::new);
    public static final IntegerProperty Stage = IntegerProperty.create("stage", 0, 3);
    public static final int MaxStage = 3;
    public static final float Hardness = 1.6f;
    public static final float Resistance = 1.6f;

    public Scp098HiveBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Stage, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
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
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState Previous, boolean Moved) {
        super.onPlace(State, World, Pos, Previous, Moved);
        if (World instanceof ServerLevel Server && !Previous.is(this)) {
            Scp098Hives.get(Server).add(Pos);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof Scp098HiveBlockEntity Hive && !User.isCreative()) {
            Hive.blame(User);
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!Replaced.is(this) && World instanceof ServerLevel Server) {
            if (World.getBlockEntity(Pos) instanceof Scp098HiveBlockEntity Hive) {
                Hive.collapse(Server);
            }
            Scp098Hives.get(Server).remove(Pos);
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp098HiveBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, Scp098Registry.HiveEntity.get(), Scp098HiveBlockEntity::clientTick)
                : createTickerHelper(Type, Scp098Registry.HiveEntity.get(), Scp098HiveBlockEntity::serverTick);
    }
}
