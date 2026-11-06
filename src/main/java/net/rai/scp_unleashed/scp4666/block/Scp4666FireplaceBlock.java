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

public class Scp4666FireplaceBlock extends Scp4666TallBlock {
    public static final MapCodec<Scp4666FireplaceBlock> BlockCodec = simpleCodec(Scp4666FireplaceBlock::new);
    public static final IntegerProperty Fire = IntegerProperty.create("fire", 0, 4);
    public static final int[] LowerLight = {0, 6, 10, 13, 15};
    public static final int[] UpperLight = {0, 2, 5, 7, 9};
    private static final Map<Direction, VoxelShape> Lower = Scp4666Shapes.table(new double[][]{{0, 0, 4, 16, 16, 16}, {0, 0, 0, 2, 16, 4}, {14, 0, 0, 16, 16, 4}, {0, 0, 0, 16, 2, 4}});
    private static final Map<Direction, VoxelShape> Upper = Scp4666Shapes.table(new double[][]{{0, 0, 1, 16, 4, 16}, {1, 4, 6, 15, 16, 16}});

    public Scp4666FireplaceBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Fire, 4));
    }

    public static int lightOf(BlockState State) {
        int Level = State.getValue(Fire);
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerLight[Level] : UpperLight[Level];
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half, Fire);
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
        if (!Stack.is(Scp4666Registry.Firewood.get()) || !Scp4666World.inside(World)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
            Scp4666Chores.feedFire(Server, Visitor);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Level = State.getValue(Fire);
        if (State.getValue(Half) != DoubleBlockHalf.LOWER || Level == 0) {
            return;
        }
        Direction Front = State.getValue(FACING);
        double X = Pos.getX() + 0.5 + Front.getStepX() * 0.1;
        double Z = Pos.getZ() + 0.5 + Front.getStepZ() * 0.1;
        if (Level == 1) {
            if (Random.nextInt(3) == 0) {
                World.addParticle(ParticleTypes.SMALL_FLAME, X + (Random.nextDouble() - 0.5) * 0.4, Pos.getY() + 0.18, Z + (Random.nextDouble() - 0.5) * 0.25, 0.0, 0.004, 0.0);
            }
            if (Random.nextInt(6) == 0) {
                World.addParticle(ParticleTypes.SMOKE, X, Pos.getY() + 0.4, Z, 0.0, 0.02, 0.0);
            }
            if (Random.nextInt(20) == 0) {
                World.playLocalSound(X, Pos.getY() + 0.3, Z, Scp4666Registry.FireCrackle.get(), SoundSource.BLOCKS, 0.3f, 0.7f + Random.nextFloat() * 0.2f, false);
            }
            return;
        }
        int Flames = Level - 1;
        for (int Index = 0; Index < Flames; Index++) {
            World.addParticle(ParticleTypes.FLAME, X + (Random.nextDouble() - 0.5) * 0.5, Pos.getY() + 0.2 + Random.nextDouble() * 0.2 * Level, Z + (Random.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
        }
        if (Random.nextInt(5 - Level) == 0) {
            World.addParticle(ParticleTypes.SMOKE, X, Pos.getY() + 0.7, Z, 0.0, 0.03, 0.0);
        }
        if (Random.nextInt(10 - Level) == 0) {
            World.playLocalSound(X, Pos.getY() + 0.3, Z, Scp4666Registry.FireCrackle.get(), SoundSource.BLOCKS, 0.4f + Level * 0.12f, 0.8f + Random.nextFloat() * 0.3f, false);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_fireplace.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
