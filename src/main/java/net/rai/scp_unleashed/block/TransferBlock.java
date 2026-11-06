package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.block.entity.OutletBlockEntity;
import net.rai.scp_unleashed.block.entity.TransferBlockEntity;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.ConnectorTier;
import net.rai.scp_unleashed.wire.WireConnector;
import net.rai.scp_unleashed.wire.Wires;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TransferBlock extends BaseEntityBlock implements WireConnector, Screwdrivable {
    public static final MapCodec<TransferBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ConnectorTier.TierCodec.fieldOf("tier").forGetter(TransferBlock::getConnectorTier),
            propertiesCodec()
    ).apply(Instance, TransferBlock::new));

    private final ConnectorTier Tier;

    public TransferBlock(ConnectorTier Tier, Properties Properties) {
        super(Properties);
        this.Tier = Tier;
    }

    @Override
    public ConnectorTier getConnectorTier() {
        return Tier;
    }

    @Override
    public boolean isFullBlock() {
        return true;
    }

    @Override
    public Direction linkFace(BlockState State, Direction Clicked) {
        return Clicked;
    }

    @Override
    public BlockPos hostPos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    public static boolean canDisguiseAs(BlockGetter World, BlockPos Pos, BlockState Look) {
        return !createsBlockEntity(Look) && Look.getRenderShape() == RenderShape.MODEL && Look.isSolidRender(World, Pos) && Look.isCollisionShapeFullBlock(World, Pos);
    }

    private static boolean createsBlockEntity(BlockState Look) {
        return Look.getBlock() instanceof DecoratableBlock ? Look.getValue(DecoratableBlock.Decorated) : Look.hasBlockEntity();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(Stack.getItem() instanceof BlockItem Placeable) || !(World.getBlockEntity(Pos) instanceof TransferBlockEntity Transfer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockState Look = Placeable.getBlock().defaultBlockState();
        if (!canDisguiseAs(World, Pos, Look)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (Look.equals(Transfer.getDisguise())) {
            return ItemInteractionResult.CONSUME;
        }
        if (!World.isClientSide) {
            Transfer.returnDisguise(User);
            boolean Paid = !User.getAbilities().instabuild;
            if (Paid) {
                Stack.shrink(1);
            }
            Transfer.setDisguise(Look, Paid);
            SoundType Sound = Look.getSoundType(World, Pos, User);
            World.playSound(null, Pos, Sound.getPlaceSound(), SoundSource.BLOCKS, (Sound.getVolume() + 1.0f) / 2.0f, Sound.getPitch() * 0.8f);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.isShiftKeyDown() || !(World.getBlockEntity(Pos) instanceof TransferBlockEntity Transfer) || !Transfer.hasDisguise()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            removeDisguise(World, Pos, User, Transfer);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public boolean accepts(BlockState State, boolean Sneaking) {
        return Sneaking;
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        if (!Sneaking || !(World.getBlockEntity(Pos) instanceof TransferBlockEntity Transfer) || !Transfer.hasDisguise()) {
            return InteractionResult.PASS;
        }
        removeDisguise(World, Pos, User, Transfer);
        return InteractionResult.SUCCESS;
    }

    private static void removeDisguise(Level World, BlockPos Pos, @Nullable Player User, TransferBlockEntity Transfer) {
        SoundType Sound = Transfer.getDisguise().getSoundType(World, Pos, User);
        if (User != null) {
            Transfer.returnDisguise(User);
        } else if (Transfer.isConsumed()) {
            Block.popResource(World, Pos, new ItemStack(Transfer.getDisguise().getBlock()));
        }
        Transfer.clearDisguise();
        World.playSound(null, Pos, Sound.getBreakSound(), SoundSource.BLOCKS, (Sound.getVolume() + 1.0f) / 2.0f, Sound.getPitch() * 0.8f);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            if (ServerWorld.getBlockEntity(Pos) instanceof TransferBlockEntity Transfer && Transfer.isConsumed() && Transfer.hasDisguise()) {
                Block.popResource(ServerWorld, Pos, new ItemStack(Transfer.getDisguise().getBlock()));
            }
            if (ServerWorld.getBlockEntity(Pos) instanceof OutletBlockEntity Connector) {
                Wires.detachAll(ServerWorld, Pos, Connector);
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.transfer.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.outlet.max_demand", Tier.getMaxDemand()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.outlet.max_connections", Wires.MaxLinksPerOutlet).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.connector.wires", Tier.getStrongest().getCoil().getDescription()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.transfer.disguise").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.transfer.clear").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new TransferBlockEntity(Pos, State);
    }
}
