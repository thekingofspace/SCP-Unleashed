package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.scp1730.puzzle.Scp1730PuzzleDirector;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.scp1730.Scp1730Sparkle;

public class Scp1730PickupBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<Scp1730PickupBlock> Codec = simpleCodec(Scp1730PickupBlock::new);
    public static final EnumProperty<Kind> Item = EnumProperty.create("item", Kind.class);
    private static final VoxelShape Flat = Block.box(3, 0, 3, 13, 5, 13);

    public enum Kind implements StringRepresentable {
        Fuse("fuse"),
        OverrideCard("override_card"),
        DutyKey("duty_key"),
        SpliceKit("splice_kit"),
        Sample("sample"),
        Fuel("fuel");

        private final String Name;

        Kind(String Name) {
            this.Name = Name;
        }

        public static Kind byIndex(int Index) {
            Kind[] All = values();
            return All[Math.floorMod(Index, All.length)];
        }

        @Override
        public String getSerializedName() {
            return Name;
        }
    }

    public Scp1730PickupBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Item, Kind.Fuse));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Item);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return !World.getBlockState(Pos.below()).getCollisionShape(World, Pos.below()).isEmpty();
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Flat;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.empty();
    }

    @Override
    public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
        Scp1730Sparkle.twinkle(World, Pos, Random, 0.05);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide) {
            Scp1730PuzzleDirector.take((ServerLevel) World, Pos, State, (ServerPlayer) User);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }
}
