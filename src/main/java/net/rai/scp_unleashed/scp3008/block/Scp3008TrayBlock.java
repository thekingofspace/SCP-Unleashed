package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;

public class Scp3008TrayBlock extends Block {
    public static final MapCodec<Scp3008TrayBlock> TrayCodec = simpleCodec(Scp3008TrayBlock::new);
    public static final EnumProperty<Food> Serving = EnumProperty.create("food", Food.class);
    public static final VoxelShape Shape = Block.box(2, 0, 3, 14, 3, 13);

    public enum Food implements StringRepresentable {
        None("none"),
        Meatballs("meatballs"),
        Fries("fries"),
        Drink("drink"),
        Jam("jam");

        private static final Food[] Ordered = values();
        public final String Name;

        Food(String Name) {
            this.Name = Name;
        }

        public static Food of(int Index) {
            return Ordered[Math.max(0, Math.min(Ordered.length - 1, Index))];
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp3008TrayBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Serving, Food.None));
    }

    @Override
    protected MapCodec<? extends Scp3008TrayBlock> codec() {
        return TrayCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Serving);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shape;
    }

    public static Item itemFor(Food Kind) {
        return switch (Kind) {
            case Meatballs -> Scp3008Registry.Meatballs.get();
            case Fries -> Scp3008Registry.Fries.get();
            case Drink -> Scp3008Registry.SoftDrink.get();
            case Jam -> Scp3008Registry.Jam.get();
            default -> null;
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        Food Kind = State.getValue(Serving);
        if (Kind == Food.None) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ItemStack Taken = new ItemStack(itemFor(Kind));
        if (!User.getInventory().add(Taken)) {
            User.drop(Taken, false);
        }
        World.setBlock(Pos, State.setValue(Serving, Food.None), Block.UPDATE_ALL);
        World.playSound(null, Pos, Scp3008Registry.TrayTake.get(), SoundSource.BLOCKS, 0.7f, 0.95f + World.getRandom().nextFloat() * 0.1f);
        return InteractionResult.CONSUME;
    }
}
