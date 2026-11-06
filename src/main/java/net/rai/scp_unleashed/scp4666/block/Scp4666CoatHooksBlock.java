package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import net.rai.scp_unleashed.scp4666.item.Scp4666WinterGearItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class Scp4666CoatHooksBlock extends BaseEntityBlock {
    public static final MapCodec<Scp4666CoatHooksBlock> BlockCodec = simpleCodec(Scp4666CoatHooksBlock::new);
    public static final DirectionProperty Facing = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty Hat = BooleanProperty.create("hat");
    public static final BooleanProperty Coat = BooleanProperty.create("coat");
    public static final BooleanProperty Boots = BooleanProperty.create("boots");
    public static final List<BooleanProperty> Pegs = List.of(Hat, Coat, Boots);
    public static final List<EquipmentSlot> Slots = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.FEET);
    private static final Map<Direction, VoxelShape> Shapes = Scp4666Shapes.table(new double[][]{{0.5, 0, 12, 15.5, 14, 16}});

    public Scp4666CoatHooksBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Hat, true).setValue(Coat, true).setValue(Boots, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Hat, Coat, Boots);
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
        return Shapes.get(State.getValue(Facing));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp4666CoatHooksBlockEntity(Pos, State);
    }

    public static int slotOf(ItemStack Stack) {
        if (Stack.is(Scp4666Registry.WinterHat.get())) {
            return 0;
        }
        if (Stack.is(Scp4666Registry.WinterCoat.get())) {
            return 1;
        }
        return Stack.is(Scp4666Registry.WinterBoots.get()) ? 2 : -1;
    }

    public static boolean usable(Level World, Player User) {
        return World instanceof ServerLevel && Scp4666World.inside(World) && !User.isSpectator() && Scp4666State.get(World.getServer()).started();
    }

    public static void ready(ServerLevel World, Scp4666CoatHooksBlockEntity Hooks) {
        int Epoch = Scp4666State.get(World.getServer()).epoch();
        if (Hooks.stocked() != Epoch) {
            Hooks.stock(Epoch);
        }
    }

    public static void show(Level World, BlockPos Pos, Scp4666CoatHooksBlockEntity Hooks) {
        BlockState State = World.getBlockState(Pos);
        BlockState Next = State;
        for (int Slot = 0; Slot < Pegs.size(); Slot++) {
            Next = Next.setValue(Pegs.get(Slot), !Hooks.get(Slot).isEmpty());
        }
        if (Next != State) {
            World.setBlock(Pos, Next, Block.UPDATE_CLIENTS);
        }
    }

    public static int take(ServerLevel World, BlockPos Pos, Player User) {
        if (!(World.getBlockEntity(Pos) instanceof Scp4666CoatHooksBlockEntity Hooks)) {
            return 0;
        }
        ready(World, Hooks);
        int Taken = 0;
        for (int Slot = 0; Slot < Slots.size(); Slot++) {
            ItemStack Stack = Hooks.get(Slot);
            if (Stack.isEmpty()) {
                continue;
            }
            if (User.getItemBySlot(Slots.get(Slot)).isEmpty()) {
                User.setItemSlot(Slots.get(Slot), Stack);
            } else if (!User.getInventory().add(Stack)) {
                continue;
            }
            Hooks.set(Slot, ItemStack.EMPTY);
            Taken++;
        }
        show(World, Pos, Hooks);
        return Taken;
    }

    public static int hang(ServerLevel World, BlockPos Pos, Player User, ItemStack Held) {
        if (!(World.getBlockEntity(Pos) instanceof Scp4666CoatHooksBlockEntity Hooks)) {
            return 0;
        }
        ready(World, Hooks);
        int Hung = 0;
        if (!Held.isEmpty()) {
            int Slot = slotOf(Held);
            if (Slot >= 0 && Hooks.get(Slot).isEmpty()) {
                Hooks.set(Slot, Held.copyAndClear());
                Hung++;
            }
        } else {
            for (int Slot = 0; Slot < Slots.size(); Slot++) {
                ItemStack Worn = User.getItemBySlot(Slots.get(Slot));
                if (Worn.getItem() instanceof Scp4666WinterGearItem && Hooks.get(Slot).isEmpty()) {
                    Hooks.set(Slot, Worn.copy());
                    User.setItemSlot(Slots.get(Slot), ItemStack.EMPTY);
                    Hung++;
                }
            }
        }
        show(World, Pos, Hooks);
        return Hung;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (slotOf(Stack) < 0 || !User.isSecondaryUseActive() || !Scp4666World.inside(World)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server && usable(World, User) && hang(Server, Pos, User, Stack) > 0) {
            World.playSound(null, Pos, Scp4666Registry.Rummage.get(), SoundSource.PLAYERS, 0.5f, 1.2f);
            say(User, "gear_hung");
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!Scp4666World.inside(World)) {
            return InteractionResult.PASS;
        }
        if (World instanceof ServerLevel Server && usable(World, User)) {
            if (User.isSecondaryUseActive()) {
                if (hang(Server, Pos, User, ItemStack.EMPTY) > 0) {
                    World.playSound(null, Pos, Scp4666Registry.Rummage.get(), SoundSource.PLAYERS, 0.5f, 1.2f);
                    say(User, "gear_hung");
                }
            } else if (take(Server, Pos, User) > 0) {
                World.playSound(null, Pos, Scp4666Registry.Rummage.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
                say(User, "gear_taken");
            } else {
                say(User, "gear_empty");
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    private static void say(Player User, String Key) {
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666." + Key).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_coat_hooks.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
