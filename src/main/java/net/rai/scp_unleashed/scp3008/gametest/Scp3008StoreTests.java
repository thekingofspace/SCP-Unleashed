package net.rai.scp_unleashed.scp3008.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.block.Scp3008CableBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008RafterBlock;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Canvas;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.world.Scp3008Healing;
import net.rai.scp_unleashed.scp3008.world.Scp3008Palette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008StoreTests {
    public static final String MiddleDump = "SCP3008_HEAL_DUMP_R3";
    public static final String OldDump = "SCP3008_HEAL_DUMP_OLD";
    public static final String NewDump = "SCP3008_HEAL_DUMP_NEW";
    public static final int ViewTop = Scp3008Plan.LightY + 24;
    public static final int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private Scp3008StoreTests() {
    }

    private static final class Codes implements Scp3008Canvas {
        private final int MinX;
        private final int MinZ;
        private final int MaxX;
        private final int MaxZ;
        private final Map<Long, Integer> All = new HashMap<>();

        private Codes(int MinX, int MinZ, int MaxX, int MaxZ) {
            this.MinX = MinX;
            this.MinZ = MinZ;
            this.MaxX = MaxX;
            this.MaxZ = MaxZ;
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
                All.put(BlockPos.asLong(X, Y, Z), Code);
            }
        }

        private int kind(BlockPos Pos) {
            Integer Code = All.get(Pos.asLong());
            return Code == null ? -1 : Scp3008Code.kind(Code);
        }

        private int variant(BlockPos Pos) {
            Integer Code = All.get(Pos.asLong());
            return Code == null ? -1 : Scp3008Code.variant(Code);
        }
    }

    private static Codes unit(int Ux, int Uz) {
        int X0 = Scp3008Plan.originX(Ux);
        int Z0 = Scp3008Plan.originZ(Uz);
        Codes Made = new Codes(X0, Z0, X0 + Scp3008Plan.Unit - 1, Z0 + Scp3008Plan.Row - 1);
        Scp3008Painter.paint(Ux, Uz, Made);
        return Made;
    }

    private static boolean clamp(Codes Plan, BlockPos Pos) {
        return Plan.kind(Pos) == Scp3008Code.Cable && Plan.variant(Pos) == Scp3008Code.CableClamp;
    }

    private static int bars(GameTestHelper Helper, Codes Plan, String Name) {
        int Bars = 0;
        for (Map.Entry<Long, Integer> Entry : Plan.All.entrySet()) {
            BlockPos Pos = BlockPos.of(Entry.getKey());
            int Kind = Scp3008Code.kind(Entry.getValue());
            if (Kind == Scp3008Code.Cable) {
                int Fit = Scp3008Code.variant(Entry.getValue());
                int Above = Plan.kind(Pos.above());
                int Below = Plan.kind(Pos.below());
                Helper.assertTrue(Above == Scp3008Code.Cable || Above == Scp3008Code.Ceiling, Name + " has a cable ending in mid-air at " + Pos);
                Helper.assertTrue(Fit != Scp3008Code.CableCanopy || Above == Scp3008Code.Ceiling, Name + " has a ceiling plate below the ceiling at " + Pos);
                if (Fit == Scp3008Code.CableClamp) {
                    Helper.assertTrue(Below == Scp3008Code.Light || Below == Scp3008Code.Sign, Name + " has a cable clamp holding nothing at " + Pos);
                } else {
                    Helper.assertTrue(Below == Scp3008Code.Cable, Name + " has a cable without a clamp at its foot at " + Pos);
                }
                continue;
            }
            if (Kind == Scp3008Code.Emergency) {
                Direction Facing = Scp3008Palette.direction(Entry.getValue());
                int Behind = Plan.kind(Pos.relative(Facing.getOpposite()));
                Helper.assertTrue(Behind == Scp3008Code.Pillar || Behind == Scp3008Code.ExitWall, Name + " has an emergency light with nothing behind it at " + Pos);
                continue;
            }
            if (Kind != Scp3008Code.Light && Kind != Scp3008Code.Sign) {
                continue;
            }
            if (Kind == Scp3008Code.Sign && Plan.kind(Pos.above()) == Scp3008Code.Sign) {
                continue;
            }
            Direction Along = Plan.kind(Pos.east()) == Kind || Plan.kind(Pos.west()) == Kind ? Direction.EAST : Direction.SOUTH;
            if (Plan.kind(Pos.relative(Along.getOpposite())) == Kind) {
                continue;
            }
            int Hung = 0;
            for (BlockPos At = Pos; Plan.kind(At) == Kind; At = At.relative(Along)) {
                if (clamp(Plan, At.above())) {
                    Hung++;
                }
                if (Kind == Scp3008Code.Light) {
                    for (BlockPos Near : BlockPos.betweenClosed(At.offset(-1, -1, -1), At.offset(1, 1, 1))) {
                        int Other = Plan.kind(Near);
                        Helper.assertTrue(Other < 0 || Other == Scp3008Code.Light || Other == Scp3008Code.Cable,
                                Name + " has a light bar touching " + Other + " at " + Near.immutable());
                    }
                }
            }
            Helper.assertTrue(Hung >= 2, Name + " has a " + (Kind == Scp3008Code.Light ? "light bar" : "sign") + " at " + Pos + " hanging from " + Hung + " cables");
            Bars++;
        }
        return Bars;
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 400, batch = "scp3008_core_lights")
    public static void lightsHangFromCables(GameTestHelper Helper) {
        BlockState Light = Scp3008Palette.state(Scp3008Code.of(Scp3008Code.Light, 0));
        BlockState Clamp = Scp3008Palette.state(Scp3008Code.of(Scp3008Code.Cable, Scp3008Code.CableClamp));
        BlockState Sign = Scp3008Registry.DepartmentSign.get().defaultBlockState();
        Helper.assertTrue(Light.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).max(Direction.Axis.Y) > 0.999, "the light bar does not reach up to its cable");
        Helper.assertTrue(Sign.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).max(Direction.Axis.Y) > 0.999, "the department sign does not reach up to its cable");
        Helper.assertTrue(Clamp.getValue(Scp3008CableBlock.Fit) == Scp3008CableBlock.Fitting.Clamp
                && Clamp.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).min(Direction.Axis.Y) < 0.001, "the cable clamp does not sit on the fixture below");
        Set<Long> Units = new LinkedHashSet<>();
        Units.add(ChunkPos.asLong(0, 0));
        Units.add(ChunkPos.asLong(1, 1));
        for (int Type : new int[]{Scp3008Plan.FoodPlot, Scp3008Plan.ExitPlot, Scp3008Plan.RackPlot, Scp3008Plan.RoomPlot}) {
            Scp3008ShowroomTests.Site Found = Scp3008ShowroomTests.anyPlot(Type);
            Helper.assertTrue(Found != null, "no plot of type " + Type + " nearby");
            Units.add(ChunkPos.asLong(Found.Ux(), Found.Uz()));
        }
        int Bars = 0;
        for (long Key : Units) {
            ChunkPos At = new ChunkPos(Key);
            Bars += bars(Helper, unit(At.x, At.z), "unit " + At.x + ", " + At.z);
        }
        Helper.assertTrue(Bars >= Units.size() * 10, "only " + Bars + " light bars and signs were checked");
        Helper.succeed();
    }

    public static Scp3008ShowroomTests.Site litRoom() {
        for (int Department = 0; Department < Scp3008Plan.Warehouse; Department++) {
            Scp3008ShowroomTests.Site Found = Scp3008ShowroomTests.find(Department, Scp3008Plan.RoomPlot);
            if (Found == null) {
                continue;
            }
            int S = Found.Plot().Start();
            for (int Lz = Scp3008Painter.LightFirst; Lz < Scp3008Plan.Row; Lz += Scp3008Painter.LightStep) {
                if (Lz > S + 1 && Lz < S + Found.Plot().Length() - 2) {
                    return Found;
                }
            }
        }
        return null;
    }

    private static List<ChunkPos> chunks(Scp3008ShowroomTests.Site Area) {
        List<ChunkPos> Found = new ArrayList<>();
        for (int Cx = Area.minX() >> 4; Cx <= Area.maxX() >> 4; Cx++) {
            for (int Cz = Area.minZ() >> 4; Cz <= Area.maxZ() >> 4; Cz++) {
                Found.add(new ChunkPos(Cx, Cz));
            }
        }
        return Found;
    }

    private static Map<BlockPos, BlockState> hung(ServerLevel World, Scp3008ShowroomTests.Site Area) {
        Map<BlockPos, BlockState> Found = new HashMap<>();
        for (BlockPos Pos : BlockPos.betweenClosed(Area.minX(), Scp3008Plan.Stand, Area.minZ() + 1, Area.maxX(), Scp3008Plan.CeilingY - 1, Area.maxZ() - 1)) {
            BlockState State = World.getBlockState(Pos);
            if (State.is(Scp3008Registry.Rafter.get()) || State.is(Scp3008Registry.Cable.get()) || State.is(Scp3008Registry.StoreLight.get())) {
                Found.put(Pos.immutable(), State);
            }
        }
        return Found;
    }

    private static int depth(int X) {
        int Lx = Scp3008Plan.localX(X);
        return Lx < 25 ? Lx - Scp3008Plan.Lane : Scp3008Plan.Unit - 1 - Lx;
    }

    private static void dump(GameTestHelper Helper, String Variable, ServerLevel World, Scp3008ShowroomTests.Site Area) {
        String Target = System.getenv(Variable);
        if (Target != null && !Target.isBlank()) {
            try {
                java.nio.file.Files.write(java.nio.file.Path.of(Target), Scp3008ShowroomTests.scan(World, Area, ViewTop), java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException Problem) {
                Helper.fail("could not write the heal dump: " + Problem.getMessage());
            }
        }
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 2400, batch = "scp3008_core_heal")
    public static void oldChunksHeal(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008ShowroomTests.Site Area = litRoom();
        Helper.assertTrue(Area != null, "no showroom under a light row nearby");
        Scp3008ShowroomTests.build(World, Area, Scp3008Plan.CeilingTop);
        Map<BlockPos, BlockState> Expected = hung(World, Area);
        BlockState OldCable = Scp3008Registry.Cable.get().defaultBlockState();
        List<BlockPos> OldColumns = new ArrayList<>();
        int Removed = 0;
        for (Map.Entry<BlockPos, BlockState> Entry : Expected.entrySet()) {
            BlockState State = Entry.getValue();
            BlockPos Pos = Entry.getKey();
            if (!State.is(Scp3008Registry.Cable.get())) {
                continue;
            }
            int Lz = Scp3008Plan.localZ(Pos.getZ());
            boolean LightRow = Pos.getY() > Scp3008Plan.LightY && Lz >= Scp3008Painter.LightFirst && (Lz - Scp3008Painter.LightFirst) % Scp3008Painter.LightStep == 0;
            World.setBlock(Pos, LightRow ? Blocks.AIR.defaultBlockState() : OldCable, Flags);
            Removed += LightRow ? 1 : 0;
            if (LightRow && Pos.getY() == Scp3008Plan.LightY + 1) {
                for (int Lx : Scp3008Healing.OldHangerX) {
                    BlockPos Column = new BlockPos(Scp3008Plan.originX(Area.Ux()) + Lx, Pos.getY(), Pos.getZ());
                    if (!OldColumns.contains(Column)) {
                        OldColumns.add(Column);
                    }
                }
            }
        }
        Helper.assertTrue(Removed > 0 && !OldColumns.isEmpty(), "the room has no light cables to age");
        for (BlockPos Column : OldColumns) {
            for (int Y = Scp3008Plan.LightY + 1; Y < Scp3008Plan.CeilingY; Y++) {
                World.setBlock(Column.atY(Y), OldCable, Flags);
            }
        }
        dump(Helper, MiddleDump, World, Area);
        for (Map.Entry<BlockPos, BlockState> Entry : Expected.entrySet()) {
            if (!Entry.getValue().is(Scp3008Registry.Rafter.get())) {
                continue;
            }
            int Arms = Scp3008RafterBlock.arms(Entry.getValue());
            int Depth = depth(Entry.getKey().getX());
            boolean NewBeam = Arms == (Scp3008RafterBlock.NorthArm | Scp3008RafterBlock.SouthArm) && Depth != Scp3008Painter.CrossDepth;
            World.setBlock(Entry.getKey(), NewBeam ? Blocks.AIR.defaultBlockState() : Scp3008Registry.Rafter.get().withArms(0), Flags);
        }
        dump(Helper, OldDump, World, Area);
        for (ChunkPos Chunk : chunks(Area)) {
            Scp3008Healing.heal(World, Chunk);
        }
        for (Map.Entry<BlockPos, BlockState> Entry : Expected.entrySet()) {
            BlockState Now = World.getBlockState(Entry.getKey());
            Helper.assertTrue(Now.equals(Entry.getValue()), "healing left " + Now + " at " + Entry.getKey() + " instead of " + Entry.getValue());
        }
        for (BlockPos Column : OldColumns) {
            for (int Y = Scp3008Plan.LightY + 1; Y < Scp3008Plan.CeilingY; Y++) {
                Helper.assertTrue(World.getBlockState(Column.atY(Y)).isAir(), "healing kept the old middle cable at " + Column.atY(Y));
            }
        }
        Scp3008ShowroomTests.rafters(Helper, World, Area, "healed room");
        dump(Helper, NewDump, World, Area);
        int Again = 0;
        for (ChunkPos Chunk : chunks(Area)) {
            Again += Scp3008Healing.heal(World, Chunk);
        }
        Helper.assertTrue(Again == 0, "healing a healed room changed " + Again + " more blocks");
        Helper.succeed();
    }

    @GameTest(template = Scp3008GameTests.Arena, timeoutTicks = 2400, batch = "scp3008_core_regen")
    public static void regenRebuildsChunks(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp3008ShowroomTests.Site Area = Scp3008ShowroomTests.find(Scp3008Plan.Living, Scp3008Plan.RoomPlot);
        Helper.assertTrue(Area != null, "no living room nearby");
        Scp3008ShowroomTests.build(World, Area, Scp3008Plan.CeilingTop);
        List<ChunkPos> Chunks = chunks(Area);
        for (ChunkPos Chunk : Chunks) {
            Scp3008Healing.rebuild(World, Chunk, true);
        }
        Scp3008Healing.Report Clean = total(World, Chunks, false);
        Helper.assertTrue(Clean.Changed() == 0, "a freshly rebuilt area still differs from the plan in " + Clean.Changed() + " blocks");
        int X0 = Scp3008Plan.originX(Area.Ux());
        int Z0 = Scp3008Plan.originZ(Area.Uz());
        int S = Area.Plot().Start();
        BlockPos Build = new BlockPos(X0 + Scp3008Painter.roomX(true, 2), Scp3008Plan.Stand + 3, Z0 + S + 2);
        BlockPos Wall = new BlockPos(X0 + Scp3008Painter.roomX(true, 7), Scp3008Plan.Stand + 1, Z0 + S);
        BlockPos Beam = new BlockPos(X0 + Scp3008Painter.roomX(true, 9), Scp3008Painter.RafterY, Z0 + S);
        BlockState WallState = World.getBlockState(Wall);
        BlockState BeamState = World.getBlockState(Beam);
        Helper.assertTrue(WallState.is(Scp3008Registry.DisplayWall.get()) && BeamState.is(Scp3008Registry.Rafter.get()), "the room is missing its wall or rafter");
        World.setBlock(Build, Blocks.STONE.defaultBlockState(), Flags);
        World.setBlock(Wall, Blocks.AIR.defaultBlockState(), Flags);
        World.setBlock(Beam, Scp3008Registry.Rafter.get().withArms(0), Flags);
        Scp3008Healing.Report Preview = total(World, Chunks, false);
        Helper.assertTrue(Preview.Changed() == 3 && Preview.Foreign() == 1, "the preview counted " + Preview.Changed() + " changes and " + Preview.Foreign() + " foreign blocks");
        Helper.assertTrue(World.getBlockState(Build).is(Blocks.STONE), "the preview changed the world");
        total(World, Chunks, true);
        Helper.assertTrue(World.getBlockState(Build).isAir(), "the rebuild kept a foreign block");
        Helper.assertTrue(World.getBlockState(Wall).equals(WallState), "the rebuild did not restore the wall");
        Helper.assertTrue(World.getBlockState(Beam).equals(BeamState), "the rebuild did not restore the rafter");
        Scp3008Healing.Report After = total(World, Chunks, false);
        Helper.assertTrue(After.Changed() == 0, "the rebuilt area still differs from the plan in " + After.Changed() + " blocks");
        Helper.succeed();
    }

    private static Scp3008Healing.Report total(ServerLevel World, List<ChunkPos> Chunks, boolean Apply) {
        Scp3008Healing.Report Sum = new Scp3008Healing.Report(0, 0, 0, 0);
        for (ChunkPos Chunk : Chunks) {
            Sum = Sum.plus(Scp3008Healing.rebuild(World, Chunk, Apply));
        }
        return Sum;
    }
}
