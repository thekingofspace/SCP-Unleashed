package net.rai.scp_unleashed.scp020.gametest;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.labware.AssayRecord;
import net.rai.scp_unleashed.labware.DisinfectingWipeItem;
import net.rai.scp_unleashed.labware.LabAnalyzerBlockEntity;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.labware.LabSwabRecord;
import net.rai.scp_unleashed.labware.LabVials;
import net.rai.scp_unleashed.labware.LabwareRegistry;
import net.rai.scp_unleashed.scp020.Scp020Affliction;
import net.rai.scp_unleashed.scp020.Scp020Infection;
import net.rai.scp_unleashed.scp020.Scp020Migration;
import net.rai.scp_unleashed.scp020.Scp020Mold;
import net.rai.scp_unleashed.scp020.Scp020MoldBlock;
import net.rai.scp_unleashed.scp020.Scp020MoldCensus;
import net.rai.scp_unleashed.scp020.Scp020Organism;
import net.rai.scp_unleashed.scp020.Scp020Registry;
import net.rai.scp_unleashed.scp049.Scp049Registry;
import net.rai.scp_unleashed.scp173.Scp173Registry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp020GameTests {
    public static final String Arena = "scp020_arena";
    public static final BlockPos ArenaLow = new BlockPos(0, 0, 0);
    public static final BlockPos ArenaHigh = new BlockPos(15, 9, 15);
    public static final int SpreadRounds = 40;
    public static final List<ClipContext.Block> Picks = List.of(ClipContext.Block.COLLIDER, ClipContext.Block.OUTLINE, ClipContext.Block.VISUAL);

    private Scp020GameTests() {
    }

    private static ServerPlayer subject(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        FakePlayer User = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "scp020_test"));
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        User.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(User);
        User.setGameMode(GameType.SURVIVAL);
        return User;
    }

    private static void dismiss(GameTestHelper Helper, ServerPlayer User) {
        if (!User.isRemoved()) {
            Helper.getLevel().removePlayerImmediately(User, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void cleanup(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        for (Pair<BlockPos, Direction> Face : faces(Helper)) {
            Scp020Infection.cure(World, Face.getFirst(), Face.getSecond());
        }
    }

    private static boolean has(GameTestHelper Helper, BlockPos Host, Direction Side) {
        return Scp020Infection.has(Helper.getLevel(), Helper.absolutePos(Host), Side);
    }

    private static boolean infect(GameTestHelper Helper, BlockPos Host, Direction Side) {
        return Scp020Infection.infect(Helper.getLevel(), Helper.absolutePos(Host), Side);
    }

    private static List<Pair<BlockPos, Direction>> faces(GameTestHelper Helper) {
        List<Pair<BlockPos, Direction>> Found = new ArrayList<>();
        Scp020Infection.forEachIn(Helper.getLevel(), Helper.absolutePos(ArenaLow), Helper.absolutePos(ArenaHigh), (Host, Side) -> Found.add(Pair.of(Host, Side)));
        return Found;
    }

    private static void shell(GameTestHelper Helper, BlockPos Low, BlockPos High) {
        for (int X = Low.getX() - 1; X <= High.getX() + 1; X++) {
            for (int Y = Low.getY() - 1; Y <= High.getY() + 1; Y++) {
                for (int Z = Low.getZ() - 1; Z <= High.getZ() + 1; Z++) {
                    boolean Inside = X >= Low.getX() && X <= High.getX() && Y >= Low.getY() && Y <= High.getY() && Z >= Low.getZ() && Z <= High.getZ();
                    Helper.setBlock(new BlockPos(X, Y, Z), Inside ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState());
                }
            }
        }
    }

    private static BlockPos relative(GameTestHelper Helper, BlockPos Absolute) {
        return Absolute.subtract(Helper.absolutePos(BlockPos.ZERO));
    }

    private static boolean inside(GameTestHelper Helper, BlockPos Cell, BlockPos Low, BlockPos High) {
        BlockPos First = Helper.absolutePos(Low);
        BlockPos Second = Helper.absolutePos(High);
        return Cell.getX() >= Math.min(First.getX(), Second.getX()) && Cell.getX() <= Math.max(First.getX(), Second.getX())
                && Cell.getY() >= Math.min(First.getY(), Second.getY()) && Cell.getY() <= Math.max(First.getY(), Second.getY())
                && Cell.getZ() >= Math.min(First.getZ(), Second.getZ()) && Cell.getZ() <= Math.max(First.getZ(), Second.getZ());
    }

    private static ItemStack use(GameTestHelper Helper, ServerPlayer User, ItemStack Held, BlockPos Host, Direction Side) {
        User.setItemInHand(InteractionHand.MAIN_HAND, Held);
        BlockPos Absolute = Helper.absolutePos(Host);
        Vec3 Point = Vec3.atCenterOf(Absolute).add(Side.getStepX() * 0.5, Side.getStepY() * 0.5, Side.getStepZ() * 0.5);
        User.gameMode.useItemOn(User, Helper.getLevel(), Held, InteractionHand.MAIN_HAND, new BlockHitResult(Point, Side, Absolute, false));
        return User.getItemInHand(InteractionHand.MAIN_HAND);
    }

    private static Villager villager(GameTestHelper Helper, double X, double Y, double Z) {
        Villager Spawned = Helper.spawn(EntityType.VILLAGER, new Vec3(X, Y, Z));
        Spawned.setNoAi(true);
        Spawned.setSilent(true);
        return Spawned;
    }

    private static Scp020Witness witness(GameTestHelper Helper, double X, double Y, double Z) {
        ServerLevel World = Helper.getLevel();
        Scp020Witness Watcher = new Scp020Witness(World, "scp020_witness");
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Watcher.moveTo(Spot.x, Spot.y, Spot.z, 0.0f, 0.0f);
        World.addNewPlayer(Watcher);
        Watcher.setGameMode(GameType.SURVIVAL);
        Watcher.enlist(true);
        Watcher.watch(AABB.encapsulatingFullBlocks(Helper.absolutePos(ArenaLow), Helper.absolutePos(ArenaHigh)));
        Watcher.forget();
        return Watcher;
    }

    private static void release(GameTestHelper Helper, Scp020Witness Watcher) {
        Watcher.enlist(false);
        dismiss(Helper, Watcher);
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020_infectionStaysSilent")
    public static void infectionStaysSilent(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Villager Subject = villager(Helper, 5.5, 2.0, 7.5);
        for (int X = 3; X <= 7; X++) {
            for (int Y = 2; Y <= 3; Y++) {
                Helper.setBlock(new BlockPos(X, Y, 9), Blocks.STONE);
            }
        }
        BlockPos Ticked = new BlockPos(11, 2, 8);
        BlockPos Loaded = new BlockPos(13, 2, 5);
        long Before = World.getDayTime();
        World.setDayTime(18000L);
        Helper.runAfterDelay(3, () -> {
            Scp020Witness Watcher = witness(Helper, 8.5, 2.0, 1.5);
            try {
                Helper.setBlock(Ticked, Scp020Registry.Mold.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
                BlockPos Absolute = Helper.absolutePos(Loaded);
                LevelChunk Chunk = World.getChunkAt(Absolute);
                Chunk.getSection(Chunk.getSectionIndex(Absolute.getY())).setBlockState(Absolute.getX() & 15, Absolute.getY() & 15, Absolute.getZ() & 15,
                        Scp020Registry.Mold.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
                Watcher.forget();
                infect(Helper, new BlockPos(5, 2, 9), Direction.NORTH);
                infect(Helper, new BlockPos(5, 1, 8), Direction.UP);
                Scp020Migration.convert(World, Helper.absolutePos(Ticked), World.getBlockState(Helper.absolutePos(Ticked)));
                Scp020Migration.chunkLoaded(World, Chunk);
                Scp020Migration.tick(World);
                RandomSource Random = RandomSource.create(31);
                for (int Round = 0; Round < 30; Round++) {
                    for (Pair<BlockPos, Direction> Face : faces(Helper)) {
                        Scp020Mold.spreadFrom(World, Face.getFirst(), Random);
                        Scp020Mold.trySpread(World, Face.getFirst(), Random);
                    }
                }
                Scp020Mold.breathe(Subject);
                Scp020Mold.breathe(Watcher);
                for (Pair<BlockPos, Direction> Face : faces(Helper)) {
                    Scp020Mold.broken(World, Face.getFirst());
                    Scp020Infection.prune(World, Face.getFirst());
                }
                List<String> Seen = Watcher.seen();
                Helper.assertTrue(faces(Helper).size() > 4, "the infection did not grow during the test: " + faces(Helper).size() + " faces");
                Helper.assertTrue(has(Helper, Ticked.below(), Direction.UP) && has(Helper, Loaded.below(), Direction.UP), "the old mold blocks did not migrate");
                Helper.assertTrue(Scp020Affliction.isAfflicted(Subject), "the villager by the infection was not affected");
                Helper.assertTrue(Seen.isEmpty(), "an infected surface sent something a client could see or hear: " + Seen);
            } finally {
                World.setDayTime(Before);
                Scp020Affliction.cure(Subject);
                Subject.discard();
                release(Helper, Watcher);
                cleanup(Helper);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020_infectedActsLikeClean")
    public static void infectedActsLikeClean(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp020Witness Watcher = witness(Helper, 8.5, 2.0, 1.5);
        ServerPlayer User = subject(Helper, 8.5, 2.0, 4.5);
        try {
            BlockPos Clean = new BlockPos(4, 2, 9);
            BlockPos Infected = new BlockPos(12, 2, 9);
            Helper.setBlock(Clean, Blocks.STONE);
            Helper.setBlock(Infected, Blocks.STONE);
            Helper.setBlock(Clean.above(), Blocks.STONE);
            Helper.setBlock(Infected.above(), Blocks.STONE);
            infect(Helper, Infected, Direction.NORTH);
            infect(Helper, Infected.above(), Direction.NORTH);
            infect(Helper, new BlockPos(12, 1, 12), Direction.UP);
            infect(Helper, new BlockPos(13, 1, 12), Direction.UP);
            Watcher.forget();
            use(Helper, User, new ItemStack(LabwareRegistry.Swab.get()), Clean, Direction.NORTH);
            List<String> CleanSwab = Watcher.seen();
            Watcher.forget();
            use(Helper, User, new ItemStack(LabwareRegistry.Swab.get()), Infected, Direction.NORTH);
            List<String> InfectedSwab = Watcher.seen();
            Helper.assertTrue(CleanSwab.equals(InfectedSwab), "swabbing an infected block looks or sounds different: " + CleanSwab + " vs " + InfectedSwab);
            Watcher.forget();
            DisinfectingWipeItem.wipe(World, User, new BlockHitResult(Vec3.atCenterOf(Helper.absolutePos(new BlockPos(4, 1, 12))).add(0.0, 0.5, 0.0), Direction.UP,
                    Helper.absolutePos(new BlockPos(4, 1, 12)), false));
            List<String> CleanWipe = Watcher.seen();
            Watcher.forget();
            DisinfectingWipeItem.wipe(World, User, new BlockHitResult(Vec3.atCenterOf(Helper.absolutePos(new BlockPos(12, 1, 12))).add(0.0, 0.5, 0.0), Direction.UP,
                    Helper.absolutePos(new BlockPos(12, 1, 12)), false));
            List<String> InfectedWipe = Watcher.seen();
            Helper.assertTrue(!CleanWipe.isEmpty() && CleanWipe.equals(InfectedWipe), "wiping an infected area looks or sounds different: " + CleanWipe + " vs " + InfectedWipe);
            Helper.assertTrue(!has(Helper, new BlockPos(12, 1, 12), Direction.UP), "the wipe left the floor infected");
            Watcher.forget();
            User.gameMode.destroyBlock(Helper.absolutePos(Clean.above()));
            List<String> CleanBreak = Watcher.seen();
            Watcher.forget();
            User.gameMode.destroyBlock(Helper.absolutePos(Infected.above()));
            List<String> InfectedBreak = Watcher.seen();
            Helper.assertTrue(!CleanBreak.isEmpty() && CleanBreak.equals(InfectedBreak), "breaking an infected block looks or sounds different: " + CleanBreak + " vs " + InfectedBreak);
            Helper.assertTrue(Scp020Infection.facesAt(World, Helper.absolutePos(Infected.above())) == 0, "breaking the block left its infection behind");
        } finally {
            dismiss(Helper, User);
            release(Helper, Watcher);
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp020_spreadsAcrossConnectedBlocks")
    public static void spreadsAcrossConnectedBlocks(GameTestHelper Helper) {
        BlockPos Low = new BlockPos(4, 3, 4);
        BlockPos High = new BlockPos(10, 6, 10);
        shell(Helper, Low, High);
        BlockPos Seed = new BlockPos(7, 4, 3);
        Helper.runAfterDelay(30, () -> {
            ServerLevel World = Helper.getLevel();
            try {
                Helper.assertTrue(!Scp020Mold.isSunlit(World, Helper.absolutePos(Seed.south())), "the closed room is still sunlit");
                Helper.assertTrue(infect(Helper, Seed, Direction.SOUTH), "the seed face could not be infected");
                RandomSource Random = RandomSource.create(20);
                for (int Round = 0; Round < SpreadRounds; Round++) {
                    for (Pair<BlockPos, Direction> Face : faces(Helper)) {
                        Scp020Mold.spreadFrom(World, Face.getFirst(), Random);
                    }
                }
                List<Pair<BlockPos, Direction>> Grown = faces(Helper);
                Set<BlockPos> Hosts = new HashSet<>();
                Set<Direction> Sides = new HashSet<>();
                for (Pair<BlockPos, Direction> Face : Grown) {
                    BlockPos Host = Face.getFirst();
                    Direction Side = Face.getSecond();
                    Helper.assertTrue(World.getBlockState(Host).is(Blocks.STONE), "infection sits on a non-solid host at " + relative(Helper, Host));
                    Helper.assertTrue(inside(Helper, Host.relative(Side), Low, High), "infection faces out of the room at " + relative(Helper, Host) + " " + Side);
                    Helper.assertTrue(World.getBlockState(Host.relative(Side)).isAir(), "an infected face is not open");
                    Hosts.add(Host);
                    Sides.add(Side);
                }
                Helper.assertTrue(Grown.size() >= 8, "the infection barely spread: " + Grown.size() + " faces");
                Helper.assertTrue(Hosts.size() >= 5, "the infection stayed on too few blocks: " + Hosts.size());
                Helper.assertTrue(Sides.size() >= 2, "the infection never crossed onto another surface");
                cleanup(Helper);
                BlockPos Crowded = new BlockPos(7, 4, 3);
                int Packed = 0;
                for (int X = 5; X <= 9 && Packed < Scp020Mold.LocalCap; X++) {
                    for (int Y = 3; Y <= 6 && Packed < Scp020Mold.LocalCap; Y++) {
                        if (infect(Helper, new BlockPos(X, Y, 3), Direction.SOUTH)) {
                            Packed++;
                        }
                    }
                }
                Helper.assertTrue(has(Helper, Crowded, Direction.SOUTH), "the crowded patch is missing its centre");
                for (int Round = 0; Round < 100; Round++) {
                    Scp020Mold.spreadFrom(World, Helper.absolutePos(Crowded), Random);
                }
                Helper.assertTrue(faces(Helper).size() == Scp020Mold.LocalCap, "a thick patch kept spreading: " + faces(Helper).size() + " faces");
            } finally {
                cleanup(Helper);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp020_noSpreadInDaylight")
    public static void noSpreadInDaylight(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        long Before = World.getDayTime();
        World.setDayTime(6000L);
        BlockPos Host = new BlockPos(6, 1, 6);
        Helper.runAfterDelay(3, () -> {
            try {
                BlockPos Cell = Helper.absolutePos(Host.above());
                Helper.assertTrue(World.isDay() && World.canSeeSky(Cell), "the open floor is not in daylight");
                Helper.assertTrue(infect(Helper, Host, Direction.UP), "the floor could not be infected");
                RandomSource Random = RandomSource.create(7);
                for (int Round = 0; Round < 200; Round++) {
                    Scp020Mold.spreadFrom(World, Helper.absolutePos(Host), Random);
                }
                Helper.assertTrue(faces(Helper).size() == 1, "the infection spread in direct daylight: " + faces(Helper).size() + " faces");
            } finally {
                cleanup(Helper);
                World.setDayTime(Before);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_blocksPlaceOverInfection")
    public static void blocksPlaceOverInfection(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 2.5, 2.0, 2.5);
        try {
            BlockPos Host = new BlockPos(5, 1, 5);
            Helper.assertTrue(infect(Helper, Host, Direction.UP), "the floor could not be infected");
            use(Helper, User, new ItemStack(Items.STONE), Host, Direction.UP);
            Helper.assertBlockPresent(Blocks.STONE, Host.above());
            Helper.assertTrue(has(Helper, Host, Direction.UP), "covering the face wiped the infection out instead of sealing it");
            Helper.assertTrue(!Scp020Infection.isOpen(World, Helper.absolutePos(Host), Direction.UP), "a covered face still counts as open");
            BlockPos Legacy = new BlockPos(9, 2, 5);
            Helper.setBlock(Legacy, Scp020Registry.Mold.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
            use(Helper, User, new ItemStack(Items.OAK_PLANKS), Legacy.below(), Direction.UP);
            Helper.assertBlockPresent(Blocks.OAK_PLANKS, Legacy);
        } finally {
            dismiss(Helper, User);
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_cursorNeverHitsIt")
    public static void cursorNeverHitsIt(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        try {
            BlockPos Wall = new BlockPos(5, 2, 9);
            Helper.setBlock(Wall, Blocks.STONE);
            Helper.assertTrue(infect(Helper, Wall, Direction.NORTH), "the wall could not be infected");
            Vec3 From = Helper.absoluteVec(new Vec3(5.5, 2.5, 4.0));
            Vec3 To = Helper.absoluteVec(new Vec3(5.5, 2.5, 9.6));
            for (ClipContext.Block Mode : Picks) {
                BlockHitResult Hit = World.clip(new ClipContext(From, To, Mode, ClipContext.Fluid.ANY, CollisionContext.empty()));
                Helper.assertTrue(Hit.getType() == HitResult.Type.BLOCK && Hit.getBlockPos().equals(Helper.absolutePos(Wall)) && Hit.getDirection() == Direction.NORTH,
                        "a " + Mode + " pick did not land on the plain wall");
                Helper.assertTrue(World.getBlockState(Hit.getBlockPos()) == Blocks.STONE.defaultBlockState(), "the picked block is not plain stone");
            }
            BlockPos LegacyWall = new BlockPos(9, 2, 9);
            BlockPos Legacy = LegacyWall.north();
            Helper.setBlock(LegacyWall, Blocks.STONE);
            BlockState Old = Scp020Registry.Mold.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.SOUTH), true);
            Helper.setBlock(Legacy, Old);
            Helper.assertBlockPresent(Scp020Registry.Mold.get(), Legacy);
            BlockPos Absolute = Helper.absolutePos(Legacy);
            Helper.assertTrue(Old.getShape(World, Absolute).isEmpty() && Old.getCollisionShape(World, Absolute).isEmpty() && Old.getVisualShape(World, Absolute, CollisionContext.empty()).isEmpty(),
                    "the old mold block still has a shape");
            Helper.assertTrue(Old.getRenderShape() == RenderShape.INVISIBLE, "the old mold block still renders");
            Helper.assertTrue(Old.getLightBlock(World, Absolute) == 0, "the old mold block changes light");
            Vec3 Start = Helper.absoluteVec(new Vec3(9.5, 2.5, 4.0));
            Vec3 End = Helper.absoluteVec(new Vec3(9.5, 2.5, 9.6));
            for (ClipContext.Block Mode : Picks) {
                BlockHitResult Hit = World.clip(new ClipContext(Start, End, Mode, ClipContext.Fluid.NONE, CollisionContext.empty()));
                Helper.assertTrue(Hit.getBlockPos().equals(Helper.absolutePos(LegacyWall)), "a " + Mode + " pick stopped on the old mold block");
            }
            Helper.setBlock(Legacy, Blocks.AIR);
        } finally {
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_jadeSeesNothing")
    public static void jadeSeesNothing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        try {
            BlockPos Wall = new BlockPos(6, 2, 9);
            Helper.setBlock(Wall, Blocks.STONE);
            BlockState Before = World.getBlockState(Helper.absolutePos(Wall));
            Helper.assertTrue(infect(Helper, Wall, Direction.NORTH), "the wall could not be infected");
            BlockPos Absolute = Helper.absolutePos(Wall);
            Helper.assertTrue(World.getBlockState(Absolute) == Before, "infecting changed the wall's block state");
            Helper.assertTrue(World.getBlockState(Absolute.north()).isAir(), "infecting put something in front of the wall");
            Helper.assertTrue(World.getBlockEntity(Absolute) == null && World.getBlockEntity(Absolute.north()) == null, "infecting added a block entity");
            boolean ProviderGone;
            try {
                Class.forName("net.rai.scp_unleashed.scp020.integration.Scp020MoldJadeProvider");
                ProviderGone = false;
            } catch (ClassNotFoundException Missing) {
                ProviderGone = true;
            }
            Helper.assertTrue(ProviderGone, "the SCP-020 Jade tooltip provider still exists");
        } finally {
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp020_oldMoldMigrates")
    public static void oldMoldMigrates(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp020MoldCensus Census = Scp020MoldCensus.get(World);
        int Counted = Census.faces();
        Census.reset(Counted + 3);
        BlockPos Ticked = new BlockPos(4, 2, 4);
        Helper.setBlock(Ticked.north(), Blocks.STONE);
        Helper.setBlock(Ticked, Scp020Registry.Mold.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true)
                .setValue(MultifaceBlock.getFaceProperty(Direction.NORTH), true));
        BlockPos Loaded = new BlockPos(10, 2, 10);
        BlockPos Absolute = Helper.absolutePos(Loaded);
        LevelChunk Chunk = World.getChunkAt(Absolute);
        LevelChunkSection Section = Chunk.getSection(Chunk.getSectionIndex(Absolute.getY()));
        Section.setBlockState(Absolute.getX() & 15, Absolute.getY() & 15, Absolute.getZ() & 15,
                Scp020Registry.Mold.get().defaultBlockState().setValue(MultifaceBlock.getFaceProperty(Direction.DOWN), true));
        Helper.assertTrue(Scp020MoldBlock.isLegacy(World.getBlockState(Absolute)), "the old mold block was not written into the chunk");
        Helper.assertTrue(Scp020Migration.holdsLegacy(Chunk), "the chunk scan missed the old mold block");
        Scp020Migration.chunkLoaded(World, Chunk);
        Helper.runAfterDelay(5, () -> {
            try {
                Helper.assertBlockPresent(Blocks.AIR, Ticked);
                Helper.assertBlockPresent(Blocks.AIR, Loaded);
                Helper.assertTrue(has(Helper, Ticked.below(), Direction.UP), "the floor face of the ticked mold did not migrate");
                Helper.assertTrue(has(Helper, Ticked.north(), Direction.SOUTH), "the wall face of the ticked mold did not migrate");
                Helper.assertTrue(has(Helper, Loaded.below(), Direction.UP), "the mold in a loaded chunk did not migrate");
                Helper.assertTrue(faces(Helper).size() == 3, "migration made the wrong number of faces: " + faces(Helper).size());
                Helper.assertTrue(Census.faces() == Counted + 3, "migration changed the patch count: " + Census.faces() + " instead of " + (Counted + 3));
            } finally {
                cleanup(Helper);
            }
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp020_swabReadsInfection")
    public static void swabReadsInfection(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 8.5, 2.0, 5.5);
        BlockPos Infected = new BlockPos(3, 2, 8);
        BlockPos Touching = new BlockPos(4, 2, 8);
        BlockPos Clean = new BlockPos(13, 2, 8);
        Helper.setBlock(Infected, Blocks.STONE);
        Helper.setBlock(Touching, Blocks.STONE);
        Helper.setBlock(Clean, Blocks.STONE);
        infect(Helper, Infected, Direction.NORTH);
        ItemStack FromInfected = use(Helper, User, new ItemStack(LabwareRegistry.Swab.get()), Infected, Direction.NORTH);
        ItemStack FromTouching = use(Helper, User, new ItemStack(LabwareRegistry.Swab.get()), Touching, Direction.UP);
        ItemStack FromClean = use(Helper, User, new ItemStack(LabwareRegistry.Swab.get()), Clean, Direction.NORTH);
        dismiss(Helper, User);
        cleanup(Helper);
        for (ItemStack Taken : List.of(FromInfected, FromTouching, FromClean)) {
            Helper.assertTrue(LabVials.isUsedSwab(Taken), "swabbing a block did not take a sample");
        }
        LabSwabRecord Positive = LabVials.recordOf(FromInfected);
        LabSwabRecord Near = LabVials.recordOf(FromTouching);
        LabSwabRecord Blank = LabVials.recordOf(FromClean);
        Helper.assertTrue(Positive != null && Positive.Findings().contains(Scp020Organism.Id), "the infected block's swab carries no SCP-020");
        Helper.assertTrue(Near != null && Near.Findings().contains(Scp020Organism.Id), "a block touching the infection swabbed clean");
        Helper.assertTrue(Blank != null && Blank.isBlank(), "a clean block's swab picked something up");
        BlockPos FirstAnalyzer = new BlockPos(6, 2, 12);
        BlockPos SecondAnalyzer = new BlockPos(10, 2, 12);
        Helper.setBlock(FirstAnalyzer, LabwareRegistry.Analyzer.get().defaultBlockState());
        Helper.setBlock(SecondAnalyzer, LabwareRegistry.Analyzer.get().defaultBlockState());
        LabAnalyzerBlockEntity First = (LabAnalyzerBlockEntity) World.getBlockEntity(Helper.absolutePos(FirstAnalyzer));
        LabAnalyzerBlockEntity Second = (LabAnalyzerBlockEntity) World.getBlockEntity(Helper.absolutePos(SecondAnalyzer));
        Helper.assertTrue(First != null && Second != null, "the analyzers did not place");
        First.getStorage().setStackInSlot(LabAnalyzerBlockEntity.InputSlot, FromInfected.copy());
        Second.getStorage().setStackInSlot(LabAnalyzerBlockEntity.InputSlot, FromClean.copy());
        Helper.onEachTick(() -> {
            First.insertPower(LabAnalyzerBlockEntity.WorkDraw);
            Second.insertPower(LabAnalyzerBlockEntity.WorkDraw);
        });
        Helper.succeedWhen(() -> {
            ItemStack PositiveSlip = First.getStorage().getStackInSlot(LabAnalyzerBlockEntity.OutputSlot);
            ItemStack CleanSlip = Second.getStorage().getStackInSlot(LabAnalyzerBlockEntity.OutputSlot);
            Helper.assertTrue(!PositiveSlip.isEmpty() && !CleanSlip.isEmpty(), "the analyzers have not printed yet");
            AssayRecord Found = PositiveSlip.get(LabwareRegistry.Assay.get());
            AssayRecord Nothing = CleanSlip.get(LabwareRegistry.Assay.get());
            Helper.assertTrue(Found != null && Found.isPositive() && Found.Findings().get(0).equals(Scp020Organism.Id), "the analyzer did not report SCP-020");
            Helper.assertTrue(LabOrganisms.nameOf(Found.Findings().get(0)).getString().equals(Scp020Organism.get().getName().getString()), "the slip names the wrong agent");
            Helper.assertTrue(Nothing != null && !Nothing.isPositive(), "the clean swab read positive");
        });
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_wipeClearsArea")
    public static void wipeClearsArea(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 6.5, 2.0, 6.5);
        try {
            BlockPos Wiped = new BlockPos(6, 1, 6);
            Helper.setBlock(new BlockPos(6, 2, 8), Blocks.STONE);
            List<Pair<BlockPos, Direction>> Near = List.of(Pair.of(Wiped, Direction.UP), Pair.of(new BlockPos(5, 1, 6), Direction.UP), Pair.of(new BlockPos(8, 1, 8), Direction.UP),
                    Pair.of(new BlockPos(6, 2, 8), Direction.NORTH));
            List<Pair<BlockPos, Direction>> Far = List.of(Pair.of(new BlockPos(9, 1, 6), Direction.UP), Pair.of(new BlockPos(6, 1, 3), Direction.UP));
            for (Pair<BlockPos, Direction> Face : Near) {
                Helper.assertTrue(infect(Helper, Face.getFirst(), Face.getSecond()), "could not infect " + Face);
            }
            for (Pair<BlockPos, Direction> Face : Far) {
                Helper.assertTrue(infect(Helper, Face.getFirst(), Face.getSecond()), "could not infect " + Face);
            }
            Vec3 Eye = Helper.absoluteVec(new Vec3(6.5, 2.0, 6.5));
            User.moveTo(Eye.x, Eye.y, Eye.z, 0.0f, 90.0f);
            ItemStack Wipes = new ItemStack(LabwareRegistry.Wipe.get(), 3);
            User.setItemInHand(InteractionHand.MAIN_HAND, Wipes);
            BlockHitResult Aimed = DisinfectingWipeItem.target(User);
            Helper.assertTrue(Aimed != null && Aimed.getBlockPos().equals(Helper.absolutePos(Wiped)) && Aimed.getDirection() == Direction.UP, "the player is not aiming at the wiped block");
            use(Helper, User, Wipes, Wiped, Direction.UP);
            Helper.assertTrue(User.isUsingItem() && User.getUseItem().is(LabwareRegistry.Wipe.get()), "using the wipe on a block did not start wiping");
            Helper.assertTrue(User.getUseItem().getUseAnimation() == UseAnim.BRUSH, "the wipe does not play a wiping animation");
            ItemStack Left = Wipes.getItem().finishUsingItem(Wipes, World, User);
            Helper.assertTrue(Left.getCount() == 2, "a finished wipe was not used up: " + Left.getCount() + " left");
            for (Pair<BlockPos, Direction> Face : Near) {
                Helper.assertTrue(!has(Helper, Face.getFirst(), Face.getSecond()), "the wipe missed " + Face.getFirst() + " " + Face.getSecond());
            }
            for (Pair<BlockPos, Direction> Face : Far) {
                Helper.assertTrue(has(Helper, Face.getFirst(), Face.getSecond()), "the wipe reached too far: " + Face.getFirst() + " " + Face.getSecond());
            }
        } finally {
            dismiss(Helper, User);
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_breakingRemovesInfection")
    public static void breakingRemovesInfection(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        try {
            BlockPos Wall = new BlockPos(7, 2, 9);
            Helper.setBlock(Wall, Blocks.STONE);
            infect(Helper, Wall, Direction.NORTH);
            infect(Helper, Wall, Direction.UP);
            World.destroyBlock(Helper.absolutePos(Wall), false);
            Helper.assertTrue(Scp020Infection.facesAt(World, Helper.absolutePos(Wall)) == 0, "breaking the block left its infection behind");
        } finally {
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_afflictionStillWorks")
    public static void afflictionStillWorks(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        ServerPlayer User = subject(Helper, 9.5, 2.0, 7.5);
        Villager Subject = villager(Helper, 5.5, 2.0, 7.5);
        try {
            BlockPos Wall = new BlockPos(5, 2, 9);
            Helper.setBlock(Wall, Blocks.STONE);
            infect(Helper, Wall, Direction.NORTH);
            Scp020Mold.breathe(Subject);
            Helper.assertTrue(Scp020Affliction.intensity(Subject) > 0, "a villager breathing near the infection was not affected");
            Helper.assertTrue(LabOrganisms.detect(Subject).contains(Scp020Organism.Id), "an affected villager does not test positive");
            Helper.assertTrue(Scp020Affliction.afflict(User), "SCP-020 could not be applied to a player");
            Helper.assertTrue(Scp020Affliction.intensity(User) >= Scp020Affliction.AfflictIntensity, "the player was not afflicted");
            Helper.assertTrue(Scp020Affliction.cure(User) && !Scp020Affliction.isAfflicted(User), "the player could not be cured");
        } finally {
            Scp020Affliction.cure(Subject);
            Subject.discard();
            dismiss(Helper, User);
            cleanup(Helper);
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 20, batch = "scp020_infectionImmuneNeverAfflicted")
    public static void infectionImmuneNeverAfflicted(GameTestHelper Helper) {
        Villager Subject = villager(Helper, 5.5, 2.0, 7.5);
        BlockPos Wall = new BlockPos(5, 2, 9);
        Helper.setBlock(Wall, Blocks.STONE);
        infect(Helper, Wall, Direction.NORTH);
        Map<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>> Original = new LinkedHashMap<>();
        BuiltInRegistries.ENTITY_TYPE.getTags().forEach(Entry -> Original.put(Entry.getFirst(), Entry.getSecond().stream().toList()));
        Original.putIfAbsent(InfectionImmunity.Immune, List.of());
        Map<TagKey<EntityType<?>>, List<Holder<EntityType<?>>>> Immune = new LinkedHashMap<>(Original);
        List<Holder<EntityType<?>>> Members = new ArrayList<>(Immune.getOrDefault(InfectionImmunity.Immune, List.of()));
        Members.add(BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(EntityType.VILLAGER));
        Immune.put(InfectionImmunity.Immune, Members);
        try {
            BuiltInRegistries.ENTITY_TYPE.bindTags(Immune);
            Helper.assertTrue(Scp020Affliction.isImmune(Subject), "the villager is not in the infection_immune tag");
            Scp020Mold.breathe(Subject);
            Scp020Mold.expose(Helper.getLevel(), Scp020Infection.faceCentre(Helper.absolutePos(Wall), Direction.NORTH));
            Helper.assertTrue(!Scp020Affliction.dose(Subject), "an immune villager took a dose");
            Helper.assertTrue(!Scp020Affliction.afflict(Subject), "an immune villager was afflicted");
            Scp020Affliction.setIntensity(Subject, 50);
            Helper.assertTrue(Scp020Affliction.intensity(Subject) == 0 && !Scp020Affliction.isAfflicted(Subject), "an immune villager carries SCP-020");
            Helper.assertTrue(!Scp020Affliction.canCatch(Subject), "an immune villager can still catch SCP-020");
        } finally {
            BuiltInRegistries.ENTITY_TYPE.bindTags(Original);
        }
        try {
            Helper.assertTrue(!Scp020Affliction.isImmune(Subject), "the test tag binding was not undone");
            Helper.assertTrue(Scp020Affliction.afflict(Subject) && Scp020Affliction.isAfflicted(Subject), "an ordinary villager could not be afflicted");
            for (EntityType<?> Kind : List.<EntityType<?>>of(Scp049Registry.Doctor.get(), Scp173Registry.Statue.get())) {
                Entity Made = Kind.create(Helper.getLevel());
                Helper.assertTrue(Made instanceof LivingEntity && Kind.is(InfectionImmunity.Immune), BuiltInRegistries.ENTITY_TYPE.getKey(Kind) + " is not in the infection_immune tag");
                LivingEntity Anomaly = (LivingEntity) Made;
                Helper.assertTrue(!Scp020Affliction.canCatch(Anomaly) && !Scp020Affliction.dose(Anomaly) && !Scp020Affliction.afflict(Anomaly),
                        BuiltInRegistries.ENTITY_TYPE.getKey(Kind) + " can catch SCP-020");
                Scp020Affliction.setIntensity(Anomaly, Scp020Affliction.MaxIntensity);
                Helper.assertTrue(!Scp020Affliction.isAfflicted(Anomaly), BuiltInRegistries.ENTITY_TYPE.getKey(Kind) + " carries SCP-020");
            }
        } finally {
            Scp020Affliction.cure(Subject);
            Subject.discard();
            cleanup(Helper);
        }
        Helper.succeed();
    }
}
