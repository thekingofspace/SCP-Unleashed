package net.rai.scp_unleashed.scp106;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.UUID;

public final class Scp106Pocket {
    public static final ResourceKey<Level> Key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp106_pocket"));
    public static final int CaptiveMinTicks = 2400;
    public static final int CaptiveMaxTicks = 9600;
    public static final float CaptiveEscapeChance = 0.3f;
    public static final int CaptiveCheckEvery = 100;
    public static final int CaptiveMissLimit = 6;
    public static final int ArrivalBlindness = 50;
    public static final int SafeRadius = 6;
    public static final int SafeVertical = 4;
    private static final Set<UUID> Queued = ConcurrentHashMap.newKeySet();

    private Scp106Pocket() {
    }

    @Nullable
    public static ServerLevel level(MinecraftServer Server) {
        return Server.getLevel(Key);
    }

    public static boolean inPocket(@Nullable Level World) {
        return World != null && World.dimension() == Key;
    }

    public static boolean inPocket(Entity Target) {
        return inPocket(Target.level());
    }

    public static void ensureBuilt(ServerLevel Pocket) {
        Scp106PocketWorld Data = Scp106PocketWorld.get(Pocket.getServer());
        if (Data.builtSignature() != Scp106PocketLayout.Signature) {
            Scp106PocketLayout.build(Pocket);
            Data.markBuilt(Scp106PocketLayout.Signature);
        }
    }

    public static boolean take(LivingEntity Victim) {
        if (Victim instanceof ServerPlayer User) {
            return takePlayer(User);
        }
        return takeCaptive(Victim);
    }

    public static void queueTake(LivingEntity Victim) {
        Queued.add(Victim.getUUID());
    }

    public static void flushTakes(MinecraftServer Server) {
        if (Queued.isEmpty()) {
            return;
        }
        for (UUID Id : new ArrayList<>(Queued)) {
            Queued.remove(Id);
            for (ServerLevel World : Server.getAllLevels()) {
                if (World.getEntity(Id) instanceof LivingEntity Victim) {
                    boolean Gone = !Victim.isAlive() || Victim instanceof ServerPlayer User && ControlManager.hasPendingDeath(User);
                    if (!Gone) {
                        take(Victim);
                    }
                    break;
                }
            }
        }
    }

    public static void clearQueue() {
        Queued.clear();
    }

    public static boolean takePlayer(ServerPlayer User) {
        ServerLevel Pocket = level(User.server);
        if (Pocket == null || !User.isAlive()) {
            return false;
        }
        ensureBuilt(Pocket);
        Scp106PocketWorld Data = Scp106PocketWorld.get(User.server);
        RandomSource Random = User.getRandom();
        if (inPocket(User)) {
            int Room = wanderRoom(Random, Scp106PocketLayout.roomAt(User.getX()));
            sendToRoom(User, Room);
            return true;
        }
        Scp106PocketWorld.Spot Return = new Scp106PocketWorld.Spot(User.level().dimension(), User.position(), User.getYRot());
        Data.startVisit(User.getUUID(), new Scp106PocketWorld.Visit(Return, 0, Scp106PocketLayout.Arrival));
        move(User, Pocket, Scp106PocketLayout.arrival(Scp106PocketLayout.Arrival), Scp106PocketLayout.arrivalYaw(Scp106PocketLayout.Arrival));
        User.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ArrivalBlindness, 0, false, false));
        Scp106Effects.pool(Pocket, User.position());
        Pocket.playSound(null, User.getX(), User.getY(), User.getZ(), Scp106Registry.Rise.get(), SoundSource.HOSTILE, 0.8f, 0.8f);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.taken").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
        return true;
    }

    public static int wanderRoom(RandomSource Random, int Except) {
        List<Integer> Choices = new ArrayList<>(Scp106PocketLayout.Wandering);
        Choices.remove(Integer.valueOf(Except));
        return Choices.get(Random.nextInt(Choices.size()));
    }

    public static void sendToRoom(ServerPlayer User, int Room) {
        ServerLevel Pocket = level(User.server);
        if (Pocket == null) {
            return;
        }
        ensureBuilt(Pocket);
        Vec3 Spot = Scp106PocketLayout.arrival(Room);
        RandomSource Random = User.getRandom();
        if (Room != Scp106PocketLayout.Trench) {
            Spot = Spot.add((Random.nextDouble() - 0.5) * 0.6, 0.0, (Random.nextDouble() - 0.5) * 0.6);
        }
        move(User, Pocket, Spot, Scp106PocketLayout.arrivalYaw(Room));
        Scp106PocketWorld Data = Scp106PocketWorld.get(User.server);
        Scp106PocketWorld.Visit Visit = Data.visitOf(User.getUUID());
        if (Visit != null) {
            Visit.Room = Room;
            Data.touchVisit();
        }
    }

    public static void move(ServerPlayer User, ServerLevel World, Vec3 Spot, float Yaw) {
        User.stopRiding();
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        World.getChunkAt(BlockPos.containing(Spot));
        User.teleportTo(World, Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        User.setDeltaMovement(Vec3.ZERO);
        User.resetFallDistance();
    }

    public static Scp106PocketWorld.Spot outside(ServerPlayer User) {
        Scp106PocketWorld.Visit Visit = Scp106PocketWorld.get(User.server).visitOf(User.getUUID());
        if (Visit != null && Visit.Return.Dimension() != Key && User.server.getLevel(Visit.Return.Dimension()) != null) {
            return Visit.Return;
        }
        DimensionTransition Home = User.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        return new Scp106PocketWorld.Spot(Home.newLevel().dimension(), Home.pos(), Home.yRot());
    }

    public static void leave(ServerPlayer User, boolean Escaped) {
        Scp106PocketWorld.Spot Spot = outside(User);
        Scp106PocketWorld.get(User.server).endVisit(User.getUUID());
        ServerLevel Target = User.server.getLevel(Spot.Dimension());
        if (Target == null) {
            Target = User.server.overworld();
        }
        if (!User.isAlive()) {
            return;
        }
        Vec3 Safe = safeSpot(Target, Spot.Pos(), EntityType.PLAYER);
        move(User, Target, Safe, Spot.Yaw());
        Scp106Effects.pool(Target, Safe);
        Target.playSound(null, Safe.x, Safe.y, Safe.z, Scp106Registry.Escape.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        if (Escaped) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.scp106.escaped").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    public static Vec3 safeSpot(ServerLevel World, Vec3 Near, EntityType<?> Type) {
        BlockPos Centre = BlockPos.containing(Near);
        World.getChunkAt(Centre);
        for (int Radius = 0; Radius <= SafeRadius; Radius++) {
            for (int Dy = 0; Dy <= SafeVertical * 2; Dy++) {
                int Offset = Dy % 2 == 0 ? Dy / 2 : -(Dy + 1) / 2;
                for (int Dx = -Radius; Dx <= Radius; Dx++) {
                    for (int Dz = -Radius; Dz <= Radius; Dz++) {
                        if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                            continue;
                        }
                        BlockPos Feet = Centre.offset(Dx, Offset, Dz);
                        if (standable(World, Feet, Type)) {
                            return Vec3.atBottomCenterOf(Feet);
                        }
                    }
                }
            }
        }
        BlockPos Top = World.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Centre);
        return Vec3.atBottomCenterOf(Top);
    }

    public static boolean standable(ServerLevel World, BlockPos Feet, EntityType<?> Type) {
        if (World.isOutsideBuildHeight(Feet) || !World.getWorldBorder().isWithinBounds(Feet)) {
            return false;
        }
        BlockPos Below = Feet.below();
        BlockState Floor = World.getBlockState(Below);
        if (!Floor.isFaceSturdy(World, Below, Direction.UP)) {
            return false;
        }
        if (!World.getFluidState(Feet).isEmpty() || !World.getFluidState(Feet.above()).isEmpty() || World.getFluidState(Below).is(FluidTags.LAVA)) {
            return false;
        }
        AABB Box = Type.getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(Feet));
        return World.noCollision(Box);
    }

    public static boolean takeCaptive(LivingEntity Victim) {
        MinecraftServer Server = Victim.getServer();
        if (Server == null || !Victim.isAlive()) {
            return false;
        }
        ServerLevel Pocket = level(Server);
        if (Pocket == null) {
            return false;
        }
        ensureBuilt(Pocket);
        RandomSource Random = Victim.getRandom();
        Scp106PocketWorld.Spot Return = new Scp106PocketWorld.Spot(Victim.level().dimension(), Victim.position(), Victim.getYRot());
        if (inPocket(Victim)) {
            Return = returnOf(Victim);
        }
        Victim.stopRiding();
        Victim.ejectPassengers();
        if (Victim instanceof Mob Held) {
            Held.setPersistenceRequired();
            Held.dropLeash(true, true);
        }
        forceWarren(Pocket, true);
        Vec3 Spot = Scp106PocketLayout.warrenSpot(Random);
        Entity Moved = Victim.changeDimension(new DimensionTransition(Pocket, Spot, Vec3.ZERO, Random.nextFloat() * 360.0f, 0.0f, DimensionTransition.DO_NOTHING));
        if (Moved == null) {
            return false;
        }
        long Due = Pocket.getGameTime() + CaptiveMinTicks + Random.nextInt(CaptiveMaxTicks - CaptiveMinTicks + 1);
        Scp106PocketWorld.get(Server).addCaptive(new Scp106PocketWorld.Captive(Moved.getUUID(), Return, Due, Random.nextFloat() < CaptiveEscapeChance));
        return true;
    }

    private static Scp106PocketWorld.Spot returnOf(LivingEntity Victim) {
        Scp106PocketWorld.Captive Known = null;
        for (Scp106PocketWorld.Captive Entry : Scp106PocketWorld.get(Victim.getServer()).captives()) {
            if (Entry.Id.equals(Victim.getUUID())) {
                Known = Entry;
            }
        }
        if (Known != null) {
            return Known.Return;
        }
        ServerLevel Home = Victim.getServer().overworld();
        return new Scp106PocketWorld.Spot(Home.dimension(), Vec3.atBottomCenterOf(Home.getSharedSpawnPos()), 0.0f);
    }

    public static void forceWarren(ServerLevel Pocket, boolean Forced) {
        Pocket.setChunkForced(Scp106PocketLayout.warrenChunkX(), Scp106PocketLayout.warrenChunkZ(), Forced);
    }

    public static void tickCaptives(MinecraftServer Server) {
        if (Server.getTickCount() % CaptiveCheckEvery != 0) {
            return;
        }
        ServerLevel Pocket = level(Server);
        if (Pocket == null) {
            return;
        }
        Scp106PocketWorld Data = Scp106PocketWorld.get(Server);
        if (Data.captives().isEmpty()) {
            return;
        }
        forceWarren(Pocket, true);
        long Now = Pocket.getGameTime();
        for (Scp106PocketWorld.Captive Entry : new ArrayList<>(Data.captives())) {
            if (Now < Entry.Due) {
                continue;
            }
            Entity Found = Pocket.getEntity(Entry.Id);
            if (!(Found instanceof LivingEntity Living) || !Living.isAlive()) {
                Entry.Misses++;
                Data.touchVisit();
                if (Entry.Misses >= CaptiveMissLimit || Found != null) {
                    Data.removeCaptive(Entry.Id);
                }
                continue;
            }
            Data.removeCaptive(Entry.Id);
            if (Entry.Escapes) {
                release(Server, Living, Entry.Return);
            } else {
                Living.hurt(Pocket.damageSources().source(Scp106Registry.Corrosion), Float.MAX_VALUE);
            }
        }
        if (Data.captives().isEmpty()) {
            forceWarren(Pocket, false);
        }
    }

    public static void release(MinecraftServer Server, LivingEntity Living, Scp106PocketWorld.Spot Return) {
        ServerLevel Target = Server.getLevel(Return.Dimension());
        if (Target == null || Return.Dimension() == Key) {
            Target = Server.overworld();
        }
        Vec3 Safe = safeSpot(Target, Return.Pos(), Living.getType());
        Entity Moved = Living.changeDimension(new DimensionTransition(Target, Safe, Vec3.ZERO, Return.Yaw(), 0.0f, DimensionTransition.DO_NOTHING));
        if (Moved != null) {
            Scp106Effects.pool(Target, Safe);
            Target.playSound(null, Safe.x, Safe.y, Safe.z, Scp106Registry.Escape.get(), SoundSource.NEUTRAL, 0.9f, 1.05f);
        }
    }

    public static void releaseAllCaptives(MinecraftServer Server) {
        ServerLevel Pocket = level(Server);
        Scp106PocketWorld Data = Scp106PocketWorld.get(Server);
        for (Scp106PocketWorld.Captive Entry : new ArrayList<>(Data.captives())) {
            Entity Found = Pocket == null ? null : Pocket.getEntity(Entry.Id);
            Data.removeCaptive(Entry.Id);
            if (Found instanceof LivingEntity Living && Living.isAlive()) {
                release(Server, Living, Entry.Return);
            }
        }
        if (Pocket != null) {
            forceWarren(Pocket, false);
        }
    }

    public static boolean isCaptive(MinecraftServer Server, UUID Id) {
        return Scp106PocketWorld.get(Server).isCaptive(Id);
    }
}
