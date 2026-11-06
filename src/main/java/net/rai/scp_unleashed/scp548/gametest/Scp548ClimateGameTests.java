package net.rai.scp_unleashed.scp548.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerClimate;
import net.rai.scp_unleashed.cooler.CoolerFreeze;
import net.rai.scp_unleashed.scp538.gametest.Scp538TestPlayer;
import net.rai.scp_unleashed.scp548.Scp548Frost;
import net.rai.scp_unleashed.scp548.Scp548Spider;
import net.rai.scp_unleashed.scp548.Scp548Swarm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp548ClimateGameTests {
    public static final String Arena = Scp548GameTests.Arena;
    public static final BlockPos Middle = Scp548GameTests.Middle;
    public static final BlockPos CoolerPos = Scp548GameTests.CoolerPos;
    public static final String Desert = "minecraft:desert";
    public static final String Snowy = "minecraft:snowy_plains";
    public static final String Plains = "minecraft:plains";
    public static final int Watch = 200;
    public static final int PondLow = 5;
    public static final int PondHigh = 9;

    private Scp548ClimateGameTests() {
    }

    private static void fillBiome(GameTestHelper Helper, String Biome) {
        BlockPos First = Helper.absolutePos(new BlockPos(-1, 0, -1));
        BlockPos Second = Helper.absolutePos(new BlockPos(15, 8, 15));
        String Command = String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s", Math.min(First.getX(), Second.getX()), Math.min(First.getY(), Second.getY()),
                Math.min(First.getZ(), Second.getZ()), Math.max(First.getX(), Second.getX()), Math.max(First.getY(), Second.getY()), Math.max(First.getZ(), Second.getZ()), Biome);
        MinecraftServer Server = Helper.getLevel().getServer();
        Server.getCommands().performPrefixedCommand(Server.createCommandSourceStack().withSuppressedOutput().withLevel(Helper.getLevel()), Command);
    }

    private static Runnable climate(GameTestHelper Helper, String Biome, int Timeout) {
        ServerLevel World = Helper.getLevel();
        String Original = World.getBiome(Helper.absolutePos(Middle)).unwrapKey().map(Key -> Key.location().toString()).orElse(Plains);
        fillBiome(Helper, Biome);
        Helper.assertTrue(World.getBiome(Helper.absolutePos(Middle)).is(ResourceLocation.parse(Biome)), "fillbiome did not switch the arena to " + Biome);
        AtomicBoolean Done = new AtomicBoolean(false);
        Runnable Restore = () -> {
            if (Done.compareAndSet(false, true)) {
                fillBiome(Helper, Original);
            }
        };
        Helper.onEachTick(() -> {
            if (Helper.getTick() >= Timeout - 2) {
                Restore.run();
            }
        });
        return Restore;
    }

    private static Runnable noGriefing(GameTestHelper Helper, int Timeout) {
        ServerLevel World = Helper.getLevel();
        GameRules.BooleanValue Rule = World.getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
        boolean Before = Rule.get();
        Rule.set(false, World.getServer());
        AtomicBoolean Done = new AtomicBoolean(false);
        Runnable Restore = () -> {
            if (Done.compareAndSet(false, true)) {
                Rule.set(Before, World.getServer());
            }
        };
        Helper.onEachTick(() -> {
            if (Helper.getTick() >= Timeout - 2) {
                Restore.run();
            }
        });
        return Restore;
    }

    private static List<BlockPos> shell(BlockPos Centre) {
        return List.of(Centre.north(), Centre.south(), Centre.east(), Centre.west(), Centre.above());
    }

    private static List<BlockPos> enclose(GameTestHelper Helper, BlockPos Centre, BlockState... Walls) {
        List<BlockPos> Around = shell(Centre);
        for (int Index = 0; Index < Around.size(); Index++) {
            Helper.setBlock(Around.get(Index), Walls[Math.min(Index, Walls.length - 1)]);
        }
        return Around;
    }

    private static AtomicInteger frostWatch(GameTestHelper Helper, List<BlockPos> Around) {
        AtomicInteger Most = new AtomicInteger();
        Helper.onEachTick(() -> {
            for (BlockPos Rel : Around) {
                Most.accumulateAndGet(Scp548Frost.progress(Helper.getLevel(), Helper.absolutePos(Rel)), Math::max);
            }
        });
        return Most;
    }

    private static Scp548Spider hostile(GameTestHelper Helper, BlockPos Rel) {
        Scp548Spider Spider = Scp548GameTests.spider(Helper, Vec3.atBottomCenterOf(Rel), false);
        Helper.runAfterDelay(1, () -> {
            Spider.setChill(0);
            Spider.scan(Helper.getLevel());
        });
        return Spider;
    }

    private static boolean intact(GameTestHelper Helper, List<BlockPos> Around, List<Block> Expected) {
        for (int Index = 0; Index < Around.size(); Index++) {
            if (!Helper.getBlockState(Around.get(Index)).is(Expected.get(Index))) {
                return false;
            }
        }
        return true;
    }

    private static List<Block> blocksOf(GameTestHelper Helper, List<BlockPos> Around) {
        List<Block> Found = new ArrayList<>();
        for (BlockPos Rel : Around) {
            Found.add(Helper.getBlockState(Rel).getBlock());
        }
        return Found;
    }

    private static int drops(GameTestHelper Helper) {
        AABB Box = new AABB(Helper.absolutePos(BlockPos.ZERO)).expandTowards(15.0, 8.0, 15.0).inflate(1.0);
        return Helper.getLevel().getEntitiesOfClass(ItemEntity.class, Box).size();
    }

    private static Block modBlock(String Path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Path));
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp548_frostWarmBreaks")
    public static void frostWarmBreaks(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Desert, 400);
        List<BlockPos> Around = enclose(Helper, Middle, Blocks.GLASS.defaultBlockState());
        Scp548Spider Spider = hostile(Helper, Middle);
        AtomicInteger Most = frostWatch(Helper, Around);
        AtomicLong Broke = new AtomicLong(-1);
        Helper.startSequence().thenIdle(3).thenExecute(() -> {
            Helper.assertTrue(Spider.isHostile(), "The spider is not hostile without a cooler " + Scp548GameTests.describe(Spider));
            Helper.assertTrue(Spider.climate() == CoolerClimate.Hot, "The desert does not count as hot: " + Spider.climate());
        }).thenWaitUntil(() -> {
            Helper.assertTrue(Around.stream().anyMatch(Rel -> Helper.getBlockState(Rel).isAir()), "No glass has shattered yet, most frost " + Most.get());
            Broke.compareAndSet(-1, Helper.getTick());
        }).thenExecute(() -> {
            try {
                Helper.assertTrue(Most.get() >= Scp548Frost.BaseStages / 2, "The glass shattered without visibly frosting over first: " + Most.get() + " stages");
                Helper.assertTrue(Broke.get() >= 60, "The glass shattered too quickly, at tick " + Broke.get());
                Helper.assertTrue(drops(Helper) == 0, "Shattered blocks dropped items");
            } finally {
                Restore.run();
                Scp548GameTests.sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 60, batch = "scp548_frostColdHolds")
    public static void frostColdHolds(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Snowy, Watch + 60);
        List<BlockPos> Around = enclose(Helper, Middle, Blocks.GLASS.defaultBlockState());
        List<Block> Expected = blocksOf(Helper, Around);
        Scp548Spider Spider = hostile(Helper, Middle);
        AtomicInteger Most = frostWatch(Helper, Around);
        Helper.startSequence().thenIdle(Watch).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.climate() == CoolerClimate.Cold, "Snowy plains do not count as cold: " + Spider.climate());
                Helper.assertTrue(Spider.isHostile(), "Without a cooler the spider should still be hostile in the cold " + Scp548GameTests.describe(Spider));
                Helper.assertTrue(intact(Helper, Around, Expected), "A spider broke its enclosure in a cold biome");
                Helper.assertTrue(Most.get() == 0, "A spider frosted blocks in a cold biome: " + Most.get());
            } finally {
                Restore.run();
                Scp548GameTests.sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 80, batch = "scp548_frostCoolerHolds")
    public static void frostCoolerHolds(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Desert, Watch + 80);
        AtomicBoolean Fed = new AtomicBoolean(true);
        Scp548GameTests.cooler(Helper, Fed);
        BlockPos Cell = CoolerPos.south(3);
        List<BlockPos> Around = enclose(Helper, Cell, Blocks.GLASS.defaultBlockState());
        List<Block> Expected = blocksOf(Helper, Around);
        Scp548Spider Spider = Scp548GameTests.spider(Helper, Vec3.atBottomCenterOf(Cell), false);
        AtomicInteger Most = frostWatch(Helper, Around);
        Helper.startSequence().thenIdle(20).thenExecute(() -> {
            Helper.assertTrue(Scp548GameTests.coolerOn(Helper), "The fed cooler did not switch on");
            double Reach = CoolerBlockEntity.effectiveRadius(Helper.getLevel(), Helper.absolutePos(CoolerPos));
            Helper.assertTrue(Reach == CoolerBlockEntity.Radius * CoolerBlockEntity.HotShare, "The cooler's reach is not cut in the desert: " + Reach);
            Spider.setChill(0);
            Spider.scan(Helper.getLevel());
            Helper.assertTrue(Spider.isContained(), "A spider close to a powered cooler is not contained in the desert " + Scp548GameTests.describe(Spider));
        }).thenIdle(Watch).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.isContained(), "The spider broke out of containment " + Scp548GameTests.describe(Spider));
                Helper.assertTrue(intact(Helper, Around, Expected), "A contained spider broke its enclosure");
                Helper.assertTrue(Most.get() == 0, "A contained spider frosted blocks: " + Most.get());
            } finally {
                Restore.run();
                Scp548GameTests.sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Watch + 60, batch = "scp548_frostNoGriefing")
    public static void frostNoGriefing(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Desert, Watch + 60);
        Runnable Allow = noGriefing(Helper, Watch + 60);
        List<BlockPos> Around = enclose(Helper, Middle, Blocks.GLASS.defaultBlockState());
        List<Block> Expected = blocksOf(Helper, Around);
        Scp548Spider Spider = hostile(Helper, Middle);
        AtomicInteger Most = frostWatch(Helper, Around);
        Helper.startSequence().thenIdle(Watch).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.isHostile(), "The spider is not hostile " + Scp548GameTests.describe(Spider));
                Helper.assertTrue(intact(Helper, Around, Expected), "A spider broke blocks with mobGriefing off");
                Helper.assertTrue(Most.get() == 0, "A spider frosted blocks with mobGriefing off: " + Most.get());
            } finally {
                Allow.run();
                Restore.run();
                Scp548GameTests.sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 380, batch = "scp548_frostProtected")
    public static void frostProtected(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Desert, 380);
        Block Reinforced = modBlock("reinforced_glass");
        Block Steel = modBlock("steel_block");
        Helper.assertTrue(Reinforced != Blocks.AIR && Steel != Blocks.AIR, "The reinforced glass or steel block is missing");
        List<BlockPos> Around = enclose(Helper, Middle, Blocks.OBSIDIAN.defaultBlockState(), Blocks.CHEST.defaultBlockState(), Reinforced.defaultBlockState(),
                Steel.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState());
        List<Block> Expected = blocksOf(Helper, Around);
        BlockPos ChestRel = Middle.south();
        ChestBlockEntity Chest = Helper.getBlockEntity(ChestRel);
        Chest.setItem(0, new ItemStack(Items.DIAMOND, 3));
        Scp548Spider Spider = hostile(Helper, Middle);
        AtomicInteger Most = frostWatch(Helper, Around);
        Helper.startSequence().thenIdle(320).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.isHostile() && Spider.climate() == CoolerClimate.Hot, "The spider is not hostile in the heat " + Scp548GameTests.describe(Spider));
                Helper.assertTrue(intact(Helper, Around, Expected), "A protected block broke: " + blocksOf(Helper, Around));
                ChestBlockEntity Still = Helper.getBlockEntity(ChestRel);
                Helper.assertTrue(Still.getItem(0).is(Items.DIAMOND) && Still.getItem(0).getCount() == 3, "The chest lost its contents");
                int Required = Scp548Frost.required(Helper.getLevel(), Helper.absolutePos(ChestRel), Helper.getBlockState(ChestRel));
                Helper.assertTrue(Most.get() < Required, "The chest frosted all the way: " + Most.get() + " of " + Required);
                Helper.assertFalse(Scp548Frost.frostable(Helper.getLevel(), Helper.absolutePos(Middle.north()), Helper.getBlockState(Middle.north())), "Obsidian can be frosted");
                Helper.assertFalse(Scp548Frost.frostable(Helper.getLevel(), Helper.absolutePos(Middle.east()), Helper.getBlockState(Middle.east())), "Reinforced glass can be frosted");
                Helper.assertFalse(Scp548Frost.frostable(Helper.getLevel(), Helper.absolutePos(Middle.west()), Helper.getBlockState(Middle.west())), "The steel block can be frosted");
                Helper.assertTrue(drops(Helper) == 0, "Something dropped items");
            } finally {
                Restore.run();
                Scp548GameTests.sweep(Helper);
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp548_coolerFreezesWater")
    public static void coolerFreezesWater(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Plains, 320);
        Helper.assertFalse(CoolerFreeze.isStillWater(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3)), "Flowing water counts as still water");
        Helper.assertFalse(CoolerFreeze.isStillWater(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true)), "A waterlogged slab counts as still water");
        List<BlockPos> Pond = new ArrayList<>();
        for (int X = PondLow; X <= PondHigh; X++) {
            for (int Z = PondLow; Z <= PondHigh; Z++) {
                Helper.setBlock(new BlockPos(X, 0, Z), Blocks.STONE);
                Helper.setBlock(new BlockPos(X, 1, Z), Blocks.WATER);
                Pond.add(new BlockPos(X, 1, Z));
            }
        }
        AtomicBoolean Fed = new AtomicBoolean(false);
        Scp548GameTests.cooler(Helper, Fed);
        BlockPos Origin = CoolerPos;
        Helper.startSequence().thenIdle(5).thenExecute(() -> {
            Helper.assertTrue(Pond.stream().allMatch(Rel -> CoolerFreeze.isStillWater(Helper.getBlockState(Rel))), "The pond is not still water before the cooler runs");
            Fed.set(true);
        }).thenIdle(70).thenExecute(() -> {
            Helper.assertTrue(Scp548GameTests.coolerOn(Helper), "The fed cooler did not switch on");
            int Frozen = 0;
            int NearestOpen = Integer.MAX_VALUE;
            int FarthestIce = 0;
            for (BlockPos Rel : Pond) {
                int Distance = (int) Rel.distSqr(Origin);
                if (Helper.getBlockState(Rel).is(Blocks.ICE)) {
                    Frozen++;
                    FarthestIce = Math.max(FarthestIce, Distance);
                } else {
                    NearestOpen = Math.min(NearestOpen, Distance);
                }
            }
            Helper.assertTrue(Frozen >= 3 && Frozen < Pond.size(), "Expected the pond to be part frozen, " + Frozen + " of " + Pond.size() + " are ice");
            Helper.assertTrue(FarthestIce <= NearestOpen, "The cooler skipped near water: ice reaches " + FarthestIce + " but water is open at " + NearestOpen);
        }).thenWaitUntil(() -> Helper.assertTrue(Pond.stream().allMatch(Rel -> Helper.getBlockState(Rel).is(Blocks.ICE)), "The pond has not frozen solid yet"))
                .thenExecute(() -> {
                    CoolerBlockEntity Device = Helper.getBlockEntity(CoolerPos);
                    Helper.assertTrue(Device.frozen() >= Pond.size(), "The cooler counted " + Device.frozen() + " frozen blocks");
                    Fed.set(false);
                }).thenIdle(CoolerBlockEntity.PowerGrace + 10).thenExecute(() -> {
                    try {
                        Helper.assertFalse(Scp548GameTests.coolerOn(Helper), "The cooler kept running without power");
                        Helper.assertTrue(Pond.stream().allMatch(Rel -> Helper.getBlockState(Rel).is(Blocks.ICE)), "The ice did not stay after the power cut");
                    } finally {
                        Restore.run();
                    }
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 100, batch = "scp548_hotCoolerShrinks")
    public static void hotCoolerShrinks(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Desert, 100);
        AtomicBoolean Fed = new AtomicBoolean(true);
        Scp548GameTests.cooler(Helper, Fed);
        Helper.startSequence().thenIdle(20).thenExecute(() -> {
            try {
                Helper.assertTrue(Scp548GameTests.coolerOn(Helper), "The fed cooler did not switch on");
                ServerLevel World = Helper.getLevel();
                Vec3 Near = Helper.absoluteVec(Vec3.atBottomCenterOf(CoolerPos.south(3)));
                Vec3 Far = Helper.absoluteVec(Vec3.atBottomCenterOf(CoolerPos.south(6)));
                Helper.assertTrue(CoolerClimate.at(World, Helper.absolutePos(CoolerPos)) == CoolerClimate.Hot, "The desert is not hot");
                Helper.assertTrue(Scp548Swarm.cooled(World, Near), "A spot 3 blocks from the cooler is not cooled in the desert");
                Helper.assertFalse(Scp548Swarm.cooled(World, Far), "A spot 6 blocks from the cooler is still cooled in the desert");
                Helper.assertTrue(CoolerBlockEntity.effectiveRadius(CoolerClimate.Mild) > 6.5, "The mild reach no longer covers 6 blocks");
            } finally {
                Restore.run();
            }
        }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 400, batch = "scp548_provokedRetaliates")
    public static void provokedRetaliates(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Plains, 400);
        AtomicBoolean Fed = new AtomicBoolean(true);
        Scp548GameTests.cooler(Helper, Fed);
        Scp548Spider Spider = Scp548GameTests.spider(Helper, Vec3.atBottomCenterOf(Middle), false);
        Scp548Spider Neighbour = Scp548GameTests.spider(Helper, Vec3.atBottomCenterOf(Middle.east()), false);
        Scp548Spider Distant = Scp548GameTests.spider(Helper, Vec3.atBottomCenterOf(CoolerPos.east(6).north(2)), false);
        Scp538TestPlayer Attacker = Scp548GameTests.player(Helper, Vec3.atBottomCenterOf(Middle.west(2)), "scp548_attacker", GameType.SURVIVAL);
        Attacker.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100.0);
        Attacker.setHealth(100.0f);
        Helper.startSequence().thenIdle(20).thenExecute(() -> {
            Helper.assertTrue(Scp548GameTests.coolerOn(Helper), "The fed cooler did not switch on");
            for (Scp548Spider Each : List.of(Spider, Neighbour, Distant)) {
                Each.setChill(0);
                Each.scan(Helper.getLevel());
                Helper.assertTrue(Each.isContained() && Each.getTarget() == null, "A spider by the cooler is not calm " + Scp548GameTests.describe(Each));
            }
            Spider.hurt(Helper.getLevel().damageSources().playerAttack(Attacker), 0.5f);
            Helper.assertTrue(Spider.isProvoked() && Spider.getTarget() == Attacker, "The struck spider did not turn on its attacker " + Scp548GameTests.describe(Spider));
            Helper.assertTrue(Neighbour.isProvoked() && Neighbour.getTarget() == Attacker, "The neighbouring spider did not join in " + Scp548GameTests.describe(Neighbour));
            Helper.assertFalse(Distant.isProvoked(), "A spider far away was provoked too");
            Helper.assertTrue(Spider.isContained(), "Provoking the spider broke its containment");
        }).thenWaitUntil(() -> Helper.assertTrue(Spider.bites() + Neighbour.bites() >= 1, "No provoked spider has bitten the attacker yet"))
                .thenExecute(() -> {
                    Vec3 Away = Helper.absoluteVec(Vec3.atBottomCenterOf(Middle)).add(0.0, 0.0, 18.0);
                    Attacker.teleportTo(Away.x, Away.y, Away.z);
                }).thenIdle(Scp548Spider.ProvokeTicks / Scp548Spider.SettleRate + 20).thenExecute(() -> {
                    try {
                        for (Scp548Spider Each : List.of(Spider, Neighbour)) {
                            Helper.assertFalse(Each.isProvoked(), "A spider stayed provoked after the attacker left " + Scp548GameTests.describe(Each));
                            Helper.assertTrue(Each.getTarget() == null, "A settled spider still has a target " + Scp548GameTests.describe(Each));
                            Helper.assertTrue(Each.isContained(), "A settled spider is not contained " + Scp548GameTests.describe(Each));
                        }
                    } finally {
                        Scp548GameTests.dismiss(Helper, Attacker);
                        Scp548GameTests.sweep(Helper);
                        Restore.run();
                    }
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp548_repeatedBites")
    public static void repeatedBites(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Plains, 320);
        Scp548Spider Spider = hostile(Helper, Middle);
        Scp538TestPlayer Prey = Scp548GameTests.player(Helper, Vec3.atBottomCenterOf(Middle.east(2)), "scp548_bitten", GameType.SURVIVAL);
        Prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        Prey.setHealth(200.0f);
        List<Long> Bites = new ArrayList<>();
        AtomicInteger Seen = new AtomicInteger();
        Helper.onEachTick(() -> {
            if (Spider.bites() > Seen.get()) {
                Seen.set(Spider.bites());
                Bites.add(Helper.getTick());
            }
        });
        Helper.startSequence().thenWaitUntil(() -> Helper.assertTrue(Bites.size() >= 4, "Only " + Bites.size() + " bites so far " + Scp548GameTests.describe(Spider)))
                .thenExecute(() -> {
                    try {
                        for (int Index = 1; Index < Bites.size(); Index++) {
                            long Gap = Bites.get(Index) - Bites.get(Index - 1);
                            Helper.assertTrue(Gap >= Scp548Spider.BiteCooldown, "Two bites landed " + Gap + " ticks apart: " + Bites);
                            Helper.assertTrue(Gap <= 60, "The spider took " + Gap + " ticks between bites: " + Bites);
                        }
                        Helper.assertTrue(Spider.getTarget() == Prey, "The spider lost interest in its prey " + Scp548GameTests.describe(Spider));
                    } finally {
                        Scp548GameTests.dismiss(Helper, Prey);
                        Scp548GameTests.sweep(Helper);
                        Restore.run();
                    }
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 320, batch = "scp548_climbsToPrey")
    public static void climbsToPrey(GameTestHelper Helper) {
        Runnable Restore = climate(Helper, Plains, 320);
        for (int X = 3; X <= 11; X++) {
            for (int Y = 2; Y <= 3; Y++) {
                for (int Z = 10; Z <= 12; Z++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
                }
            }
        }
        Scp548Spider Spider = hostile(Helper, Middle);
        Scp538TestPlayer Prey = Scp548GameTests.player(Helper, Vec3.atBottomCenterOf(new BlockPos(7, 4, 11)), "scp548_ledge", GameType.SURVIVAL);
        Prey.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        Prey.setHealth(200.0f);
        double Ledge = Helper.absoluteVec(Vec3.atBottomCenterOf(new BlockPos(7, 4, 11))).y;
        AtomicBoolean Climbed = new AtomicBoolean(false);
        Helper.onEachTick(() -> {
            if (Spider.onClimbable() && Spider.getY() > Helper.absoluteVec(Vec3.atBottomCenterOf(Middle)).y + 0.5) {
                Climbed.set(true);
            }
        });
        Helper.startSequence().thenIdle(3).thenExecute(() -> Helper.assertTrue(Spider.isHostile(), "The spider is not hostile " + Scp548GameTests.describe(Spider)))
                .thenWaitUntil(() -> Helper.assertTrue(Spider.bites() >= 1 || Spider.getY() >= Ledge - 0.05, "The spider has not reached the ledge: y " + Spider.getY() + " of " + Ledge + " " + Scp548GameTests.describe(Spider)))
                .thenExecute(() -> {
                    try {
                        Helper.assertTrue(Climbed.get(), "The spider reached its prey without climbing the wall");
                        Helper.assertTrue(Spider.getTarget() == Prey, "The spider is not after the player on the ledge " + Scp548GameTests.describe(Spider));
                    } finally {
                        Scp548GameTests.dismiss(Helper, Prey);
                        Scp548GameTests.sweep(Helper);
                        Restore.run();
                    }
                }).thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = 60, batch = "scp548_ignoresCreative")
    public static void ignoresCreative(GameTestHelper Helper) {
        Scp548Spider Spider = hostile(Helper, Middle);
        Scp538TestPlayer Builder = Scp548GameTests.player(Helper, Vec3.atBottomCenterOf(Middle.east(2)), "scp548_builder", GameType.CREATIVE);
        Scp538TestPlayer Ghost = Scp548GameTests.player(Helper, Vec3.atBottomCenterOf(Middle.west(2)), "scp548_ghost", GameType.SPECTATOR);
        Helper.startSequence().thenIdle(40).thenExecute(() -> {
            try {
                Helper.assertTrue(Spider.isHostile(), "The spider is not hostile " + Scp548GameTests.describe(Spider));
                Helper.assertFalse(Scp548Swarm.isPrey(Builder) || Scp548Swarm.isPrey(Ghost), "A creative or spectator player counts as prey");
                Helper.assertTrue(Spider.getTarget() == null, "The spider targeted a creative or spectator player " + Scp548GameTests.describe(Spider));
                Helper.assertTrue(Spider.bites() == 0, "The spider bit a creative or spectator player");
            } finally {
                Scp548GameTests.dismiss(Helper, Builder);
                Scp548GameTests.dismiss(Helper, Ghost);
                Scp548GameTests.sweep(Helper);
            }
        }).thenSucceed();
    }
}
