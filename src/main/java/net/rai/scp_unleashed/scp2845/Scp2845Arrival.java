package net.rai.scp_unleashed.scp2845;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.archive.investigation.Investigation;
import net.rai.scp_unleashed.archive.investigation.ReaderTargets;
import net.rai.scp_unleashed.archive.investigation.StartCondition;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.scp002.Scp002Impact;
import net.rai.scp_unleashed.scp002.Scp002Strikes;
import net.rai.scp_unleashed.scp2845.network.Scp2845ImpactPayload;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record Scp2845Arrival(int MinDelay, int MaxDelay, int MinDistance, int MaxDistance) implements StartCondition {
    public static final String TypeName = "scp2845_comet";
    public static final String ReadyKey = "Ready";
    public static final String LaunchedKey = "Launched";
    public static final String SiteKey = "Site";
    public static final String DimensionKey = "Dimension";
    public static final String CometKey = "Comet";
    public static final String FromKey = "From";
    public static final int RetryTicks = 100;
    public static final int WatchdogTicks = 300;
    public static final int TicketRadius = 2;
    public static final double BlastRadius = 8.0;
    public static final float BlastDamage = 12.0f;
    public static final double AnnounceRange = 256.0;
    public static final int FoundReach = 4;
    public static final TicketType<ChunkPos> Ticket = TicketType.create("scp_unleashed_scp2845", Comparator.comparingLong(ChunkPos::toLong), 100);
    public static final MapCodec<Scp2845Arrival> ConditionCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            Codec.intRange(0, 86400).optionalFieldOf("min_delay", 120).forGetter(Scp2845Arrival::MinDelay),
            Codec.intRange(0, 86400).optionalFieldOf("max_delay", 480).forGetter(Scp2845Arrival::MaxDelay),
            Codec.intRange(12, 256).optionalFieldOf("min_distance", 30).forGetter(Scp2845Arrival::MinDistance),
            Codec.intRange(12, 256).optionalFieldOf("max_distance", 60).forGetter(Scp2845Arrival::MaxDistance)
    ).apply(Instance, Scp2845Arrival::new));

    @Override
    public String getTypeName() {
        return TypeName;
    }

    private long rollDelay(ServerPlayer Reader) {
        int Low = Math.min(MinDelay, MaxDelay);
        int High = Math.max(MinDelay, MaxDelay);
        return (Low + (long) Reader.getRandom().nextInt(High - Low + 1)) * 20L;
    }

    @Override
    public List<Component> begin(ServerPlayer Reader, Investigation Case) {
        Case.getState().putLong(ReadyKey, Reader.serverLevel().getGameTime() + rollDelay(Reader));
        Case.markDirty();
        return List.of(Component.translatable("archive.scp_unleashed.notes.scp2845.0"), Component.translatable("archive.scp_unleashed.notes.scp2845.1"));
    }

    public static boolean validReader(ServerPlayer Reader) {
        return Scp002Strikes.isValidTarget(Reader) && ReaderTargets.isReady(Reader) && AnomalyRealm.allows(Reader.level());
    }

    @Override
    public Optional<Finding> tick(ServerPlayer Reader, Investigation Case) {
        CompoundTag State = Case.getState();
        Optional<BlockPos> Landed = Scp2845Records.get(Reader.server).landing(Case.getId());
        if (Landed.isPresent()) {
            return landedHere(Reader, State) ? Optional.of(new Finding(Landed.get(), true)) : Optional.empty();
        }
        long Now = Reader.server.overworld().getGameTime();
        if (!State.contains(ReadyKey)) {
            State.putLong(ReadyKey, Now + rollDelay(Reader));
            Case.markDirty();
            return Optional.empty();
        }
        if (!State.contains(LaunchedKey)) {
            if (Now < State.getLong(ReadyKey) || !validReader(Reader)) {
                return Optional.empty();
            }
            ServerLevel World = Reader.serverLevel();
            Optional<BlockPos> Site = Scp002Impact.chooseSite(World, Reader.position(), MinDistance, MaxDistance, World.random);
            if (Site.isEmpty()) {
                State.putLong(ReadyKey, Now + RetryTicks);
                Case.markDirty();
                return Optional.empty();
            }
            Scp2845Comet Comet = Scp2845Comet.launch(World, Site.get(), Reader.position(), Case.getId());
            if (Comet == null) {
                State.putLong(ReadyKey, Now + RetryTicks);
                Case.markDirty();
                return Optional.empty();
            }
            keepLoaded(World, Site.get());
            State.putLong(LaunchedKey, Now);
            State.putLong(SiteKey, Site.get().asLong());
            State.putString(DimensionKey, World.dimension().location().toString());
            State.putUUID(CometKey, Comet.getUUID());
            State.putDouble(FromKey + "X", Reader.getX());
            State.putDouble(FromKey + "Z", Reader.getZ());
            Case.markDirty();
            Notices.sendFalling(World, Scp2845Registry.DocumentId, Site.get(), Component.translatable("scp2845.scp_unleashed.lead.falling"));
            return Optional.empty();
        }
        ServerLevel World = levelOf(Reader, State.getString(DimensionKey));
        if (World == null) {
            return Optional.empty();
        }
        if (!Scp002Strikes.fallsIn(World)) {
            State.remove(LaunchedKey);
            State.remove(SiteKey);
            State.remove(DimensionKey);
            State.remove(CometKey);
            State.putLong(ReadyKey, Now + RetryTicks);
            Case.markDirty();
            return Optional.empty();
        }
        BlockPos Site = BlockPos.of(State.getLong(SiteKey));
        keepLoaded(World, Site);
        if (Now - State.getLong(LaunchedKey) < Scp2845Comet.FlightTicks + WatchdogTicks) {
            return Optional.empty();
        }
        UUID Comet = State.hasUUID(CometKey) ? State.getUUID(CometKey) : null;
        if (Comet != null && World.getEntity(Comet) instanceof Scp2845Comet Live && Live.isAlive() && Live.getPhase() == Scp2845Comet.PhaseFlight) {
            return Optional.empty();
        }
        Vec3 From = new Vec3(State.getDouble(FromKey + "X"), Site.getY(), State.getDouble(FromKey + "Z"));
        BlockPos Stone = impact(World, Site, From, Case.getId());
        return landedHere(Reader, State) ? Optional.of(new Finding(Stone, true)) : Optional.empty();
    }

    private static boolean landedHere(ServerPlayer Reader, CompoundTag State) {
        String Dimension = State.contains(DimensionKey) ? State.getString(DimensionKey) : Level.OVERWORLD.location().toString();
        return Reader.level().dimension().location().toString().equals(Dimension);
    }

    @Nullable
    private static ServerLevel levelOf(ServerPlayer Reader, String Dimension) {
        ResourceLocation Id = ResourceLocation.tryParse(Dimension);
        return Id == null ? null : Reader.server.getLevel(ResourceKey.create(Registries.DIMENSION, Id));
    }

    public static void keepLoaded(ServerLevel World, BlockPos Ground) {
        ChunkPos Centre = new ChunkPos(Ground);
        World.getChunkSource().addRegionTicket(Ticket, Centre, TicketRadius, Centre);
    }

    public static BlockPos impact(ServerLevel World, BlockPos Ground, Vec3 From, @Nullable UUID CaseId) {
        Vec3 Centre = Vec3.atBottomCenterOf(Ground.above());
        blast(World, Centre);
        Scp2845Crater.Result Crater = Scp2845Crater.strike(World, Ground, World.random);
        spawnDeer(World, Crater.Stone(), From);
        Scp2845Sounds.play(World, Centre, Scp2845Registry.ImpactLayers, 4.0f, 0.9f);
        World.sendParticles(ParticleTypes.EXPLOSION_EMITTER, Centre.x, Centre.y + 1.0, Centre.z, 2, 1.0, 0.5, 1.0, 0.0);
        World.sendParticles(ParticleTypes.END_ROD, Centre.x, Centre.y + 1.0, Centre.z, 120, 2.0, 1.5, 2.0, 0.25);
        World.sendParticles(ParticleTypes.CHERRY_LEAVES, Centre.x, Centre.y + 2.0, Centre.z, 80, 4.0, 2.0, 4.0, 0.05);
        World.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, Centre.x, Centre.y + 0.5, Centre.z, 40, 3.0, 0.5, 3.0, 0.03);
        Scp2845ImpactPayload Payload = new Scp2845ImpactPayload(BlockPos.containing(Centre), Crater.Radius());
        for (ServerPlayer Viewer : World.players()) {
            if (Viewer.distanceToSqr(Centre) < AnnounceRange * AnnounceRange) {
                PacketDistributor.sendToPlayer(Viewer, Payload);
            }
        }
        if (CaseId != null) {
            Scp2845Records.get(World.getServer()).landed(CaseId, Crater.Stone());
        }
        return Crater.Stone();
    }

    private static void blast(ServerLevel World, Vec3 Centre) {
        AABB Reach = new AABB(Centre, Centre).inflate(BlastRadius);
        for (LivingEntity Caught : World.getEntitiesOfClass(LivingEntity.class, Reach, LivingEntity::isAlive)) {
            if (Caught instanceof Scp2845) {
                continue;
            }
            Vec3 Away = Caught.position().subtract(Centre);
            double Distance = Away.length();
            if (Distance > BlastRadius) {
                continue;
            }
            float Strength = (float) (1.0 - Distance / BlastRadius);
            Caught.hurt(World.damageSources().explosion(null, null), BlastDamage * Strength * Strength + 1.0f);
            Vec3 Push = Distance < 0.01 ? new Vec3(0.0, 1.0, 0.0) : Away.normalize();
            Caught.push(Push.x * Strength * 1.2, 0.3 + Strength * 0.5, Push.z * Strength * 1.2);
            Caught.hurtMarked = true;
        }
    }

    @Nullable
    public static Scp2845 spawnDeer(ServerLevel World, BlockPos Stone, Vec3 From) {
        Scp2845 Deer = Scp2845Registry.DeerType.get().create(World);
        if (Deer == null) {
            return null;
        }
        Vec3 Centre = Vec3.atBottomCenterOf(Stone);
        Vec3 Toward = From.subtract(Centre).multiply(1.0, 0.0, 1.0);
        Toward = Toward.lengthSqr() < 1.0e-3 ? new Vec3(0.0, 0.0, 1.0) : Toward.normalize();
        Vec3 Spot = Centre.add(Toward.scale(Scp2845.StandOff + 0.4));
        for (int Lift = 0; Lift <= 3; Lift++) {
            Vec3 Try = Spot.add(0.0, Lift, 0.0);
            if (World.noCollision(Deer, Deer.getType().getDimensions().makeBoundingBox(Try))) {
                Spot = Try;
                break;
            }
        }
        float Yaw = (float) (Mth.atan2(From.z - Spot.z, From.x - Spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        Deer.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        Deer.setYHeadRot(Yaw);
        Deer.setYBodyRot(Yaw);
        Deer.finalizeSpawn(World, World.getCurrentDifficultyAt(Stone), MobSpawnType.EVENT, null);
        if (!World.addFreshEntity(Deer)) {
            return null;
        }
        Deer.arrive(World, Stone);
        return Deer;
    }

    public static boolean present(ServerLevel World, BlockPos Pos) {
        for (BlockPos Each : BlockPos.betweenClosed(Pos.offset(-FoundReach, -FoundReach, -FoundReach), Pos.offset(FoundReach, FoundReach, FoundReach))) {
            if (Scp2845Circle.isStone(World, Each)) {
                return true;
            }
        }
        return !World.getEntitiesOfClass(Scp2845.class, new AABB(Pos).inflate(24.0)).isEmpty();
    }

    public static boolean manifest(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Scp2845Records Records = Scp2845Records.get(World.getServer());
        if (present(World, Found.Pos())) {
            return true;
        }
        if (!Scp002Strikes.fallsIn(World)) {
            return false;
        }
        BlockPos Ground = new BlockPos(Found.Pos().getX(), Scp2845Crater.groundY(World, Found.Pos().getX(), Found.Pos().getZ()), Found.Pos().getZ());
        Scp2845Comet.launch(World, Ground, Found.Investigator().position(), null);
        keepLoaded(World, Ground);
        Records.setDirty();
        return true;
    }
}
