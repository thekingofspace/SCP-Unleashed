package net.rai.scp_unleashed.scp085;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class Scp085DrawingBlock extends Scp085SheetBlock {
    public static final MapCodec<Scp085DrawingBlock> BlockCodec = simpleCodec(Scp085DrawingBlock::new);
    public static final EnumProperty<Scp085Scene> Scene = EnumProperty.create("scene", Scp085Scene.class, Scp085Scene::isDrawing);

    public Scp085DrawingBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Scene, Scp085Scene.Room));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Scene);
    }

    @Override
    public Scp085Scene sceneOf(BlockState State) {
        return State.getValue(Scene);
    }

    @Override
    public boolean isHomeSheet() {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = super.getStateForPlacement(Context);
        if (Placed == null) {
            return null;
        }
        Scp085Scene Chosen = sceneOf(Context.getItemInHand());
        if (Chosen == null) {
            Chosen = Scp085Scene.Drawings.get(Context.getLevel().getRandom().nextInt(Scp085Scene.Drawings.size()));
        }
        return Placed.setValue(Scene, Chosen);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader World, BlockPos Pos, BlockState State) {
        return stackOf(State.getValue(Scene), 1);
    }

    @Nullable
    public static Scp085Scene sceneOf(ItemStack Stack) {
        BlockItemStateProperties Stored = Stack.get(DataComponents.BLOCK_STATE);
        return Stored == null ? null : Stored.get(Scene);
    }

    public static ItemStack stackOf(Scp085Scene Chosen, int Count) {
        ItemStack Stack = new ItemStack(Scp085Registry.DrawingItem.get(), Count);
        Stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(Scene, Chosen));
        return Stack;
    }
}
