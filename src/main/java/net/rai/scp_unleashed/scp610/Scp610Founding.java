package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class Scp610Founding {
    public static final int DarkLight = 7;
    public static final int VillageReach = 48;
    public static final int Footprint = 2;
    public static final int MaxStep = 2;
    public static final int Headroom = 4;
    public static final int HiveGap = 48;

    public enum Verdict {
        Ready,
        Rule,
        Pact,
        Capped,
        Crowded,
        Realm,
        Ground,
        Exposed
    }

    private Scp610Founding() {
    }

    public static Verdict allowed(ServerLevel World, BlockPos Feet) {
        MinecraftServer Server = World.getServer();
        FactionState State = Scp610Hives.state(Server);
        if (!Scp610Rules.spreads(Server)) {
            return Verdict.Rule;
        }
        if (Scp610Pact.active(State) && State.phase() != FactionPhase.Defeated) {
            return Verdict.Pact;
        }
        if (!AnomalyRealm.allows(World)) {
            return Verdict.Realm;
        }
        int Count = 0;
        for (Scp610Hive Hive : Scp610Hives.list(Server)) {
            Count++;
            if (Hive.Site.dimension().equals(World.dimension()) && Hive.anchor().distSqr(Feet) < (double) HiveGap * HiveGap) {
                return Verdict.Crowded;
            }
        }
        if (State.phase() != FactionPhase.Defeated && Count >= Scp610Config.maxHives()) {
            return Verdict.Capped;
        }
        return Verdict.Ready;
    }

    public static boolean groundFits(ServerLevel World, BlockPos Feet) {
        if (!World.getFluidState(Feet).isEmpty() || !World.getFluidState(Feet.below()).isEmpty()) {
            return false;
        }
        int Low = Integer.MAX_VALUE;
        int High = Integer.MIN_VALUE;
        for (int Dx = -Footprint; Dx <= Footprint; Dx++) {
            for (int Dz = -Footprint; Dz <= Footprint; Dz++) {
                BlockPos Column = Feet.offset(Dx, 0, Dz);
                Integer Top = groundAt(World, Column);
                if (Top == null) {
                    return false;
                }
                Low = Math.min(Low, Top);
                High = Math.max(High, Top);
            }
        }
        if (High - Low > MaxStep) {
            return false;
        }
        for (int Up = 0; Up < Headroom; Up++) {
            BlockState Air = World.getBlockState(Feet.above(Up));
            if (!Air.getCollisionShape(World, Feet.above(Up)).isEmpty() || !Air.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Integer groundAt(ServerLevel World, BlockPos Column) {
        for (int Dy = 2; Dy >= -3; Dy--) {
            BlockPos Pos = Column.offset(0, Dy, 0);
            BlockState Below = World.getBlockState(Pos.below());
            if (Below.isFaceSturdy(World, Pos.below(), Direction.UP) && Below.getFluidState().isEmpty() && World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty()) {
                return Pos.getY();
            }
        }
        return null;
    }

    public static boolean sheltered(ServerLevel World, BlockPos Feet) {
        if (World.getBrightness(LightLayer.BLOCK, Feet) <= DarkLight && World.getBrightness(LightLayer.SKY, Feet) - World.getSkyDarken() <= DarkLight) {
            return true;
        }
        return World.getPoiManager().findClosest(Holder -> Holder.is(PoiTypes.MEETING), Feet, VillageReach, PoiManager.Occupancy.ANY).isPresent();
    }

    public static boolean qualifies(ServerLevel World, BlockPos Feet) {
        return allowed(World, Feet) == Verdict.Ready && groundFits(World, Feet) && sheltered(World, Feet);
    }

    @Nullable
    public static Scp610Spewer found(ServerLevel World, BlockPos Feet, float Yaw) {
        MinecraftServer Server = World.getServer();
        FactionState State = Scp610Hives.state(Server);
        if (State.phase() == FactionPhase.Defeated) {
            FactionControl.reset(Server, Scp610Faction.Instance);
            State = Scp610Hives.state(Server);
        }
        if (State.phase() == FactionPhase.Dormant) {
            FactionControl.awaken(World, Scp610Faction.Instance, Feet, null);
        }
        Scp610Creature Made = Scp610Spawning.raise(World, Scp610Form.Spewer, Vec3.atBottomCenterOf(Feet), Yaw, true);
        if (!(Made instanceof Scp610Spewer Spewer)) {
            return null;
        }
        adopt(World, State, Spewer);
        World.playSound(null, Feet, Scp610Sounds.Evolve.get(), SoundSource.HOSTILE, 2.0f, 0.6f);
        return Spewer;
    }

    public static Scp610Hive adopt(ServerLevel World, FactionState State, Scp610Spewer Spewer) {
        BlockPos Anchor = Spewer.blockPosition();
        FactionSite Site = State.addSite(Scp610Hive.Kind, World.dimension(), Anchor, World.random.nextLong());
        Site.markBuilt(Anchor, Direction.fromYRot(Spewer.getYRot()), Map.of());
        Site.setLastReinforce(World.getServer().overworld().getGameTime());
        Site.extra().putUUID(Scp610Hive.SpewerKey, Spewer.getUUID());
        Site.extra().putDouble(Scp610Hive.BiomassKey, Scp610Biomass.Start);
        State.changed();
        Spewer.setHive(Site.id());
        Spewer.setHome(Anchor);
        FactionControl.stamp(Scp610Faction.Instance, State, Spewer);
        Scp610Hive Hive = Scp610Hives.hive(World.getServer(), Site.id());
        if (Hive != null) {
            BlockPos Base = Anchor.below();
            if (!Scp610Growth.isFlesh(World.getBlockState(Base)) && Scp610Growth.convertible(World, Base)) {
                World.setBlock(Base, Scp610Growth.wild(Scp610Registry.Flesh.get().defaultBlockState()), Scp610Growth.Flags);
            }
            if (Scp610Growth.isFlesh(World.getBlockState(Base))) {
                Scp610Hives.own(World, Hive, Base);
            }
        }
        Scp610Faction.Instance.refresh(World.getServer(), State);
        return Hive;
    }
}
