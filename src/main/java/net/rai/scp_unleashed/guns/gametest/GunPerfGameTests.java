package net.rai.scp_unleashed.guns.gametest;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.level.Level;
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
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunKind;
import net.rai.scp_unleashed.guns.npc.NpcAmmoSource;
import net.rai.scp_unleashed.guns.npc.NpcGunner;
import net.rai.scp_unleashed.raid.chaos.ChaosInsurgent;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.serpentshand.SerpentsHandAgent;
import org.slf4j.Logger;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@GameTestHolder(Scp_unleashed.ModId)
@PrefixGameTestTemplate(false)
public final class GunPerfGameTests {
    private static final Logger Log = LogUtils.getLogger();
    public static final String Arena = "gun_perf_arena";
    public static final int Warmup = 40;
    public static final int Window = 300;
    public static final int Grace = 60;
    public static final float Sturdy = 100000.0F;
    public static final float Accuracy = 0.7F;
    public static final double ShooterX = 4.5;
    public static final double TargetX = 20.5;
    public static final int WallX = 24;
    public static final int CoverX = 14;
    public static final int Squad = 10;
    public static final int RaiderLevel = 5;
    public static final double MaxShotMillis = 3.0;
    public static final double MaxTickMillis = 60.0;
    public static final double MaxNearSoundsPerShot = 12.0;
    public static final double MaxFarSoundsPerShot = 0.6;
    public static final GunKind[] Mix = {
            GunKind.AssaultRifle, GunKind.Pistol, GunKind.Shotgun, GunKind.AssaultRifle, GunKind.SniperRifle,
            GunKind.Revolver, GunKind.AssaultRifle, GunKind.DoubleBarrel, GunKind.Pistol, GunKind.Spiker,
            GunKind.AssaultRifle, GunKind.Shotgun, GunKind.Pistol, GunKind.AssaultRifle, GunKind.Revolver,
            GunKind.Shotgun, GunKind.SniperRifle, GunKind.Pistol, GunKind.AssaultRifle, GunKind.DoubleBarrel
    };

    private GunPerfGameTests() {
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

    private static List<GunPerfListener> listeners(GameTestHelper Helper, int F, double CentreZ) {
        ServerLevel World = Helper.getLevel();
        List<GunPerfListener> Made = new ArrayList<>();
        double[] Offsets = {2.0, -38.0, -100.0};
        String[] Names = {"gunperf_near", "gunperf_mid", "gunperf_far"};
        for (int Index = 0; Index < Offsets.length; Index++) {
            GunPerfListener Ear = new GunPerfListener(World, Names[Index], Helper.absoluteVec(new Vec3(Offsets[Index], F + 1.6, CentreZ)));
            Ear.enlist(true);
            Made.add(Ear);
        }
        return Made;
    }

    private static void release(List<GunPerfListener> Ears) {
        for (GunPerfListener Ear : Ears) {
            Ear.enlist(false);
        }
    }

    private static void discard(List<? extends Entity> Bodies) {
        for (Entity Body : Bodies) {
            if (!Body.isRemoved()) {
                Body.discard();
            }
        }
    }

    private static long cpuNow() {
        ThreadMXBean Threads = ManagementFactory.getThreadMXBean();
        return Threads.isCurrentThreadCpuTimeSupported() ? Threads.getCurrentThreadCpuTime() : System.nanoTime();
    }

    private static long lastTick(MinecraftServer Server) {
        long[] Times = Server.getTickTimesNanos();
        return Times[Math.floorMod(Server.getTickCount() - 1, Times.length)];
    }

    private static long lastLevelTick(MinecraftServer Server) {
        long[] Times = Server.getTickTime(Level.OVERWORLD);
        return Times == null ? 0L : Times[Math.floorMod(Server.getTickCount() - 1, Times.length)];
    }

    private static String millis(double Nanos) {
        return String.format(Locale.ROOT, "%.3f", Nanos / 1.0E6);
    }

    private static String summary(long[] Ticks, int Count) {
        long[] Sorted = Arrays.copyOf(Ticks, Count);
        Arrays.sort(Sorted);
        long Total = 0L;
        for (long Each : Sorted) {
            Total += Each;
        }
        double Mean = Count == 0 ? 0.0 : (double) Total / Count;
        long P95 = Count == 0 ? 0L : Sorted[Math.min(Count - 1, (int) Math.floor(Count * 0.95))];
        long Worst = Count == 0 ? 0L : Sorted[Count - 1];
        return "avg=" + millis(Mean) + "ms p95=" + millis(P95) + "ms max=" + millis(Worst) + "ms";
    }

    private static double mean(long[] Ticks, int Count) {
        long Total = 0L;
        for (int Index = 0; Index < Count; Index++) {
            Total += Ticks[Index];
        }
        return Count == 0 ? 0.0 : (double) Total / Count;
    }

    private static String ears(List<GunPerfListener> Ears) {
        StringBuilder Text = new StringBuilder();
        for (GunPerfListener Ear : Ears) {
            Text.append(' ').append(Ear.getGameProfile().getName()).append("[sounds=").append(Ear.sounds()).append(" channels=")
                    .append(String.format(Locale.ROOT, "%.1f", Ear.channels())).append(" peak=").append(Ear.peak()).append(" afterBudget=")
                    .append(String.format(Locale.ROOT, "%.1f", Ear.kept())).append(" budgetPeak=").append(Ear.keptPeak()).append(" refused=").append(Ear.refused())
                    .append(" stolen=").append(Ear.cut()).append(']');
        }
        return Text.toString();
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "gunperf_volley")
    public static void scriptedVolley(GameTestHelper Helper) {
        int F = floor(Helper);
        wall(Helper, WallX, WallX, 1, 30, F, 5);
        for (int Z = 3; Z <= 27; Z += 7) {
            wall(Helper, CoverX, CoverX, Z, Z + 1, F, 2);
        }
        List<Husk> Targets = new ArrayList<>();
        for (int Index = 0; Index < Squad; Index++) {
            Husk Target = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(TargetX, F, 2.5 + Index * 2.9));
            Target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
            Target.setHealth(Sturdy);
            Target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            Targets.add(Target);
        }
        List<Husk> Gunners = new ArrayList<>();
        Set<Entity> Friends = new HashSet<>();
        for (int Index = 0; Index < Mix.length; Index++) {
            Husk Rifleman = Helper.spawnWithNoFreeWill(EntityType.HUSK, new Vec3(ShooterX + (Index % 2), F, 1.5 + Index * 1.45));
            Rifleman.setItemSlot(EquipmentSlot.MAINHAND, GunItem.loaded(Mix[Index]));
            Gunners.add(Rifleman);
            Friends.add(Rifleman);
        }
        NpcAmmoSource Ammo = (AmmoItem, Wanted) -> Wanted;
        List<GunPerfListener> Ears = listeners(Helper, F, 15.5);
        MinecraftServer Server = Helper.getLevel().getServer();
        int Kinds = GunKind.values().length;
        long[] KindShots = new long[Kinds];
        long[] KindNanos = new long[Kinds];
        long[] SightNanos = {0L, 0L};
        long[] Ticks = new long[Window];
        long[] LevelTicks = new long[Window];
        long[] CpuTicks = new long[Window];
        long[] CpuMark = {cpuNow()};
        int[] Clock = {0};
        Helper.onEachTick(() -> {
            int Now = Clock[0]++;
            long Cpu = cpuNow();
            long CpuSpent = Cpu - CpuMark[0];
            CpuMark[0] = Cpu;
            if (Now > Warmup && Now <= Warmup + Window) {
                Ticks[Now - Warmup - 1] = lastTick(Server);
                LevelTicks[Now - Warmup - 1] = lastLevelTick(Server);
                CpuTicks[Now - Warmup - 1] = CpuSpent;
                for (GunPerfListener Ear : Ears) {
                    Ear.sample();
                }
            }
            if (Now == Warmup) {
                for (GunPerfListener Ear : Ears) {
                    Ear.reset();
                }
            }
            if (Now >= Warmup + Window + Grace) {
                release(Ears);
                discard(Gunners);
                discard(Targets);
                return;
            }
            for (Husk Target : Targets) {
                if (Target.isAlive()) {
                    Target.setHealth(Sturdy);
                }
            }
            if (Now >= Warmup + Window) {
                return;
            }
            boolean Measuring = Now >= Warmup;
            for (int Index = 0; Index < Gunners.size(); Index++) {
                Husk Rifleman = Gunners.get(Index);
                Husk Target = Targets.get(Index / 2 % Targets.size());
                if (!Rifleman.isAlive() || !Target.isAlive()) {
                    continue;
                }
                long Start = System.nanoTime();
                boolean Clear = NpcGunner.clearShot(Rifleman, Target, Friends::contains);
                long Middle = System.nanoTime();
                boolean Fired = Clear && NpcGunner.ready(Rifleman, Rifleman.getMainHandItem())
                        && NpcGunner.fireAt(Rifleman, Rifleman.getMainHandItem(), Target, Ammo, Accuracy);
                long End = System.nanoTime();
                if (Measuring) {
                    SightNanos[0] += Middle - Start;
                    SightNanos[1]++;
                    if (Fired) {
                        GunKind Kind = GunItem.kindOf(Rifleman.getMainHandItem());
                        if (Kind != null) {
                            KindShots[Kind.ordinal()]++;
                            KindNanos[Kind.ordinal()] += End - Middle;
                        }
                    }
                }
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Clock[0] > Warmup + Window + 1, "benchmark still running"))
                .thenExecute(() -> {
                    long Shots = 0L;
                    long ShotNanos = 0L;
                    StringBuilder PerKind = new StringBuilder();
                    for (GunKind Kind : GunKind.values()) {
                        int Slot = Kind.ordinal();
                        if (KindShots[Slot] > 0) {
                            Shots += KindShots[Slot];
                            ShotNanos += KindNanos[Slot];
                            PerKind.append(' ').append(Kind.getName()).append('=').append(KindShots[Slot]).append('x')
                                    .append(String.format(Locale.ROOT, "%.1f", KindNanos[Slot] / 1000.0 / KindShots[Slot])).append("us");
                        }
                    }
                    double PerShot = Shots == 0 ? 0.0 : (double) ShotNanos / Shots;
                    double PerSight = SightNanos[1] == 0 ? 0.0 : (double) SightNanos[0] / SightNanos[1];
                    Log.info("GUNPERF volley ticks={} shots={} server[{}] level[{}] cpu[{}] shotAvg={}us sightAvg={}us{}{}", Window, Shots, summary(Ticks, Window),
                            summary(LevelTicks, Window), summary(CpuTicks, Window), String.format(Locale.ROOT, "%.1f", PerShot / 1000.0), String.format(Locale.ROOT, "%.1f", PerSight / 1000.0),
                            PerKind, ears(Ears));
                    int NearSounds = Ears.get(0).sounds();
                    int FarSounds = Ears.get(2).sounds();
                    release(Ears);
                    discard(Gunners);
                    discard(Targets);
                    Helper.assertTrue(Shots > Window, "too few shots: " + Shots);
                    Helper.assertTrue(PerShot / 1.0E6 < MaxShotMillis, "a shot costs " + millis(PerShot) + " ms");
                    Helper.assertTrue(mean(Ticks, Window) / 1.0E6 < MaxTickMillis, "average tick " + millis(mean(Ticks, Window)) + " ms");
                    Helper.assertTrue(NearSounds <= Shots * MaxNearSoundsPerShot, "near listener got " + NearSounds + " sounds for " + Shots + " shots");
                    Helper.assertTrue(FarSounds <= Shots * MaxFarSoundsPerShot, "far listener got " + FarSounds + " sounds for " + Shots + " shots");
                })
                .thenSucceed();
    }

    public static final class ShotCounter {
        private int Shots;

        public int shots() {
            return Shots;
        }

        @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
        public void heard(VanillaGameEvent Event) {
            if (Event.getVanillaEvent().is(GameEvent.PROJECTILE_SHOOT) && Event.getCause() instanceof HostileRaider) {
                Shots++;
            }
        }
    }

    private static <T extends HostileRaider> T raider(GameTestHelper Helper, EntityType<T> Type, double X, int Y, double Z, int Index, float Yaw) {
        ServerLevel World = Helper.getLevel();
        T Made = Type.create(World);
        if (Made == null) {
            throw new IllegalStateException("could not create " + Type);
        }
        Made.configure(RaiderLevel, HostileRaider.RoleFighter, Index, World.random);
        Made.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Sturdy);
        Made.setHealth(Sturdy);
        Vec3 Spot = Helper.absoluteVec(new Vec3(X, Y, Z));
        Made.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0F);
        World.addFreshEntity(Made);
        return Made;
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "gunperf_firefight")
    public static void raiderFirefight(GameTestHelper Helper) {
        skirmish(Helper, "firefight", 4.5, 27.5);
    }

    @GameTest(template = Arena, timeoutTicks = 1200, batch = "gunperf_close")
    public static void raiderCloseQuarters(GameTestHelper Helper) {
        skirmish(Helper, "close", 12.5, 19.5);
    }

    private static void skirmish(GameTestHelper Helper, String Name, double ChaosX, double SerpentX) {
        int F = floor(Helper);
        wall(Helper, 0, 31, 0, 0, F, 4);
        wall(Helper, 0, 31, 31, 31, F, 4);
        wall(Helper, 0, 0, 1, 30, F, 4);
        wall(Helper, 31, 31, 1, 30, F, 4);
        for (int Z = 5; Z <= 26; Z += 7) {
            wall(Helper, 15, 16, Z, Z + 1, F, 3);
        }
        List<HostileRaider> Fighters = new ArrayList<>();
        for (int Index = 0; Index < Squad; Index++) {
            double Z = 3.5 + Index * 2.7;
            Fighters.add(raider(Helper, HostileRegistry.InsurgentType.get(), ChaosX, F, Z, Index, -90.0F));
            Fighters.add(raider(Helper, HostileRegistry.AgentType.get(), SerpentX, F, Z, Index, 90.0F));
        }
        ShotCounter Counter = new ShotCounter();
        NeoForge.EVENT_BUS.register(Counter);
        List<GunPerfListener> Ears = listeners(Helper, F, 15.5);
        MinecraftServer Server = Helper.getLevel().getServer();
        long[] Ticks = new long[Window];
        long[] LevelTicks = new long[Window];
        long[] CpuTicks = new long[Window];
        long[] CpuMark = {cpuNow()};
        int[] Clock = {0};
        int[] Before = {0};
        Helper.onEachTick(() -> {
            int Now = Clock[0]++;
            long Cpu = cpuNow();
            long CpuSpent = Cpu - CpuMark[0];
            CpuMark[0] = Cpu;
            if (Now > Warmup && Now <= Warmup + Window) {
                Ticks[Now - Warmup - 1] = lastTick(Server);
                LevelTicks[Now - Warmup - 1] = lastLevelTick(Server);
                CpuTicks[Now - Warmup - 1] = CpuSpent;
                for (GunPerfListener Ear : Ears) {
                    Ear.sample();
                }
            }
            if (Now == Warmup) {
                for (GunPerfListener Ear : Ears) {
                    Ear.reset();
                }
                Before[0] = Counter.shots();
            }
            if (Now >= Warmup + Window + Grace) {
                NeoForge.EVENT_BUS.unregister(Counter);
                release(Ears);
                discard(Fighters);
                return;
            }
            for (HostileRaider Fighter : Fighters) {
                if (Fighter.isAlive()) {
                    Fighter.setHealth(Sturdy);
                }
            }
        });
        Helper.startSequence()
                .thenWaitUntil(() -> Helper.assertTrue(Clock[0] > Warmup + Window + 1, "benchmark still running"))
                .thenExecute(() -> {
                    NeoForge.EVENT_BUS.unregister(Counter);
                    int Shots = Counter.shots() - Before[0];
                    int Standing = 0;
                    for (HostileRaider Fighter : Fighters) {
                        if (Fighter.isAlive() && !Fighter.isRemoved()) {
                            Standing++;
                        }
                    }
                    int Chaos = 0;
                    int Serpents = 0;
                    for (HostileRaider Fighter : Fighters) {
                        if (Fighter instanceof ChaosInsurgent) {
                            Chaos++;
                        } else if (Fighter instanceof SerpentsHandAgent) {
                            Serpents++;
                        }
                    }
                    Log.info("GUNPERF {} ticks={} shots={} standing={} chaos={} serpents={} server[{}] level[{}] cpu[{}]{}", Name, Window, Shots, Standing, Chaos, Serpents,
                            summary(Ticks, Window), summary(LevelTicks, Window), summary(CpuTicks, Window), ears(Ears));
                    release(Ears);
                    discard(Fighters);
                    Helper.assertTrue(Shots > Window / 4, "the raiders barely fired: " + Shots);
                    Helper.assertTrue(mean(Ticks, Window) / 1.0E6 < MaxTickMillis, "average tick " + millis(mean(Ticks, Window)) + " ms");
                })
                .thenSucceed();
    }
}
