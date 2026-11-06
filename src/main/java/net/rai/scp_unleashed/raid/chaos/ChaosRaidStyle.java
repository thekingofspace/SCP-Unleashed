package net.rai.scp_unleashed.raid.chaos;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.mtf.MtfLandingZone;
import net.rai.scp_unleashed.mtf.MtfRegistry;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidRules;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.underground.RaidBreach;
import net.rai.scp_unleashed.raid.underground.RaidSurvey;
import net.rai.scp_unleashed.raid.underground.UndergroundRules;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

public final class ChaosRaidStyle extends HostileRaidStyle {
    private static final Logger Log = LogUtils.getLogger();
    public static final ResourceLocation Id = HostileRegistry.location("chaos_insurgency");
    public static final ChaosRaidStyle Instance = new ChaosRaidStyle();
    public static final long RetreatTicks = 1200L;
    public static final int[] Squads = {3, 3, 4, 5, 6, 7};
    public static final int RadioSeconds = 4;
    public static final double RadioDistance = 20.0;
    public static final double ArrivalSpread = 1.0;
    public static final String ZoneKey = "Zone";
    public static final String HoverKey = "Hover";
    public static final String YawKey = "Yaw";
    public static final String HeliKey = "Heli";
    public static final String FootKey = "Foot";
    public static final String CalledKey = "Called";
    public static final String SentKey = "Sent";
    public static final long Redispatch = 600L;

    private ChaosRaidStyle() {
    }

    @Override
    public ResourceLocation id() {
        return Id;
    }

    @Override
    public HostileFaction faction() {
        return ChaosFaction.Instance;
    }

    @Override
    public String key() {
        return "chaos_insurgency";
    }

    @Override
    public long retreatTicks() {
        return RetreatTicks;
    }

    @Override
    protected int[] squadTable() {
        return Squads;
    }

    @Override
    protected int seekers(int Level) {
        return Level >= 3 ? 2 : 1;
    }

    @Nullable
    @Override
    protected HostileRaider create(ServerLevel World) {
        return HostileRegistry.InsurgentType.get().create(World);
    }

    @Override
    public void warn(RaidContext Context) {
        radio(Context, 1.0f);
    }

    @Override
    public void tickWarning(RaidContext Context) {
        long Seconds = (Context.Now() - Context.Current().stageAt()) / 20L;
        if (Seconds > 0 && Seconds % RadioSeconds == 0) {
            radio(Context, 0.85f + (Seconds % 3) * 0.1f);
        }
    }

    private static void radio(RaidContext Context, float Pitch) {
        double Heading = Context.Current().heading();
        RandomSource Random = Context.random();
        for (ServerPlayer User : Context.defenders(RaidRules.NoticeRange)) {
            double X = User.getX() + Math.cos(Heading) * RadioDistance;
            double Z = User.getZ() + Math.sin(Heading) * RadioDistance;
            User.connection.send(new ClientboundSoundPacket(MtfRegistry.RadioClick, SoundSource.HOSTILE, X, User.getY() + 1.0, Z, 1.0f, Pitch * 0.8f, Random.nextLong()));
            User.connection.send(new ClientboundSoundPacket(MtfRegistry.RadioBeep, SoundSource.HOSTILE, X, User.getY() + 1.0, Z, 0.8f, Pitch * 0.7f, Random.nextLong()));
        }
    }

    @Nullable
    public static MtfLandingZone.Zone zone(Raid Current) {
        CompoundTag Data = Current.extra();
        if (!Data.contains(ZoneKey)) {
            return null;
        }
        return new MtfLandingZone.Zone(BlockPos.of(Data.getLong(ZoneKey)), Data.getDouble(HoverKey), Data.getFloat(YawKey));
    }

    @Nullable
    public static Vec3 standPoint(Raid Current, int Side) {
        MtfLandingZone.Zone Found = zone(Current);
        if (Found == null) {
            return null;
        }
        Vec3 Stand = MtfLandingZone.standPoint(Found.hover(), Found.Yaw(), Side);
        return new Vec3(Stand.x, Found.Ground().getY(), Stand.z);
    }

    @Nullable
    public static MtfLandingZone.Zone landing(RaidContext Context) {
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        for (int Attempt = 0; Attempt < 12; Attempt++) {
            Vec3 Point = outside(Context, Attempt * 2.0, ArrivalSpread + Attempt * 0.2);
            BlockPos Probe = BlockPos.containing(Point);
            if (!World.isPositionEntityTicking(Probe)) {
                continue;
            }
            int Top = MtfLandingZone.surface(World, Point.x, Point.z);
            float Yaw = yawToward(Point, Vec3.atBottomCenterOf(Current.target()));
            Optional<MtfLandingZone.Zone> Found = MtfLandingZone.find(World, new Vec3(Point.x, Top, Point.z), Yaw);
            if (Found.isPresent() && World.isPositionEntityTicking(Found.get().Ground())) {
                return Found.get();
            }
        }
        return null;
    }

    public static byte underground(RaidContext Context) {
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        RaidSurvey Survey = RaidSurvey.survey(World, Current);
        if (!Survey.Underground()) {
            return RaidBreach.ModeNone;
        }
        Current.setTarget(Survey.Anchor());
        BlockPos Entrance = Survey.Flood().entrance();
        RaidBreach.Plan Found = null;
        ChaosInsurgent Probe = HostileRegistry.InsurgentType.get().create(World);
        if (Probe != null) {
            BlockPos Anchor = Survey.Anchor();
            Probe.moveTo(Anchor.getX() + 0.5, Anchor.getY(), Anchor.getZ() + 0.5, 0.0f, 0.0f);
            if (RaidBreach.allowed(World, Probe)) {
                Found = RaidBreach.plan(World, Survey, Probe, UndergroundRules.BlockCap);
            }
            Probe.discard();
        }
        if (Found != null) {
            RaidBreach.storeShaft(Current, Found, UndergroundRules.BlockCap, Entrance);
            Log.info("Raid {} targets an underground base, breaching down {} blocks at {} ({} blocks to clear)", Current.id(), Found.Top() - Found.Floor(),
                    Found.Lip().toShortString(), Found.Cells());
            return RaidBreach.ModeShaft;
        }
        if (Entrance != null) {
            RaidBreach.storeEntrance(Current, Entrance);
            Log.info("Raid {} targets an underground base and will use its entrance at {}", Current.id(), Entrance.toShortString());
            return RaidBreach.ModeEntrance;
        }
        RaidBreach.storeNone(Current);
        Log.info("Raid {} targets an underground base with no breach point or entrance", Current.id());
        return RaidBreach.ModeNone;
    }

    @Override
    protected boolean arrival(RaidContext Context) {
        CompoundTag Data = Context.data();
        underground(Context);
        MtfLandingZone.Zone Found = landing(Context);
        if (Found != null) {
            Data.putLong(ZoneKey, Found.Ground().asLong());
            Data.putDouble(HoverKey, Found.HoverY());
            Data.putFloat(YawKey, Found.Yaw());
            Data.putLong(RaidDirector.ExitKey, Found.Ground().asLong());
            ChaosHelicopter Ride = ChaosHelicopter.dispatch(Context.World(), Context.Current(), Found, Data.getInt(PendingKey), false);
            if (Ride != null) {
                Data.putString(HeliKey, Ride.getUUID().toString());
                Data.putLong(SentKey, Context.Now());
                Context.changed();
                return true;
            }
        }
        BlockPos Foot = footSpot(Context);
        if (Foot == null) {
            return false;
        }
        Data.remove(ZoneKey);
        Data.putLong(FootKey, Foot.asLong());
        Data.putLong(RaidDirector.ExitKey, Foot.asLong());
        Context.changed();
        return true;
    }

    @Nullable
    private static BlockPos footSpot(RaidContext Context) {
        ServerLevel World = Context.World();
        BlockPos Target = Context.Current().target();
        for (int Attempt = 0; Attempt < 10; Attempt++) {
            Vec3 Point = outside(Context, Attempt * 1.5, ArrivalSpread);
            BlockPos Found = RaidGround.surfaceNear(World, Mth.floor(Point.x), Mth.floor(Point.z), Target.getY(), 16, false);
            if (Found != null && World.isPositionEntityTicking(Found)) {
                return Found;
            }
        }
        double Ring = Context.Current().footprint();
        BlockPos Fallback = RaidGround.standAround(World, Target, Ring + 8.0, Ring + 30.0, Target.getY(), 16, Context.random(), 24, false);
        return Fallback != null && World.isPositionEntityTicking(Fallback) ? Fallback : null;
    }

    @Nullable
    public static ChaosHelicopter helicopter(RaidContext Context) {
        UUID Found = Raid.uuid(Context.data().getString(HeliKey));
        return Found != null && Context.World().getEntity(Found) instanceof ChaosHelicopter Ride && Ride.isAlive() ? Ride : null;
    }

    @Nullable
    public static ChaosHelicopter helicopterFor(ChaosInsurgent Insurgent) {
        RaidContext Context = context(Insurgent);
        return Context == null ? null : helicopter(Context);
    }

    @Nullable
    public static ChaosInsurgent rider(ServerLevel World, Raid Current, ChaosHelicopter Ride, int Side) {
        RaidContext Context = RaidDirector.context(World.getServer(), Current, World.getServer().overworld().getGameTime());
        if (Context == null || !(Context.Style() instanceof ChaosRaidStyle Style)) {
            return null;
        }
        Vec3 Stand = MtfLandingZone.standPoint(Ride.hover(), Ride.hoverYaw(), Side);
        double Top = Stand.y - ChaosInsurgent.HandReach;
        float Yaw = MtfLandingZone.inwardYaw(Ride.hoverYaw(), Side);
        HostileRaider Made = Style.spawn(Context, new Vec3(Stand.x, Top, Stand.z), Yaw);
        if (!(Made instanceof ChaosInsurgent Insurgent)) {
            return null;
        }
        Insurgent.beginRope(Ride, Side, Top);
        if (!World.addFreshEntity(Insurgent)) {
            Current.release(Insurgent.getUUID(), false);
            return null;
        }
        return Insurgent;
    }

    public static void deployed(ServerLevel World, Raid Current) {
        Current.extra().putInt(PendingKey, 0);
        Current.changed();
    }

    public static boolean expecting(ServerLevel World, Raid Current) {
        for (UUID Id : Current.raiders()) {
            if (!(World.getEntity(Id) instanceof ChaosInsurgent Insurgent) || !Insurgent.isAlive()) {
                continue;
            }
            byte State = Insurgent.state();
            if (State == HostileRaider.StateWaiting || State == HostileRaider.StateActive && (Insurgent.hauling() || Current.stage() == RaidStage.Retreating)) {
                return true;
            }
        }
        return false;
    }

    private static boolean anyWaiting(RaidContext Context) {
        for (HostileRaider Raider : raiders(Context)) {
            if (Raider.state() == HostileRaider.StateWaiting) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void upkeep(RaidContext Context, int Seconds) {
        CompoundTag Data = Context.data();
        Raid Current = Context.Current();
        if (Data.contains(FootKey)) {
            if (Current.stage() == RaidStage.Active && pending(Context) > 0) {
                walkIn(Context, BlockPos.of(Data.getLong(FootKey)));
            }
            return;
        }
        MtfLandingZone.Zone Found = zone(Current);
        if (Found == null) {
            return;
        }
        boolean Needed = Current.stage() == RaidStage.Retreating || anyWaiting(Context);
        if (!Needed) {
            if (Current.stage() == RaidStage.Active && pending(Context) > 0 && helicopter(Context) == null && Context.Now() - Data.getLong(SentKey) > Redispatch) {
                ChaosHelicopter Made = ChaosHelicopter.dispatch(Context.World(), Current, Found, pending(Context), false);
                if (Made != null) {
                    Data.putString(HeliKey, Made.getUUID().toString());
                    Data.putLong(SentKey, Context.Now());
                }
            }
            return;
        }
        ChaosHelicopter Ride = helicopter(Context);
        if (Ride != null && !Ride.isDeparting()) {
            if (!Ride.isPickup()) {
                Ride.toPickup(Found);
                deployed(Context.World(), Current);
            }
            return;
        }
        if (Ride != null) {
            return;
        }
        ChaosHelicopter Made = ChaosHelicopter.dispatch(Context.World(), Current, Found, 0, true);
        if (Made != null) {
            Data.putString(HeliKey, Made.getUUID().toString());
            Data.putLong(SentKey, Context.Now());
            if (Current.stage() == RaidStage.Active && !Data.getBoolean(CalledKey)) {
                Data.putBoolean(CalledKey, true);
                Context.notice("extraction");
            }
        }
    }

    private void walkIn(RaidContext Context, BlockPos Foot) {
        ServerLevel World = Context.World();
        Vec3 At = Vec3.atBottomCenterOf(Foot);
        HostileRaider Made = spawn(Context, At, yawToward(At, Vec3.atBottomCenterOf(Context.Current().target())));
        if (Made == null) {
            return;
        }
        World.addFreshEntity(Made);
        World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, At.x, At.y + 0.4, At.z, 6, 0.6, 0.2, 0.6, 0.01);
    }

    @Override
    protected void retreating(RaidContext Context) {
    }

    @Override
    public void arrive(RaidContext Context, HostileRaider Raider) {
        Raid Current = Context.Current();
        if (Raider instanceof ChaosInsurgent Climber && RaidBreach.open(Current) && RaidBreach.inside(Context.World(), Current, Climber)) {
            CompoundTag Tag = RaidBreach.data(Current);
            Climber.beginClimb(RaidBreach.axis(Tag), Tag.getInt("Top"), RaidBreach.lip(Tag));
            return;
        }
        BlockPos Door = RaidBreach.mode(Current) == RaidBreach.ModeEntrance ? RaidBreach.entrance(RaidBreach.data(Current)) : null;
        if (Door != null && Raider.position().distanceToSqr(Vec3.atBottomCenterOf(Door)) < 9.0) {
            return;
        }
        if (Context.data().contains(FootKey) || !(Raider instanceof ChaosInsurgent Insurgent)) {
            escape(Context, Raider);
            return;
        }
        Insurgent.beginWaiting();
    }

    @Override
    protected void fade(ServerLevel World, HostileRaider Raider) {
        if (Raider instanceof ChaosInsurgent Insurgent && Insurgent.roping()) {
            return;
        }
        Vec3 At = Raider.position();
        World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, At.x, At.y + 0.6, At.z, 5, 0.3, 0.4, 0.3, 0.01);
        World.sendParticles(ParticleTypes.POOF, At.x, At.y + 0.8, At.z, 8, 0.3, 0.5, 0.3, 0.02);
    }

    @Override
    protected void close(RaidContext Context, RaidOutcome Outcome) {
    }

    @Override
    protected void equip(RaidContext Context, HostileRaider Raider) {
        ChaosArsenal.equip(Context, Raider);
    }
}
