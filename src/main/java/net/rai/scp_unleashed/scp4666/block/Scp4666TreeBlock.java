package net.rai.scp_unleashed.scp4666.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.rai.scp_unleashed.scp4666.Scp4666Director;
import net.rai.scp_unleashed.scp4666.Scp4666Map;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666State;
import net.rai.scp_unleashed.scp4666.Scp4666World;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public class Scp4666TreeBlock extends Scp4666TallBlock {
    public static final MapCodec<Scp4666TreeBlock> BlockCodec = simpleCodec(Scp4666TreeBlock::new);
    public static final IntegerProperty Lights = IntegerProperty.create("lights", 0, 4);
    private static final Map<Direction, VoxelShape> Lower = Scp4666Shapes.table(new double[][]{{1, 0, 1, 15, 16, 15}});
    private static final Map<Direction, VoxelShape> Upper = Scp4666Shapes.table(new double[][]{{3, 0, 3, 13, 15, 13}});
    private static final Vector3f[] Bulbs = {new Vector3f(1.0f, 0.3f, 0.25f), new Vector3f(1.0f, 0.82f, 0.3f), new Vector3f(0.35f, 0.9f, 0.45f), new Vector3f(0.4f, 0.6f, 1.0f)};

    public Scp4666TreeBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Lights, 4));
    }

    public static int lightOf(BlockState State) {
        return State.getValue(Lights) * 3;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Lights);
    }

    @Override
    protected Map<Direction, VoxelShape> lowerShapes() {
        return Lower;
    }

    @Override
    protected Map<Direction, VoxelShape> upperShapes() {
        return Upper;
    }

    public static void setLights(Level World, BlockPos Pos, BlockState State, int Value) {
        BlockPos Base = lower(Pos, State);
        for (BlockPos Part : new BlockPos[]{Base, Base.above()}) {
            BlockState Found = World.getBlockState(Part);
            if (Found.getBlock() instanceof Scp4666TreeBlock && Found.getValue(Lights) != Value) {
                World.setBlock(Part, Found.setValue(Lights, Value), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Scp4666Registry.Gift.get()) || !Scp4666World.inside(World)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World instanceof ServerLevel Server && User instanceof ServerPlayer Visitor && Scp4666Chores.active(Server, Visitor)) {
            Scp4666Chores.offer(Server, Visitor);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int Next = State.getValue(Lights) == 4 ? 0 : 4;
        if (World instanceof ServerLevel Server && Scp4666World.inside(World)) {
            Scp4666Director.setTree(Server, Scp4666Map.get(Server.getServer()), Scp4666State.get(Server.getServer()), Next);
        } else {
            setLights(World, Pos, State, Next);
        }
        World.playSound(null, Pos, Scp4666Registry.Twinkle.get(), SoundSource.BLOCKS, 0.6f, Next > 0 ? 1.4f : 0.7f);
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        int Count = State.getValue(Lights);
        if (Count > 0 && Random.nextInt(3) == 0) {
            Vector3f Colour = Bulbs[Random.nextInt(Math.min(Bulbs.length, Count))];
            double X = Pos.getX() + 0.2 + Random.nextDouble() * 0.6;
            double Y = Pos.getY() + Random.nextDouble();
            double Z = Pos.getZ() + 0.2 + Random.nextDouble() * 0.6;
            World.addParticle(new DustParticleOptions(Colour, 0.45f), X, Y, Z, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp4666_christmas_tree.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
