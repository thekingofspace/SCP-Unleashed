package net.rai.scp_unleashed.raid.serpentshand;

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
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidRules;
import net.rai.scp_unleashed.raid.RaidStage;
import net.rai.scp_unleashed.raid.hostile.HostileFaction;
import net.rai.scp_unleashed.raid.hostile.HostileHauls;
import net.rai.scp_unleashed.raid.hostile.HostileLedger;
import net.rai.scp_unleashed.raid.hostile.HostileRaidStyle;
import net.rai.scp_unleashed.raid.hostile.HostileRaider;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;
import net.rai.scp_unleashed.raid.underground.RaidCavities;
import net.rai.scp_unleashed.raid.underground.RaidSurvey;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

public final class SerpentsHandRaidStyle extends HostileRaidStyle {
    public static final ResourceLocation Id = HostileRegistry.location("serpents_hand");
    public static final SerpentsHandRaidStyle Instance = new SerpentsHandRaidStyle();
    public static final long RetreatTicks = 600L;
    public static final int[] Squads = {2, 3, 3, 4, 5, 6};
    public static final int ChimeSeconds = 5;
    public static final double ChimeDistance = 16.0;
    public static final double PodChance = 0.25;
    public static final int PodMax = 2;
    public static final double ArrivalSpread = 0.9;
    public static final double EmergeSpread = 0.35;
    public static final String SealKey = "Seal";
    public static final String SealIdKey = "SealId";
    public static final String TierKey = "SealTier";
    private static final Logger Log = LogUtils.getLogger();

    private SerpentsHandRaidStyle() {
    }

    @Override
    public ResourceLocation id() {
        return Id;
    }

    @Override
    public HostileFaction faction() {
        return SerpentsHandFaction.Instance;
    }

    @Override
    public String key() {
        return "serpents_hand";
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
        return Level >= 4 ? 2 : 1;
    }

    @Nullable
    @Override
    protected HostileRaider create(ServerLevel World) {
        return HostileRegistry.AgentType.get().create(World);
    }

    @Override
    public void warn(RaidContext Context) {
        chime(Context);
    }

    @Override
    public void tickWarning(RaidContext Context) {
        long Seconds = (Context.Now() - Context.Current().stageAt()) / 20L;
        if (Seconds > 0 && Seconds % ChimeSeconds == 0) {
            chime(Context);
        }
    }

    private static void chime(RaidContext Context) {
        double Heading = Context.Current().heading();
        RandomSource Random = Context.random();
        for (ServerPlayer User : Context.defenders(RaidRules.NoticeRange)) {
            double X = User.getX() + Math.cos(Heading) * ChimeDistance;
            double Z = User.getZ() + Math.sin(Heading) * ChimeDistance;
            User.connection.send(new ClientboundSoundPacket(HostileRegistry.SerpentWarning, SoundSource.HOSTILE, X, User.getY() + 2.0, Z, 1.1f, 1.0f, Random.nextLong()));
            User.connection.send(new ClientboundSoundPacket(HostileRegistry.SealChime, SoundSource.HOSTILE, X, User.getY() + 1.0, Z, 0.9f, 1.0f, Random.nextLong()));
        }
    }

    @Nullable
    public static BlockPos sealSpot(RaidContext Context) {
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        BlockPos Target = Current.target();
        for (int Attempt = 0; Attempt < 14; Attempt++) {
            Vec3 Point = outside(Context, Attempt * 1.5, ArrivalSpread + Attempt * 0.15);
            BlockPos Found = RaidGround.surfaceNear(World, Mth.floor(Point.x), Mth.floor(Point.z), Target.getY(), 16, false);
            if (Found != null && World.isPositionEntityTicking(Found)) {
                return Found;
            }
        }
        double Ring = Current.footprint();
        BlockPos Fallback = RaidGround.standAround(World, Target, Ring + 8.0, Ring + 30.0, Target.getY(), 16, Context.random(), 24, false);
        return Fallback != null && World.isPositionEntityTicking(Fallback) ? Fallback : null;
    }

    public static boolean underground(RaidContext Context) {
        return Context.data().contains(TierKey);
    }

    @Nullable
    public static RaidCavities.Spot cavity(RaidContext Context) {
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        RaidSurvey Survey = RaidSurvey.survey(World, Current);
        if (!Survey.Underground()) {
            return null;
        }
        Current.setTarget(Survey.Anchor());
        RaidCavities.Spot Found = null;
        SerpentsHandAgent Probe = HostileRegistry.AgentType.get().create(World);
        if (Probe != null) {
            Found = RaidCavities.find(World, Survey, Context.defenders(RaidRules.PresenceReach), Probe);
            Probe.discard();
        }
        if (Found == null) {
            BlockPos Legacy = sealSpot(Context);
            if (Legacy != null && RaidCavities.safeStand(World, Legacy)) {
                Found = new RaidCavities.Spot(Legacy, RaidCavities.TierSurface);
            }
        }
        Context.data().putInt(TierKey, Found == null ? 0 : Found.Tier());
        Context.changed();
        Log.info("Raid {} targets an underground base, warp point {} (tier {})", Current.id(), Found == null ? "none" : Found.Feet().toShortString(),
                Found == null ? 0 : Found.Tier());
        return Found;
    }

    @Override
    protected boolean arrival(RaidContext Context) {
        RaidCavities.Spot Below = cavity(Context);
        if (Below != null) {
            openAt(Context, Below.Feet());
            return true;
        }
        if (underground(Context)) {
            return false;
        }
        BlockPos Spot = sealSpot(Context);
        if (Spot == null) {
            return false;
        }
        openAt(Context, Spot);
        return true;
    }

    public static SerpentSeal openAt(RaidContext Context, BlockPos Spot) {
        CompoundTag Data = Context.data();
        SerpentSeal Seal = SerpentSeal.open(Context.World(), Vec3.atBottomCenterOf(Spot), Context.Current().id(), false);
        Data.putLong(SealKey, Spot.asLong());
        Data.putString(SealIdKey, Seal.getUUID().toString());
        Data.putLong(RaidDirector.ExitKey, Spot.asLong());
        Context.changed();
        return Seal;
    }

    public static Vec3 centre(RaidContext Context) {
        SerpentSeal Seal = seal(Context);
        return Seal != null ? Seal.position() : Vec3.atBottomCenterOf(RaidDirector.exitOf(Context.Current()));
    }

    @Nullable
    public static SerpentSeal seal(RaidContext Context) {
        UUID Found = Raid.uuid(Context.data().getString(SealIdKey));
        return Found != null && Context.World().getEntity(Found) instanceof SerpentSeal Seal && Seal.isAlive() ? Seal : null;
    }

    @Override
    protected void upkeep(RaidContext Context, int Seconds) {
        CompoundTag Data = Context.data();
        if (!Data.contains(SealKey)) {
            return;
        }
        ServerLevel World = Context.World();
        BlockPos Spot = BlockPos.of(Data.getLong(SealKey));
        if (!World.isPositionEntityTicking(Spot)) {
            return;
        }
        SerpentSeal Seal = seal(Context);
        if (Seal == null) {
            Seal = SerpentSeal.open(World, Vec3.atBottomCenterOf(Spot), Context.Current().id(), true);
            Data.putString(SealIdKey, Seal.getUUID().toString());
        }
        Seal.setFlare(Context.Current().stage() == RaidStage.Retreating);
        if (Context.Current().stage() == RaidStage.Active && pending(Context) > 0) {
            emerge(Context, Spot);
        }
    }

    private void emerge(RaidContext Context, BlockPos Spot) {
        ServerLevel World = Context.World();
        RandomSource Random = Context.random();
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        double Reach = Random.nextDouble() * EmergeSpread;
        Vec3 At = new Vec3(Spot.getX() + 0.5 + Math.cos(Angle) * Reach, Spot.getY(), Spot.getZ() + 0.5 + Math.sin(Angle) * Reach);
        HostileRaider Made = spawn(Context, At, yawToward(At, Vec3.atBottomCenterOf(Context.Current().target())));
        if (Made == null) {
            return;
        }
        Made.beginRise(At);
        World.addFreshEntity(Made);
        World.playSound(null, At.x, At.y, At.z, HostileRegistry.SealRise.get(), SoundSource.HOSTILE, 1.4f, 0.9f + Random.nextFloat() * 0.2f);
    }

    @Override
    protected void retreating(RaidContext Context) {
        SerpentSeal Seal = seal(Context);
        if (Seal != null) {
            Seal.setFlare(true);
            Context.World().playSound(null, Seal.getX(), Seal.getY(), Seal.getZ(), HostileRegistry.SealChime.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
        }
    }

    @Override
    public void arrive(RaidContext Context, HostileRaider Raider) {
        Raider.beginSink(centre(Context));
    }

    @Override
    public void sinking(RaidContext Context, HostileRaider Raider) {
        Context.World().playSound(null, Raider.getX(), Raider.getY(), Raider.getZ(), HostileRegistry.SealSink.get(), SoundSource.HOSTILE, 1.2f,
                0.9f + Context.random().nextFloat() * 0.2f);
    }

    @Override
    protected void fade(ServerLevel World, HostileRaider Raider) {
        Vec3 At = Raider.position();
        World.sendParticles(SerpentPods.Teal, At.x, At.y + 0.8, At.z, 24, 0.3, 0.7, 0.3, 0.0);
        World.sendParticles(SerpentPods.Gold, At.x, At.y + 0.4, At.z, 12, 0.3, 0.4, 0.3, 0.0);
        World.sendParticles(ParticleTypes.END_ROD, At.x, At.y + 0.6, At.z, 4, 0.2, 0.4, 0.2, 0.02);
        if (Raider.state() != HostileRaider.StateSinking) {
            World.playSound(null, At.x, At.y, At.z, HostileRegistry.SealSink.get(), SoundSource.HOSTILE, 1.0f, 1.1f);
        }
    }

    @Override
    protected void close(RaidContext Context, RaidOutcome Outcome) {
        SerpentSeal Seal = seal(Context);
        if (Seal != null) {
            Seal.close();
        }
    }

    @Override
    protected void equip(RaidContext Context, HostileRaider Raider) {
        if (HostileHauls.issuedCount(Context.Current()) >= PodMax || Context.random().nextDouble() >= PodChance) {
            return;
        }
        issuePod(Context, Raider);
    }

    public static boolean issuePod(RaidContext Context, HostileRaider Raider) {
        FactionState State = Context.State();
        Raid Current = Context.Current();
        List<HostileLedger.Entry> Free = HostileLedger.entries(State, One -> One.Trophy().creature() && !HostileHauls.isIssued(Current, One.Id()));
        if (Free.isEmpty()) {
            return false;
        }
        HostileLedger.Entry Pick = Free.get(Context.random().nextInt(Free.size()));
        Raider.issue(Pick.Id(), SerpentPodItem.filled(Pick.Trophy(), Context.World().registryAccess()), HostileRaider.SlotHidden);
        HostileHauls.issue(Current, Raider.getUUID(), Pick.Id());
        return true;
    }
}
