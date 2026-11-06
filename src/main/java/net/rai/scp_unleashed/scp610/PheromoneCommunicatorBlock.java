package net.rai.scp_unleashed.scp610;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PheromoneCommunicatorBlock extends BaseEntityBlock {
    public static final MapCodec<PheromoneCommunicatorBlock> Codec0 = simpleCodec(PheromoneCommunicatorBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final VoxelShape Shape = Block.box(1.0, 0.0, 1.0, 15.0, 15.0, 15.0);

    public PheromoneCommunicatorBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return Codec0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new PheromoneCommunicatorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp610Registry.CommunicatorEntity.get(), PheromoneCommunicatorBlockEntity::serverTick);
    }

    public static boolean listening(ServerLevel World, BlockPos Pos) {
        if (!World.getBlockState(Pos).is(Scp610Registry.Communicator.get())) {
            return false;
        }
        return Scp610Lord.inChamber(World, Pos) && Scp610Lord.lord(World.getServer(), Scp610Hives.state(World.getServer())) != null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World instanceof ServerLevel Server) || !(User instanceof ServerPlayer Speaker)) {
            return InteractionResult.SUCCESS;
        }
        if (!listening(Server, Pos)) {
            hiss(Server, Pos, Speaker);
            return InteractionResult.CONSUME;
        }
        Scp610Dialogue.open(Speaker, Pos);
        return InteractionResult.CONSUME;
    }

    public static void hiss(ServerLevel World, BlockPos Pos, ServerPlayer User) {
        World.playSound(null, Pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6f, 0.7f + World.random.nextFloat() * 0.2f);
        World.sendParticles(ParticleTypes.SMOKE, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 8, 0.15, 0.05, 0.15, 0.01);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.pheromone_communicator.hiss").withStyle(ChatFormatting.GRAY), true);
    }
}
