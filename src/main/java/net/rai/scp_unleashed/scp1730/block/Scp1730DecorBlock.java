package net.rai.scp_unleashed.scp1730.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public final class Scp1730DecorBlock {
    private Scp1730DecorBlock() {
    }

    public static Block make(Scp1730Decor Entry, BlockBehaviour.Properties Props) {
        return switch (Entry.kind()) {
            case Cube -> new Block(Props);
            case FacingCube -> new Facing(Props);
            case Prop -> new Prop(Props, Entry);
            case Decal -> new Decal(Props, false);
            case Light -> new Decal(Props, Entry.sparks());
        };
    }

    public static class Facing extends HorizontalDirectionalBlock {
        public static final MapCodec<Facing> FacingCodec = simpleCodec(Facing::new);

        public Facing(Properties Props) {
            super(Props);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return FacingCodec;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
            Builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext Context) {
            return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        }
    }

    public static class Prop extends HorizontalDirectionalBlock {
        public static final MapCodec<Prop> PropCodec = simpleCodec(Properties -> new Prop(Properties, Scp1730Decor.Shape.Full));
        private final Map<Direction, VoxelShape> Shaped = new EnumMap<>(Direction.class);
        private final Map<Direction, VoxelShape> Resting = new EnumMap<>(Direction.class);
        private final Scp1730Decor.Shape Form;

        public Prop(Properties Props, Scp1730Decor.Shape Form) {
            this(Props, Form, null, null);
        }

        public Prop(Properties Props, Scp1730Decor Entry) {
            this(Props, Entry.shape(), Scp1730PropShapes.fitted(Entry.id()), Scp1730PropShapes.resting(Entry.id()));
        }

        private Prop(Properties Props, Scp1730Decor.Shape Form, @Nullable VoxelShape Fitted, @Nullable VoxelShape Rest) {
            super(Props);
            this.Form = Form;
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                VoxelShape Body = Fitted == null ? shapeOf(Form, Side) : PropShapes.turn(Fitted, Side);
                Shaped.put(Side, Body);
                Resting.put(Side, Rest == null ? Body : PropShapes.turn(Rest, Side));
            }
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        public static VoxelShape shapeOf(Scp1730Decor.Shape Form, Direction Side) {
            return switch (Form) {
                case Full -> Shapes.block();
                case Half -> Block.box(0, 0, 0, 16, 8, 16);
                case Low -> Block.box(0, 0, 0, 16, 3, 16);
                case None -> Block.box(2, 0, 2, 14, 2, 14);
                case Wall -> switch (Side) {
                    case SOUTH -> Block.box(0, 0, 0, 16, 16, 3);
                    case EAST -> Block.box(0, 0, 0, 3, 16, 16);
                    case WEST -> Block.box(13, 0, 0, 16, 16, 16);
                    default -> Block.box(0, 0, 13, 16, 16, 16);
                };
            };
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return PropCodec;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
            Builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext Context) {
            return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
            return Shaped.get(State.getValue(FACING));
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
            if (Form == Scp1730Decor.Shape.None) {
                return Shapes.empty();
            }
            return (Scp1730PropShapes.item(Context) ? Resting : Shaped).get(State.getValue(FACING));
        }

        @Override
        protected boolean isPathfindable(BlockState State, PathComputationType Type) {
            return Form == Scp1730Decor.Shape.None || Form == Scp1730Decor.Shape.Low;
        }
    }

    public static class Decal extends DirectionalBlock {
        public static final MapCodec<Decal> DecalCodec = simpleCodec(Properties -> new Decal(Properties, false));
        private static final Map<Direction, VoxelShape> Thin = new EnumMap<>(Direction.class);
        private final boolean Sparking;

        static {
            Thin.put(Direction.UP, Block.box(0, 0, 0, 16, 1, 16));
            Thin.put(Direction.DOWN, Block.box(0, 15, 0, 16, 16, 16));
            Thin.put(Direction.NORTH, Block.box(0, 0, 15, 16, 16, 16));
            Thin.put(Direction.SOUTH, Block.box(0, 0, 0, 16, 16, 1));
            Thin.put(Direction.WEST, Block.box(15, 0, 0, 16, 16, 16));
            Thin.put(Direction.EAST, Block.box(0, 0, 0, 1, 16, 16));
        }

        public Decal(Properties Props, boolean Sparking) {
            super(Props);
            this.Sparking = Sparking;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
        }

        @Override
        protected MapCodec<? extends DirectionalBlock> codec() {
            return DecalCodec;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
            Builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext Context) {
            return defaultBlockState().setValue(FACING, Context.getClickedFace());
        }

        @Override
        protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
            return Thin.get(State.getValue(FACING));
        }

        @Override
        protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
            return Shapes.empty();
        }

        @Override
        protected boolean isPathfindable(BlockState State, PathComputationType Type) {
            return true;
        }

        @Override
        public void animateTick(BlockState State, Level World, BlockPos Pos, RandomSource Random) {
            if (!Sparking || Random.nextInt(5) != 0) {
                return;
            }
            Direction Side = State.getValue(FACING);
            double X = Pos.getX() + 0.5 - Side.getStepX() * 0.4 + (Random.nextDouble() - 0.5) * 0.6;
            double Y = Pos.getY() + 0.5 - Side.getStepY() * 0.4 + (Random.nextDouble() - 0.5) * 0.6;
            double Z = Pos.getZ() + 0.5 - Side.getStepZ() * 0.4 + (Random.nextDouble() - 0.5) * 0.6;
            for (int Index = 0; Index < 2 + Random.nextInt(3); Index++) {
                World.addParticle(ParticleTypes.ELECTRIC_SPARK, X, Y, Z, (Random.nextDouble() - 0.5) * 0.2, -0.05 - Random.nextDouble() * 0.1,
                        (Random.nextDouble() - 0.5) * 0.2);
            }
        }
    }
}
