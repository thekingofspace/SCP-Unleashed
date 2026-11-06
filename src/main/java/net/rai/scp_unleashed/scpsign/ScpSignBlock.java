package net.rai.scp_unleashed.scpsign;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.archive.AnomalyDocumentItem;
import net.rai.scp_unleashed.archive.ArchiveRegistry;
import net.rai.scp_unleashed.archive.SealedDocumentItem;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.document.DocumentStamp;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class ScpSignBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int GlowLight = 5;
    public static final String Key = "block.scp_unleashed.scp_sign.";

    protected ScpSignBlock(Properties Settings) {
        super(Settings);
    }

    public abstract ScpSignKind getKind();

    public abstract BlockPos mainPos(BlockState State, BlockPos Pos);

    public abstract List<BlockPos> parts(BlockState State, BlockPos Pos);

    public static Direction rightOf(Direction Facing) {
        return Facing.getCounterClockWise();
    }

    public static boolean onWall(LevelReader World, BlockPos Pos, Direction Facing) {
        BlockPos Support = Pos.relative(Facing.getOpposite());
        return World.getBlockState(Support).isFaceSturdy(World, Support, Facing);
    }

    protected static List<Direction> facingChoices(BlockPlaceContext Context) {
        List<Direction> Found = new ArrayList<>();
        Direction Clicked = Context.getClickedFace();
        if (Clicked.getAxis().isHorizontal()) {
            Found.add(Clicked);
        }
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (Side.getAxis().isHorizontal() && !Found.contains(Side.getOpposite())) {
                Found.add(Side.getOpposite());
            }
        }
        return Found;
    }

    protected static void refuse(BlockPlaceContext Context, String Reason) {
        Player User = Context.getPlayer();
        if (User != null && !Context.getLevel().isClientSide) {
            User.displayClientMessage(Component.translatable(Key + Reason).withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Lit);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return onWall(World, Pos, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean Moved) {
        if (!World.isClientSide) {
            refreshPower(World, State, Pos);
        }
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, Moved);
    }

    protected boolean poweredAt(Level World, BlockState State, BlockPos Pos) {
        for (BlockPos Part : parts(State, Pos)) {
            if (World.hasNeighborSignal(Part)) {
                return true;
            }
        }
        return false;
    }

    public void refreshPower(Level World, BlockState State, BlockPos Pos) {
        boolean Powered = poweredAt(World, State, Pos);
        for (BlockPos Part : parts(State, Pos)) {
            BlockState Current = World.getBlockState(Part);
            if (Current.is(this) && Current.getValue(Lit) != Powered) {
                World.setBlock(Part, Current.setValue(Lit, Powered), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Nullable
    public ScpSignBlockEntity signAt(LevelReader World, BlockState State, BlockPos Pos) {
        return World.getBlockEntity(mainPos(State, Pos)) instanceof ScpSignBlockEntity Sign ? Sign : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (Stack.getItem() instanceof SealedDocumentItem) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable(Key + "sealed").withStyle(ChatFormatting.GRAY), true);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!(Stack.getItem() instanceof AnomalyDocumentItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        DocumentStamp Stamp = Stack.get(ArchiveRegistry.StampComponent.get());
        ScpSignBlockEntity Sign = signAt(World, State, Pos);
        if (Stamp == null || Sign == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        AnomalyDocument Document = AnomalyDocuments.server().get(Stamp.Document());
        if (Document == null) {
            User.displayClientMessage(Component.translatable(Key + "missing").withStyle(ChatFormatting.RED), true);
            return ItemInteractionResult.CONSUME;
        }
        if (Stamp.Document().equals(Sign.getDocument())) {
            User.displayClientMessage(Component.translatable(Key + "already", Document.designation()).withStyle(ChatFormatting.GRAY), true);
            return ItemInteractionResult.CONSUME;
        }
        Sign.assign(Stamp.Document());
        World.playSound(null, Pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8f, 0.85f);
        World.playSound(null, Pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.7f, 1.1f);
        User.displayClientMessage(Component.translatable(Key + "set", Document.designation()).withStyle(Look -> Look.withColor(Document.Classification().getColour() & 0xFFFFFF)), true);
        World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        ScpSignBlockEntity Sign = signAt(World, State, Pos);
        if (Sign == null || Sign.getDocument() == null) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            Sign.assign(null);
            World.playSound(null, Pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.8f, 0.85f);
            User.displayClientMessage(Component.translatable(Key + "cleared").withStyle(ChatFormatting.GRAY), true);
            World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    protected void applyStackDocument(Level World, BlockState State, BlockPos Pos, ItemStack Stack) {
        ResourceLocation Id = Stack.get(ScpSignRegistry.DocumentComponent.get());
        ScpSignBlockEntity Sign = signAt(World, State, Pos);
        if (Id != null && Sign != null && Sign.getDocument() == null) {
            Sign.assign(Id);
        }
    }
}
