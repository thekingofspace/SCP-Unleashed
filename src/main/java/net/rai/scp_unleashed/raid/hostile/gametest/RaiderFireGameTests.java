package net.rai.scp_unleashed.raid.hostile.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.hostile.HostileLevels;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class RaiderFireGameTests {
    public static final String Arena = "gun_perf_arena";
    public static final float Sturdy = 600.0f;
    public static final int Window = 400;
    public static final int BurstGap = 40;

    private RaiderFireGameTests() {
    }

    public static final class ShotLog {
        public final Map<UUID, List<Long>> Shots = new HashMap<>();

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void heard(VanillaGameEvent Event) {
            if (Event.getVanillaEvent().is(GameEvent.PROJECTILE_SHOOT) && Event.getCause() instanceof HostileRaider Raider) {
                Shots.computeIfAbsent(Raider.getUUID(), Key -> new ArrayList<>()).add(Raider.level().getGameTime());
            }
        }

        public List<Long> of(HostileRaider Raider) {
            return Shots.getOrDefault(Raider.getUUID(), List.of());
        }
    }

    private static int floor(GameTestHelper Helper) {
        for (int Y = 0; Y < 5; Y++) {
            if (Helper.getBlockState(new BlockPos(1, Y, 1)).is(Blocks.SMOOTH_STONE) && Helper.getBlockState(new BlockPos(1, Y + 1, 1)).isAir()) {
                return Y + 1;
            }
        }
        return 1;
    }

    private static void wall(GameTestHelper Helper, int FromX, int ToX, int FromZ, int ToZ, int Bottom, int Height) {
        for (int X = FromX; X <= ToX; X++) {
            for (int Z = FromZ; Z <= ToZ; Z++) {
                for (int Y = Bottom; Y < Bottom + Height; Y++) {
                    Helper.setBlock(new BlockPos(X, Y, Z), Blocks.STONE);
                }
            }
        }
    }

    private static <T extends HostileRaider> T raider(GameTestHelper Helper, EntityType<T> Type, int Level, int Index, double X, int Y, double Z, float Yaw) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        if (Made == null) {
            throw new IllegalStateException("could not create " + Type);
        }
        Made.configure(Level, HostileRaider.RoleFighter, Index, World.random);
        Made.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Made.setHealth(Sturdy);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0F);
        World.addFreshEntity(Made);
        return Made;
    }

    private static int longestRun(List<Long> Times) {
        int Longest = Times.isEmpty() ? 0 : 1;
        int Run = Longest;
        for (int Index = 1; Index < Times.size(); Index++) {
            Run = Times.get(Index) - Times.get(Index - 1) < BurstGap ? Run + 1 : 1;
            Longest = Math.max(Longest, Run);
        }
        return Longest;
    }

    @GameTest(template = Arena, timeoutTicks = 40, batch = "raider_fire_tables")
    public static void levelTablesClimb(GameTestHelper Helper) {
        for (int Index = 1; Index < HostileLevels.MaxLevel; Index++) {
            Helper.assertTrue(HostileLevels.Accuracy[Index] > HostileLevels.Accuracy[Index - 1], "accuracy does not climb at level " + (Index + 1));
            Helper.assertTrue(HostileLevels.BurstMax[Index] >= HostileLevels.BurstMax[Index - 1], "bursts shrink at level " + (Index + 1));
            Helper.assertTrue(HostileLevels.PauseMin[Index] < HostileLevels.PauseMin[Index - 1], "pauses do not shorten at level " + (Index + 1));
            Helper.assertTrue(HostileLevels.BurstMin[Index] <= HostileLevels.BurstMax[Index] && HostileLevels.PauseMin[Index] <= HostileLevels.PauseMax[Index],
                    "a range is upside down at level " + (Index + 1));
        }
        Helper.succeed();
    }

    @GameTest(template = Arena, timeoutTicks = Window + 200, batch = "raider_fire_discipline")
    public static void rookiesFireInShortBursts(GameTestHelper Helper) {
        int F = floor(Helper);
        wall(Helper, 0, 31, 0, 0, F, 6);
        wall(Helper, 0, 31, 31, 31, F, 6);
        wall(Helper, 0, 0, 1, 30, F, 6);
        wall(Helper, 31, 31, 1, 30, F, 6);
        wall(Helper, 1, 30, 15, 16, F, 6);
        ChaosInsurgent RookieChaos = raider(Helper, HostileRegistry.InsurgentType.get(), 1, 0, 6.5, F, 7.5, -90.0F);
        SerpentsHandAgent RookieSerpent = raider(Helper, HostileRegistry.AgentType.get(), 1, 0, 24.5, F, 7.5, 90.0F);
        ChaosInsurgent VeteranChaos = raider(Helper, HostileRegistry.InsurgentType.get(), 6, 1, 6.5, F, 23.5, -90.0F);
        SerpentsHandAgent VeteranSerpent = raider(Helper, HostileRegistry.AgentType.get(), 6, 0, 24.5, F, 23.5, 90.0F);
        ShotLog Log = new ShotLog();
        NeoForge.EVENT_BUS.register(Log);
        Helper.runAfterDelay(Window, () -> {
            NeoForge.EVENT_BUS.unregister(Log);
            int Rookies = 0;
            for (HostileRaider Rookie : List.of(RookieChaos, RookieSerpent)) {
                List<Long> Times = Log.of(Rookie);
                Rookies += Times.size();
                Helper.assertTrue(Times.size() >= 2, "a level 1 raider barely fired: " + Times.size());
                Helper.assertTrue(longestRun(Times) <= HostileLevels.BurstMax[0], "a level 1 raider fired " + longestRun(Times) + " shots without pausing: " + Times);
            }
            int Veterans = Log.of(VeteranChaos).size() + Log.of(VeteranSerpent).size();
            Helper.assertTrue(Veterans > Rookies, "level 6 raiders fired " + Veterans + " shots against " + Rookies + " from level 1 raiders");
            Helper.succeed();
        });
    }
}
