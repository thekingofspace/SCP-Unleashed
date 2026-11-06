package net.rai.scp_unleashed.labware;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LabPetriDishBlock extends BaseEntityBlock {
    public static final MapCodec<LabPetriDishBlock> BlockCodec = simpleCodec(LabPetriDishBlock::new);
    public static final BooleanProperty Cultured = BooleanProperty.create("cultured");

    public LabPetriDishBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Cultured, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Cultured);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return LabwareShapes.PetriDish;
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        BlockPos Below = Pos.below();
        return World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return Side == Direction.DOWN && !canSurvive(State, World, Pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof LabPetriDishBlockEntity Dish)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (LabVials.isVial(Stack) && LabVials.hasSample(Stack)) {
            return pour(Stack, World, Pos, User, Hand, Dish);
        }
        if (LabVials.isSterileSwab(Stack)) {
            return copy(Stack, World, Pos, User, Hand, Dish);
        }
        if (LabVials.isUsedSwab(Stack)) {
            return streak(Stack, World, Pos, User, Dish);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static ItemInteractionResult pour(ItemStack Stack, Level World, BlockPos Pos, Player User, InteractionHand Hand, LabPetriDishBlockEntity Dish) {
        if (!LabVials.isOpen(Stack)) {
            return refuse(World, User, "message.scp_unleashed.labware.vial_unopened");
        }
        if (Dish.hasCulture()) {
            return refuse(World, User, "message.scp_unleashed.labware.dish_full");
        }
        if (!World.isClientSide) {
            ResourceLocation Sample = Dish.culture(LabVials.sampleOf(Stack));
            Item Used = Stack.getItem();
            User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, LabVials.openVial(null)));
            User.awardStat(Stats.ITEM_USED.get(Used));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.dish_poured", LabOrganisms.nameOf(Sample)).withStyle(ChatFormatting.GRAY), true);
            LabwareSounds.play(World, Pos, LabwareSounds.Streaked, LabwareSounds.pitch(World));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    private static ItemInteractionResult copy(ItemStack Stack, Level World, BlockPos Pos, Player User, InteractionHand Hand, LabPetriDishBlockEntity Dish) {
        ResourceLocation Sample = Dish.getSample();
        if (Sample == null) {
            return refuse(World, User, "message.scp_unleashed.labware.dish_sterile");
        }
        if (!World.isClientSide) {
            Item Used = Stack.getItem();
            User.setItemInHand(Hand, ItemUtils.createFilledResult(Stack, User, LabVials.usedSwab(LabSwabItem.fromDish(Sample, World.getDayTime()))));
            User.awardStat(Stats.ITEM_USED.get(Used));
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.dish_copied", LabOrganisms.nameOf(Sample)).withStyle(ChatFormatting.GRAY), true);
            LabwareSounds.play(World, Pos, LabwareSounds.Swabbed, LabwareSounds.pitch(World));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    private static ItemInteractionResult streak(ItemStack Stack, Level World, BlockPos Pos, Player User, LabPetriDishBlockEntity Dish) {
        LabSwabRecord Record = LabVials.recordOf(Stack);
        if (Record == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!Record.Labelled()) {
            return refuse(World, User, "message.scp_unleashed.labware.swab_unread");
        }
        if (Dish.hasCulture()) {
            return refuse(World, User, "message.scp_unleashed.labware.dish_full");
        }
        ResourceLocation Sample = Record.cultureSample();
        if (!World.isClientSide) {
            if (Sample == null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.swab_blank").withStyle(ChatFormatting.GRAY), true);
            } else {
                Dish.culture(Sample);
            }
            Stack.consume(1, User);
            LabwareSounds.play(World, Pos, LabwareSounds.Streaked, LabwareSounds.pitch(World));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    private static ItemInteractionResult refuse(Level World, Player User, String Key) {
        if (!World.isClientSide) {
            User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.GRAY), true);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.isSecondaryUseActive() || !(World.getBlockEntity(Pos) instanceof LabPetriDishBlockEntity Dish) || !Dish.hasCulture()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            Dish.setSample(null);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.labware.dish_wiped").withStyle(ChatFormatting.GRAY), true);
            LabwareSounds.play(World, Pos, LabwareSounds.Streaked, 1.3f);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Cultured) && World.getBlockEntity(Pos) instanceof LabPetriDishBlockEntity Dish) {
            LabOrganisms.find(Dish.getSample()).ifPresent(Organism -> Organism.animateCulture(World, Pos, Random));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new LabPetriDishBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide || !State.getValue(Cultured) ? null : createTickerHelper(Type, LabwareRegistry.PetriDishEntity.get(), LabPetriDishBlockEntity::serverTick);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.lab_petri_dish.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.lab_petri_dish.transfer").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.lab_petri_dish.copy").withStyle(ChatFormatting.AQUA));
            Lines.add(Component.translatable("block.scp_unleashed.lab_petri_dish.active", (int) LabExposure.Radius).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.lab_petri_dish.wipe").withStyle(ChatFormatting.GRAY));
        }
    }
}
