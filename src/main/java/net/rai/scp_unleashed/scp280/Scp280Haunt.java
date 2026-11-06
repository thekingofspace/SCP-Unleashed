package net.rai.scp_unleashed.scp280;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.archive.ArchiveRules;
import net.rai.scp_unleashed.archive.document.DocumentClass;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.Notices;
import net.rai.scp_unleashed.scp280.network.Scp280EyesPayload;
import net.rai.scp_unleashed.scp280.network.Scp280MeterPayload;
import net.rai.scp_unleashed.discord.DiscordNotices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class Scp280Haunt {
    public static final int StepTicks = 20;
    public static final int StareTicks = 4;
    public static final float SightingGain = 5.0f;
    public static final float ExtraPairGain = 1.5f;
    public static final float StareGain = 3.0f;
    public static final float DarkGain = 0.05f;
    public static final int DarkGrace = 60;
    public static final float Decay = 0.04f;
    public static final float RevealDrop = 30.0f;
    public static final int FirstDark = 6;
    public static final int SlowMin = 70;
    public static final int SlowMax = 130;
    public static final int FastMin = 15;
    public static final int FastMax = 32;
    public static final int RetrySeconds = 5;
    public static final double NearSlow = 16.0;
    public static final double FarSlow = 24.0;
    public static final double NearFast = 8.0;
    public static final double FarFast = 14.0;
    public static final int LifeMin = 60;
    public static final int LifeMax = 110;
    public static final int StaggerMax = 14;
    public static final int Variants = 4;
    public static final double StareCone = 0.985;
    public static final double StareWidth = 0.7;
    public static final double VanishRange = 6.0;
    public static final float SyncStep = 0.25f;
    public static final int SyncRefresh = 100;
    public static final float SoundVolume = 0.55f;

    private static final Map<UUID, Haunt> Haunts = new HashMap<>();

    private Scp280Haunt() {
    }

    public static final class Haunt {
        private int DarkSeconds;
        private int Countdown = -1;
        private final List<Vec3> Spots = new ArrayList<>();
        private long SightingEnd;
        private boolean Stared;
        private float Synced = -1.0f;
        private long SyncedAt;

        public int darkSeconds() {
            return DarkSeconds;
        }

        public int countdown() {
            return Countdown;
        }

        public void setCountdown(int Seconds) {
            Countdown = Seconds;
        }

        public List<Vec3> spots() {
            return List.copyOf(Spots);
        }

        public boolean stared() {
            return Stared;
        }
    }

    public static Haunt haunt(UUID Player) {
        return Haunts.computeIfAbsent(Player, Key -> new Haunt());
    }

    public static void forget(UUID Player) {
        Haunts.remove(Player);
    }

    public static void reset() {
        Haunts.clear();
    }

    public static boolean realm(Level World) {
        return World.dimension().equals(Level.OVERWORLD) && AnomalyRealm.allows(World);
    }

    public static boolean covered(ServerPlayer Player) {
        Scp280World Data = Scp280World.get(Player.server);
        return Data.covers(Player.getUUID()) && !Data.isCaptured();
    }

    public static boolean eligible(ServerPlayer Player) {
        return Player.isAlive() && !Player.isSpectator() && Targeting.isTargetable(Player) && realm(Player.level()) && covered(Player);
    }

    public static void unlock(ServerPlayer Reader) {
        Scp280World.get(Reader.server).unlock(Reader.getUUID(), ArchiveRules.mode(Reader.server) == ArchiveRules.Mode.World);
    }

    public static float fraction(float Meter) {
        return Mth.clamp(Meter / Scp280World.MaxMeter, 0.0f, 1.0f);
    }

    public static int nextDelay(float Meter, RandomSource Random) {
        float Share = fraction(Meter);
        int Low = Math.round(Mth.lerp(Share, SlowMin, FastMin));
        int High = Math.round(Mth.lerp(Share, SlowMax, FastMax));
        return Low + Random.nextInt(Math.max(1, High - Low + 1));
    }

    public static int pairsFor(float Meter, RandomSource Random) {
        float Share = fraction(Meter);
        if (Share < 0.35f) {
            return 1;
        }
        if (Share < 0.7f) {
            return 1 + Random.nextInt(2);
        }
        return 2 + Random.nextInt(3);
    }

    public static void tick(ServerPlayer Player) {
        Haunt State = Haunts.get(Player.getUUID());
        if (State != null && !State.Spots.isEmpty() && Player.tickCount % StareTicks == 0) {
            watch(Player, State);
        }
        if (Player.tickCount % StepTicks == 0) {
            step(Player);
        }
    }

    public static void step(ServerPlayer Player) {
        Scp280World Data = Scp280World.get(Player.server);
        Haunt State = haunt(Player.getUUID());
        if (!eligible(Player)) {
            State.DarkSeconds = 0;
            State.Countdown = -1;
            sync(Player, State, Data.isCaptured() || !Data.covers(Player.getUUID()) ? 0.0f : Data.meter(Player.getUUID()), false);
            return;
        }
        ServerLevel World = Player.serverLevel();
        float Meter = Data.meter(Player.getUUID());
        int Light = Scp280Light.of(Player);
        boolean Dark = Light <= Scp280Light.DarkLevel;
        State.DarkSeconds = Dark ? State.DarkSeconds + 1 : 0;
        if (Dark && State.DarkSeconds > DarkGrace) {
            Meter += DarkGain;
        }
        if (Light >= Scp280Light.WellLitLevel) {
            Meter -= Decay;
        }
        if (World.getGameTime() >= State.SightingEnd) {
            State.Spots.clear();
        }
        if (Dark && State.DarkSeconds >= FirstDark) {
            if (State.Countdown < 0) {
                State.Countdown = nextDelay(Meter, Player.getRandom());
            }
            State.Countdown--;
            if (State.Countdown <= 0 && State.Spots.isEmpty()) {
                int Pairs = sight(Player, Meter);
                if (Pairs > 0) {
                    Meter += SightingGain + ExtraPairGain * (Pairs - 1);
                    State.Countdown = nextDelay(Meter, Player.getRandom());
                } else {
                    State.Countdown = RetrySeconds;
                }
            }
        }
        Data.setMeter(Player.getUUID(), Meter);
        if (Data.meter(Player.getUUID()) >= Scp280World.MaxMeter) {
            reveal(Player);
        }
        sync(Player, State, Data.meter(Player.getUUID()), false);
    }

    public static int sight(ServerPlayer Player, float Meter) {
        ServerLevel World = Player.serverLevel();
        if (!realm(World)) {
            return 0;
        }
        RandomSource Random = Player.getRandom();
        float Share = fraction(Meter);
        double Near = Mth.lerp(Share, NearSlow, NearFast);
        double Far = Mth.lerp(Share, FarSlow, FarFast);
        Optional<Vec3> First = Scp280Spots.eyeSpot(World, Player, Near, Far, Random);
        if (First.isEmpty()) {
            return 0;
        }
        List<Vec3> Points = Scp280Spots.cluster(World, Player, First.get(), pairsFor(Meter, Random), Random);
        List<Scp280EyesPayload.Spot> Shown = new ArrayList<>();
        int Longest = 0;
        for (int Index = 0; Index < Points.size(); Index++) {
            Vec3 Point = Points.get(Index);
            int Delay = Index == 0 ? 0 : 4 + Random.nextInt(StaggerMax);
            int Life = LifeMin + Random.nextInt(LifeMax - LifeMin + 1);
            Longest = Math.max(Longest, Delay + Life);
            Shown.add(new Scp280EyesPayload.Spot((float) Point.x, (float) Point.y, (float) Point.z, (byte) Random.nextInt(Variants), Delay, Life));
        }
        Haunt State = haunt(Player.getUUID());
        State.Spots.clear();
        State.Spots.addAll(Points);
        State.Stared = false;
        State.SightingEnd = World.getGameTime() + Longest;
        send(Player, Scp280EyesPayload.show(Shown));
        Scp280World.get(Player.server).witness(Player.getUUID());
        return Points.size();
    }

    private static void watch(ServerPlayer Player, Haunt State) {
        ServerLevel World = Player.serverLevel();
        if (World.getGameTime() >= State.SightingEnd || !eligible(Player)) {
            State.Spots.clear();
            return;
        }
        State.Spots.removeIf(Point -> Player.getEyePosition().distanceTo(Point) < VanishRange || Scp280Light.at(World, BlockPos.containing(Point)) > Scp280Light.SpotLevel + 2);
        if (State.Stared) {
            return;
        }
        for (Vec3 Point : State.Spots) {
            if (staring(Player, Point)) {
                State.Stared = true;
                Scp280World Data = Scp280World.get(Player.server);
                Data.setMeter(Player.getUUID(), Data.meter(Player.getUUID()) + StareGain);
                return;
            }
        }
    }

    public static boolean staring(ServerPlayer Player, Vec3 Point) {
        Vec3 Eye = Player.getEyePosition();
        Vec3 Toward = Point.subtract(Eye);
        double Distance = Toward.length();
        if (Distance < 1.0e-3) {
            return false;
        }
        double Facing = Player.getViewVector(1.0f).dot(Toward.scale(1.0 / Distance));
        if (Facing < Math.min(StareCone, Math.cos(Math.atan2(StareWidth, Distance)))) {
            return false;
        }
        return Player.level().clip(new ClipContext(Eye, Point, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Player)).getType() == HitResult.Type.MISS;
    }

    public static boolean reveal(ServerPlayer Player) {
        ServerLevel World = Player.serverLevel();
        Scp280World Data = Scp280World.get(Player.server);
        if (!realm(World) || Data.isCaptured() || Data.looseActive(World.getGameTime())) {
            return false;
        }
        Optional<Vec3> Spot = Scp280Spots.revealSpot(World, Player, Player.getRandom());
        if (Spot.isEmpty()) {
            return false;
        }
        Optional<Scp280Shade> Made = Scp280Shade.reveal(World, Spot.get(), Player);
        if (Made.isEmpty()) {
            return false;
        }
        Data.setMeter(Player.getUUID(), RevealDrop);
        Haunt State = haunt(Player.getUUID());
        State.Spots.clear();
        State.Countdown = nextDelay(RevealDrop, Player.getRandom());
        send(Player, Scp280EyesPayload.clear());
        playTo(Player, Scp280Registry.StingLayers, 1.0f, 1.0f);
        sync(Player, State, RevealDrop, true, Scp280MeterPayload.Reveal);
        return true;
    }

    public static void playTo(ServerPlayer Player, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            Player.playNotifySound(Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static void playAt(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static void captured(ServerLevel World, Scp280Shade Shade) {
        MinecraftServer Server = World.getServer();
        Scp280World Data = Scp280World.get(Server);
        Data.capture(Shade.getUUID());
        Haunts.clear();
        for (ServerPlayer Online : Server.getPlayerList().getPlayers()) {
            send(Online, Scp280EyesPayload.clear());
            sync(Online, haunt(Online.getUUID()), 0.0f, true, Scp280MeterPayload.Cleared);
        }
        playAt(World, Shade.position().add(0.0, 1.2, 0.0), Scp280Registry.ContainedLayers, 1.0f, 1.0f);
        Notices.sendToAll(Server, Notice.anomaly(DocumentClass.Keter, Component.translatable("notice.scp_unleashed.scp280.contained"))
                .withDetail(Component.translatable("notice.scp_unleashed.scp280.contained.detail")));
        DiscordNotices.containment(World, Shade.blockPosition(), Scp280Registry.DocumentId, false);
    }

    public static void released(ServerLevel World, Scp280Shade Shade, boolean Announce) {
        Scp280World Data = Scp280World.get(World.getServer());
        if (!Data.isCaptured()) {
            return;
        }
        Data.release(Shade.getUUID());
        if (Announce) {
            Notices.sendToAll(World.getServer(), Notice.anomaly(DocumentClass.Keter, Component.translatable("notice.scp_unleashed.scp280.breach"))
                    .withDetail(Component.translatable("notice.scp_unleashed.scp280.breach.detail")));
            DiscordNotices.containment(World, Shade.blockPosition(), Scp280Registry.DocumentId, true);
        }
    }

    public static void sync(ServerPlayer Player, Haunt State, float Meter, boolean Force) {
        sync(Player, State, Meter, Force, Scp280MeterPayload.None);
    }

    public static void sync(ServerPlayer Player, Haunt State, float Meter, boolean Force, byte Cue) {
        long Now = Player.serverLevel().getGameTime();
        boolean Changed = Math.abs(Meter - State.Synced) >= SyncStep || Meter <= 0.0f && State.Synced > 0.0f || Meter >= Scp280World.MaxMeter && State.Synced < Scp280World.MaxMeter;
        boolean Stale = State.Synced > 0.0f && Now - State.SyncedAt >= SyncRefresh;
        if (!Force && !Changed && !Stale) {
            return;
        }
        if (!Force && State.Synced < 0.0f && Meter <= 0.0f) {
            State.Synced = 0.0f;
            State.SyncedAt = Now;
            return;
        }
        State.Synced = Meter;
        State.SyncedAt = Now;
        send(Player, new Scp280MeterPayload(fraction(Meter), Cue));
    }

    public static void send(ServerPlayer Player, CustomPacketPayload Payload) {
        if (Player.connection != null && Player.connection.hasChannel(Payload.type())) {
            PacketDistributor.sendToPlayer(Player, Payload);
        }
    }

    public static void joined(ServerPlayer Player) {
        Haunt State = haunt(Player.getUUID());
        State.Synced = -1.0f;
        Scp280World Data = Scp280World.get(Player.server);
        float Meter = Data.isCaptured() ? 0.0f : Data.meter(Player.getUUID());
        if (Meter > 0.0f) {
            sync(Player, State, Meter, true);
        }
    }

    public static void setMeter(ServerPlayer Player, float Meter) {
        Scp280World.get(Player.server).setMeter(Player.getUUID(), Meter);
        Haunt State = haunt(Player.getUUID());
        State.Countdown = Math.min(State.Countdown < 0 ? Integer.MAX_VALUE : State.Countdown, nextDelay(Meter, Player.getRandom()));
        sync(Player, State, Scp280World.get(Player.server).meter(Player.getUUID()), true);
    }
}
