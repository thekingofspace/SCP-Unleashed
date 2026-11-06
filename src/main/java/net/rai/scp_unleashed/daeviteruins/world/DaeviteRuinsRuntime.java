package net.rai.scp_unleashed.daeviteruins.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPages;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DaeviteRuinsRuntime {
    public static final int StrictTries = 4;
    public static final int LenientTries = 10;
    public static final int RelocateTries = 24;
    public static final int SearchEvery = 40;
    public static final int WakeEvery = 10;
    public static final int TrapEvery = 2;
    public static final int TrapRange = 24;
    private static final int CacheSize = 6;
    private static final Map<Long, RuinPlan> Plans = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, RuinPlan> Eldest) {
            return size() > CacheSize;
        }
    };

    private DaeviteRuinsRuntime() {
    }

    public static RuinPlan plan(MinecraftServer Server, int Page) {
        long Seed = Server.overworld().getSeed();
        long Key = Seed * 64L + Page;
        synchronized (Plans) {
            RuinPlan Found = Plans.get(Key);
            if (Found == null) {
                Found = RuinPages.build(Seed, Page);
                Plans.put(Key, Found);
            }
            return Found;
        }
    }

    public static void tick(ServerLevel World) {
        long Now = World.getGameTime();
        DaeviteRuinsWorld Data = DaeviteRuinsWorld.get(World.getServer());
        if (Data.ruins().isEmpty()) {
            return;
        }
        List<DaeviteRuin> Ruins = new ArrayList<>(Data.ruins());
        if (Now % SearchEvery == 0) {
            for (DaeviteRuin Ruin : Ruins) {
                if (!Ruin.built()) {
                    attempt(World, Data, Ruin);
                }
            }
        }
        if (Now % WakeEvery == 0) {
            for (DaeviteRuin Ruin : Ruins) {
                if (Ruin.built() && !Ruin.Woken && World.isLoaded(Ruin.Centre)
                        && DaeviteRuinsGuardians.near(World, Ruin, DaeviteRuinsGuardians.wakeRadius(RuinPages.tierOf(Ruin.Page)))) {
                    DaeviteRuinsGuardians.wake(World, Ruin);
                }
            }
        }
        if (Now % TrapEvery == 0) {
            for (DaeviteRuin Ruin : Ruins) {
                if (!Ruin.built() || Ruin.Traps.isEmpty() || !World.isLoaded(Ruin.Centre)) {
                    continue;
                }
                AABB Area = Ruin.bounds().inflate(TrapRange);
                if (!World.getEntitiesOfClass(Player.class, Area, Player::isAlive).isEmpty()) {
                    DaeviteRuinsTraps.tick(World, Ruin, Now);
                }
            }
        }
    }

    private static void attempt(ServerLevel World, DaeviteRuinsWorld Data, DaeviteRuin Ruin) {
        BlockPos Target = Ruin.Target;
        if (World.getChunkSource().getChunkNow(Target.getX() >> 4, Target.getZ() >> 4) == null) {
            return;
        }
        RuinPlan Plan = plan(World.getServer(), Ruin.Page);
        if (Plan == null) {
            return;
        }
        if (!DaeviteRuinsSites.footprintLoaded(World, Plan, Target, DaeviteRuinsSites.toward(Target, Ruin.Origin))) {
            return;
        }
        int Mode = Ruin.Tries < StrictTries ? DaeviteRuinsSites.Strict : Ruin.Tries < LenientTries ? DaeviteRuinsSites.Lenient : DaeviteRuinsSites.Forced;
        int Spread = Math.min(96, 16 + Ruin.Tries * 8);
        RandomSource Random = RandomSource.create(Ruin.Id * 0x5DEECE66DL + Ruin.Tries * 31L + World.getSeed());
        Optional<DaeviteRuinsSites.Pick> Found = DaeviteRuinsSites.find(World, Plan, Target, Ruin.Origin, Spread, Mode, Random);
        if (Found.isPresent()) {
            DaeviteRuinsBuilder.build(World, Ruin, Plan, Found.get().Origin(), Found.get().Front());
            Data.setDirty();
            return;
        }
        Ruin.Tries++;
        if (Ruin.Tries >= RelocateTries) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Distance = 48 + Random.nextDouble() * 72;
            int X = Target.getX() + Mth.floor(Math.cos(Angle) * Distance);
            int Z = Target.getZ() + Mth.floor(Math.sin(Angle) * Distance);
            int Y = World.getChunkSource().getChunkNow(X >> 4, Z >> 4) != null ? World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z)
                    : World.getChunkSource().getGenerator().getBaseHeight(X, Z, Heightmap.Types.WORLD_SURFACE_WG, World, World.getChunkSource().randomState());
            Ruin.Target = new BlockPos(X, Y, Z);
            Ruin.Tries = LenientTries;
        }
        Data.setDirty();
    }

    public static void buildHere(ServerLevel World, DaeviteRuin Ruin, BlockPos Origin, Direction Front) {
        RuinPlan Plan = plan(World.getServer(), Ruin.Page);
        if (Plan != null) {
            DaeviteRuinsBuilder.build(World, Ruin, Plan, Origin, Front);
        }
    }
}
