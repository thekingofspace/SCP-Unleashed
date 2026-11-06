package net.rai.scp_unleashed.scp163;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp163ApparatusBlock extends BaseEntityBlock {
    public static final MapCodec<Scp163ApparatusBlock> BlockCodec = simpleCodec(Scp163ApparatusBlock::new);
    public static final BooleanProperty Supplying = BooleanProperty.create("supplying");
    public static final int ActiveLight = 9;
    public static final int IdleLight = 4;
    public static final VoxelShape Shape = Shapes.or(Block.box(3.0, 0.0, 3.0, 13.0, 11.0, 13.0), Block.box(2.0, 3.0, 7.0, 14.0, 8.0, 9.0),
            Block.box(5.0, 11.0, 5.0, 11.0, 13.0, 11.0), Block.box(7.0, 13.0, 7.0, 9.0, 16.0, 9.0));
    public static final VoxelShape Fence = Block.box(2.0, 0.0, 3.0, 14.0, Scp163Footing.FenceHeight, 13.0);

    public Scp163ApparatusBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Supplying, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Supplying);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Scp163Footing.keepsOff(Context, Pos) ? Fence : Shape;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Computation) {
        return false;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState Previous, boolean Moved) {
        super.onPlace(State, World, Pos, Previous, Moved);
        if (World instanceof ServerLevel Server && !Previous.is(this)) {
            Scp163Breathing.get(Server).add(Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!Replaced.is(this) && World instanceof ServerLevel Server) {
            Scp163Breathing.get(Server).remove(Pos);
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp163ApparatusBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp163Registry.ApparatusEntity.get(), Scp163ApparatusBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (!State.getValue(Supplying) || Random.nextInt(3) != 0) {
            return;
        }
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double Reach = 0.6 + Random.nextDouble() * 0.5;
        World.addParticle(Scp163Registry.Breath.get(), Pos.getX() + 0.5 + Math.cos(Angle) * Reach, Pos.getY() + 0.3 + Random.nextDouble() * 0.5,
                Pos.getZ() + 0.5 + Math.sin(Angle) * Reach, -Math.cos(Angle) * 0.03, 0.012, -Math.sin(Angle) * 0.03);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp163_apparatus.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp163_apparatus.details").withStyle(ChatFormatting.GOLD));
        }
    }
}
