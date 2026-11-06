package net.rai.scp_unleashed.raid.daevite;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daevite.DaeviteCollapse;
import net.rai.scp_unleashed.daevite.DaeviteFaction;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;
import net.rai.scp_unleashed.daevite.DaeviteRitual;
import net.rai.scp_unleashed.daevite.DaeviteSites;
import net.rai.scp_unleashed.daevite.entity.DaeviteCircle;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.faction.FactionMember;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidContext;
import net.rai.scp_unleashed.raid.RaidDirector;
import net.rai.scp_unleashed.raid.RaidGround;
import net.rai.scp_unleashed.raid.RaidMark;
import net.rai.scp_unleashed.raid.RaidOutcome;
import net.rai.scp_unleashed.raid.RaidRegistry;
import net.rai.scp_unleashed.raid.RaidRules;
import net.rai.scp_unleashed.raid.RaidStyle;
import net.rai.scp_unleashed.raid.base.BaseEstimate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DaeviteRaidStyle implements RaidStyle {
    public static final ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "daevite_host");
    public static final DaeviteRaidStyle Instance = new DaeviteRaidStyle();
    public static final ResourceLocation CasterHealthId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "raid_caster_health");
    public static final ResourceLocation CasterArmourId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "raid_caster_armour");
    public static final double RaidPower = 300.0;
    public static final double SecondCirclePower = 600.0;
    public static final double SeerPower = 500.0;
    public static final int WarningTicks = 600;
    public static final long DurationTicks = 6000L;
    public static final long RetreatTicks = 300L;
    public static final long CooldownTicks = 48000L;
    public static final int MaxAlive = 8;
    public static final float CircleRadius = 3.2f;
    public static final double RingMin = 14.0;
    public static final double RingMax = 24.0;
    public static final double CircleSpread = 0.7;
    public static final double CircleGap = 9.0;
    public static final double InsideShare = 0.35;
    public static final int CastSeconds = 3;
    public static final int StepsPerSecond = 2;
    public static final int WaveSeconds = 25;
    public static final int DrumSeconds = 6;
    public static final int MarchSeconds = 12;
    public static final int RouteSeconds = 3;
    public static final int CalmSeconds = 8;
    public static final double CasterHealth = 0.75;
    public static final double CasterArmour = 4.0;
    public static final double CasterLoss = 40.0;
    public static final double DefeatLoss = 60.0;
    public static final double ProvokeRange = 12.0;
    public static final int HurtMemory = 100;
    public static final double ChannelOffset = 2.6;
    public static final double DrumDistance = 14.0;
    public static final double VanishReach = 3.0;
    public static final double Regroup = 20.0;
    public static final String CirclesKey = "Circles";
    public static final String MainKey = "Main";

    private DaeviteRaidStyle() {
    }

    public static boolean ready(FactionState State) {
        if (!State.phase().active() || State.power() < RaidPower || DaeviteRitual.active(State)) {
            return false;
        }
        FactionSite Temple = DaeviteSites.templeSite(State);
        if (Temple == null || !Temple.built()) {
            return false;
        }
        for (FactionMember Member : State.roster()) {
            if (DaeviteFaction.RoleOverseer.equals(Member.Role())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResourceLocation id() {
        return Id;
    }

    @Override
    public int warningTicks() {
        return WarningTicks;
    }

    @Override
    public long durationTicks() {
        return DurationTicks;
    }

    @Override
    public long retreatTicks() {
        return RetreatTicks;
    }

    @Override
    public long cooldownTicks() {
        return CooldownTicks;
    }

    @Override
    public int maxAlive() {
        return MaxAlive;
    }

    @Override
    public boolean allowsDimension(ResourceKey<Level> Dimension) {
        return Dimension == Level.OVERWORLD;
    }

    @Override
    public String noticeKey(String Event) {
        return "raid.scp_unleashed.daevite." + Event;
    }

    @Override
    public double heading(ServerLevel World, FactionType Faction, FactionState State, BlockPos Target) {
        FactionSite Temple = DaeviteSites.templeSite(State);
        double Jitter = (World.random.nextDouble() - 0.5) * 1.2;
        if (Temple == null || !Temple.dimension().equals(World.dimension())) {
            return World.random.nextDouble() * Mth.TWO_PI;
        }
        double Dx = Temple.anchor().getX() - Target.getX();
        double Dz = Temple.anchor().getZ() - Target.getZ();
        if (Dx * Dx + Dz * Dz < 1.0) {
            return World.random.nextDouble() * Mth.TWO_PI;
        }
        return Math.atan2(Dz, Dx) + Jitter;
    }

    private static List<CompoundTag> circles(RaidContext Context) {
        ListTag Saved = Context.data().getList(CirclesKey, Tag.TAG_COMPOUND);
        List<CompoundTag> Found = new ArrayList<>();
        for (int Index = 0; Index < Saved.size(); Index++) {
            Found.add(Saved.getCompound(Index));
        }
        return Found;
    }

    private static void storeCircles(RaidContext Context, List<CompoundTag> Circles) {
        ListTag Saved = new ListTag();
        Saved.addAll(Circles);
        Context.data().put(CirclesKey, Saved);
        Context.changed();
    }

    @Nullable
    private static DaeviteCircle circleEntity(ServerLevel World, CompoundTag Entry) {
        UUID Found = Raid.uuid(Entry.getString("Uuid"));
        return Found != null && World.getEntity(Found) instanceof DaeviteCircle Circle ? Circle : null;
    }

    private static boolean apart(List<BlockPos> Spots, BlockPos Candidate) {
        for (BlockPos Spot : Spots) {
            if (Spot.distSqr(Candidate) < CircleGap * CircleGap) {
                return false;
            }
        }
        return true;
    }

    private static void layer(ServerLevel World, Vec3 At, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, At.x, At.y, At.z, Sound, SoundSource.HOSTILE, Volume, Pitch);
    }

    private static void castEffects(ServerLevel World, Vec3 At) {
        RandomSource Random = World.random;
        layer(World, At, RaidRegistry.Kindle.get(), 3.0f, 0.9f + Random.nextFloat() * 0.1f);
        layer(World, At, DaeviteRegistry.RitualBegin.get(), 2.0f, 1.15f);
        layer(World, At, DaeviteRegistry.Cast.get(), 1.6f, 0.7f);
        for (int Step = 0; Step < 28; Step++) {
            double Angle = Step * Mth.TWO_PI / 28;
            World.sendParticles(DaeviteCollapse.Shard, At.x + Math.cos(Angle) * CircleRadius, At.y + 0.15, At.z + Math.sin(Angle) * CircleRadius, 1, 0.02, 0.05, 0.02, 0.01);
        }
        World.sendParticles(DaeviteCollapse.Cinder, At.x, At.y + 1.5, At.z, 40, 0.8, 1.4, 0.8, 0.06);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 0.4, At.z, 12, 1.2, 0.3, 1.2, 0.01);
        for (int Rise = 0; Rise < 8; Rise++) {
            World.sendParticles(ParticleTypes.FLAME, At.x, At.y + 0.4 + Rise * 0.7, At.z, 2, 0.12, 0.25, 0.12, 0.01);
        }
    }

    private static void runes(ServerLevel World, Vec3 At, int Seconds, float Glow) {
        int Glyphs = 6;
        double Turn = Seconds * 0.35;
        for (int Index = 0; Index < Glyphs; Index++) {
            double Angle = Turn + Index * Mth.TWO_PI / Glyphs;
            double X = At.x + Math.cos(Angle) * CircleRadius * 0.82;
            double Z = At.z + Math.sin(Angle) * CircleRadius * 0.82;
            World.sendParticles(DaeviteCollapse.Cinder, X, At.y + 0.1, Z, 2, 0.05, 0.25, 0.05, 0.02 + Glow * 0.03);
        }
        World.sendParticles(ParticleTypes.SMALL_FLAME, At.x, At.y + 0.1, At.z, 3, CircleRadius * 0.35, 0.02, CircleRadius * 0.35, 0.01);
        if (Seconds % 2 == 0) {
            World.sendParticles(DaeviteCollapse.Shard, At.x, At.y + 0.3, At.z, 4, CircleRadius * 0.5, 0.1, CircleRadius * 0.5, 0.0);
        }
    }

    private static void emerge(ServerLevel World, Vec3 At) {
        DaeviteSites.arrival(World, At);
        World.sendParticles(DaeviteCollapse.Shard, At.x, At.y + 1.0, At.z, 16, 0.3, 0.9, 0.3, 0.02);
    }

    @Override
    public void warn(RaidContext Context) {
        beat(Context, 0);
    }

    @Override
    public void tickWarning(RaidContext Context) {
        long Seconds = (Context.Now() - Context.Current().stageAt()) / 20L;
        if (Seconds > 0 && Seconds % DrumSeconds == 0) {
            beat(Context, (int) (Seconds / DrumSeconds));
        }
    }

    private static void beat(RaidContext Context, int Count) {
        Raid Current = Context.Current();
        double Heading = Current.heading();
        RandomSource Random = Context.random();
        for (ServerPlayer User : Context.defenders(RaidRules.NoticeRange)) {
            double X = User.getX() + Math.cos(Heading) * DrumDistance;
            double Z = User.getZ() + Math.sin(Heading) * DrumDistance;
            User.connection.send(new ClientboundSoundPacket(DaeviteRegistry.Drum, SoundSource.HOSTILE, X, User.getY() + 2.0, Z, 1.4f, 1.0f, Random.nextLong()));
            if (Count % 2 == 0) {
                User.connection.send(new ClientboundSoundPacket(RaidRegistry.Warhorn, SoundSource.HOSTILE, X, User.getY() + 4.0, Z, 1.2f, 0.85f, Random.nextLong()));
            } else {
                User.connection.send(new ClientboundSoundPacket(DaeviteRegistry.Horn, SoundSource.HOSTILE, X, User.getY() + 4.0, Z, 0.9f, 1.0f, Random.nextLong()));
            }
        }
    }

    @Override
    public boolean begin(RaidContext Context) {
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        RandomSource Random = Context.random();
        BlockPos Target = Current.target();
        FactionState State = Context.State();
        double Power = State.power();
        double Ring = Current.footprint();
        int Outside = Power >= SecondCirclePower || State.phase() == FactionPhase.Ascendant ? 2 : 1;
        List<BlockPos> Spots = new ArrayList<>();
        for (int Index = 0; Index < Outside; Index++) {
            double Angle = Current.heading() + (Index - (Outside - 1) * 0.5) * CircleSpread;
            BlockPos Spot = null;
            for (int Attempt = 0; Attempt < 10 && Spot == null; Attempt++) {
                double Reach = Ring + RingMin + Random.nextDouble() * (RingMax - RingMin) + Attempt * 2.0;
                double Turn = Angle + (Random.nextDouble() - 0.5) * 0.3 * (Attempt + 1);
                BlockPos Found = RaidGround.surfaceNear(World, Target.getX() + Mth.floor(Math.cos(Turn) * Reach), Target.getZ() + Mth.floor(Math.sin(Turn) * Reach),
                        Target.getY(), 16, true);
                if (Found != null && apart(Spots, Found)) {
                    Spot = Found;
                }
            }
            if (Spot != null) {
                Spots.add(Spot);
            }
        }
        if (Spots.isEmpty()) {
            BlockPos Fallback = RaidGround.standAround(World, Target, Ring + RingMin, Ring + RingMax + 10.0, Target.getY(), 16, Random, 24, false);
            if (Fallback != null) {
                Spots.add(Fallback);
            }
        }
        BlockPos Inside = null;
        if (Ring >= BaseEstimate.LargeFootprint) {
            Inside = RaidGround.standAround(World, Target, 2.0, Ring * InsideShare, Target.getY(), 6, Random, 16, false);
            if (Inside != null && !apart(Spots, Inside)) {
                Inside = null;
            }
        }
        if (Spots.isEmpty() && Inside == null) {
            return false;
        }
        BlockPos Main = Spots.isEmpty() ? Inside : Spots.get(0);
        List<CompoundTag> Circles = new ArrayList<>();
        for (BlockPos Spot : Spots) {
            Circles.add(openCircle(World, Spot, false));
        }
        if (Inside != null) {
            Circles.add(openCircle(World, Inside, true));
        }
        storeCircles(Context, Circles);
        CompoundTag Data = Context.data();
        Data.putLong(MainKey, Main.asLong());
        Data.putLong(RaidDirector.ExitKey, Main.asLong());
        Data.putInt("Seconds", 0);
        Data.putInt("LastFight", -CalmSeconds);
        int Knights = Mth.clamp(2 + (int) (Power / 300.0), 2, 5);
        int Budget = Mth.clamp(2 + (int) (Power / 250.0), 2, 6);
        if (World.getDifficulty() == Difficulty.HARD) {
            Knights++;
            Budget++;
        } else if (World.getDifficulty() == Difficulty.EASY) {
            Knights = Math.max(2, Knights - 1);
            Budget = Math.max(1, Budget - 1);
        }
        if (State.phase() == FactionPhase.Ascendant) {
            Budget += 2;
        }
        Data.putInt("Pending", Knights);
        Data.putInt("PendingSeers", Power >= SeerPower ? 1 : 0);
        Data.putInt("Budget", Budget);
        Data.putInt("NextWave", CastSeconds + WaveSeconds);
        Data.putLong("Routed", Target.asLong());
        Context.changed();
        summonCaster(Context, Main);
        return true;
    }

    private static CompoundTag openCircle(ServerLevel World, BlockPos Spot, boolean Inside) {
        Vec3 Floor = Vec3.atBottomCenterOf(Spot);
        DaeviteCircle Circle = DaeviteCircle.ritual(World, Floor, CircleRadius);
        Circle.setPower(0.0f);
        castEffects(World, Floor);
        CompoundTag Entry = new CompoundTag();
        Entry.putLong("Pos", Spot.asLong());
        Entry.putString("Uuid", Circle.getUUID().toString());
        Entry.putBoolean("Inside", Inside);
        Entry.putBoolean("Open", true);
        return Entry;
    }

    private static BlockPos channelSpot(ServerLevel World, BlockPos Main, BlockPos Target) {
        double Dx = Main.getX() - Target.getX();
        double Dz = Main.getZ() - Target.getZ();
        double Length = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Length < 0.5) {
            return Main;
        }
        int X = Main.getX() + Mth.floor(Dx / Length * ChannelOffset + 0.5);
        int Z = Main.getZ() + Mth.floor(Dz / Length * ChannelOffset + 0.5);
        BlockPos Found = RaidGround.standNear(World, X, Z, Main.getY(), 3);
        return Found != null ? Found : Main;
    }

    private static void summonCaster(RaidContext Context, BlockPos Main) {
        ServerLevel World = Context.World();
        DaeviteOverseer Caster = DaeviteRegistry.Overseer.get().create(World);
        if (Caster == null) {
            return;
        }
        BlockPos Stand = channelSpot(World, Main, Context.Current().target());
        Vec3 Facing = Vec3.atCenterOf(Context.Current().target()).subtract(Vec3.atCenterOf(Stand));
        float Yaw = (float) (Mth.atan2(Facing.z, Facing.x) * Mth.RAD_TO_DEG) - 90.0f;
        Caster.moveTo(Stand.getX() + 0.5, Stand.getY(), Stand.getZ() + 0.5, Yaw, 0.0f);
        Caster.finalizeSpawn(World, World.getCurrentDifficultyAt(Stand), MobSpawnType.EVENT, null);
        Caster.setHome(Main);
        AttributeInstance Health = Caster.getAttribute(Attributes.MAX_HEALTH);
        if (Health != null && !Health.hasModifier(CasterHealthId)) {
            Health.addPermanentModifier(new AttributeModifier(CasterHealthId, CasterHealth, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        AttributeInstance Armour = Caster.getAttribute(Attributes.ARMOR);
        if (Armour != null && !Armour.hasModifier(CasterArmourId)) {
            Armour.addPermanentModifier(new AttributeModifier(CasterArmourId, CasterArmour, AttributeModifier.Operation.ADD_VALUE));
        }
        Caster.setHealth(Caster.getMaxHealth());
        FactionControl.enlist(DaeviteFaction.Instance, Context.State(), Caster, -1, DaeviteFaction.RoleOverseer);
        Context.enlist(Caster, RaidMark.Caster);
        World.addFreshEntity(Caster);
        Caster.assignChannel(Vec3.atBottomCenterOf(Stand), Vec3.atBottomCenterOf(Main));
        emerge(World, Vec3.atBottomCenterOf(Stand));
    }

    @Nullable
    private static DaeviteMob raider(RaidContext Context, BlockPos Circle, boolean Seer) {
        ServerLevel World = Context.World();
        RandomSource Random = Context.random();
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        double Reach = Random.nextDouble() * CircleRadius * 0.5;
        BlockPos At = RaidGround.standNear(World, Circle.getX() + Mth.floor(Math.cos(Angle) * Reach + 0.5), Circle.getZ() + Mth.floor(Math.sin(Angle) * Reach + 0.5),
                Circle.getY(), 3);
        if (At == null) {
            At = Circle;
        }
        DaeviteMob Member = Seer ? DaeviteRegistry.Overseer.get().create(World) : DaeviteRegistry.Knight.get().create(World);
        if (Member == null) {
            return null;
        }
        BlockPos Target = Context.Current().target();
        Vec3 Facing = Vec3.atCenterOf(Target).subtract(Vec3.atCenterOf(At));
        float Yaw = (float) (Mth.atan2(Facing.z, Facing.x) * Mth.RAD_TO_DEG) - 90.0f;
        Member.moveTo(At.getX() + 0.5, At.getY(), At.getZ() + 0.5, Yaw, 0.0f);
        Member.finalizeSpawn(World, World.getCurrentDifficultyAt(At), MobSpawnType.EVENT, null);
        FactionControl.enlist(DaeviteFaction.Instance, Context.State(), Member, -1, Seer ? DaeviteFaction.RoleOverseer : DaeviteFaction.RoleKnight);
        Context.enlist(Member, RaidMark.Raider);
        World.addFreshEntity(Member);
        Member.setHome(Target);
        Member.setPatrol(Vec3.atBottomCenterOf(Target), Context.Current().deadline() + RetreatTicks);
        emerge(World, Vec3.atBottomCenterOf(At));
        return Member;
    }

    private static void upkeep(RaidContext Context, int Seconds, float Glow) {
        ServerLevel World = Context.World();
        List<CompoundTag> Circles = circles(Context);
        boolean Changed = false;
        for (CompoundTag Entry : Circles) {
            if (!Entry.getBoolean("Open")) {
                continue;
            }
            BlockPos Pos = BlockPos.of(Entry.getLong("Pos"));
            if (!World.isPositionEntityTicking(Pos)) {
                continue;
            }
            DaeviteCircle Circle = circleEntity(World, Entry);
            if (Circle == null) {
                Circle = DaeviteCircle.ritual(World, Vec3.atBottomCenterOf(Pos), CircleRadius);
                Entry.putString("Uuid", Circle.getUUID().toString());
                Changed = true;
            }
            Circle.setPower(Glow);
            runes(World, Vec3.atBottomCenterOf(Pos), Seconds, Glow);
        }
        if (Changed) {
            storeCircles(Context, Circles);
        }
    }

    @Nullable
    private static BlockPos nextCircle(RaidContext Context) {
        List<CompoundTag> Open = new ArrayList<>();
        for (CompoundTag Entry : circles(Context)) {
            if (Entry.getBoolean("Open") && Context.World().isPositionEntityTicking(BlockPos.of(Entry.getLong("Pos")))) {
                Open.add(Entry);
            }
        }
        if (Open.isEmpty()) {
            return null;
        }
        int Cursor = Context.data().getInt("Cursor");
        Context.data().putInt("Cursor", Cursor + 1);
        return BlockPos.of(Open.get(Math.floorMod(Cursor, Open.size())).getLong("Pos"));
    }

    private static boolean threatened(DaeviteOverseer Caster) {
        LivingEntity Prey = Caster.getTarget();
        if (Prey == null) {
            return false;
        }
        boolean Hurt = Caster.getLastHurtByMob() != null && Caster.tickCount - Caster.getLastHurtByMobTimestamp() < HurtMemory;
        return Hurt || Caster.distanceTo(Prey) <= ProvokeRange;
    }

    @Override
    public void tick(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Seconds = Data.getInt("Seconds") + 1;
        Data.putInt("Seconds", Seconds);
        ServerLevel World = Context.World();
        Raid Current = Context.Current();
        boolean CasterDown = Data.getBoolean("CasterDown");
        Mob Found = Context.caster();
        DaeviteOverseer Caster = Found instanceof DaeviteOverseer Seer ? Seer : null;
        if (Caster != null) {
            if (Caster.channelPoint() != null && threatened(Caster)) {
                Caster.stopChannel();
                Data.putInt("LastFight", Seconds);
            } else if (Caster.channelPoint() == null && Caster.getTarget() != null) {
                Data.putInt("LastFight", Seconds);
            } else if (Caster.channelPoint() == null && Seconds - Data.getInt("LastFight") >= CalmSeconds) {
                BlockPos Main = BlockPos.of(Data.getLong(MainKey));
                Caster.assignChannel(Vec3.atBottomCenterOf(channelSpot(World, Main, Current.target())), Vec3.atBottomCenterOf(Main));
            }
        }
        boolean Channeling = Caster != null && Caster.channelPoint() != null;
        float Glow = Math.min(1.0f, Seconds / (float) CastSeconds) * (Channeling || Caster == null ? 1.0f : 0.6f);
        upkeep(Context, Seconds, Glow);
        int Pending = Data.getInt("Pending");
        int Seers = Data.getInt("PendingSeers");
        int Budget = Data.getInt("Budget");
        if (!CasterDown && Seconds >= CastSeconds && Pending + Seers > 0) {
            int Steps = Math.min(StepsPerSecond, Context.room());
            for (int Step = 0; Step < Steps && Pending + Seers > 0; Step++) {
                BlockPos Circle = nextCircle(Context);
                if (Circle == null) {
                    break;
                }
                boolean Seer = Seers > 0 && (Pending == 0 || Context.random().nextBoolean());
                if (raider(Context, Circle, Seer) == null) {
                    break;
                }
                if (Seer) {
                    Seers--;
                } else {
                    Pending--;
                }
            }
            Data.putInt("Pending", Pending);
            Data.putInt("PendingSeers", Seers);
        }
        if (!CasterDown && Channeling && Pending + Seers == 0 && Budget > 0 && Seconds >= Data.getInt("NextWave") && Context.room() > 0) {
            int Wave = Math.min(Budget, 1 + Context.random().nextInt(2));
            Data.putInt("Pending", Wave);
            Data.putInt("Budget", Budget - Wave);
            Data.putInt("NextWave", Seconds + WaveSeconds);
            Vec3 Heart = Vec3.atBottomCenterOf(BlockPos.of(Data.getLong(MainKey)));
            layer(World, Heart, RaidRegistry.Kindle.get(), 2.0f, 1.05f);
            layer(World, Heart, RaidRegistry.Warhorn.get(), 3.0f, 0.9f);
        }
        if (Seconds % RouteSeconds == 0) {
            route(Context);
        }
        if (Seconds % MarchSeconds == 0) {
            drums(Context, Seconds / MarchSeconds);
        }
        Context.changed();
    }

    private static void route(RaidContext Context) {
        CompoundTag Data = Context.data();
        BlockPos Target = Context.Current().target();
        boolean Moved = Data.getLong("Routed") != Target.asLong();
        Data.putLong("Routed", Target.asLong());
        Vec3 Destination = Vec3.atBottomCenterOf(Target);
        long Until = Context.Current().deadline() + RetreatTicks;
        for (Mob Raider : Context.loadedRaiders()) {
            if (!(Raider instanceof DaeviteMob Member) || Context.Current().isCaster(Raider.getUUID()) || Member.getTarget() != null) {
                continue;
            }
            Member.setHome(Target);
            boolean Far = Member.position().distanceToSqr(Destination) > Regroup * Regroup;
            if (Moved && Member.onPatrol() || !Member.onPatrol() && Far) {
                Member.setPatrol(Destination, Until);
            }
        }
    }

    private static void drums(RaidContext Context, int Count) {
        List<Mob> Marching = new ArrayList<>();
        for (Mob Raider : Context.loadedRaiders()) {
            if (!Context.Current().isCaster(Raider.getUUID())) {
                Marching.add(Raider);
            }
        }
        if (Marching.isEmpty()) {
            return;
        }
        double X = 0.0;
        double Y = 0.0;
        double Z = 0.0;
        for (Mob Raider : Marching) {
            X += Raider.getX();
            Y += Raider.getY();
            Z += Raider.getZ();
        }
        Vec3 Middle = new Vec3(X / Marching.size(), Y / Marching.size() + 1.0, Z / Marching.size());
        ServerLevel World = Context.World();
        layer(World, Middle, DaeviteRegistry.Drum.get(), 3.0f, 1.0f + Context.random().nextFloat() * 0.1f);
        if (Count % 2 == 0) {
            layer(World, Middle, RaidRegistry.Warhorn.get(), 3.5f, 0.95f);
        }
    }

    @Override
    public boolean exhausted(RaidContext Context) {
        CompoundTag Data = Context.data();
        return Data.getBoolean("CasterDown") || Data.getInt("Pending") + Data.getInt("PendingSeers") + Data.getInt("Budget") == 0;
    }

    @Override
    public void raiderLost(RaidContext Context, LivingEntity Raider, DamageSource Source, boolean WasCaster) {
        FactionState State = Context.State();
        boolean Unmade = Raider instanceof DaeviteMob Member && Member.unmade();
        if (WasCaster) {
            CompoundTag Data = Context.data();
            Data.putBoolean("CasterDown", true);
            Data.putInt("Pending", 0);
            Data.putInt("PendingSeers", 0);
            Data.putInt("Budget", 0);
            Context.changed();
            closeCircles(Context, true);
            if (!Unmade) {
                State.setPower(State.power() - CasterLoss, DaeviteFaction.MaxPower);
                Context.notice("caster_down");
            }
            return;
        }
        if (Unmade) {
            return;
        }
        double Loss = Raider instanceof DaeviteOverseer ? DaeviteFaction.OverseerLoss : DaeviteFaction.KnightLoss;
        State.setPower(State.power() - Loss, DaeviteFaction.MaxPower);
    }

    private static void closeCircles(RaidContext Context, boolean Shatter) {
        ServerLevel World = Context.World();
        List<CompoundTag> Circles = circles(Context);
        for (CompoundTag Entry : Circles) {
            if (!Entry.getBoolean("Open")) {
                continue;
            }
            Entry.putBoolean("Open", false);
            DaeviteCircle Circle = circleEntity(World, Entry);
            if (Circle != null) {
                Circle.dismiss();
            }
            Vec3 At = Vec3.atBottomCenterOf(BlockPos.of(Entry.getLong("Pos")));
            if (Shatter) {
                World.sendParticles(DaeviteCollapse.Shard, At.x, At.y + 0.4, At.z, 60, CircleRadius * 0.6, 0.3, CircleRadius * 0.6, 0.1);
                World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 0.4, At.z, 16, CircleRadius * 0.5, 0.3, CircleRadius * 0.5, 0.02);
                layer(World, At, RaidRegistry.Rout.get(), 3.0f, 0.9f);
                layer(World, At, DaeviteRegistry.RitualBreak.get(), 2.5f, 1.0f);
            } else {
                World.sendParticles(ParticleTypes.SMOKE, At.x, At.y + 0.2, At.z, 20, CircleRadius * 0.5, 0.1, CircleRadius * 0.5, 0.01);
                World.sendParticles(DaeviteCollapse.Ash, At.x, At.y + 0.3, At.z, 16, CircleRadius * 0.5, 0.2, CircleRadius * 0.5, 0.01);
            }
        }
        storeCircles(Context, Circles);
    }

    @Nullable
    private static Vec3 nearestCircle(RaidContext Context, Vec3 From) {
        Vec3 Best = null;
        double BestGap = Double.MAX_VALUE;
        for (CompoundTag Entry : circles(Context)) {
            if (!Entry.getBoolean("Open")) {
                continue;
            }
            Vec3 At = Vec3.atBottomCenterOf(BlockPos.of(Entry.getLong("Pos")));
            double Gap = At.distanceToSqr(From);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = At;
            }
        }
        return Best;
    }

    private static Vec3 haven(RaidContext Context, Vec3 From) {
        Vec3 Circle = nearestCircle(Context, From);
        return Circle != null ? Circle : Vec3.atBottomCenterOf(RaidDirector.exitOf(Context.Current()));
    }

    @Override
    public void retreat(RaidContext Context) {
        long Until = Context.Now() + RetreatTicks + 100L;
        for (Mob Raider : Context.loadedRaiders()) {
            if (!(Raider instanceof DaeviteMob Member)) {
                continue;
            }
            Vec3 Haven = haven(Context, Member.position());
            if (Member instanceof DaeviteOverseer Seer) {
                Seer.stopChannel();
            }
            Member.setTarget(null);
            Member.setHome(BlockPos.containing(Haven));
            Member.setPatrol(Haven, Until);
        }
        Vec3 Heart = Vec3.atBottomCenterOf(RaidDirector.exitOf(Context.Current()));
        layer(Context.World(), Heart, RaidRegistry.Recall.get(), 3.0f, 0.8f);
        layer(Context.World(), Heart, RaidRegistry.Warhorn.get(), 3.0f, 0.75f);
    }

    @Override
    public void tickRetreat(RaidContext Context) {
        CompoundTag Data = Context.data();
        int Seconds = Data.getInt("Seconds") + 1;
        Data.putInt("Seconds", Seconds);
        upkeep(Context, Seconds, 0.5f);
        for (Mob Raider : Context.loadedRaiders()) {
            Vec3 Haven = haven(Context, Raider.position());
            if (Raider.position().distanceToSqr(Haven.x, Raider.getY(), Haven.z) <= VanishReach * VanishReach) {
                Context.Current().release(Raider.getUUID(), false);
                dismiss(Context.World(), Raider);
            }
        }
    }

    @Override
    public void dismiss(ServerLevel World, Mob Raider) {
        Vec3 At = Raider.position();
        World.sendParticles(DaeviteCollapse.Cinder, At.x, At.y + 1.0, At.z, 30, 0.35, 0.8, 0.35, 0.05);
        World.sendParticles(DaeviteCollapse.Ash, At.x, At.y + 0.8, At.z, 20, 0.3, 0.6, 0.3, 0.02);
        World.sendParticles(ParticleTypes.FLAME, At.x, At.y + 0.1, At.z, 10, 0.4, 0.05, 0.4, 0.02);
        layer(World, At, RaidRegistry.Recall.get(), 1.2f, 0.9f + World.random.nextFloat() * 0.2f);
        layer(World, At, DaeviteRegistry.Arrive.get(), 0.8f, 0.9f);
        Raider.discard();
    }

    @Override
    public void ended(RaidContext Context, RaidOutcome Outcome) {
        closeCircles(Context, Outcome == RaidOutcome.Defeated);
        if (Outcome == RaidOutcome.Defeated) {
            FactionState State = Context.State();
            State.setPower(State.power() - DefeatLoss, DaeviteFaction.MaxPower);
        }
    }
}
