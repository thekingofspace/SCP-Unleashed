package net.rai.scp_unleashed.scp107;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Scp107ShellBlock extends BaseEntityBlock {
    public static final MapCodec<Scp107ShellBlock> BlockCodec = simpleCodec(Scp107ShellBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final double BowlTop = 8.0 / 16.0;
    public static final float MilkPitch = 0.82f;
    private static final int[][] Boxes = {
            {5, 2, 5, 11, 3, 11}, {4, 3, 3, 12, 4, 13}, {3, 4, 2, 13, 5, 14}, {2, 5, 1, 14, 7, 15}, {1, 7, 0, 15, 8, 16},
            {4, 0, 4, 6, 3, 6}, {10, 0, 4, 12, 3, 6}, {4, 0, 10, 6, 3, 12}, {10, 0, 10, 12, 3, 12}
    };
    private static final VoxelShape LengthwiseShape = shape(false);
    private static final VoxelShape CrosswiseShape = shape(true);

    public Scp107ShellBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    private static VoxelShape shape(boolean Crosswise) {
        VoxelShape Result = Shapes.empty();
        for (int[] Box : Boxes) {
            Result = Shapes.or(Result, Crosswise ? Block.box(Box[2], Box[1], Box[0], Box[5], Box[4], Box[3]) : Block.box(Box[0], Box[1], Box[2], Box[3], Box[4], Box[5]));
        }
        return Result.optimize();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Facing).getAxis() == Direction.Axis.X ? CrosswiseShape : LengthwiseShape;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    public static Optional<Scp107RainKind> pourable(ItemStack Stack) {
        if (Stack.is(Tags.Items.BUCKETS_MILK)) {
            return Optional.of(Scp107RainKind.Milk);
        }
        if (!(Stack.getItem() instanceof BucketItem Bucket) || Stack.getItem() instanceof MobBucketItem) {
            return Optional.empty();
        }
        return Scp107RainKind.of(Bucket.content);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        Optional<Scp107RainKind> Kind = pourable(Stack);
        if (Kind.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server) {
            Item Used = Stack.getItem();
            Scp107Storm.pour(Server.getServer(), Kind.get(), Server.random);
            if (World.getBlockEntity(Pos) instanceof Scp107ShellBlockEntity Shell) {
                Shell.poured(Kind.get());
            }
            pourEffects(Server, Pos, Kind.get());
            if (!User.hasInfiniteMaterials()) {
                User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, emptied(Stack), false));
            }
            User.awardStat(Stats.ITEM_USED.get(Used));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp107.drink").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            World.gameEvent(User, GameEvent.FLUID_PLACE, Pos);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    private static ItemStack emptied(ItemStack Stack) {
        ItemStack Remainder = Stack.getCraftingRemainingItem();
        return Remainder.isEmpty() ? new ItemStack(Items.BUCKET) : Remainder;
    }

    private static void pourEffects(ServerLevel World, BlockPos Pos, Scp107RainKind Kind) {
        double X = Pos.getX() + 0.5;
        double Y = Pos.getY() + BowlTop;
        double Z = Pos.getZ() + 0.5;
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        switch (Kind) {
            case Lava -> {
                World.playSound(null, X, Y, Z, Scp107Registry.PourLava.get(), SoundSource.BLOCKS, 1.0f, Pitch);
                World.playSound(null, X, Y, Z, Scp107Registry.PourLavaPop.get(), SoundSource.BLOCKS, 0.7f, Pitch);
                World.sendParticles(ParticleTypes.LAVA, X, Y, Z, 3, 0.18, 0.02, 0.22, 0.0);
                World.sendParticles(ParticleTypes.SMOKE, X, Y + 0.05, Z, 6, 0.2, 0.05, 0.25, 0.01);
            }
            case Caustic -> {
                World.playSound(null, X, Y, Z, Scp107Registry.Pour.get(), SoundSource.BLOCKS, 1.0f, Pitch);
                World.playSound(null, X, Y, Z, Scp107Registry.PourHiss.get(), SoundSource.BLOCKS, 0.6f, Pitch);
                World.sendParticles(Scp107Registry.CausticSplash.get(), X, Y, Z, 10, 0.2, 0.02, 0.25, 0.0);
            }
            case Milk -> {
                World.playSound(null, X, Y, Z, Scp107Registry.Pour.get(), SoundSource.BLOCKS, 1.0f, Pitch * MilkPitch);
                World.playSound(null, X, Y, Z, Scp107Registry.PourSlosh.get(), SoundSource.BLOCKS, 0.6f, Pitch * MilkPitch);
                World.sendParticles(Scp107Registry.MilkSplash.get(), X, Y, Z, 12, 0.2, 0.02, 0.25, 0.0);
            }
            case Oil -> {
                World.playSound(null, X, Y, Z, Scp107Registry.PourOil.get(), SoundSource.BLOCKS, 1.0f, Pitch);
                World.playSound(null, X, Y, Z, Scp107Registry.PourOilGlug.get(), SoundSource.BLOCKS, 0.7f, Pitch);
                World.sendParticles(Scp107Registry.OilSplash.get(), X, Y, Z, 12, 0.2, 0.02, 0.25, 0.0);
            }
            case Water -> {
                World.playSound(null, X, Y, Z, Scp107Registry.Pour.get(), SoundSource.BLOCKS, 1.0f, Pitch);
                World.playSound(null, X, Y, Z, Scp107Registry.PourSlosh.get(), SoundSource.BLOCKS, 0.7f, Pitch);
                World.sendParticles(ParticleTypes.SPLASH, X, Y, Z, 12, 0.2, 0.02, 0.25, 0.0);
            }
        }
        World.playSound(null, X, Y, Z, Scp107Registry.Drink.get(), SoundSource.BLOCKS, 0.8f, Pitch);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp107_shell.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp107_shell.pour").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp107_shell.answer").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("block.scp_unleashed.scp107_shell.move").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp107ShellBlockEntity(Pos, State);
    }
}
