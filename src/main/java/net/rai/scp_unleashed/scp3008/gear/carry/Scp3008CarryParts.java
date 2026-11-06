package net.rai.scp_unleashed.scp3008.gear.carry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008CarryParts {
    public static final int MaxParts = 8;
    public static final int Reach = 2;
    public static final int PieceReach = 3;
    public static final String PartName = "part";
    public static final List<String> MainValues = List.of("main", "0");
    public static final List<String> MainFinders = List.of("main", "mainPos");
    private static final Map<Class<?>, Optional<Method>> Finders = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Method>> Placers = new ConcurrentHashMap<>();

    private Scp3008CarryParts() {
    }

    public static Optional<Direction> facingOf(BlockState State) {
        if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return Optional.of(State.getValue(BlockStateProperties.HORIZONTAL_FACING));
        }
        if (State.hasProperty(BlockStateProperties.FACING)) {
            Direction Way = State.getValue(BlockStateProperties.FACING);
            if (Way.getAxis().isHorizontal()) {
                return Optional.of(Way);
            }
        }
        return Optional.empty();
    }

    public static Rotation between(Direction From, Direction To) {
        return Rotation.values()[Math.floorMod(To.get2DDataValue() - From.get2DDataValue(), 4)];
    }

    public static Direction turned(Direction Base, int Turns) {
        Direction Result = Base;
        for (int Step = 0; Step < Math.floorMod(Turns, 4); Step++) {
            Result = Result.getClockWise();
        }
        return Result;
    }

    public static Rotation rotationFor(Optional<Direction> Facing, Direction Wanted) {
        return Facing.map(From -> between(From, Wanted)).orElse(Rotation.NONE);
    }

    @SuppressWarnings("deprecation")
    public static BlockState rotated(BlockState State, Rotation Turn) {
        if (Turn == Rotation.NONE) {
            return State;
        }
        BlockState Result = State.rotate(Turn);
        Optional<Direction> Before = facingOf(State);
        if (Before.isPresent() && facingOf(Result).equals(Before)) {
            if (Result.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                Result = Result.setValue(BlockStateProperties.HORIZONTAL_FACING, Turn.rotate(Before.get()));
            } else if (Result.hasProperty(BlockStateProperties.FACING)) {
                Result = Result.setValue(BlockStateProperties.FACING, Turn.rotate(Before.get()));
            }
        }
        return Result;
    }

    public static BlockPos rotated(BlockPos Offset, Rotation Turn) {
        return Offset.rotate(Turn);
    }

    @Nullable
    public static Property<?> partProperty(BlockState State) {
        return State.getBlock().getStateDefinition().getProperty(PartName);
    }

    private static <T extends Comparable<T>> String valueName(BlockState State, Property<T> Part) {
        return Part.getName(State.getValue(Part));
    }

    public static boolean isMainPart(BlockState State) {
        if (State.hasProperty(BlockStateProperties.BED_PART)) {
            return State.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD;
        }
        Property<?> Part = partProperty(State);
        return Part == null || MainValues.contains(valueName(State, Part));
    }

    public static boolean isMain(BlockState State) {
        if (!isMainPart(State)) {
            return false;
        }
        return !State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || State.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER;
    }

    private static Optional<Method> finder(Class<?> Owner) {
        return Finders.computeIfAbsent(Owner, Key -> {
            for (String Name : MainFinders) {
                try {
                    Method Found = Key.getMethod(Name, BlockPos.class, BlockState.class);
                    if (Modifier.isStatic(Found.getModifiers()) && BlockPos.class.isAssignableFrom(Found.getReturnType())) {
                        return Optional.of(Found);
                    }
                } catch (NoSuchMethodException Missing) {
                }
            }
            return Optional.empty();
        });
    }

    public static Optional<Method> placer(Class<?> Owner) {
        return Placers.computeIfAbsent(Owner, Key -> {
            try {
                Method Found = Key.getMethod("placeWhole", Level.class, BlockPos.class, BlockState.class);
                return Modifier.isStatic(Found.getModifiers()) ? Optional.of(Found) : Optional.empty();
            } catch (NoSuchMethodException Missing) {
                return Optional.empty();
            }
        });
    }

    public static boolean placeWhole(Level World, BlockPos Pos, BlockState State) {
        Optional<Method> Found = placer(State.getBlock().getClass());
        if (Found.isEmpty()) {
            return false;
        }
        try {
            Found.get().invoke(null, World, Pos, State);
            return true;
        } catch (ReflectiveOperationException | RuntimeException Problem) {
            return false;
        }
    }

    private static boolean confirmsMain(BlockGetter World, BlockPos Pos, BlockState Part) {
        BlockState There = World.getBlockState(Pos);
        return There.is(Part.getBlock()) && isMainPart(There) && facingOf(There).equals(facingOf(Part));
    }

    @Nullable
    public static BlockPos mainOf(BlockGetter World, BlockPos Pos, BlockState State) {
        if (isMainPart(State)) {
            return Pos;
        }
        Optional<Method> Found = finder(State.getBlock().getClass());
        if (Found.isPresent()) {
            try {
                Object Result = Found.get().invoke(null, Pos, State);
                if (Result instanceof BlockPos Main && confirmsMain(World, Main, State)) {
                    return Main.immutable();
                }
            } catch (ReflectiveOperationException | RuntimeException Problem) {
            }
        }
        List<BlockPos> Candidates = new ArrayList<>();
        Candidates.add(Pos.below());
        facingOf(State).ifPresent(Way -> Candidates.add(Pos.relative(Way.getCounterClockWise())));
        for (BlockPos Candidate : Candidates) {
            if (confirmsMain(World, Candidate, State)) {
                return Candidate;
            }
        }
        List<BlockPos> Around = new ArrayList<>();
        for (BlockPos Spot : BlockPos.betweenClosed(Pos.offset(-Reach, -Reach, -Reach), Pos.offset(Reach, Reach, Reach))) {
            Around.add(Spot.immutable());
        }
        Around.sort(Comparator.comparingDouble(Spot -> Spot.distSqr(Pos)));
        for (BlockPos Spot : Around) {
            if (!Spot.equals(Pos) && confirmsMain(World, Spot, State)) {
                return Spot;
            }
        }
        return null;
    }

    private static List<BlockPos> pieceParts(BlockGetter World, BlockPos Start, BlockState State) {
        BlockPos Main = mainOf(World, Start, State);
        if (Main == null) {
            return List.of(Start);
        }
        BlockState MainState = World.getBlockState(Main);
        List<BlockPos> Found = new ArrayList<>();
        Set<BlockPos> Seen = new HashSet<>();
        Deque<BlockPos> Queue = new ArrayDeque<>();
        Found.add(Main);
        Seen.add(Main);
        Queue.add(Main);
        while (!Queue.isEmpty() && Found.size() <= MaxParts) {
            BlockPos Here = Queue.poll();
            for (Direction Side : Direction.values()) {
                BlockPos Next = Here.relative(Side).immutable();
                if (!Seen.add(Next) || Math.abs(Next.getX() - Main.getX()) > PieceReach || Math.abs(Next.getY() - Main.getY()) > PieceReach
                        || Math.abs(Next.getZ() - Main.getZ()) > PieceReach) {
                    continue;
                }
                BlockState There = World.getBlockState(Next);
                if (There.is(MainState.getBlock()) && !isMainPart(There) && facingOf(There).equals(facingOf(MainState)) && Main.equals(mainOf(World, Next, There))) {
                    Found.add(Next);
                    Queue.add(Next);
                }
            }
        }
        if (!Found.contains(Start)) {
            Found.add(Start.immutable());
        }
        return Found;
    }

    public static List<BlockPos> parts(BlockGetter World, BlockPos Start, BlockState State, boolean Spread) {
        Block Kind = State.getBlock();
        if (State.hasProperty(BlockStateProperties.BED_PART) && State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            BedPart Piece = State.getValue(BlockStateProperties.BED_PART);
            Direction Way = State.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos Other = Piece == BedPart.FOOT ? Start.relative(Way) : Start.relative(Way.getOpposite());
            BlockState OtherState = World.getBlockState(Other);
            if (OtherState.is(Kind) && OtherState.hasProperty(BlockStateProperties.BED_PART) && OtherState.getValue(BlockStateProperties.BED_PART) != Piece) {
                return Piece == BedPart.HEAD ? List.of(Start, Other) : List.of(Other, Start);
            }
            return List.of(Start);
        }
        if (partProperty(State) != null) {
            return pieceParts(World, Start, State);
        }
        if (State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            DoubleBlockHalf Half = State.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF);
            BlockPos Other = Half == DoubleBlockHalf.LOWER ? Start.above() : Start.below();
            BlockState OtherState = World.getBlockState(Other);
            if (OtherState.is(Kind) && OtherState.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
                    && OtherState.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) != Half) {
                return Half == DoubleBlockHalf.LOWER ? List.of(Start, Other) : List.of(Other, Start);
            }
            return List.of(Start);
        }
        if (!Spread) {
            return List.of(Start);
        }
        Set<BlockPos> Found = new LinkedHashSet<>();
        Deque<BlockPos> Queue = new ArrayDeque<>();
        Found.add(Start);
        Queue.add(Start);
        while (!Queue.isEmpty() && Found.size() <= MaxParts) {
            BlockPos Here = Queue.poll();
            for (Direction Side : Direction.values()) {
                BlockPos Next = Here.relative(Side);
                if (Found.contains(Next) || Math.abs(Next.getX() - Start.getX()) > Reach || Math.abs(Next.getY() - Start.getY()) > Reach
                        || Math.abs(Next.getZ() - Start.getZ()) > Reach) {
                    continue;
                }
                if (World.getBlockState(Next).is(Kind)) {
                    Found.add(Next.immutable());
                    Queue.add(Next.immutable());
                }
            }
        }
        List<BlockPos> Ordered = new ArrayList<>(Found);
        Ordered.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).thenComparingDouble(Spot -> Spot.distSqr(Start)));
        return Ordered;
    }
}
