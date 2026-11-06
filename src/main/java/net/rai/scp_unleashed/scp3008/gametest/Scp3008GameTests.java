package net.rai.scp_unleashed.scp3008.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.document.AnomalyDocument;
import net.rai.scp_unleashed.archive.document.AnomalyDocuments;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.gateway.gametest.GatewayGameTests;
import net.rai.scp_unleashed.scp3008.Scp3008Clock;
import net.rai.scp_unleashed.scp3008.Scp3008Department;
import net.rai.scp_unleashed.scp3008.Scp3008Events;
import net.rai.scp_unleashed.scp3008.Scp3008Hours;
import net.rai.scp_unleashed.scp3008.Scp3008Registry;
import net.rai.scp_unleashed.scp3008.Scp3008State;
import net.rai.scp_unleashed.scp3008.Scp3008StoreCondition;
import net.rai.scp_unleashed.scp3008.Scp3008Storefront;
import net.rai.scp_unleashed.scp3008.Scp3008Transit;
import net.rai.scp_unleashed.scp3008.Scp3008World;
import net.rai.scp_unleashed.scp3008.block.Scp3008CounterBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008CrateBlockEntity;
import net.rai.scp_unleashed.scp3008.block.Scp3008EntranceBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008TrayBlock;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Canvas;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;
import net.rai.scp_unleashed.scp3008.world.Scp3008BiomeSource;
import net.rai.scp_unleashed.scp3008.world.Scp3008ChunkGenerator;
import net.rai.scp_unleashed.scp3008.world.Scp3008Furnishing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp3008GameTests {
    public static final String Arena = GatewayGameTests.Arena;
    public static final int StoreOffset = 400;

    private Scp3008GameTests() {
    }

    private static final class MapCanvas implements Scp3008Canvas {
        private final int MinX;
        private final int MinZ;
        private final int MaxX;
        private final int MaxZ;
        private final Map<BlockPos, Integer> Cells = new HashMap<>();

        private MapCanvas(int MinX, int MinZ, int MaxX, int MaxZ) {
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
            Cells.put(new BlockPos(X, Y, Z), Code);
        }

        int kindAt(int X, int Y, int Z) {
            Integer Code = Cells.get(new BlockPos(X, Y, Z));
            return Code == null ? 0 : Scp3008Code.kind(Code);
        }
    }

    private static void fresh(GameTestHelper Helper) {
        if (realStore(Helper)) {
            return;
        }
        Scp3008World.standIn(null);
        Scp3008Clock.bind(null);
        GatewayGameTests.whenDone(Helper, () -> {
            Scp3008World.standIn(null);
            Scp3008Clock.bind(null);
        });
    }

    private static ServerLevel store(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        fresh(Helper);
        ServerLevel Found = Scp3008World.level(Server);
        if (Found == null) {
            Scp3008World.standIn(Level.NETHER);
            Found = Scp3008World.level(Server);
        }
        return Found;
    }

    private static boolean realStore(GameTestHelper Helper) {
        return Helper.getLevel().getServer().getLevel(Scp3008World.Key) != null;
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_core_layout")
    public static void layoutIsSealed(GameTestHelper Helper) {
        int Ux = 5;
        int Uz = -3;
        int X0 = Scp3008Plan.originX(Ux);
        int Z0 = Scp3008Plan.originZ(Uz);
        MapCanvas Canvas = new MapCanvas(X0, Z0, X0 + Scp3008Plan.Unit - 1, Z0 + Scp3008Plan.Row - 1);
        Scp3008Painter.paint(Ux, Uz, Canvas);
        for (int X = X0; X < X0 + Scp3008Plan.Unit; X += 3) {
            for (int Z = Z0; Z < Z0 + Scp3008Plan.Row; Z += 3) {
                int Kind = Canvas.kindAt(X, Scp3008Plan.FloorY, Z);
                Helper.assertTrue(Kind == Scp3008Code.Floor || Kind == Scp3008Code.Line || Kind == Scp3008Code.Arrow || Kind == Scp3008Code.Hatch,
                        "no floor at " + X + ", " + Z + " (kind " + Kind + ")");
                Helper.assertTrue(Canvas.kindAt(X, Scp3008Plan.CeilingY, Z) == Scp3008Code.Ceiling, "no ceiling over " + X + ", " + Z);
            }
        }
        for (int Start : Scp3008Plan.PillarRows) {
            for (int Y = Scp3008Plan.Stand; Y < Scp3008Plan.CeilingY; Y += 17) {
                Helper.assertTrue(Canvas.kindAt(X0 + Scp3008Plan.PillarLow, Y, Z0 + Start) == Scp3008Code.Pillar, "pillar gap at height " + Y);
            }
        }
        Player Shopper = Helper.makeMockPlayer(GameType.SURVIVAL);
        BlockState[] Sealed = {Scp3008Registry.Floor.get().defaultBlockState(), Scp3008Registry.Pillar.get().defaultBlockState(),
                Scp3008Registry.Ceiling.get().defaultBlockState(), Scp3008Registry.Facade.get().defaultBlockState()};
        for (int Index = 0; Index < Sealed.length; Index++) {
            BlockPos Relative = new BlockPos(4 + Index * 2, 2, 4);
            Helper.setBlock(Relative, Sealed[Index]);
            BlockPos Pos = Helper.absolutePos(Relative);
            BlockState Placed = Helper.getLevel().getBlockState(Pos);
            Helper.assertTrue(Placed.is(Scp3008Registry.Unbreakable), Placed + " is not in the unbreakable tag");
            Helper.assertTrue(Placed.getDestroySpeed(Helper.getLevel(), Pos) < 0.0f, Placed + " can be mined");
            BlockEvent.BreakEvent Breaking = new BlockEvent.BreakEvent(Helper.getLevel(), Pos, Placed, Shopper);
            NeoForge.EVENT_BUS.post(Breaking);
            Helper.assertTrue(Breaking.isCanceled(), "breaking " + Placed + " was not refused");
        }
        BlockPos Above = Helper.absolutePos(new BlockPos(14, 2, 4));
        Helper.setBlock(new BlockPos(14, 2, 4), Scp3008Registry.Ceiling.get().defaultBlockState());
        Helper.assertTrue(Helper.getLevel().getBlockState(Above).getLightBlock(Helper.getLevel(), Above) == 0, "the ceiling blocks the store's daylight");
        BlockPos Shelf = Helper.absolutePos(new BlockPos(4, 2, 8));
        Helper.setBlock(new BlockPos(4, 2, 8), Scp3008Registry.Shelf.get().defaultBlockState());
        BlockEvent.BreakEvent Fixture = new BlockEvent.BreakEvent(Helper.getLevel(), Shelf, Helper.getLevel().getBlockState(Shelf), Shopper);
        NeoForge.EVENT_BUS.post(Fixture);
        Helper.assertFalse(Fixture.isCanceled(), "store shelves should stay breakable");
        Helper.succeed();
    }

    private static Scp3008ChunkGenerator generator(ServerLevel World) {
        Registry<Biome> Biomes = World.registryAccess().registryOrThrow(Registries.BIOME);
        Map<String, Holder<Biome>> Named = new HashMap<>();
        for (Scp3008Department Department : Scp3008Department.values()) {
            Named.put(Department.Key, Biomes.getHolderOrThrow(Department.BiomeKey));
        }
        return new Scp3008ChunkGenerator(new Scp3008BiomeSource(Named));
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp3008_core_generated")
    public static void generatorBuildsTheHall(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Registry<Biome> Biomes = World.registryAccess().registryOrThrow(Registries.BIOME);
        Scp3008ChunkGenerator Generator = generator(World);
        BlockPos Spot = Scp3008World.arrivalBlock(World.getServer());
        int Ux = Scp3008Plan.unitX(Spot.getX());
        int Uz = Scp3008Plan.unitZ(Spot.getZ());
        BlockPos Pillar = new BlockPos(Scp3008Plan.originX(Ux) + Scp3008Plan.PillarLow, Scp3008Plan.Stand + 30, Scp3008Plan.originZ(Uz) + Scp3008Plan.PillarRows[1]);
        for (BlockPos Probe : List.of(Spot, Pillar)) {
            ProtoChunk Chunk = new ProtoChunk(new ChunkPos(Probe), UpgradeData.EMPTY, LevelHeightAccessor.create(0, Scp3008ChunkGenerator.Height), Biomes, null);
            Generator.fillFromNoise(Blender.empty(), null, null, Chunk).join();
            if (Probe == Spot) {
                Helper.assertTrue(Chunk.getBlockState(Spot.below()).is(Scp3008Registry.Unbreakable), "the arrival floor is " + Chunk.getBlockState(Spot.below()));
                Helper.assertTrue(Chunk.getBlockState(Spot).isAir() && Chunk.getBlockState(Spot.above()).isAir(), "the arrival spot is blocked by " + Chunk.getBlockState(Spot));
                Helper.assertTrue(Chunk.getBlockState(new BlockPos(Spot.getX(), Scp3008Plan.CeilingY, Spot.getZ())).is(Scp3008Registry.Ceiling.get()), "no ceiling above the arrival");
                Helper.assertTrue(Chunk.getBlockState(new BlockPos(Spot.getX(), Scp3008Plan.CeilingY - 1, Spot.getZ())).isAir(), "the space under the ceiling is filled");
            } else {
                Helper.assertTrue(Chunk.getBlockState(Probe).is(Scp3008Registry.Pillar.get()), "no pillar at " + Probe + " but " + Chunk.getBlockState(Probe));
                Helper.assertTrue(Chunk.getBlockState(Probe.atY(Scp3008Plan.CeilingY - 1)).is(Scp3008Registry.Pillar.get()), "the pillar stops short of the ceiling");
            }
        }
        Scp3008Department Expected = Scp3008Department.of(Scp3008Plan.departmentAt(Spot.getX(), Spot.getZ()));
        Holder<Biome> Found = Generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(Spot.getX()), QuartPos.fromBlock(Spot.getY()), QuartPos.fromBlock(Spot.getZ()), null);
        Helper.assertTrue(Found.is(Expected.BiomeKey), "the biome source put " + Found + " where " + Expected.BiomeKey.location() + " belongs");
        Helper.assertTrue(Generator.getBaseHeight(Spot.getX(), Spot.getZ(), Heightmap.Types.MOTION_BLOCKING, LevelHeightAccessor.create(0, Scp3008ChunkGenerator.Height), null)
                == Scp3008Plan.CeilingTop + 1, "the base height does not reach the ceiling");
        Optional<Registry<LevelStem>> Stems = World.registryAccess().registry(Registries.LEVEL_STEM);
        LevelStem Stem = Stems.map(Stored -> Stored.get(Scp3008World.Key.location())).orElse(null);
        if (Stem != null) {
            Helper.assertTrue(Stem.generator() instanceof Scp3008ChunkGenerator, "the store dimension does not use the store generator");
        }
        if (realStore(Helper)) {
            ServerLevel Store = World.getServer().getLevel(Scp3008World.Key);
            Store.getChunk(Spot);
            Helper.assertTrue(Store.getBlockState(Spot.below()).is(Scp3008Registry.Unbreakable), "the live store floor is " + Store.getBlockState(Spot.below()));
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_core_departments")
    public static void departmentsAreStable(GameTestHelper Helper) {
        Set<Integer> Seen = new HashSet<>();
        for (int Ux = -60; Ux <= 60; Ux += 3) {
            for (int Uz = -20; Uz <= 20; Uz++) {
                int First = Scp3008Plan.department(Ux, Uz);
                Helper.assertTrue(First == Scp3008Plan.department(Ux, Uz) && First == Scp3008Plan.cachedDepartment(Ux, Uz), "department changed between calls");
                Helper.assertTrue(First >= 0 && First < Scp3008Plan.Departments.length, "department out of range");
                Seen.add(First);
                int X = Scp3008Plan.originX(Ux) + 20;
                int Z = Scp3008Plan.originZ(Uz) + 60;
                Helper.assertTrue(Scp3008Plan.departmentAt(X, Z) == First, "a block inside a unit reported another department");
            }
        }
        Helper.assertTrue(Seen.size() == Scp3008Plan.Departments.length, "only " + Seen.size() + " departments appeared in a large sample");
        int Exits = 0;
        int Foods = 0;
        for (int Ux = -128; Ux < 128; Ux++) {
            for (int Uz = -42; Uz < 42; Uz++) {
                Exits += Scp3008Plan.isExit(Ux, Uz) ? 1 : 0;
                Foods += Scp3008Plan.isFoodCourt(Ux, Uz) ? 1 : 0;
            }
        }
        Helper.assertTrue(Exits == 16, "expected one exit per exit region, found " + Exits);
        Helper.assertTrue(Foods > 100 && Foods < 3000, "food courts are off balance: " + Foods);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_core_loot")
    public static void containersGetLoot(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (Scp3008Department Department : Scp3008Department.values()) {
            LootTable Table = World.getServer().reloadableRegistries().getLootTable(Department.Loot);
            Helper.assertTrue(Table != LootTable.EMPTY, "missing loot table " + Department.Loot.location());
        }
        Helper.assertTrue(World.getServer().reloadableRegistries().getLootTable(Scp3008Furnishing.FoodCourtLoot) != LootTable.EMPTY, "missing food court loot");
        BlockPos Crate = Helper.absolutePos(new BlockPos(6, 2, 6));
        Scp3008Furnishing.place(World, Crate, Scp3008Code.of(Scp3008Code.Crate, Scp3008Code.crate(true, false), Scp3008Code.North), 3008L);
        Helper.assertTrue(World.getBlockEntity(Crate) instanceof Scp3008CrateBlockEntity, "the crate has no block entity");
        Scp3008CrateBlockEntity Box = (Scp3008CrateBlockEntity) World.getBlockEntity(Crate);
        Helper.assertTrue(Scp3008Furnishing.lootAt(Crate.getX(), Crate.getZ()).equals(Box.getLootTable()), "the crate got the wrong loot table " + Box.getLootTable());
        Box.unpackLootTable(null);
        boolean Filled = false;
        for (int Slot = 0; Slot < Box.getContainerSize(); Slot++) {
            Filled |= !Box.getItem(Slot).isEmpty();
        }
        Helper.assertTrue(Filled, "the crate loot table produced nothing");
        BlockPos Slot = Helper.absolutePos(new BlockPos(10, 2, 6));
        int Department = Scp3008Plan.departmentAt(Slot.getX(), Slot.getZ());
        Scp3008Furnishing.place(World, Slot, Scp3008Code.of(Scp3008Code.Furniture, Scp3008Code.slot(Department, Scp3008Code.RoleContainer, 5), Scp3008Code.North), 3008L);
        Helper.assertTrue(World.getBlockEntity(Slot) instanceof RandomizableContainer Holder && Holder.getLootTable() != null,
                "the container slot " + World.getBlockState(Slot) + " got no loot table");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_core_meatballs")
    public static void foodCourtServes(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        FakePlayer Diner = GatewayGameTests.person(Helper, 8.5, 9.5, "Diner");
        BlockPos Counter = Helper.absolutePos(new BlockPos(8, 2, 8));
        World.setBlock(Counter, Scp3008Registry.Counter.get().defaultBlockState(), 3);
        Helper.assertTrue(Scp3008CounterBlock.serve(World, Counter, Diner), "the counter did not serve a fresh visitor");
        Helper.assertTrue(Diner.getInventory().countItem(Scp3008Registry.Meatballs.get()) >= 1, "no meatballs were handed over");
        Helper.assertFalse(Scp3008CounterBlock.serve(World, Counter, Diner), "the counter served twice in a row");
        BlockPos Tray = Helper.absolutePos(new BlockPos(11, 2, 8));
        World.setBlock(Tray, Scp3008Registry.Tray.get().defaultBlockState().setValue(Scp3008TrayBlock.Serving, Scp3008TrayBlock.Food.Fries), 3);
        World.getBlockState(Tray).useWithoutItem(World, Diner, new BlockHitResult(Tray.getCenter(), Direction.UP, Tray, false));
        Helper.assertTrue(Diner.getInventory().countItem(Scp3008Registry.Fries.get()) >= 1, "the tray did not hand over its fries");
        Helper.assertTrue(World.getBlockState(Tray).getValue(Scp3008TrayBlock.Serving) == Scp3008TrayBlock.Food.None, "the tray still shows food");
        ItemStack Plate = new ItemStack(Scp3008Registry.Meatballs.get());
        Helper.assertTrue(Plate.getFoodProperties(Diner) != null && Plate.getFoodProperties(Diner).nutrition() >= 8, "meatballs are not a good food");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 300, batch = "scp3008_core_transit")
    public static void doorsLeadInAndOut(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerLevel Store = store(Helper);
        Helper.assertTrue(Store != null, "no store level or stand-in");
        Scp3008State State = Scp3008State.get(World.getServer());
        BlockPos KeptDoor = State.door();
        Direction KeptFront = State.front();
        ResourceKey<Level> KeptDimension = State.doorDimension();
        BlockPos Door = Helper.absolutePos(new BlockPos(16, 2, 4));
        State.recordStore(World.dimension(), Door, Direction.NORTH);
        FakePlayer Shopper = GatewayGameTests.person(Helper, 16.5, 6.5, "Shopper");
        Scp3008Transit.enter(Shopper, Door, Direction.NORTH);
        Helper.runAfterDelay(Scp3008Transit.FadeTicks + 6, () -> GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(Scp3008World.inside(Shopper), "the shopper did not arrive in the store, still in " + Shopper.level().dimension().location());
            Helper.assertTrue(Shopper.blockPosition().distManhattan(Scp3008World.arrivalBlock(World.getServer())) < 4, "the shopper arrived away from the entrance");
        }));
        Helper.runAfterDelay(Scp3008Transit.FadeTicks + Scp3008Transit.Grace + 10, () -> GatewayGameTests.guard(Helper, () -> {
            Scp3008Transit.leave(Shopper, Shopper.blockPosition());
        }));
        Helper.runAfterDelay(Scp3008Transit.FadeTicks * 2 + Scp3008Transit.Grace + 20, () -> GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(Shopper.level() == World, "the exit did not return the shopper to the overworld");
            Helper.assertTrue(Shopper.position().distanceTo(Scp3008Transit.doorstep(Door, Direction.NORTH)) < 1.5, "the shopper was not returned outside the store");
            if (KeptDoor != null) {
                State.recordStore(KeptDimension, KeptDoor, KeptFront);
            } else {
                State.forgetStore();
            }
            Scp3008World.standIn(null);
            Helper.succeed();
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp3008_core_clock")
    public static void lightsKeepTheirOwnHours(GameTestHelper Helper) {
        MinecraftServer Server = Helper.getLevel().getServer();
        ServerLevel Store = store(Helper);
        long Kept = Scp3008Clock.time();
        long Overworld = Helper.getLevel().getDayTime();
        Scp3008Clock.bind(Store);
        Scp3008Events.setClock(Server, 2000L);
        Helper.assertTrue(Store.getDayTime() == 2000L, "the store reports day time " + Store.getDayTime());
        Helper.assertFalse(Scp3008Hours.night(Store), "two thousand ticks should be day");
        Scp3008Events.setClock(Server, Scp3008Hours.Dusk);
        Helper.assertTrue(Scp3008Hours.night(Store), "dusk did not switch to night");
        Scp3008Events.setClock(Server, Scp3008Hours.Dawn - 1L);
        Helper.assertTrue(Scp3008Hours.night(Store), "the night ended early");
        Scp3008Events.setClock(Server, Scp3008Hours.Dawn);
        Helper.assertFalse(Scp3008Hours.night(Store), "dawn did not switch the lights back on");
        Scp3008Events.setClock(Server, Scp3008Hours.Day * 3L + 15000L);
        Helper.assertTrue(Scp3008Hours.night(Store), "later nights are not recognised");
        Helper.assertFalse(Scp3008Hours.night(Helper.getLevel()), "the overworld must never report store night");
        Helper.assertTrue(Helper.getLevel().getDayTime() == Overworld, "the store clock moved the overworld");
        Scp3008Events.setClock(Server, Kept);
        if (!realStore(Helper)) {
            Scp3008World.standIn(null);
            Scp3008Clock.bind(null);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3008_core_sleep")
    public static void bedsDoNotSleep(GameTestHelper Helper) {
        ServerLevel Store = store(Helper);
        FakePlayer Tired = GatewayGameTests.person(Helper, 20.5, 20.5, "Tired");
        BlockPos Spot = Scp3008World.arrivalBlock(Helper.getLevel().getServer());
        Scp3008Transit.move(Tired, Store, Spot.getCenter(), 0.0f);
        Helper.runAfterDelay(5, () -> GatewayGameTests.guard(Helper, () -> {
            Helper.assertTrue(Scp3008World.inside(Tired), "the sleeper did not reach the store");
            CanPlayerSleepEvent Asked = new CanPlayerSleepEvent(Tired, Spot, null);
            NeoForge.EVENT_BUS.post(Asked);
            Helper.assertTrue(Asked.getProblem() == Player.BedSleepingProblem.OTHER_PROBLEM, "sleeping was allowed in the store");
            CanPlayerSleepEvent Outside = new CanPlayerSleepEvent(Tired, Spot, null);
            Scp3008Transit.move(Tired, Helper.getLevel(), Helper.absoluteVec(new Vec3(20.5, 2.0, 20.5)), 0.0f);
            NeoForge.EVENT_BUS.post(Outside);
            Helper.assertTrue(Outside.getProblem() == null, "sleeping was refused outside the store");
            Scp3008World.standIn(null);
            Helper.succeed();
        }));
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp3008_core_document")
    public static void documentLeadsToTheStore(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        AnomalyDocument Document = AnomalyDocuments.get(World, Scp3008Registry.DocumentId);
        Helper.assertTrue(Document != null, "the SCP-3008 document did not load");
        Scp3008State State = Scp3008State.get(World.getServer());
        BlockPos KeptDoor = State.door();
        Direction KeptFront = State.front();
        ResourceKey<Level> KeptDimension = State.doorDimension();
        State.forgetStore();
        FakePlayer Reader = GatewayGameTests.person(Helper, 4.5, 4.5, "Reader");
        Scp3008StoreCondition Condition = new Scp3008StoreCondition(240, 520, 32, 64);
        Investigation Case = new Investigation(Scp3008Registry.DocumentId, World.getGameTime());
        List<?> Notes = Condition.begin(Reader, Case);
        Helper.assertTrue(Notes.size() >= 2, "the field notes are empty");
        Helper.assertTrue(Case.getState().contains("X") && Case.getState().contains("Z"), "no store site was planned");
        double Distance = Math.hypot(Case.getState().getInt("X") - Reader.getX(), Case.getState().getInt("Z") - Reader.getZ());
        Helper.assertTrue(Distance >= 200 && Distance <= 1200, "the planned site is " + (int) Distance + " blocks away");
        BlockPos Site = Helper.absolutePos(new BlockPos(16, 1, 16)).offset(StoreOffset, 0, 0);
        boolean Built = Scp3008Storefront.spawn(new AnomalySpawners.Discovery(World, Site, Reader, Scp3008Registry.DocumentId, Document, false));
        Helper.assertTrue(Built && State.hasStore(), "the store building was not recorded");
        BlockPos Door = State.door();
        Helper.assertTrue(World.getBlockState(Door).getBlock() instanceof Scp3008EntranceBlock, "no entrance at the recorded door, found " + World.getBlockState(Door));
        Helper.assertTrue(World.getBlockState(Door.above(6)).is(Scp3008Registry.Unbreakable), "the facade above the entrance is not sealed");
        Helper.assertTrue(Condition.tick(Reader, Case).map(Found -> Found.Pos().equals(Door)).orElse(false), "the condition does not point at the built store");
        Helper.assertTrue(Scp3008Storefront.isStructure(World, Door.above(3)), "the facade is not recognised as the store");
        if (KeptDoor != null) {
            State.recordStore(KeptDimension, KeptDoor, KeptFront);
        } else {
            State.forgetStore();
        }
        Helper.succeed();
    }
}
