package net.rai.scp_unleashed.scp610.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp610.Scp610Biomass;
import net.rai.scp_unleashed.scp610.Scp610Chambers;
import net.rai.scp_unleashed.scp610.Scp610Config;
import net.rai.scp_unleashed.scp610.Scp610Faction;
import net.rai.scp_unleashed.scp610.Scp610FleshStore;
import net.rai.scp_unleashed.scp610.Scp610Founding;
import net.rai.scp_unleashed.scp610.Scp610Growth;
import net.rai.scp_unleashed.scp610.Scp610Hive;
import net.rai.scp_unleashed.scp610.Scp610Hives;
import net.rai.scp_unleashed.scp610.Scp610Limits;
import net.rai.scp_unleashed.scp610.Scp610Owned;
import net.rai.scp_unleashed.scp610.Scp610Rules;
import net.rai.scp_unleashed.scp610.Scp610Spewer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class Scp610ScaleGameTests {
    public static final int StressBlocks = 200000;
    public static final int StressTicks = 200;
    public static final double MeanTickMillis = 4.0;
    public static final double WorstTickMillis = 60.0;
    public static final double SeedMillis = 3000.0;
    public static final double SaveMillis = 1500.0;
    public static final double BytesPerBlock = 2.2;
    private static final Logger Log = LogUtils.getLogger();

    private Scp610ScaleGameTests() {
    }

    private static Scp610Hive hive(GameTestHelper Helper, int X, int Z) {
        Scp610Spewer Made = Scp610Founding.found(Helper.getLevel(), Scp610TestKit.ground(Helper, X, Z), 0.0f);
        Helper.assertTrue(Made != null, "no hive could be founded");
        Scp610Hive Found = Scp610Hives.hive(Helper.getLevel().getServer(), Made.hive());
        Helper.assertTrue(Found != null, "the hive has no record");
        return Found;
    }

    private static long seed(ServerLevel World, Scp610Hive Hive, int Count) {
        BlockPos Anchor = Hive.anchor();
        int Side = 400;
        long Began = System.nanoTime();
        for (int Index = 0; Index < Count; Index++) {
            int Layer = Index / (Side * Side);
            int Flat = Index % (Side * Side);
            BlockPos Pos = Anchor.offset(Flat % Side - Side / 2, -40 - Layer, Flat / Side - Side / 2);
            Scp610Hives.own(World, Hive, Pos);
        }
        return System.nanoTime() - Began;
    }

    private static double millis(long Nanos) {
        return Nanos / 1.0e6;
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 400, batch = "scp610_stress")
    public static void aMegaHiveGrowsPastTheCapsWithinItsTickBudget(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp610Rules.setSpread(Server, false);
        Scp610Rules.setUnlimited(Server, true);
        Scp610Hive Hive = hive(Helper, 16, 16);
        FactionState State = Scp610Hives.state(Server);
        long Seeding = seed(World, Hive, StressBlocks);
        int Seeded = Hive.flesh();
        Helper.assertTrue(Seeded >= StressBlocks, "the stress hive holds only " + Seeded + " blocks");
        Helper.assertTrue(millis(Seeding) < SeedMillis, "owning " + StressBlocks + " blocks took " + millis(Seeding) + " ms");
        Helper.assertTrue(Hive.Frontier.size() < 2000, "buried flesh leaked into the surface frontier: " + Hive.Frontier.size());
        Hive.setBiomass(1.0e6);
        Helper.assertTrue(Hive.biomass() > Scp610Limits.BiomassBase * 100.0, "the biomass cap held with the rule on: " + Hive.biomass());
        int Budget = Scp610Config.blocksPerTick();
        long Total = 0L;
        long Worst = 0L;
        int Changed = 0;
        for (int Tick = 0; Tick < StressTicks; Tick++) {
            Hive.SurfaceCredit = Scp610Growth.CreditCap;
            Hive.ChamberCredit = Scp610Growth.CreditCap;
            long Began = System.nanoTime();
            int Used = Scp610Growth.grow(World, State, Hive, Budget);
            Scp610Hives.ownerOf(World, BlockPos.of(Hive.Owned.random(World.random)));
            Scp610Hives.nearLivingHive(World, Hive.anchor().offset(World.random.nextInt(600) - 300, -20, World.random.nextInt(600) - 300));
            if (Tick % 20 == 0) {
                Scp610Hives.flush(Server);
            }
            long Spent = System.nanoTime() - Began;
            Helper.assertTrue(Used <= Budget, "one pulse made " + Used + " changes against a budget of " + Budget);
            Changed += Used;
            Total += Spent;
            Worst = Math.max(Worst, Spent);
        }
        double Mean = millis(Total) / StressTicks;
        Helper.assertTrue(Changed > 0, "the mega hive did not grow with the rule on");
        Helper.assertTrue(Hive.flesh() > Seeded, "the flesh count did not rise past the old caps: " + Hive.flesh());
        Helper.assertTrue(Mean < MeanTickMillis, "a growth tick took " + Mean + " ms on average at " + Hive.flesh() + " blocks");
        Helper.assertTrue(millis(Worst) < WorstTickMillis, "the worst growth tick took " + millis(Worst) + " ms at " + Hive.flesh() + " blocks");
        Scp610Rules.setUnlimited(Server, false);
        int Before = Hive.flesh();
        for (int Tick = 0; Tick < 20; Tick++) {
            Hive.SurfaceCredit = Scp610Growth.CreditCap;
            Hive.ChamberCredit = Scp610Growth.CreditCap;
            Helper.assertTrue(Scp610Growth.grow(World, State, Hive, Budget) == 0, "the hive grew past its cap with the rule off");
        }
        Helper.assertTrue(Hive.flesh() == Before, "the flesh changed with the rule off");
        Hive.setBiomass(Hive.biomass());
        Helper.assertTrue(Hive.biomass() <= Scp610Biomass.capOf(Hive) + 1.0e-6, "the biomass cap did not return with the rule off");
        Scp610FleshStore Store = Scp610Hives.store(Server);
        long Saving = System.nanoTime();
        CompoundTag Saved = Store.save(new CompoundTag(), Server.registryAccess());
        Saving = System.nanoTime() - Saving;
        int Bytes;
        try {
            ByteArrayOutputStream Sink = new ByteArrayOutputStream();
            NbtIo.write(Saved, new DataOutputStream(Sink));
            Bytes = Sink.size();
        } catch (IOException Failed) {
            throw new IllegalStateException(Failed);
        }
        double PerBlock = Bytes / (double) Hive.flesh();
        Helper.assertTrue(millis(Saving) < SaveMillis, "saving the flesh store took " + millis(Saving) + " ms");
        Helper.assertTrue(PerBlock < BytesPerBlock, "the flesh store takes " + PerBlock + " bytes per block");
        long Loading = System.nanoTime();
        Scp610FleshStore Back = Scp610FleshStore.load(Saved, Server.registryAccess());
        Loading = System.nanoTime() - Loading;
        Helper.assertTrue(millis(Loading) < SaveMillis, "loading the flesh store took " + millis(Loading) + " ms");
        Log.info(String.format(Locale.ROOT, "scp610 stress: %d blocks, seed %.1f ms, mean tick %.3f ms, worst tick %.3f ms, %d changes, save %.1f ms, load %.1f ms, %d bytes (%.3f per block)",
                Hive.flesh(), millis(Seeding), Mean, millis(Worst), Changed, millis(Saving), millis(Loading), Bytes, PerBlock));
        Scp610Owned Restored = Back.owned(Hive.id());
        Helper.assertTrue(Restored.size() == Hive.flesh(), "the store lost blocks: " + Restored.size() + " vs " + Hive.flesh());
        RandomSource Random = RandomSource.create(610L);
        for (int Check = 0; Check < 2000; Check++) {
            long Pos = Hive.Owned.random(Random);
            Helper.assertTrue(Restored.contains(Pos), "the store dropped " + BlockPos.of(Pos));
        }
        FactionControl.reset(Server, Scp610Faction.Instance);
        Scp610Hives.hives(Server);
        Helper.assertFalse(Scp610Hives.store(Server).has(Hive.id()), "the store kept a razed hive");
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }

    @GameTest(template = Scp610TestKit.Arena, timeoutTicks = 100, batch = "scp610_envelope")
    public static void theEnvelopeIsLargerAndTheRuleLiftsIt(GameTestHelper Helper) {
        Scp610TestKit.fresh(Helper);
        ServerLevel World = Helper.getLevel();
        MinecraftServer Server = World.getServer();
        Scp610Rules.setSpread(Server, false);
        Helper.assertTrue(Scp610Config.DefaultMaxFleshPerHive >= 4096, "the default flesh cap is still small");
        Helper.assertTrue(Scp610Config.DefaultMaxChamberBlocksPerHive >= 24000, "the default chamber cap is still small");
        Helper.assertFalse(Scp610Rules.unlimited(Server), "mega hives are on by default");
        Scp610Hive Hive = hive(Helper, 16, 16);
        seed(World, Hive, 4096);
        Helper.assertTrue(Scp610Growth.surfaceRadius(Hive) == Scp610Limits.RadiusCap, "the surface radius at 4096 blocks is " + Scp610Growth.surfaceRadius(Hive));
        Helper.assertTrue(Scp610Growth.fleshCapped(Hive), "a full hive was not capped");
        Helper.assertTrue(Scp610Biomass.capOf(Hive) > 1000.0, "the biomass cap did not scale: " + Scp610Biomass.capOf(Hive));
        Helper.assertTrue(Scp610Biomass.trickleFor(Hive) > Scp610Biomass.Trickle * 10.0, "the income did not scale: " + Scp610Biomass.trickleFor(Hive));
        Scp610Rules.setUnlimited(Server, true);
        Helper.assertFalse(Scp610Growth.fleshCapped(Hive), "the flesh cap held with the rule on");
        Helper.assertFalse(Scp610Growth.chamberCapped(Hive), "the chamber cap held with the rule on");
        Helper.assertTrue(Scp610Growth.surfaceRadius(Hive) > Scp610Limits.RadiusCap, "the surface radius held with the rule on");
        Scp610Rules.setUnlimited(Server, false);
        Helper.assertTrue(Scp610Growth.fleshCapped(Hive), "the cap did not return with the rule off");
        boolean Great = false;
        for (int Room = 0; Room < 12; Room++) {
            Great |= Scp610Chambers.GreatCavern.equals(Scp610Growth.roomKind(Hive, Room));
        }
        Helper.assertTrue(Great, "a large hive plans no great caverns");
        Helper.assertTrue(Scp610Chambers.ops(new Scp610Chambers.Chamber(Scp610Chambers.GreatCavern, Hive.anchor(), Hive.anchor().below(20), 0)).size()
                > Scp610Chambers.ops(new Scp610Chambers.Chamber(Scp610Chambers.Cavern, Hive.anchor(), Hive.anchor().below(20), 0)).size(), "a great cavern is no bigger than a cavern");
        Scp610Config.overrideForTests(-1, 1000000, -1, 1000000);
        BlockPos Tail = Hive.tail();
        for (int Room = 0; Room < 4; Room++) {
            Hive.Plan.clear();
            Helper.assertTrue(Scp610Growth.extend(World, Hive), "the chain stopped extending at room " + Room);
        }
        Helper.assertTrue(Hive.junctions().size() == 4, "the chain kept " + Hive.junctions().size() + " junctions");
        Helper.assertTrue(Hive.tail().getY() < Tail.getY(), "the chain did not go deeper");
        Hive.Plan.clear();
        FactionControl.reset(Server, Scp610Faction.Instance);
        Scp610TestKit.restore(Helper);
        Helper.succeed();
    }
}
