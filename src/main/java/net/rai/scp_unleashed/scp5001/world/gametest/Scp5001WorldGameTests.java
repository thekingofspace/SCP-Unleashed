package net.rai.scp_unleashed.scp5001.world.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.scp5001.engine.Scp5001EngineBlockEntity;
import net.rai.scp_unleashed.scp5001.world.Scp5001Emitter;
import net.rai.scp_unleashed.scp5001.world.Scp5001FacilityCondition;
import net.rai.scp_unleashed.scp5001.world.Scp5001Palettes;
import net.rai.scp_unleashed.scp5001.world.Scp5001Sites;
import net.rai.scp_unleashed.scp5001.world.Scp5001WorldRegistry;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001PanelDoorBlock;
import net.rai.scp_unleashed.scp5001.world.block.Scp5001WallFaces;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Anchor;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Blocks;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Plan;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001PlanCheck;
import net.rai.scp_unleashed.scp5001.world.plan.Scp5001Slot;
import net.rai.scp_unleashed.wire.WireDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp5001WorldGameTests {
    public static final String Arena = "gateway_arena";
    public static final long Seed = 5001L;
    public static final int FarX = 2_400_000;
    public static final int FarZ = 2_400_000;

    private Scp5001WorldGameTests() {
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp5001world_plan")
    public static void planDeterministic(GameTestHelper Helper) {
        Scp5001Plan First = Scp5001Plan.build(Seed);
        Scp5001Plan Second = Scp5001Plan.build(Seed);
        Scp5001Plan Other = Scp5001Plan.build(42L);
        Helper.assertTrue(First.checksum() == Second.checksum(), "the same seed built different facilities");
        Helper.assertTrue(First.checksum() != Other.checksum(), "different seeds built the same facility");
        Helper.assertTrue(First.roomCount() >= Scp5001PlanCheck.MinimumRooms, "only " + First.roomCount() + " rooms");
        List<String> Problems = Scp5001PlanCheck.check(First);
        Helper.assertTrue(Problems.isEmpty(), "plan check failed: " + Problems);
        List<String> OtherProblems = Scp5001PlanCheck.check(Other);
        Helper.assertTrue(OtherProblems.isEmpty(), "plan check failed for seed 42: " + OtherProblems);
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "scp5001world_plan")
    public static void shellSealedAndAnchors(GameTestHelper Helper) {
        Scp5001Plan Plan = Scp5001Emitter.plan(Seed);
        Helper.assertTrue(Scp5001PlanCheck.enclosure(Plan).isEmpty(), "the shell has gaps: " + Scp5001PlanCheck.enclosure(Plan));
        int Doors = 0;
        for (int X = 0; X < Scp5001Plan.Width; X++) {
            for (int Y = 0; Y < Scp5001Plan.Height; Y++) {
                if (Scp5001Blocks.idOf(Plan.get(X, Y, 0)).equals(Scp5001Blocks.EntryDoor)) {
                    Doors++;
                }
            }
        }
        Helper.assertTrue(Doors == Scp5001Plan.EntryWidth * Scp5001Plan.EntryHeight, "entry door has " + Doors + " parts");
        for (Map.Entry<String, Integer> Entry : Scp5001PlanCheck.AnchorCounts.entrySet()) {
            Helper.assertTrue(Plan.anchors(Entry.getKey()).size() == Entry.getValue(), Entry.getKey() + " count is " + Plan.anchors(Entry.getKey()).size());
        }
        Scp5001Anchor Intake = Plan.anchors(Scp5001Blocks.EmergencyIntake).get(0);
        Direction Out = Direction.byName(Intake.Facing());
        for (int Ahead = 1; Ahead <= 3; Ahead++) {
            for (int Across = -1; Across <= 1; Across++) {
                BlockPos Cell = new BlockPos(Intake.X(), Intake.Y(), Intake.Z()).relative(Out, Ahead).relative(Out.getClockWise(), Across);
                Helper.assertTrue(Plan.air(Cell.getX(), Cell.getY(), Cell.getZ()), "the intake bay is blocked at " + Ahead + "," + Across);
            }
        }
        Scp5001Anchor Console = Plan.anchors(Scp5001Blocks.ConsoleMain).get(0);
        Direction Side = Direction.byName(Console.Facing()).getClockWise();
        BlockPos Left = new BlockPos(Console.X(), Console.Y(), Console.Z()).relative(Side);
        BlockPos Right = new BlockPos(Console.X(), Console.Y(), Console.Z()).relative(Side.getOpposite());
        Helper.assertTrue(Plan.air(Left.getX(), Left.getY(), Left.getZ()) && Plan.air(Right.getX(), Right.getY(), Right.getZ()), "the main console is crowded");
        for (Scp5001Anchor Generator : Plan.anchors(Scp5001Blocks.Generator)) {
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    for (int Dy = 0; Dy <= 2; Dy++) {
                        if (Dx != 0 || Dz != 0 || Dy != 0) {
                            Helper.assertTrue(Plan.air(Generator.X() + Dx, Generator.Y() + Dy, Generator.Z() + Dz), "a generator volume is blocked");
                        }
                    }
                }
            }
        }
        Helper.assertTrue(Plan.roomCount() >= 110 && Plan.roomCount() <= 170, "room count " + Plan.roomCount() + " is outside 110 to 170");
        Scp5001Anchor Core = Plan.anchors(Scp5001Blocks.ChamberCore).get(0);
        for (int Dy = 1; Dy <= 48; Dy++) {
            for (int[] Step : new int[][]{{12, 0}, {-12, 0}, {0, 12}, {0, -12}, {0, 0}}) {
                Helper.assertTrue(Plan.air(Core.X() + Step[0], Core.Y() + Dy, Core.Z() + Step[1]), "the vessel column is blocked at height " + Dy);
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 80, batch = "scp5001world_shell")
    public static void shellResists(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Hull = Helper.absolutePos(new BlockPos(6, 2, 6));
        BlockState State = Scp5001WorldRegistry.Shell.get().defaultBlockState();
        World.setBlock(Hull, State, Block.UPDATE_ALL);
        Helper.assertTrue(State.getDestroySpeed(World, Hull) < 0.0f, "the shell can be mined");
        Helper.assertTrue(State.getBlock().getExplosionResistance() >= 1_000_000.0f, "the shell is not blast proof");
        Helper.assertTrue(State.getPistonPushReaction() == PushReaction.BLOCK, "pistons can push the shell");
        WitherBoss Wither = EntityType.WITHER.create(World);
        Helper.assertTrue(Wither != null && !State.canEntityDestroy(World, Hull, Wither), "the wither can break the shell");
        World.explode(null, Hull.getX() + 1.5, Hull.getY() + 0.5, Hull.getZ() + 0.5, 8.0f, Level.ExplosionInteraction.TNT);
        Helper.assertTrue(World.getBlockState(Hull).is(Scp5001WorldRegistry.Shell.get()), "an explosion removed the shell");
        BlockPos Piston = Hull.west();
        World.setBlock(Piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST), Block.UPDATE_ALL);
        World.setBlock(Piston.west(), Blocks.REDSTONE_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        BlockState Door = Scp5001WorldRegistry.EntryDoor.get().defaultBlockState();
        Helper.assertTrue(Door.getDestroySpeed(World, Hull) < 0.0f, "the entry door can be mined");
        Helper.runAfterDelay(20, () -> {
            Helper.assertTrue(World.getBlockState(Hull).is(Scp5001WorldRegistry.Shell.get()), "a piston moved the shell");
            Helper.assertTrue(!World.getBlockState(Hull.east()).is(Scp5001WorldRegistry.Shell.get()), "the shell was pushed");
            Helper.succeed();
        });
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001world_engine")
    public static void engineSuppliesNothing(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Pos = Helper.absolutePos(new BlockPos(8, 2, 8));
        World.setBlock(Pos, Scp5001WorldRegistry.Engine.get().defaultBlockState(), Block.UPDATE_ALL);
        BlockEntity Found = World.getBlockEntity(Pos);
        Helper.assertTrue(Found instanceof Scp5001EngineBlockEntity, "the engine has no block entity");
        Helper.assertTrue(!(Found instanceof WireDevice), "the engine is a wire device");
        for (Direction Side : Direction.values()) {
            Helper.assertTrue(World.getCapability(Capabilities.EnergyStorage.BLOCK, Pos, Side) == null, "the engine exposes energy on " + Side);
        }
        Helper.assertTrue(World.getCapability(Capabilities.EnergyStorage.BLOCK, Pos, null) == null, "the engine exposes energy");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001world_door")
    public static void doorsToggleTogether(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        BlockPos Origin = Helper.absolutePos(new BlockPos(4, 2, 10));
        Scp5001PanelDoorBlock Door = Scp5001WorldRegistry.EntryDoor.get();
        for (int Column = 0; Column < Door.columns(); Column++) {
            for (int Row = 0; Row < Door.rows(); Row++) {
                World.setBlock(Origin.east(Column).above(Row), Door.defaultBlockState().setValue(Door.column(), Column).setValue(Door.row(), Row), Block.UPDATE_ALL);
            }
        }
        BlockPos Corner = Origin.east(2).above(3);
        Door.setOpen(World, World.getBlockState(Corner), Corner, true);
        for (int Column = 0; Column < Door.columns(); Column++) {
            for (int Row = 0; Row < Door.rows(); Row++) {
                Helper.assertTrue(World.getBlockState(Origin.east(Column).above(Row)).getValue(Scp5001PanelDoorBlock.Open), "a door part stayed shut");
            }
        }
        Helper.assertTrue(World.getBlockState(Origin.east()).getCollisionShape(World, Origin.east()).isEmpty(), "the open door middle still blocks");
        Door.setOpen(World, World.getBlockState(Origin), Origin, false);
        Helper.assertTrue(!World.getBlockState(Corner).getValue(Scp5001PanelDoorBlock.Open), "a door part stayed open");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001world_door")
    public static void doorsSealTheirOpenings(GameTestHelper Helper) {
        for (long Each : new long[]{7L, 1234L}) {
            List<String> Problems = Scp5001PlanCheck.doors(Scp5001Plan.build(Each));
            Helper.assertTrue(Problems.isEmpty(), "door openings leak for seed " + Each + ": " + Problems);
        }
        List<String> Gaps = new ArrayList<>();
        for (Scp5001PanelDoorBlock Door : List.of(Scp5001WorldRegistry.Door.get(), Scp5001WorldRegistry.EntryDoor.get())) {
            for (int Column = 0; Column < Door.columns(); Column++) {
                for (int Row = 0; Row < Door.rows(); Row++) {
                    BlockState State = Door.defaultBlockState().setValue(Scp5001PanelDoorBlock.Facing, Direction.NORTH).setValue(Scp5001PanelDoorBlock.Open, false)
                            .setValue(Door.column(), Column).setValue(Door.row(), Row);
                    String Gap = Scp5001ModelCover.throughGap(State);
                    if (Gap != null) {
                        Gaps.add(Gap);
                    }
                    Helper.assertTrue(State.getCollisionShape(Helper.getLevel(), BlockPos.ZERO).bounds().getXsize() >= 1.0, State + " does not span its cell when shut");
                }
            }
        }
        Helper.assertTrue(Gaps.isEmpty(), Gaps.size() + " closed door parts can be seen through, first: " + String.join(" | ", Gaps.subList(0, Math.min(3, Gaps.size()))));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001world_site")
    public static void siteIsUniqueAndFar(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel().getServer().overworld();
        Scp5001Sites Data = Scp5001Sites.get(World.getServer());
        Data.forget();
        BlockPos Reader = new BlockPos(0, 80, 0);
        Scp5001Sites First = Scp5001Sites.ensure(World.getServer(), Reader, 900, 2400, World.getRandom());
        BlockPos Centre = First.centre();
        double Distance = Math.hypot(Centre.getX(), Centre.getZ());
        Helper.assertTrue(Distance >= 890 && Distance <= 2410, "site is " + Distance + " blocks away");
        Scp5001Sites Second = Scp5001Sites.ensure(World.getServer(), new BlockPos(5000, 80, 5000), 900, 2400, World.getRandom());
        Helper.assertTrue(Second.centre().equals(Centre), "a second reading moved the site");
        Helper.assertTrue(Scp5001Sites.farFromStrongholds(World, Centre.getX(), Centre.getZ()), "the site is near a stronghold");
        FakePlayer Player = new FakePlayer(World, new GameProfile(UUID.randomUUID(), "reader5001"));
        Player.moveTo(0.5, 80, 0.5);
        Investigation Case = new Investigation(Scp5001WorldRegistry.DocumentId, World.getGameTime());
        Scp5001FacilityCondition Condition = new Scp5001FacilityCondition(900, 2400, 48, 64);
        List<Component> Notes = Condition.begin(Player, Case);
        BlockPos Hatch = Data.door();
        boolean Rough = false;
        for (Component Line : Notes) {
            if (Line.getContents() instanceof TranslatableContents Words && Words.getKey().equals("archive.scp_unleashed.notes.site")) {
                int ShownX = (Integer) Words.getArgs()[0];
                int ShownZ = (Integer) Words.getArgs()[1];
                Rough = Math.abs(ShownX - Hatch.getX()) <= 53 && Math.abs(ShownZ - Hatch.getZ()) <= 53;
            }
        }
        Helper.assertTrue(Rough, "the notes do not give rough coordinates near the hatch");
        CompoundTag State = Case.getState();
        Helper.assertTrue(State.getInt("X") == Hatch.getX() && State.getInt("Z") == Hatch.getZ() && State.getInt("Y") == Hatch.getY(), "the case does not point at the door");
        Helper.assertTrue(Data.version() == Scp5001Plan.Version && !Data.legacy(), "a fresh site is not on the current plan version");
        CompoundTag Old = Data.save(new CompoundTag(), World.registryAccess());
        Old.remove("Version");
        Helper.assertTrue(Scp5001Sites.load(Old, World.registryAccess()).legacy(), "an old site is not treated as legacy");
        Helper.assertTrue(Data.bounds().getLength().getX() + 1 == Scp5001Plan.Width, "the site bounds are wrong");
        Data.forget();
        Helper.succeed();
    }

    private static Scp5001Sites standin(ServerLevel World) {
        Scp5001Sites Data = new Scp5001Sites();
        Data.plan(FarX, FarZ, World.getMinBuildHeight() + 10, Seed);
        return Data;
    }

    private static int mismatches(ServerLevel World, Scp5001Sites Data, Scp5001Plan Plan, ChunkPos Pos) {
        BlockPos Origin = Data.origin();
        int[] Count = {0};
        int MinX = Pos.getMinBlockX() - Origin.getX();
        int MinZ = Pos.getMinBlockZ() - Origin.getZ();
        java.util.Set<Long> Props = new java.util.HashSet<>();
        for (Scp5001Slot Each : Plan.slotsIn(MinX, MinZ, MinX + 15, MinZ + 15)) {
            for (int Dy = -1; Dy <= 3; Dy++) {
                Props.add(BlockPos.asLong(Each.X(), Each.Y() + Dy, Each.Z()));
            }
            Props.add(BlockPos.asLong(Each.X(), Each.Ceiling(), Each.Z()));
        }
        for (int Phase = 0; Phase <= 1; Phase++) {
            Plan.paint(MinX, MinZ, MinX + 15, MinZ + 15, Phase, (X, Y, Z, Text) -> {
                if (Props.contains(BlockPos.asLong(X, Y, Z))) {
                    return;
                }
                BlockState Wanted = Scp5001Emitter.state(Text);
                if (World.getBlockState(Origin.offset(X, Y, Z)) != Wanted) {
                    Count[0]++;
                }
            });
        }
        return Count[0];
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp5001world_emit")
    public static void emissionMatchesPlan(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp5001Sites Data = standin(World);
        Scp5001Plan Plan = Scp5001Emitter.plan(Seed);
        BlockPos Origin = Data.origin();
        ChunkPos Shell = new ChunkPos(Origin.offset(Scp5001Plan.CentreX, 0, 0));
        ChunkPos Middle = new ChunkPos(Origin.offset(Scp5001Plan.CentreX, 0, Scp5001Plan.CentreZ));
        BlockPos Corner = Origin.offset(Shell.getMinBlockX() - Origin.getX(), 20, 0);
        World.getChunk(Corner);
        World.getChunk(Shell.x, Shell.z);
        World.getChunk(Middle.x, Middle.z);
        World.setBlock(Origin.offset(Scp5001Plan.CentreX, 30, 5), Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Origin.offset(Scp5001Plan.CentreX + 1, 31, 5), Blocks.DIAMOND_ORE.defaultBlockState(), Block.UPDATE_ALL);
        for (int Step = 0; Step < 4; Step++) {
            Scp5001Emitter.step(World, Data, Shell, Step);
        }
        CompoundTag Saved = Data.save(new CompoundTag(), World.registryAccess());
        Scp5001Sites Restarted = Scp5001Sites.load(Saved, World.registryAccess());
        Helper.assertTrue(!Restarted.done(Shell.toLong()), "a half built chunk was marked done");
        Scp5001Emitter.emitChunk(World, Restarted, Shell);
        Scp5001Emitter.emitChunk(World, Restarted, Middle);
        Helper.assertTrue(Restarted.done(Shell.toLong()) && Restarted.done(Middle.toLong()), "built chunks were not recorded");
        Scp5001Sites Again = Scp5001Sites.load(Restarted.save(new CompoundTag(), World.registryAccess()), World.registryAccess());
        Helper.assertTrue(Again.done(Shell.toLong()) && Again.finished() == 2, "progress did not survive a save");
        int Wrong = mismatches(World, Again, Plan, Shell) + mismatches(World, Again, Plan, Middle);
        Helper.assertTrue(Wrong == 0, Wrong + " cells differ from the plan");
        Helper.assertTrue(!World.getBlockState(Origin.offset(Scp5001Plan.CentreX + 1, 31, 5)).is(Blocks.DIAMOND_ORE), "a foreign block survived inside");
        Helper.assertTrue(World.getFluidState(Origin.offset(Scp5001Plan.CentreX, 30, 5)).isEmpty(), "fluid survived inside the box");
        for (int Dx = 0; Dx < 16; Dx++) {
            BlockPos Probe = new BlockPos(Shell.getMinBlockX() + Dx, Origin.getY() + 50, Origin.getZ());
            if (!Scp5001Plan.hullCell(Probe.getX() - Origin.getX(), 50, 0)) {
                Helper.assertTrue(!World.getBlockState(Probe).is(Scp5001WorldRegistry.Shell.get()), "the emitter wrote outside the oval hull");
            }
        }
        Scp5001Emitter.emitChunk(World, Again, Middle);
        Helper.assertTrue(mismatches(World, Again, Plan, Middle) == 0, "a second pass changed the chunk");
        BlockState Core = World.getBlockState(Origin.offset(Scp5001Plan.CentreX, Scp5001Plan.floorY(0) + 1, Scp5001Plan.CentreZ));
        Helper.assertTrue(!Core.isAir(), "the chamber core anchor was not placed");
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 200, batch = "scp5001world_blockentity")
    public static void emissionClearsBlockEntities(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp5001Sites Data = new Scp5001Sites();
        Data.plan(FarX + 4000, FarZ, World.getMinBuildHeight() + 10, Seed);
        Scp5001Plan Plan = Scp5001Emitter.plan(Seed);
        BlockPos Origin = Data.origin();
        ChunkPos Pos = new ChunkPos(Origin.offset(Scp5001Plan.CentreX, 0, 0));
        net.minecraft.world.level.chunk.LevelChunk Chunk = World.getChunk(Pos.x, Pos.z);
        java.util.List<BlockPos> Spots = new java.util.ArrayList<>();
        for (int Dx = 0; Dx < 16 && Spots.size() < 3; Dx++) {
            for (int Dz = 0; Dz < 16 && Spots.size() < 3; Dz++) {
                int X = Pos.getMinBlockX() + Dx - Origin.getX();
                int Z = Pos.getMinBlockZ() + Dz - Origin.getZ();
                if (Plan.get(X, 46, Z).equals(Scp5001Blocks.Graphite)) {
                    Spots.add(Origin.offset(X, 46, Z));
                }
            }
        }
        Helper.assertTrue(Spots.size() == 3, "no filler cells to test with");
        World.setBlock(Spots.get(0), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (World.getBlockEntity(Spots.get(0)) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity Chest) {
            Chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
        }
        World.setBlock(Spots.get(1), Blocks.SPAWNER.defaultBlockState(), Block.UPDATE_ALL);
        World.setBlock(Spots.get(2), Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        Chunk.removeBlockEntity(Spots.get(2));
        CompoundTag Dummy = new CompoundTag();
        Dummy.putString("id", "DUMMY");
        Dummy.putInt("x", Spots.get(2).getX());
        Dummy.putInt("y", Spots.get(2).getY());
        Dummy.putInt("z", Spots.get(2).getZ());
        Chunk.setBlockEntityNbt(Dummy);
        Helper.assertTrue(Chunk.getBlockEntityNbt(Spots.get(2)) != null, "the pending block entity was not staged");
        Scp5001Emitter.emitChunk(World, Data, Pos);
        for (BlockPos Spot : Spots) {
            Helper.assertTrue(World.getBlockState(Spot).is(Scp5001WorldRegistry.Graphite.get()), "a test cell was not overwritten");
            Helper.assertTrue(Chunk.getBlockEntities().get(Spot) == null, "a stale block entity survived at " + Spot);
            Helper.assertTrue(Chunk.getBlockEntityNbt(Spot) == null, "pending block entity data survived at " + Spot);
        }
        net.minecraft.world.phys.AABB Area = new net.minecraft.world.phys.AABB(Spots.get(0)).inflate(4.0);
        Helper.assertTrue(World.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, Area).isEmpty(), "the overwritten chest dropped its items");
        for (Scp5001Anchor Each : Plan.anchors()) {
            BlockPos At = Origin.offset(Each.X(), Each.Y(), Each.Z());
            if (new ChunkPos(At).equals(Pos) && World.getBlockState(At).hasBlockEntity()) {
                Helper.assertTrue(World.getBlockEntity(At) != null, "a placed block lost its block entity");
            }
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001world_xray")
    public static void wallBlocksNeverHideOpenFaces(GameTestHelper Helper) {
        List<String> Problems = new ArrayList<>();
        int Checked = 0;
        for (Block Each : BuiltInRegistries.BLOCK) {
            ResourceLocation Id = BuiltInRegistries.BLOCK.getKey(Each);
            if (!Scp_unleashed.ModId.equals(Id.getNamespace()) || !Id.getPath().startsWith("scp5001_") || Each instanceof StairBlock || Each instanceof SlabBlock) {
                continue;
            }
            for (BlockState State : Each.getStateDefinition().getPossibleStates()) {
                Scp5001ModelCover.Cover Found = Scp5001ModelCover.cover(State);
                if (Found != null) {
                    Problems.addAll(Found.Problems());
                }
                if (!State.canOcclude()) {
                    continue;
                }
                if (Found == null) {
                    Problems.add(State + " occludes but has no readable model");
                    continue;
                }
                if (!Scp5001WallFaces.known(State) && !State.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                    Problems.add(State + " occludes without an occlusion table row");
                }
                Problems.addAll(Scp5001ModelCover.leaks(State, Found));
                if (State.getLightBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO) < 15) {
                    Problems.add(State + " lets light through the wall");
                }
                Checked++;
            }
        }
        Scp5001ModelCover.Cover Panel = Scp5001ModelCover.cover(Scp5001WorldRegistry.Panel.get().defaultBlockState());
        int Recessed = 0;
        for (boolean[][] Side : Panel.Sides()) {
            for (boolean[] Row : Side) {
                for (boolean Cell : Row) {
                    Recessed += Cell ? 0 : 1;
                }
            }
        }
        Helper.assertTrue(Recessed > 0, "the checker no longer sees the inset panel recess, so a full occluder would slip through");
        Helper.assertTrue(Checked >= 30, "only " + Checked + " occluding states were checked");
        Helper.assertTrue(Problems.isEmpty(), Problems.size() + " x-ray problems, first: " + String.join(" | ", Problems.subList(0, Math.min(4, Problems.size()))));
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = 600, batch = "scp5001world_wallscan")
    public static void builtWallsAreClosed(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Scp5001Sites Data = standin(World);
        BlockPos Origin = Data.origin();
        int[][] Picks = {{Scp5001Plan.CentreX, Scp5001Plan.CentreZ}, {Scp5001Plan.CentreX - 40, Scp5001Plan.CentreZ}, {Scp5001Plan.CentreX - 56, Scp5001Plan.CentreZ},
                {Scp5001Plan.CentreX + 30, Scp5001Plan.CentreZ + 30}, {Scp5001Plan.CentreX, 12}, {Scp5001Plan.CentreX + 16, 12}};
        List<ChunkPos> Built = new ArrayList<>();
        for (int[] Pick : Picks) {
            ChunkPos Pos = new ChunkPos(Origin.offset(Pick[0], 0, Pick[1]));
            if (!Built.contains(Pos)) {
                Built.add(Pos);
                World.getChunk(Pos.x, Pos.z);
                Scp5001Emitter.emitChunk(World, Data, Pos);
            }
        }
        Map<BlockState, Scp5001ModelCover.Cover> Covers = new HashMap<>();
        List<String> Problems = new ArrayList<>();
        int[] Walls = {0};
        for (ChunkPos Pos : Built) {
            for (int X = Pos.getMinBlockX(); X <= Pos.getMaxBlockX(); X++) {
                for (int Z = Pos.getMinBlockZ(); Z <= Pos.getMaxBlockZ(); Z++) {
                    for (int Y = Origin.getY(); Y < Origin.getY() + Scp5001Plan.Height; Y++) {
                        BlockPos Here = new BlockPos(X, Y, Z);
                        if (!Scp5001Plan.interiorCell(X - Origin.getX(), Y - Origin.getY(), Z - Origin.getZ()) || !World.getBlockState(Here).isAir()) {
                            continue;
                        }
                        for (Direction Side : Direction.values()) {
                            BlockPos Wall = Here.relative(Side);
                            if (!Built.contains(new ChunkPos(Wall))) {
                                continue;
                            }
                            BlockState State = World.getBlockState(Wall);
                            if (State.isAir()) {
                                continue;
                            }
                            String Why = wallProblem(World, Wall, State, Side, Built, Covers);
                            Walls[0]++;
                            if (Why != null && Problems.size() < 64) {
                                Problems.add(Why + " at " + Wall.subtract(Origin).toShortString());
                            }
                        }
                    }
                }
            }
        }
        Helper.assertTrue(Walls[0] > 2000, "only " + Walls[0] + " wall faces were scanned");
        Helper.assertTrue(Problems.isEmpty(), Problems.size() + " open walls, first: " + String.join(" | ", Problems.subList(0, Math.min(4, Problems.size()))));
        Helper.succeed();
    }

    private static String wallProblem(ServerLevel World, BlockPos Wall, BlockState State, Direction Side, List<ChunkPos> Built, Map<BlockState, Scp5001ModelCover.Cover> Covers) {
        ResourceLocation Id = BuiltInRegistries.BLOCK.getKey(State.getBlock());
        if (State.is(Blocks.STRUCTURE_VOID) || State.is(Blocks.BARRIER) || State.is(Blocks.LIGHT) || !State.getFluidState().isEmpty()) {
            return "placeholder " + Id + " in the wall layer";
        }
        if (!State.canOcclude()) {
            return null;
        }
        if (!Scp_unleashed.ModId.equals(Id.getNamespace()) || !Id.getPath().startsWith("scp5001_")) {
            return "foreign block " + Id + " in the wall layer";
        }
        Scp5001ModelCover.Cover Own = Covers.computeIfAbsent(State, Scp5001ModelCover::cover);
        if (Own == null) {
            return null;
        }
        for (Direction Back : Direction.values()) {
            if (Back == Side.getOpposite()) {
                continue;
            }
            BlockPos Behind = Wall.relative(Back);
            if (!Built.contains(new ChunkPos(Behind))) {
                continue;
            }
            BlockState Other = World.getBlockState(Behind);
            if (Other.isAir() || Block.shouldRenderFace(Other, World, Behind, Back.getOpposite(), Wall)) {
                continue;
            }
            Scp5001ModelCover.Cover Theirs = Covers.computeIfAbsent(Other, Scp5001ModelCover::cover);
            boolean[][] Mine = Own.Sides()[Scp5001ModelCover.index(Back)];
            boolean[][] Hidden = Theirs == null ? null : Theirs.Sides()[Scp5001ModelCover.index(Back.getOpposite())];
            for (int A = 0; A < Scp5001ModelCover.Grid; A++) {
                for (int B = 0; B < Scp5001ModelCover.Grid; B++) {
                    if ((Hidden == null || Hidden[A][B]) && !Mine[A][B]) {
                        return BuiltInRegistries.BLOCK.getKey(Other.getBlock()).getPath() + " hides its face behind " + Id.getPath() + " whose model is open on its "
                                + Back.getName() + " side";
                    }
                }
            }
        }
        return null;
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp5001world_palette")
    public static void palettesPlaceByKind(GameTestHelper Helper) {
        ServerLevel World = Helper.getLevel();
        Map<String, Scp5001Palettes.Role> Before = Scp5001Palettes.roles();
        try {
            Map<ResourceLocation, JsonElement> Files = new HashMap<>();
            Files.put(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "test"), JsonParser.parseString(
                    "{\"roles\":{\"tall_test\":{\"kind\":\"floor_tall\",\"entries\":[{\"block\":\"minecraft:tall_grass\",\"weight\":2}]},"
                            + "\"floor_test\":{\"entries\":[{\"block\":\"minecraft:lectern\",\"weight\":1}]},"
                            + "\"wall_test\":{\"kind\":\"wall\",\"entries\":[{\"block\":\"minecraft:wall_torch\",\"weight\":1}]}}}"));
            Map<String, Scp5001Palettes.Role> Parsed = Scp5001Palettes.parse(Files);
            Helper.assertTrue(Parsed.get("floor_test").Kind().equals("floor"), "a role without a kind is not floor");
            Scp5001Palettes.replace(Parsed);
            BlockPos Base = Helper.absolutePos(new BlockPos(3, 3, 3));
            BlockPos Origin = Base.offset(-10, -2, -10);
            for (int Dx = 0; Dx <= 8; Dx++) {
                for (int Dy = 0; Dy <= 3; Dy++) {
                    World.setBlock(Base.east(Dx).above(Dy), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            Scp5001Emitter.prop(World, Origin, new Scp5001Slot("tall_test", 10, 2, 10, "north", false, 6, 0), Seed);
            Helper.assertTrue(World.getBlockState(Base).is(Blocks.TALL_GRASS) && World.getBlockState(Base.above()).is(Blocks.TALL_GRASS), "floor_tall did not set both halves");
            Helper.assertTrue(World.getBlockState(Base.above()).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                    == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER, "the upper half is wrong");
            Scp5001Emitter.prop(World, Origin, new Scp5001Slot("floor_test", 12, 2, 10, "east", false, 6, 0), Seed);
            Helper.assertTrue(World.getBlockState(Base.east(2)).is(Blocks.LECTERN) && World.getBlockState(Base.east(2)).getValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST, "floor prop facing is wrong");
            Scp5001Emitter.prop(World, Origin, new Scp5001Slot("missing_role", 14, 2, 10, "north", true, 6, 0), Seed);
            Helper.assertTrue(World.getBlockState(Base.east(4)).isAir(), "a missing role placed something");
            Scp5001Emitter.prop(World, Origin, new Scp5001Slot("wall_test", 16, 2, 10, "south", false, 6, 0), Seed);
            Helper.assertTrue(World.getBlockState(Base.east(6).above()).isAir(), "a wall prop was hung without a wall");
        } finally {
            Scp5001Palettes.replace(Before);
        }
        Helper.succeed();
    }
}
