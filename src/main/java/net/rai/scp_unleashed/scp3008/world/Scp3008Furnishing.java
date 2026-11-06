package net.rai.scp_unleashed.scp3008.world;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.loot.LootTable;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Department;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Canvas;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Hash;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Standins;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp3008Furnishing {
    public static final double ExtraLootChance = 0.4;
    public static final ResourceKey<LootTable> FoodCourtLoot = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chests/scp3008/food_court"));
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<String, Optional<BlockState>> Standins = new ConcurrentHashMap<>();

    private Scp3008Furnishing() {
    }

    private static final class DecorCanvas implements Scp3008Canvas {
        private final WorldGenLevel Level;
        private final int MinX;
        private final int MinZ;
        private final long Seed;

        private final int MaxX;
        private final int MaxZ;

        private DecorCanvas(WorldGenLevel Level, int MinX, int MinZ, int MaxX, int MaxZ) {
            this.Level = Level;
            this.MinX = MinX;
            this.MinZ = MinZ;
            this.MaxX = MaxX;
            this.MaxZ = MaxZ;
            this.Seed = Level.getSeed();
        }

        @Override
        public int minX() {
            return MinX;
        }

        @Override
        public int minZ() {
            return MinZ;
        }

        @Override
        public int maxX() {
            return MaxX;
        }

        @Override
        public int maxZ() {
            return MaxZ;
        }

        @Override
        public void put(int X, int Y, int Z, int Code) {
            if (!Scp3008Code.decor(Code)) {
                return;
            }
            try {
                place(Level, new BlockPos(X, Y, Z), Code, Seed);
            } catch (RuntimeException Problem) {
                Log.warn("SCP-3008 could not furnish {}, {}, {}", X, Y, Z, Problem);
            }
        }
    }

    public static final double ShowroomLootChance = 0.7;

    public static void decorate(WorldGenLevel Level, ChunkAccess Chunk) {
        ChunkPos Pos = Chunk.getPos();
        decorateArea(Level, Pos.getMinBlockX(), Pos.getMinBlockZ(), Pos.getMaxBlockX(), Pos.getMaxBlockZ());
    }

    public static void decorateArea(WorldGenLevel Level, int MinX, int MinZ, int MaxX, int MaxZ) {
        DecorCanvas Canvas = new DecorCanvas(Level, MinX, MinZ, MaxX, MaxZ);
        Scp3008Painter.paintArea(MinX, MinZ, MaxX, MaxZ, Canvas);
        showrooms(Level, MinX, MinZ, MaxX, MaxZ);
    }

    public static void showrooms(WorldGenLevel Level, int MinX, int MinZ, int MaxX, int MaxZ) {
        long Seed = Level.getSeed();
        for (int Ux = Scp3008Plan.unitX(MinX); Ux <= Scp3008Plan.unitX(MaxX); Ux++) {
            for (int Uz = Scp3008Plan.unitZ(MinZ); Uz <= Scp3008Plan.unitZ(MaxZ); Uz++) {
                int Z0 = Scp3008Plan.originZ(Uz);
                for (Scp3008Plan.Plot Next : Scp3008Plan.plots(Ux, Uz)) {
                    if (Next.Type() != Scp3008Plan.RoomPlot || Z0 + Next.Start() > MaxZ || Z0 + Next.Start() + Next.Length() < MinZ) {
                        continue;
                    }
                    for (Scp3008Showrooms.Placement Piece : Scp3008Showrooms.plot(Ux, Uz, Next)) {
                        BlockPos Pos = Piece.Pos();
                        if (Pos.getX() < MinX || Pos.getX() > MaxX || Pos.getZ() < MinZ || Pos.getZ() > MaxZ) {
                            continue;
                        }
                        try {
                            Level.setBlock(Pos, Piece.State(), Block.UPDATE_CLIENTS);
                            long Mixed = lootSeed(Seed, Pos);
                            if (Piece.Main() && Level.getBlockEntity(Pos) instanceof RandomizableContainer && Scp3008Hash.chance((int) (Mixed >>> 20), ShowroomLootChance)) {
                                RandomizableContainer.setBlockEntityLootTable(Level, RandomSource.create(Mixed), Pos, lootAt(Pos.getX(), Pos.getZ()));
                            }
                        } catch (RuntimeException Problem) {
                            Log.warn("SCP-3008 could not place {} at {}", Piece.State(), Pos, Problem);
                        }
                    }
                }
            }
        }
    }

    public static ResourceKey<LootTable> lootAt(int X, int Z) {
        return Scp3008Department.of(Scp3008Plan.departmentAt(X, Z)).Loot;
    }

    public static long lootSeed(long Seed, BlockPos Pos) {
        return Seed ^ (long) Scp3008Hash.mix(Pos.getX(), Pos.getY(), Pos.getZ(), 3008) << 16 ^ Scp3008Hash.mix(Pos.getZ(), Pos.getX(), Pos.getY());
    }

    public static void place(WorldGenLevel Level, BlockPos Pos, int Code, long Seed) {
        int Kind = Scp3008Code.kind(Code);
        if (Kind == Scp3008Code.Crate) {
            BlockState State = Scp3008Palette.state(Code);
            Level.setBlock(Pos, State, Block.UPDATE_CLIENTS);
            int Variant = Scp3008Code.variant(Code);
            if (Scp3008Code.crateLoot(Variant)) {
                ResourceKey<LootTable> Table = Scp3008Code.crateFoodCourt(Variant) ? FoodCourtLoot : lootAt(Pos.getX(), Pos.getZ());
                RandomizableContainer.setBlockEntityLootTable(Level, RandomSource.create(lootSeed(Seed, Pos)), Pos, Table);
            }
            return;
        }
        if (Kind == Scp3008Code.Counter) {
            Level.setBlock(Pos, Scp3008Palette.state(Code), Block.UPDATE_CLIENTS);
            return;
        }
        if (Kind == Scp3008Code.Furniture) {
            furnish(Level, Pos, Code, Seed);
        }
    }

    public static Direction facingOf(int Code) {
        return Scp3008Palette.direction(Code);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockState face(BlockState State, Direction Facing) {
        if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return State.setValue(BlockStateProperties.HORIZONTAL_FACING, Facing);
        }
        if (State.hasProperty(BlockStateProperties.FACING)) {
            return State.setValue(BlockStateProperties.FACING, Facing);
        }
        for (Property<?> Candidate : State.getProperties()) {
            if (Candidate.getName().equals("facing") && Candidate.getValueClass() == Direction.class && Candidate.getPossibleValues().contains(Facing)) {
                return State.setValue((Property) Candidate, (Comparable) Facing);
            }
        }
        return State;
    }

    public static Optional<BlockState> standin(String Text) {
        return Standins.computeIfAbsent(Text, Key -> {
            int Open = Key.indexOf('[');
            String Id = Open < 0 ? Key : Key.substring(0, Open);
            ResourceLocation Location = ResourceLocation.tryParse(Id);
            if (Location == null || !BuiltInRegistries.BLOCK.containsKey(Location)) {
                return Optional.empty();
            }
            BlockState State = BuiltInRegistries.BLOCK.get(Location).defaultBlockState();
            if (Open >= 0 && Key.endsWith("]")) {
                for (String Pair : Key.substring(Open + 1, Key.length() - 1).split(",")) {
                    String[] Parts = Pair.split("=");
                    if (Parts.length == 2) {
                        State = with(State, Parts[0].trim(), Parts[1].trim());
                    }
                }
            }
            return Optional.of(State);
        });
    }

    private static <T extends Comparable<T>> BlockState withValue(BlockState State, Property<T> Candidate, String Value) {
        Optional<T> Parsed = Candidate.getValue(Value);
        return Parsed.map(Found -> State.setValue(Candidate, Found)).orElse(State);
    }

    private static BlockState with(BlockState State, String Name, String Value) {
        for (Property<?> Candidate : State.getProperties()) {
            if (Candidate.getName().equals(Name)) {
                return withValue(State, Candidate, Value);
            }
        }
        return State;
    }

    public static BlockState standinFor(int Code) {
        int Variant = Scp3008Code.variant(Code);
        String Text = Scp3008Standins.pick(Variant);
        Optional<BlockState> Found = standin(Text);
        if (Found.isEmpty()) {
            return null;
        }
        Direction Facing = facingOf(Code);
        return face(Found.get(), Scp3008Standins.reversed(Text) ? Facing.getOpposite() : Facing);
    }

    public static BlockState resolve(BlockPos Pos, int Code) {
        int Variant = Scp3008Code.variant(Code);
        int Department = Scp3008Code.slotDepartment(Variant);
        int Role = Scp3008Code.slotRole(Variant);
        List<Block> Candidates = Scp3008Catalogue.candidates(Department, Role);
        if (Candidates.isEmpty()) {
            return standinFor(Code);
        }
        int Index = Scp3008Hash.pick(Scp3008Hash.mix(Pos.getX(), Pos.getY(), Pos.getZ(), Scp3008Code.slotSalt(Variant)), Candidates.size());
        return numbered(face(Candidates.get(Index).defaultBlockState(), facingOf(Code)), Pos);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static BlockState numbered(BlockState State, BlockPos Pos) {
        for (Property<?> Candidate : State.getProperties()) {
            if ((Candidate.getName().contains("number") || Candidate.getName().contains("aisle")) && Candidate instanceof IntegerProperty Numbers) {
                List<Integer> Values = List.copyOf(Numbers.getPossibleValues());
                int Aisle = Scp3008Plan.unitX(Pos.getX()) * 7 + Scp3008Plan.unitZ(Pos.getZ()) * 13 + (Pos.getZ() - Scp3008Plan.originZ(Scp3008Plan.unitZ(Pos.getZ()))) / 10;
                return State.setValue((Property) Numbers, (Comparable) Values.get(Math.floorMod(Aisle, Values.size())));
            }
        }
        return State;
    }

    public static void furnish(WorldGenLevel Level, BlockPos Pos, int Code, long Seed) {
        BlockState State = resolve(Pos, Code);
        if (State == null || State.isAir()) {
            return;
        }
        List<Scp3008Footprint.Part> Parts = Scp3008Footprint.parts(State);
        for (Scp3008Footprint.Part Next : Parts) {
            if (!Level.getBlockState(Next.at(Pos)).canBeReplaced()) {
                return;
            }
        }
        for (Scp3008Footprint.Part Next : Parts) {
            Level.setBlock(Next.at(Pos), Next.State(), Block.UPDATE_CLIENTS);
        }
        int Role = Scp3008Code.slotRole(Scp3008Code.variant(Code));
        long Mixed = lootSeed(Seed, Pos);
        if (Level.getBlockEntity(Pos) instanceof RandomizableContainer && (Role == Scp3008Code.RoleContainer || Scp3008Hash.chance((int) (Mixed >>> 20), ExtraLootChance))) {
            RandomizableContainer.setBlockEntityLootTable(Level, RandomSource.create(Mixed), Pos, lootAt(Pos.getX(), Pos.getZ()));
        }
    }
}
