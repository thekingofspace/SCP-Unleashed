package net.rai.scp_unleashed.scp020;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class Scp020MoldBlock extends MultifaceBlock {
    public static final MapCodec<Scp020MoldBlock> BlockCodec = simpleCodec(Scp020MoldBlock::new);
    public static final int SettleTicks = 1;
    private final MultifaceSpreader Spreader = new MultifaceSpreader(this);

    public Scp020MoldBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends MultifaceBlock> codec() {
        return BlockCodec;
    }

    @Override
    public MultifaceSpreader getSpreader() {
        return Spreader;
    }

    public static boolean isLegacy(BlockState State) {
        return State.getBlock() instanceof Scp020MoldBlock;
    }

    public static int faceCount(BlockState State) {
        int Count = 0;
        for (Direction Side : DIRECTIONS) {
            if (hasFace(State, Side)) {
                Count++;
            }
        }
        return Count;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState State, BlockGetter Getter, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState State, BlockGetter Getter, BlockPos Pos) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState State, BlockGetter Getter, BlockPos Pos) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, BlockPlaceContext Context) {
        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState State, Fluid Liquid) {
        return true;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter Getter, BlockPos Pos) {
        return true;
    }

    @Override
    protected int getLightBlock(BlockState State, BlockGetter Getter, BlockPos Pos) {
        return 0;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return true;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!World.isClientSide && State.is(this)) {
            World.scheduleTick(Pos, this, SettleTicks);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        Scp020Migration.convert(World, Pos, State);
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        Scp020Migration.convert(World, Pos, State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp020_mold.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
