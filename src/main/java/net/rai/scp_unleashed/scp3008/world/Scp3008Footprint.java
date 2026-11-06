package net.rai.scp_unleashed.scp3008.world;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008Footprint {
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<BlockState, List<Part>> Cache = new ConcurrentHashMap<>();

    public record Part(int Dx, int Dy, int Dz, BlockState State) {
        public BlockPos at(BlockPos Main) {
            return Main.offset(Dx, Dy, Dz);
        }
    }

    private Scp3008Footprint() {
    }

    public static void forget() {
        Cache.clear();
    }

    public static Direction facingOf(BlockState State) {
        if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return State.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (State.hasProperty(BlockStateProperties.FACING) && State.getValue(BlockStateProperties.FACING).getAxis().isHorizontal()) {
            return State.getValue(BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    public static List<Part> parts(BlockState Main) {
        return Cache.computeIfAbsent(Main, Scp3008Footprint::resolve);
    }

    public static BlockState mainState(BlockState State) {
        return parts(State).get(0).State();
    }

    private static Optional<Method> method(Object Target, String Name, Class<?>... Types) {
        try {
            return Optional.of(Target.getClass().getMethod(Name, Types));
        } catch (NoSuchMethodException Missing) {
            return Optional.empty();
        }
    }

    private static Optional<Object> field(Object Target, String Name) {
        try {
            Field Found = Target.getClass().getField(Name);
            return Optional.ofNullable(Found.get(Target));
        } catch (ReflectiveOperationException Missing) {
            return Optional.empty();
        }
    }

    private static List<Part> resolve(BlockState State) {
        try {
            List<Part> Found = halves(State);
            if (Found == null) {
                Found = cells(State);
            }
            if (Found == null) {
                Found = numbered(State);
            }
            if (Found == null) {
                Found = paired(State);
            }
            if (Found != null && !Found.isEmpty()) {
                return List.copyOf(Found);
            }
        } catch (ReflectiveOperationException | RuntimeException Problem) {
            Log.warn("SCP-3008 could not read the footprint of {}", State, Problem);
        }
        return List.of(new Part(0, 0, 0, State));
    }

    private static List<Part> halves(BlockState State) {
        if (!State.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            return null;
        }
        return List.of(new Part(0, 0, 0, State.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER)),
                new Part(0, 1, 0, State.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER)));
    }

    private static List<Part> cells(BlockState State) throws ReflectiveOperationException {
        Block Piece = State.getBlock();
        Optional<Method> Kind = method(Piece, "kind");
        Optional<Method> Property = method(Piece, "partProperty");
        if (Kind.isEmpty() || Property.isEmpty()) {
            return null;
        }
        Object Type = Kind.get().invoke(Piece);
        Optional<Object> Layout = Type == null ? Optional.empty() : field(Type, "Cells");
        if (Layout.isEmpty() || !(Layout.get() instanceof int[][] Cells) || !(Property.get().invoke(Piece) instanceof IntegerProperty Part0)) {
            return null;
        }
        if (Cells.length <= 1 || !State.hasProperty(Part0)) {
            return List.of(new Part(0, 0, 0, State));
        }
        Direction Facing = facingOf(State);
        List<Part> Found = new ArrayList<>();
        for (int Index = 0; Index < Cells.length; Index++) {
            int[] Cell = Cells[Index];
            BlockPos Offset = BlockPos.ZERO.relative(Facing.getClockWise(), Cell[0]).above(Cell[1]).relative(Facing.getOpposite(), Cell[2]);
            Found.add(new Part(Offset.getX(), Offset.getY(), Offset.getZ(), State.setValue(Part0, Index)));
        }
        return Found;
    }

    private static List<Part> numbered(BlockState State) throws ReflectiveOperationException {
        Block Piece = State.getBlock();
        Optional<Method> Place = method(Piece, "partPos", BlockPos.class, Direction.class, int.class);
        Optional<Method> With = method(Piece, "withPart", BlockState.class, int.class);
        Optional<Method> Kind = method(Piece, "piece");
        if (Place.isEmpty() || With.isEmpty() || Kind.isEmpty()) {
            return null;
        }
        Object Type = Kind.get().invoke(Piece);
        Optional<Method> Count = Type == null ? Optional.empty() : method(Type, "parts");
        if (Count.isEmpty()) {
            return null;
        }
        int Parts = (Integer) Count.get().invoke(Type);
        Direction Facing = facingOf(State);
        List<Part> Found = new ArrayList<>();
        for (int Index = 0; Index < Math.max(1, Parts); Index++) {
            BlockPos Offset = (BlockPos) Place.get().invoke(Piece, BlockPos.ZERO, Facing, Index);
            BlockState Made = Parts > 1 ? (BlockState) With.get().invoke(Piece, State, Index) : State;
            Found.add(new Part(Offset.getX(), Offset.getY(), Offset.getZ(), Made));
        }
        return Found;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Part> paired(BlockState State) throws ReflectiveOperationException {
        Block Piece = State.getBlock();
        Optional<Method> Kind = method(Piece, "piece");
        if (Kind.isEmpty()) {
            return null;
        }
        Object Type = Kind.get().invoke(Piece);
        Optional<Object> Reach = Type == null ? Optional.empty() : field(Type, "Reach");
        Property<?> Part0 = null;
        for (Property<?> Candidate : State.getProperties()) {
            if (Candidate.getName().equals("part")) {
                Part0 = Candidate;
            }
        }
        if (Reach.isEmpty() || Part0 == null) {
            return null;
        }
        String Span = ((Enum<?>) Reach.get()).name();
        Optional<?> Main = Part0.getValue("main");
        Optional<?> Extra = Part0.getValue("extra");
        if (Main.isEmpty() || Extra.isEmpty()) {
            return null;
        }
        BlockState First = State.setValue((Property) Part0, (Comparable) Main.get());
        if (Span.equals("None")) {
            return List.of(new Part(0, 0, 0, First));
        }
        BlockPos Offset = Span.equals("Up") ? BlockPos.ZERO.above() : BlockPos.ZERO.relative(facingOf(State).getClockWise());
        return List.of(new Part(0, 0, 0, First), new Part(Offset.getX(), Offset.getY(), Offset.getZ(), State.setValue((Property) Part0, (Comparable) Extra.get())));
    }
}
