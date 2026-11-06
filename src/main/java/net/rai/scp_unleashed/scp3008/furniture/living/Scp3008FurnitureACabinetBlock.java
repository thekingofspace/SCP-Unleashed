package net.rai.scp_unleashed.scp3008.furniture.living;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp3008FurnitureACabinetBlock extends Scp3008FurnitureABlock implements EntityBlock {
    public static final MapCodec<Scp3008FurnitureACabinetBlock> CabinetCodec = simpleCodec(Properties -> new Scp3008FurnitureACabinetBlock(Properties, Scp3008FurnitureAPiece.BaseCabinet));
    public static final BooleanProperty Open = BlockStateProperties.OPEN;

    public Scp3008FurnitureACabinetBlock(Properties Properties, Scp3008FurnitureAPiece Piece) {
        super(Properties, Piece);
    }

    @Override
    protected BlockState startState(BlockState State) {
        return State.setValue(Open, false);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CabinetCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Open);
    }

    public BlockPos containerPos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    public Vec3 soundAt(BlockState State, BlockPos Pos) {
        return Vec3.atCenterOf(Pos);
    }

    public void setOpen(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        if (State.getValue(Open) != Opened) {
            World.setBlock(Pos, State.setValue(Open, Opened), Block.UPDATE_ALL);
        }
    }

    public void voice(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        float Pitch = 0.95f + World.random.nextFloat() * 0.1f;
        Vec3 Where = soundAt(State, Pos);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : layers(Opened)) {
            World.playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.BLOCKS, 1.0f, Pitch);
        }
    }

    private List<DeferredHolder<SoundEvent, SoundEvent>> layers(boolean Opened) {
        return switch (Piece.Sound) {
            case Drawer -> List.of(Opened ? Scp3008FurnitureARegistry.DrawerOpen : Scp3008FurnitureARegistry.DrawerClose);
            case Glass -> List.of(Opened ? Scp3008FurnitureARegistry.DoorOpen : Scp3008FurnitureARegistry.DoorClose, Scp3008FurnitureARegistry.GlassRattle);
            case Fridge -> List.of(Opened ? Scp3008FurnitureARegistry.FridgeOpen : Scp3008FurnitureARegistry.FridgeClose, Scp3008FurnitureARegistry.FridgeSeal);
            case Books -> List.of(Opened ? Scp3008FurnitureARegistry.BooksOut : Scp3008FurnitureARegistry.BooksIn);
            default -> Opened ? List.of(Scp3008FurnitureARegistry.DoorOpen) : List.of(Scp3008FurnitureARegistry.DoorClose, Scp3008FurnitureARegistry.SoftClick);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(containerPos(State, Pos)) instanceof Scp3008FurnitureAStorageBlockEntity Storage) {
            User.openMenu(Storage);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        Containers.dropContentsOnDestroy(State, NewState, World, Pos);
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (World.getBlockEntity(Pos) instanceof Scp3008FurnitureAStorageBlockEntity Storage) {
            Storage.recheckOpen();
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(World.getBlockEntity(containerPos(State, Pos)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp3008FurnitureAStorageBlockEntity(Pos, State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        super.appendHoverText(Stack, Context, Lines, Flag);
        Lines.add(Component.translatable("block.scp_unleashed.props.slots", Piece.Slots).withStyle(ChatFormatting.DARK_GRAY));
    }
}
