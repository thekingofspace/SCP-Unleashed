package net.rai.scp_unleashed.scp335;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.computer.ComputerBlockEntity;
import net.rai.scp_unleashed.computer.ComputerDeviceBlock;
import net.rai.scp_unleashed.computer.net.ComputerSessions;
import net.rai.scp_unleashed.computer.network.ComputerNetworking;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.scp335.network.Scp335StatePayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp335Episodes {
    public static final int GlimpseTicks = 30;
    public static final int FirstLine = 24;
    public static final int LineGap = 38;
    public static final int CrawlTail = 30;
    public static final int RiseTicks = 24;
    public static final int WalkLimit = 220;
    public static final int StallTicks = 30;
    public static final double StallGain = 0.2;
    public static final double ArriveRange = 1.05;
    public static final double AssistStep = 0.2;
    public static final double LostRange = 40.0;
    public static final double DriftLimit = 3.5;
    public static final double LitRange = 4.0;
    public static final double Shove = 0.35;
    public static final int DissolveTicks = 50;
    public static final int HideAfter = 10;
    public static final int RushGap = 40;
    public static final int MinLines = 4;
    public static final int MaxLines = 6;
    public static final int LineCount = 12;
    public static final List<Integer> Early = List.of(0, 1, 2);
    public static final List<Integer> Middle = List.of(3, 4, 5, 6, 7);
    public static final List<Integer> Late = List.of(8, 9, 10, 11);
    public static final String LineKey = "chat.scp_unleashed.scp335.line.";
    public static final double ScreenX = 11.0 / 16.0;
    public static final double ScreenY = 7.0 / 16.0;
    public static final double ScreenZ = 4.5 / 16.0;
    public static final Set<UUID> ClientLocked = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> Synced = new HashSet<>();
    private static final Set<UUID> Fallen = new HashSet<>();
    private static int Absorptions;

    private Scp335Episodes() {
    }

    public static boolean isLocked(Player User) {
        if (User.level().isClientSide) {
            return ClientLocked.contains(User.getUUID());
        }
        return User.getExistingDataOrNull(Scp335Registry.Episode.get()) != null;
    }

    public static Scp335Episode of(Player User) {
        return User.getExistingDataOrNull(Scp335Registry.Episode.get());
    }

    public static int absorptions() {
        return Absorptions;
    }

    public static boolean canBegin(ServerPlayer User) {
        return !User.isCreative() && !User.isSpectator() && User.isAlive() && !User.isDeadOrDying() && of(User) == null;
    }

    public static void begin(ServerPlayer User, BlockPos Computer) {
        ServerLevel World = User.serverLevel();
        BlockState State = World.getBlockState(Computer);
        Direction Front = State.hasProperty(ComputerDeviceBlock.Facing) ? State.getValue(ComputerDeviceBlock.Facing) : User.getDirection().getOpposite();
        Vec3 Screen = screenOf(Computer, Front);
        Vec3 Approach = new Vec3(Computer.getX() + 0.5 + Front.getStepX(), User.getY(), Computer.getZ() + 0.5 + Front.getStepZ());
        Scp335Episode Episode = new Scp335Episode(Scp335Phase.Glimpse, 0, World.dimension().location(), Computer, Approach, Screen, User.position(),
                pickLines(User.getRandom()), 0, User.getInventory().selected, Double.MAX_VALUE, 0, false, true);
        User.setData(Scp335Registry.Episode.get(), Episode);
        sync(User, Episode);
    }

    public static Vec3 screenOf(BlockPos Pos, Direction Front) {
        double[] Turned = switch (Front) {
            case EAST -> new double[]{1.0 - ScreenZ, ScreenX};
            case SOUTH -> new double[]{1.0 - ScreenX, 1.0 - ScreenZ};
            case WEST -> new double[]{ScreenZ, 1.0 - ScreenX};
            default -> new double[]{ScreenX, ScreenZ};
        };
        return new Vec3(Pos.getX() + Turned[0], Pos.getY() + ScreenY, Pos.getZ() + Turned[1]);
    }

    public static List<Integer> pickLines(RandomSource Random) {
        int Total = MinLines + Random.nextInt(MaxLines - MinLines + 1);
        int EarlyCount = 1 + Random.nextInt(2);
        int LateCount = 1 + Random.nextInt(2);
        int MiddleCount = Math.max(1, Total - EarlyCount - LateCount);
        List<Integer> Chosen = new ArrayList<>();
        Chosen.addAll(pick(Early, EarlyCount, Random));
        Chosen.addAll(pick(Middle, MiddleCount, Random));
        Chosen.addAll(pick(Late, LateCount, Random));
        return Chosen;
    }

    private static List<Integer> pick(List<Integer> Pool, int Count, RandomSource Random) {
        List<Integer> Shuffled = new ArrayList<>(Pool);
        for (int Index = Shuffled.size() - 1; Index > 0; Index--) {
            Collections.swap(Shuffled, Index, Random.nextInt(Index + 1));
        }
        List<Integer> Taken = new ArrayList<>(Shuffled.subList(0, Math.min(Count, Shuffled.size())));
        Collections.sort(Taken);
        return Taken;
    }

    public static int crawlLength(int Lines) {
        return FirstLine + Math.max(0, Lines - 1) * LineGap + CrawlTail;
    }

    public static void tickAll(MinecraftServer Server) {
        for (ServerLevel World : Server.getAllLevels()) {
            for (ServerPlayer User : List.copyOf(World.players())) {
                tick(User);
            }
        }
    }

    public static void tick(ServerPlayer User) {
        Scp335Episode Episode = of(User);
        if (Episode == null) {
            if (Synced.contains(User.getUUID())) {
                release(User);
            }
            return;
        }
        if (User.isRemoved() || User.isDeadOrDying()) {
            return;
        }
        if (User.isCreative() || User.isSpectator()) {
            abort(User);
            return;
        }
        if (!Synced.contains(User.getUUID())) {
            resume(User, Episode);
            Episode = of(User);
            if (Episode == null) {
                return;
            }
        }
        Episode.Tick++;
        hold(User, Episode);
        switch (Episode.Phase) {
            case Glimpse -> {
                if (Episode.Tick >= GlimpseTicks) {
                    startCrawl(User, Episode);
                }
            }
            case Crawl -> crawl(User, Episode);
            case Rise -> {
                if (Episode.Tick >= RiseTicks) {
                    startWalk(User, Episode);
                }
            }
            case Walk -> walk(User, Episode);
            case Dissolve -> {
                if (Episode.Tick == HideAfter) {
                    User.setInvisible(true);
                }
                if (Episode.Tick == DissolveTicks / 2) {
                    playAt(User.serverLevel(), User.position(), List.of(Scp335Registry.DissolveGlitch), 0.8f, 1.35f);
                }
                if (Episode.Tick >= DissolveTicks) {
                    absorb(User, Episode);
                }
            }
            default -> {
            }
        }
    }

    private static void hold(ServerPlayer User, Scp335Episode Episode) {
        if (User.containerMenu != User.inventoryMenu) {
            User.closeContainer();
        }
        int Slot = Mth.clamp(Episode.Slot, 0, 8);
        if (User.getInventory().selected != Slot) {
            User.getInventory().selected = Slot;
            User.connection.send(new ClientboundSetCarriedItemPacket(Slot));
        }
        if (User.isSprinting()) {
            User.setSprinting(false);
        }
        if (User.isUsingItem()) {
            User.stopUsingItem();
        }
        boolean Free = Episode.Phase == Scp335Phase.Walk || Episode.Phase == Scp335Phase.Glimpse;
        if (!Free && sameDimension(User, Episode) && flat(User.position(), Episode.Anchor) > DriftLimit) {
            User.teleportTo(User.serverLevel(), Episode.Anchor.x, Episode.Anchor.y, Episode.Anchor.z, User.getYRot(), User.getXRot());
        }
    }

    private static void startCrawl(ServerPlayer User, Scp335Episode Episode) {
        BlockPos Pos = Episode.Computer;
        if (ComputerSessions.of(User, Pos) != null) {
            ComputerSessions.close(User);
            ComputerNetworking.close(User, Pos, ComputerSessions.CloseReason.Closed);
        }
        User.closeContainer();
        User.stopRiding();
        User.setSprinting(false);
        if (User.isFallFlying()) {
            User.stopFallFlying();
        }
        Episode.enter(Scp335Phase.Crawl);
        Episode.Anchor = User.position();
        Vec3 Away = new Vec3(User.getX() - Episode.Screen.x, 0.0, User.getZ() - Episode.Screen.z);
        if (Away.lengthSqr() > 1.0E-4) {
            Away = Away.normalize().scale(Shove);
            User.setDeltaMovement(Away.x, 0.12, Away.z);
            User.hurtMarked = true;
        }
        User.setForcedPose(Pose.SWIMMING);
        playAt(User.serverLevel(), User.position(), Scp335Registry.RushLayers, 0.5f, 0.7f);
        sync(User, Episode);
    }

    private static void crawl(ServerPlayer User, Scp335Episode Episode) {
        int Count = Episode.Lines.size();
        if (Episode.Said < Count && Episode.Tick >= FirstLine + Episode.Said * LineGap) {
            say(User, Episode.Lines.get(Episode.Said));
            Episode.Said++;
        }
        int Length = crawlLength(Count);
        if (Episode.Tick > 0 && Episode.Tick % RushGap == 0) {
            float Share = Mth.clamp((float) Episode.Tick / Length, 0.0f, 1.0f);
            playAt(User.serverLevel(), User.position(), Scp335Registry.RushLayers, 0.5f + Share * 0.5f, 0.7f + Share * 0.6f);
        }
        if (Episode.Tick >= Length) {
            Episode.enter(Scp335Phase.Rise);
            Episode.Anchor = User.position();
            User.setForcedPose(null);
            sync(User, Episode);
        }
    }

    private static void startWalk(ServerPlayer User, Scp335Episode Episode) {
        Episode.enter(Scp335Phase.Walk);
        Episode.Best = flat(User.position(), Episode.Approach);
        Episode.Stalled = 0;
        Episode.Assisted = false;
        sync(User, Episode);
    }

    private static void walk(ServerPlayer User, Scp335Episode Episode) {
        double Distance = flat(User.position(), Episode.Approach);
        if (!sameDimension(User, Episode) || Distance > LostRange) {
            startDissolve(User, Episode);
            return;
        }
        if (Distance <= ArriveRange) {
            startDissolve(User, Episode);
            return;
        }
        if (Distance < Episode.Best - StallGain) {
            Episode.Best = Distance;
            Episode.Stalled = 0;
        } else {
            Episode.Stalled++;
        }
        if (!Episode.Assisted && (Episode.Stalled >= StallTicks || Episode.Tick >= WalkLimit)) {
            Episode.Assisted = true;
            sync(User, Episode);
        }
        if (Episode.Assisted) {
            assist(User, Episode, Distance);
        }
    }

    private static void assist(ServerPlayer User, Scp335Episode Episode, double Distance) {
        ServerLevel World = User.serverLevel();
        Vec3 Here = User.position();
        Vec3 Flat = new Vec3(Episode.Approach.x - Here.x, 0.0, Episode.Approach.z - Here.z);
        Vec3 Step = Flat.scale(Math.min(AssistStep, Distance) / Math.max(1.0E-4, Distance));
        float Yaw = yawTowards(Here, Episode.Screen);
        AABB Moved = User.getBoundingBox().move(Step);
        Vec3 Next = Here.add(Step);
        if (!World.noCollision(User, Moved)) {
            Moved = Moved.move(0.0, 1.0, 0.0);
            Next = Next.add(0.0, 1.0, 0.0);
            if (!World.noCollision(User, Moved)) {
                AABB Landing = User.getBoundingBox().move(Episode.Approach.subtract(Here));
                if (World.noCollision(User, Landing)) {
                    User.teleportTo(World, Episode.Approach.x, Episode.Approach.y, Episode.Approach.z, Yaw, 0.0f);
                }
                startDissolve(User, Episode);
                return;
            }
        }
        User.teleportTo(World, Next.x, Next.y, Next.z, Yaw, 0.0f);
    }

    private static void startDissolve(ServerPlayer User, Scp335Episode Episode) {
        ServerLevel World = User.serverLevel();
        Episode.enter(Scp335Phase.Dissolve);
        Episode.Anchor = User.position();
        Episode.Lit = sameDimension(User, Episode) && computerRunning(World, Episode.Computer) && User.getEyePosition().distanceTo(Episode.Screen) <= LitRange;
        User.setForcedPose(null);
        playAt(World, User.position(), Scp335Registry.DissolveLayers, 1.0f, 1.0f);
        sync(User, Episode);
    }

    public static boolean computerRunning(ServerLevel World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.getBlockEntity(Pos) instanceof ComputerBlockEntity Computer && Computer.isRunning();
    }

    private static void absorb(ServerPlayer User, Scp335Episode Episode) {
        ServerLevel World = User.serverLevel();
        User.removeData(Scp335Registry.Episode.get());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Scp335StatePayload(User.getId(), Scp335Phase.Gone, 0, Episode.Approach, Episode.Screen,
                Episode.Assisted, Episode.Lit));
        if (Episode.Lit) {
            Scp335Echo.echo(World, Episode.Computer, User.getRandom());
        }
        Absorptions++;
        DamageSource Source = World.damageSources().source(Scp335Registry.Absorbed);
        User.invulnerableTime = 0;
        User.hurt(Source, Float.MAX_VALUE);
        if (ControlManager.hasPendingDeath(User)) {
            LivingEntity Corpse = ControlManager.processDeath(User, Body -> Body.setInvisible(true));
            if (Corpse != null && Corpse.isDeadOrDying() && !Corpse.isRemoved()) {
                Corpse.remove(Entity.RemovalReason.KILLED);
            }
            release(User);
            return;
        }
        if (User.isDeadOrDying()) {
            Synced.remove(User.getUUID());
            Fallen.add(User.getUUID());
            return;
        }
        release(User);
    }

    public static void abort(ServerPlayer User) {
        User.removeData(Scp335Registry.Episode.get());
        release(User);
    }

    public static void release(ServerPlayer User) {
        Synced.remove(User.getUUID());
        User.setForcedPose(null);
        User.setInvisible(User.hasEffect(MobEffects.INVISIBILITY));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, Scp335StatePayload.cleared(User.getId()));
    }

    public static void resume(ServerPlayer User, Scp335Episode Episode) {
        Episode.Anchor = User.position();
        switch (Episode.Phase) {
            case Glimpse -> {
                startCrawl(User, Episode);
                return;
            }
            case Crawl -> User.setForcedPose(Pose.SWIMMING);
            case Dissolve -> {
                Episode.Tick = Math.min(Episode.Tick, DissolveTicks / 2);
                User.setInvisible(Episode.Tick >= HideAfter);
            }
            default -> User.setForcedPose(null);
        }
        sync(User, Episode);
    }

    public static void loggedOut(ServerPlayer User) {
        Synced.remove(User.getUUID());
        Fallen.remove(User.getUUID());
    }

    public static void respawned(ServerPlayer User) {
        if (Fallen.remove(User.getUUID()) && of(User) == null) {
            release(User);
        }
    }

    public static void died(ServerPlayer User) {
        if (of(User) != null) {
            User.removeData(Scp335Registry.Episode.get());
            Synced.remove(User.getUUID());
            Fallen.add(User.getUUID());
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, Scp335StatePayload.cleared(User.getId()));
        }
    }

    public static void resync(ServerPlayer User) {
        Scp335Episode Episode = of(User);
        if (Episode != null) {
            sync(User, Episode);
        }
    }

    public static void watched(ServerPlayer Watcher, ServerPlayer Target) {
        Scp335Episode Episode = of(Target);
        if (Episode != null) {
            PacketDistributor.sendToPlayer(Watcher, payloadOf(Target, Episode));
        }
    }

    private static void sync(ServerPlayer User, Scp335Episode Episode) {
        Synced.add(User.getUUID());
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, payloadOf(User, Episode));
    }

    private static Scp335StatePayload payloadOf(ServerPlayer User, Scp335Episode Episode) {
        return new Scp335StatePayload(User.getId(), Episode.Phase, Episode.Tick, Episode.Approach, Episode.Screen, Episode.Assisted, Episode.Lit);
    }

    private static void say(ServerPlayer User, int Line) {
        Component Spoken = Component.translatable("chat.type.text", speakerName(User), Component.translatable(LineKey + Line));
        User.server.getPlayerList().broadcastSystemMessage(Spoken, false);
        playAt(User.serverLevel(), User.position(), List.of(Scp335Registry.Chatter), 0.6f, 0.8f + User.getRandom().nextFloat() * 0.4f);
    }

    public static Component speakerName(ServerPlayer User) {
        if (ControlSessions.isControlling(User)) {
            String Name = ControlSessions.session(User).Identity().getString("ControlName");
            if (!Name.isBlank()) {
                return Component.literal(Name);
            }
        }
        return User.getDisplayName();
    }

    public static void playAt(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y + 0.5, At.z, Layer.get(), SoundSource.PLAYERS, Volume, Pitch);
        }
    }

    private static boolean sameDimension(ServerPlayer User, Scp335Episode Episode) {
        return User.level().dimension().location().equals(Episode.Dimension);
    }

    public static double flat(Vec3 From, Vec3 To) {
        double X = To.x - From.x;
        double Z = To.z - From.z;
        return Math.sqrt(X * X + Z * Z);
    }

    public static float yawTowards(Vec3 From, Vec3 To) {
        return (float) (Mth.atan2(To.z - From.z, To.x - From.x) * Mth.RAD_TO_DEG) - 90.0f;
    }
}
