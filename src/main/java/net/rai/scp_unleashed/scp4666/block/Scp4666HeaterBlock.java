package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp4666.Scp4666Chores;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;

import java.util.List;
import java.util.Map;

public class Scp4666HeaterBlock extends Scp4666TallBlock {
    public static final MapCodec<Scp4666HeaterBlock> BlockCodec = simpleCodec(Scp4666HeaterBlock::new);
    public static final IntegerProperty Heat = IntegerProperty.create("heat", 0, 3);
    public static final int[] LowerLight = {0, 5, 9, 12};
    public static final int[] UpperLight = {0, 0, 3, 5};
    private static final Map<Direction, VoxelShape> Lower = Scp4666Shapes.table(new double[][]{{1, 0, 1, 15, 16, 15}});
    private static final Map<Direction, VoxelShape> Upper = Scp4666Shapes.table(new double[][]{{2, 0, 2, 14, 11, 14}, {6, 11, 6, 10, 16, 10}});

    public Scp4666HeaterBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Heat, 0));
    }

    public static int lightOf(BlockState State) {
        int Level = State.getValue(Heat);
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerLight[Level] : UpperLight[Level];
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half, Heat);
    }

    @Override
    protected Map<Direction, VoxelShape> lowerShapes() {
        return Lower;
    }

    @Override
    protected Map<Direction, VoxelShape> upperShapes() {
        return Upper;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp4666Registry.Scuttle.get()) || !Scp4666World.inside(World)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
            Scp4666Chores.stoke(Server, Visitor);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Level = State.getValue(Heat);
        if (Level == 0) {
            return;
        }
        Direction Front = State.getValue(FACING);
        if (State.getValue(Half) == DoubleBlockHalf.LOWER) {
            double X = Pos.getX() + 0.5 + Front.getStepX() * 0.47;
            double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.47;
            if (Random.nextInt(4 - Level) == 0) {
                World.addParticle(ParticleTypes.SMALL_FLAME, X + (Random.nextDouble() - 0.5) * 0.3 * Front.getStepZ(), Pos.getY() + 0.3, Z + (Random.nextDouble() - 0.5) * 0.3 * Front.getStepX(), 0.0, 0.0, 0.0);
            }
            if (Random.nextInt(14 - Level * 3) == 0) {
                World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Scp4666Registry.BoilerRumble.get(), SoundSource.BLOCKS, 0.35f + Level * 0.12f, 0.7f + Random.nextFloat() * 0.15f, false);
            }
            return;
        }
        if (Random.nextInt(3) == 0) {
            World.addParticle(ParticleTypes.SMOKE, Pos.getX() + 0.5, Pos.getY() + 1.0, Pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
        if (Level >= 2 && Random.nextInt(30) == 0) {
            World.playLocalSound(Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Scp4666Registry.BoilerTick.get(), SoundSource.BLOCKS, 0.4f, 1.0f + Random.nextFloat() * 0.3f, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_boiler.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
