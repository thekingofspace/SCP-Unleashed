package net.rai.scp_unleashed.scp001day.gametest;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001day.Scp001DayClientConfig;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayGridStore;
import net.rai.scp_unleashed.scp001day.sun.Scp001DayRay;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySunUpload;
import net.rai.scp_unleashed.scp001day.sun.SunVector;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp001DayNativeGameTests {
    public static final String Arena = Scp001DayGameTests.Arena;
    public static final byte Poison = (byte) 0x5A;
    public static final int Unset = Integer.MIN_VALUE;
    public static final int LoopCeiling = 4096;
    public static final List<String> Shaders = List.of("pinwheel/shaders/include/scp001day/sun_trace.glsl", "pinwheel/shaders/program/scp001day/sun_shafts.fsh",
            "pinwheel/shaders/program/scp001day/sun_volume.fsh", "pinwheel/shaders/program/scp001day/sun_composite.fsh");
    private static final Pattern ForHeader = Pattern.compile("\\bfor\\s*\\(([^;]*);([^;]*);([^)]*)\\)");
    private static final Pattern ForInit = Pattern.compile("\\s*int\\s+(\\w+)\\s*=\\s*-?\\d+\\s*");
    private static final Pattern ForTest = Pattern.compile("\\s*(\\w+)\\s*(<=|<)\\s*(\\w+)\\s*");
    private static final Pattern ForStep = Pattern.compile("\\s*(?:(\\w+)\\s*\\+\\+|\\+\\+\\s*(\\w+)|(\\w+)\\s*\\+=\\s*1)\\s*");
    private static final Pattern Constant = Pattern.compile("\\bconst\\s+int\\s+(\\w+)\\s*=\\s*(\\d+)\\s*;");
    private static final Pattern OtherLoop = Pattern.compile("\\bwhile\\s*\\(|\\bdo\\s*\\{");

    private Scp001DayNativeGameTests() {
    }

    private static final class Tracked implements Scp001DayGridStore.Memory {
        private final Map<ByteBuffer, Boolean> Held = new IdentityHashMap<>();
        private final List<ByteBuffer> Freed = new ArrayList<>();
        private final Set<Thread> Freers = ConcurrentHashMap.newKeySet();
        private int Allocations;
        private int Frees;
        private int Peak;
        private String Problem;

        @Override
        public synchronized ByteBuffer alloc(int Bytes) {
            ByteBuffer Made = ByteBuffer.allocate(Bytes);
            Arrays.fill(Made.array(), (byte) 0x33);
            return hold(Made);
        }

        @Override
        public synchronized ByteBuffer calloc(int Bytes) {
            return hold(ByteBuffer.allocate(Bytes));
        }

        private ByteBuffer hold(ByteBuffer Made) {
            Held.put(Made, Boolean.TRUE);
            Allocations++;
            Peak = Math.max(Peak, Held.size());
            return Made;
        }

        @Override
        public synchronized void free(ByteBuffer Buffer) {
            if (Buffer == null || Held.remove(Buffer) == null) {
                Problem = "a buffer was freed twice or was never allocated";
                return;
            }
            Arrays.fill(Buffer.array(), Poison);
            Freed.add(Buffer);
            Frees++;
            Freers.add(Thread.currentThread());
        }

        synchronized int outstanding() {
            return Held.size();
        }

        synchronized boolean freed(ByteBuffer Buffer) {
            for (ByteBuffer Each : Freed) {
                if (Each == Buffer) {
                    return true;
                }
            }
            return false;
        }

        synchronized String touchedAfterFree() {
            for (ByteBuffer Each : Freed) {
                for (byte Value : Each.array()) {
                    if (Value != Poison) {
                        return "a freed grid buffer was written after it was freed";
                    }
                }
            }
            return null;
        }
    }

    private static final class Synthetic implements Scp001DayGridStore.Cells {
        private final Long2IntOpenHashMap Overrides = new Long2IntOpenHashMap();
        private int Calls;

        Synthetic() {
            Overrides.defaultReturnValue(Unset);
        }

        static boolean unloaded(int X, int Z) {
            return Math.floorMod((X >> 4) * 7 + (Z >> 4) * 3, 11) == 0;
        }

        int base(int X, int Y, int Z) {
            if (Y < 60) {
                return Scp001DayRay.Solid;
            }
            if (Y == 60 && (X & 7) == 0) {
                return Scp001DayRay.cell(0x0F, 255);
            }
            if (Y > 60 && Y < 72 && Math.floorMod(X, 13) == 0 && Math.floorMod(Z, 11) == 0) {
                return Scp001DayRay.cell(0, 191);
            }
            return Scp001DayRay.Clear;
        }

        void set(int X, int Y, int Z, int Cell) {
            Overrides.put(BlockPos.asLong(X, Y, Z), Cell);
        }

        @Override
        public int cell(int X, int Y, int Z) {
            int Found = Overrides.get(BlockPos.asLong(X, Y, Z));
            return Found == Unset ? base(X, Y, Z) : Found;
        }

        int expected(int X, int Y, int Z) {
            return unloaded(X, Z) ? Scp001DayRay.Clear : cell(X, Y, Z);
        }

        @Override
        public boolean column(int X, int Z, int Y0, int[] Out) {
            Calls++;
            boolean Loaded = !unloaded(X, Z);
            for (int Ly = 0; Ly < Out.length; Ly++) {
                Out[Ly] = Loaded ? cell(X, Y0 + Ly, Z) : Scp001DayRay.Clear;
            }
            return Loaded;
        }
    }

    private static SunVector noon() {
        return Scp001DaySun.direction(Scp001DaySun.NoonTicks);
    }

    private static int settle(GameTestHelper Helper, Scp001DayGridStore Store, Tracked Pool, Object Dimension, double X, double Y, double Z, Synthetic World, int Cap) {
        int Steps = 0;
        do {
            Store.step(Dimension, X, Y, Z, noon(), World, Long.MAX_VALUE, Cap);
            Steps++;
            if (Pool.outstanding() > 3) {
                Helper.fail("the sun grid held " + Pool.outstanding() + " buffers at once");
            }
            if (Pool.Problem != null) {
                Helper.fail(Pool.Problem);
            }
            ByteBuffer Shown = Store.live();
            if (Shown != null && Pool.freed(Shown)) {
                Helper.fail("the sun grid exposed a freed buffer");
            }
        } while ((Store.building() || !Store.ready()) && Steps < 200);
        Helper.assertTrue(Store.ready() && !Store.building(), "the sun grid never finished building");
        return Steps;
    }

    private static void checkContents(GameTestHelper Helper, Scp001DayGridStore Store, Synthetic World, RandomSource Random, int Samples) {
        ByteBuffer Shown = Store.live();
        Helper.assertTrue(Shown != null && Shown.capacity() == Scp001DayGridStore.GridBytes, "the live grid is missing or the wrong size");
        for (int Sample = 0; Sample < Samples; Sample++) {
            int Lx = Random.nextInt(Scp001DayGridStore.Span);
            int Ly = Random.nextInt(Scp001DayGridStore.Span);
            int Lz = Random.nextInt(Scp001DayGridStore.Span);
            int Want = World.expected(Store.originX() + Lx, Store.originY() + Ly, Store.originZ() + Lz);
            int Offset = Scp001DayGridStore.index(Lx, Ly, Lz);
            if ((Shown.get(Offset) & 255) != Scp001DayRay.mask(Want) || (Shown.get(Offset + 1) & 255) != Scp001DayRay.filter(Want)) {
                Helper.fail("grid cell " + Lx + " " + Ly + " " + Lz + " does not match the world");
            }
        }
        checkCoarse(Helper, Store);
    }

    private static void checkCoarse(GameTestHelper Helper, Scp001DayGridStore Store) {
        ByteBuffer Shown = Store.live();
        ByteBuffer Coarse = Store.coarse();
        int[] Counts = new int[Scp001DayGridStore.CoarseBytes];
        for (int Lz = 0; Lz < Scp001DayGridStore.Span; Lz++) {
            for (int Ly = 0; Ly < Scp001DayGridStore.Span; Ly++) {
                for (int Lx = 0; Lx < Scp001DayGridStore.Span; Lx++) {
                    if (!Scp001DayGridStore.empty(Shown, Scp001DayGridStore.index(Lx, Ly, Lz))) {
                        Counts[Scp001DayGridStore.coarseIndex(Lx, Ly, Lz)]++;
                    }
                }
            }
        }
        for (int Index = 0; Index < Counts.length; Index++) {
            if (Coarse.get(Index) != (byte) (Counts[Index] > 0 ? 1 : 0)) {
                Helper.fail("coarse cell " + Index + " disagrees with the grid");
            }
        }
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "snativecrash_gridIndex")
    public static void gridIndex(GameTestHelper Helper) {
        int Span = Scp001DayGridStore.Span;
        BitSet Seen = new BitSet(Span * Span * Span);
        for (int Lz = 0; Lz < Span; Lz++) {
            for (int Ly = 0; Ly < Span; Ly++) {
                for (int Lx = 0; Lx < Span; Lx++) {
                    int Offset = Scp001DayGridStore.index(Lx, Ly, Lz);
                    if (Offset < 0 || Offset + 1 >= Scp001DayGridStore.GridBytes || (Offset & 1) != 0) {
                        Helper.fail("grid offset out of range at " + Lx + " " + Ly + " " + Lz);
                    }
                    if (Seen.get(Offset >> 1)) {
                        Helper.fail("two grid cells share an offset at " + Lx + " " + Ly + " " + Lz);
                    }
                    Seen.set(Offset >> 1);
                    int CoarseIndex = Scp001DayGridStore.coarseIndex(Lx, Ly, Lz);
                    if (CoarseIndex < 0 || CoarseIndex >= Scp001DayGridStore.CoarseBytes) {
                        Helper.fail("coarse index out of range at " + Lx + " " + Ly + " " + Lz);
                    }
                    if (!Scp001DayGridStore.inside(Lx, Ly, Lz)) {
                        Helper.fail("a grid cell counted as outside at " + Lx + " " + Ly + " " + Lz);
                    }
                }
            }
        }
        Helper.assertTrue(Seen.cardinality() == Span * Span * Span, "the grid offsets are not dense");
        int[] Edges = {-1, Span, Integer.MIN_VALUE, Integer.MAX_VALUE};
        for (int Edge : Edges) {
            Helper.assertTrue(!Scp001DayGridStore.inside(Edge, 0, 0) && !Scp001DayGridStore.inside(0, Edge, 0) && !Scp001DayGridStore.inside(0, 0, Edge), "an edge cell counted as inside");
        }
        Helper.assertTrue(Scp001DaySunUpload.Grid.bytes() == Scp001DayGridStore.GridBytes && Scp001DaySunUpload.Coarse.bytes() == Scp001DayGridStore.CoarseBytes,
                "the upload shapes do not match the grid buffers");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "snativecrash_unpackBounds")
    public static void unpackBounds(GameTestHelper Helper) {
        Scp001DaySunUpload.Unpack Clean = Scp001DaySunUpload.Clean;
        Helper.assertTrue(Clean.RowLength() == 0 && Clean.ImageHeight() == 0 && Clean.SkipPixels() == 0 && Clean.SkipRows() == 0 && Clean.SkipImages() == 0 && Clean.Alignment() == 1,
                "the clean unpack state is not the GL default layout");
        List<Scp001DaySunUpload.Shape> Shapes = List.of(Scp001DaySunUpload.Grid, Scp001DaySunUpload.Coarse, Scp001DaySunUpload.Far);
        for (Scp001DaySunUpload.Shape Each : Shapes) {
            Helper.assertTrue(Scp001DaySunUpload.span(Each, Clean) == Each.bytes(), "a clean upload does not read exactly its buffer: " + Each);
            Helper.assertTrue(Scp001DaySunUpload.fits(Each, Clean, Each.bytes()) && !Scp001DaySunUpload.fits(Each, Clean, Each.bytes() - 1), "the fit check is off by one: " + Each);
        }
        Helper.assertTrue(Scp001DaySunUpload.Far.bytes() == (long) Scp001DaySunUpload.FarSpan * Scp001DaySunUpload.FarSpan * 8 * Float.BYTES, "the far box shape does not match its float buffer");
        Scp001DaySunUpload.Unpack Glyph = new Scp001DaySunUpload.Unpack(128, 0, 120, 120, 0, 4);
        Helper.assertTrue(Scp001DaySunUpload.span(Scp001DaySunUpload.Grid, Glyph) > Scp001DayGridStore.GridBytes, "a leftover glyph unpack state should overread the grid");
        Helper.assertTrue(Scp001DaySunUpload.span(Scp001DaySunUpload.Coarse, Glyph) > 8L * Scp001DayGridStore.CoarseBytes, "a leftover glyph unpack state should overread the coarse grid");
        Helper.assertTrue(Scp001DaySunUpload.span(Scp001DaySunUpload.Far, Glyph) > 10L * Scp001DaySunUpload.Far.bytes(), "a leftover glyph unpack state should overread the far boxes");
        Scp001DaySunUpload.Unpack Frame = new Scp001DaySunUpload.Unpack(0, 0, 0, 496, 0, 4);
        Helper.assertTrue(Scp001DaySunUpload.span(Scp001DaySunUpload.Grid, Frame) == Scp001DayGridStore.GridBytes + 496L * Scp001DayGridStore.Span * 2,
                "a leftover animation frame unpack state should overread by its skipped rows");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "snativecrash_gridLifecycle")
    public static void gridLifecycle(GameTestHelper Helper) {
        Tracked Pool = new Tracked();
        Scp001DayGridStore Store = new Scp001DayGridStore(Pool);
        Synthetic World = new Synthetic();
        RandomSource Random = RandomSource.create(7L);
        Object Home = new Object();
        Object Away = new Object();
        int Steps = settle(Helper, Store, Pool, Home, 0.5, 64.0, 0.5, World, 4096);
        Helper.assertTrue(Steps == 4, "the grid took " + Steps + " capped steps instead of 4");
        Helper.assertTrue(Pool.outstanding() == 2, "a settled grid should hold the live grid and the coarse grid only");
        Helper.assertTrue(Store.dimension() == Home, "the grid kept the wrong dimension");
        checkContents(Helper, Store, World, Random, 4000);
        int Rounds = 12;
        for (int Round = 1; Round <= Rounds; Round++) {
            settle(Helper, Store, Pool, Home, 0.5 + Round * 37.0, 64.0 + (Round % 3) * 20.0, 0.5 - Round * 23.0, World, 3000 + Round * 100);
            Helper.assertTrue(Pool.outstanding() == 2, "the grid leaked a buffer after move " + Round);
        }
        checkContents(Helper, Store, World, Random, 4000);
        Helper.assertTrue(Pool.Allocations - Pool.Frees == 2, "allocations and frees do not balance after moves");
        Store.step(Home, 5000.5, 64.0, 5000.5, noon(), World, Long.MAX_VALUE, 1000);
        Helper.assertTrue(Store.building(), "a far move did not start a rebuild");
        int Before = Pool.Allocations;
        settle(Helper, Store, Pool, Away, 5000.5, 64.0, 5000.5, World, 5000);
        Helper.assertTrue(Pool.Allocations == Before, "switching dimension mid build allocated a new buffer");
        Helper.assertTrue(Store.dimension() == Away, "the grid did not switch dimension");
        Store.step(Away, 0.5, 64.0, 0.5, noon(), World, Long.MAX_VALUE, 1000);
        int FreesBefore = Pool.Frees;
        Thread Spammer = new Thread(() -> {
            for (int Index = 0; Index <= Scp001DayGridStore.DirtyBacklog + 10; Index++) {
                Store.markDirty(BlockPos.asLong(Index % 4096, 64, Index / 4096));
            }
        }, "snativecrash dirty spammer");
        Spammer.start();
        try {
            Spammer.join(20000L);
        } catch (InterruptedException Problem) {
            Thread.currentThread().interrupt();
        }
        Helper.assertTrue(!Spammer.isAlive(), "the dirty spammer did not finish");
        Helper.assertTrue(Pool.Frees == FreesBefore, "marking blocks dirty freed native memory off the render thread");
        Helper.assertTrue(!Store.ready(), "an overflowing dirty queue did not drop the grid");
        Helper.assertTrue(Store.pending() <= Scp001DayGridStore.DirtyBacklog, "the dirty queue grew past its backlog");
        settle(Helper, Store, Pool, Away, 0.5, 64.0, 0.5, World, 6000);
        Helper.assertTrue(Pool.Freers.size() == 1 && Pool.Freers.contains(Thread.currentThread()), "native grid memory was freed on another thread");
        checkContents(Helper, Store, World, Random, 2000);
        Store.release();
        Helper.assertTrue(Pool.outstanding() == 0 && Store.live() == null && !Store.ready() && !Store.building(), "release left native memory or state behind");
        Store.release();
        Helper.assertTrue(Pool.Problem == null, "releasing twice freed a buffer twice");
        settle(Helper, Store, Pool, Home, 0.5, 64.0, 0.5, World, Integer.MAX_VALUE);
        checkContents(Helper, Store, World, Random, 2000);
        Store.release();
        Helper.assertTrue(Pool.outstanding() == 0, "release after a rebuild left native memory behind");
        String Touched = Pool.touchedAfterFree();
        Helper.assertTrue(Touched == null, String.valueOf(Touched));
        Helper.assertTrue(Pool.Problem == null, String.valueOf(Pool.Problem));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "snativecrash_gridDirty")
    public static void gridDirty(GameTestHelper Helper) {
        Tracked Pool = new Tracked();
        Scp001DayGridStore Store = new Scp001DayGridStore(Pool);
        Synthetic World = new Synthetic();
        RandomSource Random = RandomSource.create(11L);
        settle(Helper, Store, Pool, World, 0.5, 64.0, 0.5, World, Integer.MAX_VALUE);
        int Version = Store.version();
        Helper.assertTrue(!Store.step(World, 0.5, 64.0, 0.5, noon(), World, Long.MAX_VALUE, Integer.MAX_VALUE) && Store.version() == Version, "an idle step asked for an upload");
        List<long[]> Changed = new ArrayList<>();
        Map<Long, Integer> Unique = new HashMap<>();
        int[] Choices = {Scp001DayRay.Solid, Scp001DayRay.Clear, Scp001DayRay.cell(0x33, 255), Scp001DayRay.cell(0, 191)};
        while (Unique.size() < 1000) {
            int X = Store.originX() + Random.nextInt(Scp001DayGridStore.Span);
            int Y = Store.originY() + Random.nextInt(Scp001DayGridStore.Span);
            int Z = Store.originZ() + Random.nextInt(Scp001DayGridStore.Span);
            if (Synthetic.unloaded(X, Z)) {
                continue;
            }
            int Cell = Choices[Random.nextInt(Choices.length)];
            World.set(X, Y, Z, Cell);
            Unique.put(BlockPos.asLong(X, Y, Z), Cell);
            Changed.add(new long[]{X, Y, Z});
        }
        for (long[] Each : Changed) {
            Store.markDirty(BlockPos.asLong((int) Each[0], (int) Each[1], (int) Each[2]));
            Store.markDirty(BlockPos.asLong((int) Each[0], (int) Each[1], (int) Each[2]));
        }
        for (int Index = 0; Index < 50; Index++) {
            Store.markDirty(BlockPos.asLong(Store.originX() - 1 - Index, Store.originY(), Store.originZ()));
        }
        Helper.assertTrue(Store.pending() == 1050, "the dirty queue did not drop duplicates: " + Store.pending());
        Helper.assertTrue(Store.step(World, 0.5, 64.0, 0.5, noon(), World, Long.MAX_VALUE, Integer.MAX_VALUE), "a dirty step did not ask for an upload");
        Helper.assertTrue(Store.pending() == 1050 - Scp001DayGridStore.DirtyBudget, "a step drained more than its budget");
        int Drains = 1;
        while (Store.pending() > 0 && Drains < 10) {
            Store.step(World, 0.5, 64.0, 0.5, noon(), World, Long.MAX_VALUE, Integer.MAX_VALUE);
            Drains++;
        }
        Helper.assertTrue(Store.pending() == 0 && Drains == 3, "the dirty queue took " + Drains + " steps to drain");
        Helper.assertTrue(Store.version() > Version, "dirty drains did not bump the grid version");
        ByteBuffer Shown = Store.live();
        for (Map.Entry<Long, Integer> Each : Unique.entrySet()) {
            int Lx = BlockPos.getX(Each.getKey()) - Store.originX();
            int Ly = BlockPos.getY(Each.getKey()) - Store.originY();
            int Lz = BlockPos.getZ(Each.getKey()) - Store.originZ();
            int Offset = Scp001DayGridStore.index(Lx, Ly, Lz);
            if ((Shown.get(Offset) & 255) != Scp001DayRay.mask(Each.getValue()) || (Shown.get(Offset + 1) & 255) != Scp001DayRay.filter(Each.getValue())) {
                Helper.fail("a dirty cell was not written into the grid");
            }
        }
        checkCoarse(Helper, Store);
        Store.release();
        Helper.assertTrue(Pool.outstanding() == 0, "release left native memory behind");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "snativecrash_gridBounds")
    public static void gridBounds(GameTestHelper Helper) {
        Tracked Pool = new Tracked();
        Scp001DayGridStore Store = new Scp001DayGridStore(Pool);
        Synthetic World = new Synthetic();
        double[][] Eyes = {{Double.NaN, 64.0, 0.5}, {0.5, Double.NaN, 0.5}, {Double.POSITIVE_INFINITY, 64.0, Double.NEGATIVE_INFINITY}, {3.0e7, 64.0, -3.0e7},
                {-3.0e7, -2048.0, 3.0e7}, {0.5, 4096.0, 0.5}, {1.0e300, -1.0e300, 1.0e300}};
        for (double[] Eye : Eyes) {
            try {
                settle(Helper, Store, Pool, World, Eye[0], Eye[1], Eye[2], World, Integer.MAX_VALUE);
                long[] Extremes = {BlockPos.asLong(33554431, 2047, 33554431), BlockPos.asLong(-33554432, -2048, -33554432), BlockPos.asLong(Store.originX(), Store.originY(), Store.originZ()),
                        BlockPos.asLong(Store.originX() + 127, Store.originY() + 127, Store.originZ() + 127), BlockPos.asLong(Store.originX() + 128, Store.originY(), Store.originZ())};
                for (long Packed : Extremes) {
                    Store.markDirty(Packed);
                }
                Store.step(World, Eye[0], Eye[1], Eye[2], noon(), World, Long.MAX_VALUE, Integer.MAX_VALUE);
            } catch (RuntimeException Problem) {
                throw new IllegalStateException("the sun grid broke for camera " + Arrays.toString(Eye) + ": " + Problem, Problem);
            }
        }
        Store.release();
        Helper.assertTrue(Pool.outstanding() == 0 && Pool.Problem == null, "the bounds run leaked or double freed");
        Helper.succeed();
    }

    public static String source(String Path) {
        try (InputStream Stream = Scp001DayNativeGameTests.class.getResourceAsStream("/assets/scp_unleashed/" + Path)) {
            if (Stream == null) {
                throw new IllegalStateException("missing shader " + Path);
            }
            return new String(Stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException Problem) {
            throw new UncheckedIOException(Problem);
        }
    }

    public static String strip(String Text) {
        return Text.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }

    public static Map<String, Integer> constants(List<String> Texts) {
        Map<String, Integer> Found = new HashMap<>();
        for (String Text : Texts) {
            Matcher Each = Constant.matcher(strip(Text));
            while (Each.find()) {
                Found.put(Each.group(1), Integer.parseInt(Each.group(2)));
            }
        }
        return Found;
    }

    public static List<String> lint(String Name, String Text, Map<String, Integer> Constants) {
        List<String> Problems = new ArrayList<>();
        String Code = strip(Text);
        if (OtherLoop.matcher(Code).find()) {
            Problems.add(Name + ": uses a while or do loop");
        }
        Matcher Header = ForHeader.matcher(Code);
        while (Header.find()) {
            Matcher Init = ForInit.matcher(Header.group(1));
            Matcher Test = ForTest.matcher(Header.group(2));
            Matcher Step = ForStep.matcher(Header.group(3));
            if (!Init.matches() || !Test.matches() || !Step.matches()) {
                Problems.add(Name + ": loop header is not a plain counted loop: " + Header.group());
                continue;
            }
            String Counter = Init.group(1);
            String Stepped = Step.group(1) != null ? Step.group(1) : Step.group(2) != null ? Step.group(2) : Step.group(3);
            if (!Counter.equals(Test.group(1)) || !Counter.equals(Stepped)) {
                Problems.add(Name + ": loop tests or steps another variable: " + Header.group());
                continue;
            }
            String Bound = Test.group(3);
            Integer Limit = Bound.matches("\\d+") ? Integer.valueOf(Bound) : Constants.get(Bound);
            if (Limit == null || Limit > LoopCeiling) {
                Problems.add(Name + ": loop bound " + Bound + " is not a constant at most " + LoopCeiling);
                continue;
            }
            int Open = Code.indexOf('{', Header.end());
            if (Open < 0 || !Code.substring(Header.end(), Open).isBlank()) {
                Problems.add(Name + ": loop body is not a braced block: " + Header.group());
                continue;
            }
            int Depth = 0;
            int Close = -1;
            for (int Index = Open; Index < Code.length(); Index++) {
                char Here = Code.charAt(Index);
                if (Here == '{') {
                    Depth++;
                } else if (Here == '}') {
                    Depth--;
                    if (Depth == 0) {
                        Close = Index;
                        break;
                    }
                }
            }
            if (Close < 0) {
                Problems.add(Name + ": loop body never closes: " + Header.group());
                continue;
            }
            String Body = Code.substring(Open + 1, Close);
            Pattern Write = Pattern.compile("\\b" + Counter + "\\s*(=(?!=)|\\+=|-=|\\*=|/=|\\+\\+|--)|(\\+\\+|--)\\s*" + Counter + "\\b");
            if (Write.matcher(Body).find()) {
                Problems.add(Name + ": loop body changes its counter " + Counter);
            }
        }
        return Problems;
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "snativecrash_shaderLoopsBounded")
    public static void shaderLoopsBounded(GameTestHelper Helper) {
        Map<String, Integer> SelfConstants = constants(List.of("const int Cap = 8;"));
        Helper.assertTrue(lint("good", "const int Cap = 8; void f() { for (int I = 0; I < Cap; I++) { if (I > 2) { break; } } }", SelfConstants).isEmpty(), "the lint rejected a bounded loop");
        Helper.assertTrue(!lint("uniform", "uniform int N; void f() { for (int I = 0; I < N; I++) { } }", SelfConstants).isEmpty(), "the lint missed a uniform bound");
        Helper.assertTrue(!lint("while", "void f() { while (true) { } }", SelfConstants).isEmpty(), "the lint missed a while loop");
        Helper.assertTrue(!lint("reset", "void f() { for (int I = 0; I < 8; I++) { I = 0; } }", SelfConstants).isEmpty(), "the lint missed a counter reset");
        Helper.assertTrue(!lint("huge", "const int Many = 100000; void f() { for (int I = 0; I < Many; I++) { } }", constants(List.of("const int Many = 100000;"))).isEmpty(), "the lint missed a huge bound");
        List<String> Texts = new ArrayList<>();
        for (String Path : Shaders) {
            Texts.add(source(Path));
        }
        Map<String, Integer> Constants = constants(Texts);
        List<String> Problems = new ArrayList<>();
        int Loops = 0;
        for (int Index = 0; Index < Shaders.size(); Index++) {
            Problems.addAll(lint(Shaders.get(Index), Texts.get(Index), Constants));
            Matcher Each = ForHeader.matcher(strip(Texts.get(Index)));
            while (Each.find()) {
                Loops++;
            }
        }
        Helper.assertTrue(Problems.isEmpty(), String.join("; ", Problems));
        Helper.assertTrue(Loops >= 4, "the lint found only " + Loops + " loops in the SCP-001 sun shaders");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "snativecrash_shaftsToggle")
    public static void shaftsToggle(GameTestHelper Helper) {
        Helper.assertTrue(Scp001DayClientConfig.SunShafts != null, "the sun shaft toggle is not part of the client config");
        Helper.assertTrue(Boolean.TRUE.equals(Scp001DayClientConfig.SunShafts.getDefault()), "the sun shaft toggle is not on by default");
        Helper.assertTrue(List.of(Scp001DayClientConfig.Section, "sunShafts").equals(Scp001DayClientConfig.SunShafts.getPath()), "the sun shaft toggle is not in its own client section: "
                + Scp001DayClientConfig.SunShafts.getPath());
        Helper.assertTrue(Scp001DayClientConfig.sunShafts(), "sun shafts read as off without a loaded client config");
        Helper.succeed();
    }
}
