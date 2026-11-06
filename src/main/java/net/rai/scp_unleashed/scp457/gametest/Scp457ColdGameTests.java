package net.rai.scp_unleashed.scp457.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.cooler.CoolerBlock;
import net.rai.scp_unleashed.cooler.CoolerBlockEntity;
import net.rai.scp_unleashed.cooler.CoolerClimate;
import net.rai.scp_unleashed.cooler.CoolerRegistry;
import net.rai.scp_unleashed.scp457.Scp457;
import net.rai.scp_unleashed.scp457.Scp457AshPileBlock;
import net.rai.scp_unleashed.scp457.Scp457Cold;
import net.rai.scp_unleashed.scp457.Scp457Registry;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp457ColdGameTests {
    public static final String Arena = Scp457GameTests.Arena;
    public static final String Desert = "minecraft:desert";
    public static final String Snowy = "minecraft:snowy_plains";
    public static final String Plains = "minecraft:plains";
    public static final BlockPos CoolerSpot = new BlockPos(16, 0, 10);
    public static final int Window = Scp457Cold.SnuffTicks + 40;
    public static final int Survive = 140;
    public static final int Timeout = 300;

    private Scp457ColdGameTests() {
    }

    private static BlockPos coolerPos(GameTestHelper Helper) {
        return new BlockPos(CoolerSpot.getX(), Scp457GameTests.stand(Helper), CoolerSpot.getZ());
    }

    private static void fillBiome(GameTestHelper Helper, String Biome) {
        BlockPos First = Helper.absolutePos(new BlockPos(0, 0, 0));
        BlockPos Second = Helper.absolutePos(new BlockPos(31, 7, 31));
        String Command = String.format(Locale.ROOT, "fillbiome %d %d %d %d %d %d %s", Math.min(First.getX(), Second.getX()), Math.min(First.getY(), Second.getY()),
                Math.min(First.getZ(), Second.getZ()), Math.max(First.getX(), Second.getX()), Math.max(First.getY(), Second.getY()), Math.max(First.getZ(), Second.getZ()), Biome);
        MinecraftServer Server = Helper.getLevel().getServer();
        Server.getCommands().performPrefixedCommand(Server.createCommandSourceStack().withSuppressedOutput().withLevel(Helper.getLevel()), Command);
    }

    private static Runnable climate(GameTestHelper Helper, String Biome) {
        ServerLevel World = Helper.getLevel();
        String Original = World.getBiome(Helper.absolutePos(coolerPos(Helper))).unwrapKey().map(Key -> Key.location().toString()).orElse(Plains);
        fillBiome(Helper, Biome);
        Helper.assertTrue(World.getBiome(Helper.absolutePos(coolerPos(Helper))).is(ResourceLocation.parse(Biome)), "fillbiome did not switch the arena to " + Biome);
        AtomicBoolean Done = new AtomicBoolean(false);
        return () -> {
            if (Done.compareAndSet(false, true)) {
                fillBiome(Helper, Original);
            }
        };
    }

    private static void cooler(GameTestHelper Helper, boolean Fed) {
        BlockPos Spot = coolerPos(Helper);
        Helper.setBlock(Spot, CoolerRegistry.Cooler.get().defaultBlockState().setValue(CoolerBlock.Facing, Direction.SOUTH));
        Helper.onEachTick(() -> {
            if (Fed && Helper.getLevel().getBlockEntity(Helper.absolutePos(Spot)) instanceof CoolerBlockEntity Device) {
                Device.insertPower(Device.getPowerDemand());
            }
        });
    }

    private static boolean coolerOn(GameTestHelper Helper) {
        BlockEntity Found = Helper.getLevel().getBlockEntity(Helper.absolutePos(coolerPos(Helper)));
        return Found instanceof CoolerBlockEntity Device && Device.isActive();
    }

    private static Scp457 burningAt(GameTestHelper Helper, double Away) {
        BlockPos Spot = coolerPos(Helper);
        Scp457 Burning = Scp457GameTests.burning(Helper, Spot.getX() + 0.5, Spot.getZ() + 0.5 + Away);
        Burning.setNoAi(true);
        return Burning;
    }

    private static void assertFrozen(GameTestHelper Helper, Scp457 Burning, String Where) {
        Helper.assertTrue(Burning.isDeadOrDying() || Burning.isRemoved(), "SCP-457 is still burning " + Where + Scp457GameTests.state(Burning) + " chill " + Scp457Cold.chill(Burning));
        Helper.assertTrue(Burning.deathKind() == Scp457.DeathFrost, "SCP-457 died without the frost death " + Where + ": " + Burning.deathKind());
    }

    private static void killedIn(GameTestHelper Helper, String Biome, double Away, CoolerClimate Expected) {
        Scp457GameTests.begin(Helper);
        Runnable Restore = climate(Helper, Biome);
        cooler(Helper, true);
        Scp457 Burning = burningAt(Helper, Away);
        long[] Started = {-1L};
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(coolerOn(Helper), "the cooler never switched on"))
                .thenExecute(() -> {
                    Started[0] = Helper.getTick();
                    Helper.assertTrue(CoolerClimate.at(Helper.getLevel(), Helper.absolutePos(coolerPos(Helper))) == Expected, "the arena climate is not " + Expected);
                })
                .thenExecuteAfter(Scp457Cold.SnuffTicks / 2, () -> {
                    Helper.assertTrue(Burning.isAlive(), "SCP-457 went out too quickly in the cold");
                    Helper.assertTrue(Scp457Cold.chill(Burning) > 0 && Scp457Cold.flameScale(Burning) < 1.0f, "SCP-457 shows no guttering in the cold");
                })
                .thenWaitUntil(() -> assertFrozen(Helper, Burning, "inside the cooler zone"))
                .thenExecute(() -> {
                    long Took = Helper.getTick() - Started[0];
                    Helper.assertTrue(Took <= Window, "the frost death took " + Took + " ticks");
                })
                .thenExecuteAfter(Scp457.CollapseTicks + 4, () -> {
                    Helper.assertTrue(Burning.isRemoved(), "the frozen SCP-457 never finished collapsing");
                    Restore.run();
                    Scp457GameTests.tidy(Helper);
                })
                .thenSucceed();
    }

    private static void survives(GameTestHelper Helper, String Biome, double Away, boolean Placed, boolean Fed) {
        Scp457GameTests.begin(Helper);
        Runnable Restore = Biome == null ? () -> {
        } : climate(Helper, Biome);
        if (Placed) {
            cooler(Helper, Fed);
        }
        Scp457 Burning = burningAt(Helper, Away);
        Helper.startSequence()
                .thenIdle(Survive)
                .thenExecute(() -> {
                    try {
                        Helper.assertTrue(Burning.isAlive(), "SCP-457 went out where it should not " + Scp457GameTests.state(Burning));
                        Helper.assertTrue(Scp457Cold.chill(Burning) == 0, "SCP-457 feels cold where it should not: " + Scp457Cold.chill(Burning));
                        if (Placed && !Fed) {
                            Helper.assertFalse(coolerOn(Helper), "an unpowered cooler switched on");
                        }
                    } finally {
                        Restore.run();
                        Scp457GameTests.tidy(Helper);
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldMild")
    public static void coldMild(GameTestHelper Helper) {
        killedIn(Helper, Plains, 6.0, CoolerClimate.Mild);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldCold")
    public static void coldCold(GameTestHelper Helper) {
        killedIn(Helper, Snowy, 6.0, CoolerClimate.Cold);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldHot")
    public static void coldHot(GameTestHelper Helper) {
        killedIn(Helper, Desert, CoolerBlockEntity.Radius * CoolerBlockEntity.HotShare - 1.0, CoolerClimate.Hot);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldHotEdge")
    public static void coldHotEdge(GameTestHelper Helper) {
        survives(Helper, Desert, CoolerBlockEntity.Radius * CoolerBlockEntity.HotShare + 1.5, true, true);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldOutside")
    public static void coldOutside(GameTestHelper Helper) {
        survives(Helper, Plains, CoolerBlockEntity.Radius + 1.5, true, true);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldUnpowered")
    public static void coldUnpowered(GameTestHelper Helper) {
        survives(Helper, Plains, 4.0, true, false);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldBiomeAlone")
    public static void coldBiomeAlone(GameTestHelper Helper) {
        survives(Helper, Snowy, 4.0, false, false);
    }

    @GameTest(template = Arena, timeoutTicks = Timeout, batch = "scp457_coldFrostedPile")
    public static void coldFrostedPile(GameTestHelper Helper) {
        Scp457GameTests.begin(Helper);
        Runnable Restore = climate(Helper, Plains);
        cooler(Helper, true);
        Scp457 Burning = burningAt(Helper, 5.0);
        Burning.setPrime(true);
        Vec3 Spot = Burning.position();
        Helper.startSequence()
                .thenWaitUntil(() -> assertFrozen(Helper, Burning, "as a prime"))
                .thenWaitUntil(() -> Helper.assertTrue(Burning.isRemoved(), "the frozen prime never settled"))
                .thenExecute(() -> {
                    try {
                        BlockPos Found = null;
                        for (BlockPos Near : BlockPos.betweenClosed(BlockPos.containing(Spot).offset(-4, -2, -4), BlockPos.containing(Spot).offset(4, 2, 4))) {
                            if (Helper.getLevel().getBlockState(Near).is(Scp457Registry.AshPile.get())) {
                                Found = Near.immutable();
                            }
                        }
                        Helper.assertTrue(Found != null, "the frozen prime left no ash pile");
                        BlockState Pile = Helper.getLevel().getBlockState(Found);
                        Helper.assertTrue(Pile.getValue(Scp457AshPileBlock.Frosted), "the frost death left an unfrosted ash pile");
                        Helper.assertTrue(Scp457AshPileBlock.glow(Pile) == 0, "the frosted ash pile still glows");
                        Helper.getLevel().removeBlock(Found, false);
                    } finally {
                        Restore.run();
                        Scp457GameTests.tidy(Helper);
                    }
                })
                .thenSucceed();
    }

    @AfterBatch(batch = "scp457_coldMild")
    public static void afterMild(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_coldCold")
    public static void afterCold(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_coldHot")
    public static void afterHot(ServerLevel World) {
        Scp457GameTests.calm(World);
    }

    @AfterBatch(batch = "scp457_coldFrostedPile")
    public static void afterPile(ServerLevel World) {
        Scp457GameTests.calm(World);
    }
}
