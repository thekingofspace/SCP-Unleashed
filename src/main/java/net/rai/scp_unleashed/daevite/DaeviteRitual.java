package net.rai.scp_unleashed.daevite;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.daevite.entity.DaeviteCircle;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import net.rai.scp_unleashed.daevite.entity.DaeviteMob;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import net.rai.scp_unleashed.daevite.network.DaeviteSkyPayload;
import net.rai.scp_unleashed.faction.FactionGround;
import net.rai.scp_unleashed.faction.FactionPhase;
import net.rai.scp_unleashed.faction.FactionSite;
import net.rai.scp_unleashed.faction.FactionState;
import net.rai.scp_unleashed.scp140.api.DaeviteSummonEvent;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class DaeviteRitual {
    private static final Logger Log = LogUtils.getLogger();
    public static final int TempleTicks = 1500;
    public static final int RemoteTicks = 900;
    public static final long RemoteDelay = 4800L;
    public static final long RetryDelay = 6000L;
    public static final double ChannelerLoss = 100.0;
    public static final double BreakLoss = 250.0;
    public static final float SetBack = 0.15f;
    public static final int Channelers = 4;
    public static final int MinimumChannelers = 3;
    public static final int RemoteChannelers = 2;
    public static final double GatherRange = 40.0;
    public static final double RallyRange = 48.0;
    public static final int SkyRange = 160;
    public static final int SkyRadius = 96;
    public static final int RemoteMin = 24;
    public static final int RemoteMax = 40;
    public static final double ChannelRing = 4.5;
    public static final double RemoteRing = 3.5;
    public static final byte None = 0;
    public static final byte Temple = 1;
    public static final byte Waiting = 2;
    public static final byte Remote = 3;
    public static final String Key = "Ritual";

    private DaeviteRitual() {
    }

    private static CompoundTag data(FactionState State) {
        CompoundTag Root = State.extra();
        if (!Root.contains(Key)) {
            Root.put(Key, new CompoundTag());
        }
        return Root.getCompound(Key);
    }

    public static byte stage(FactionState State) {
        return data(State).getByte("Stage");
    }

    public static boolean active(FactionState State) {
        byte Stage = stage(State);
        return Stage == Temple || Stage == Remote;
    }

    public static float share(FactionState State) {
        CompoundTag Ritual = data(State);
        int Total = Ritual.getByte("Stage") == Remote ? RemoteTicks : TempleTicks;
        return Mth.clamp(Ritual.getInt("Progress") / (float) Total, 0.0f, 1.0f);
    }

    @Nullable
    public static BlockPos centre(FactionState State) {
        CompoundTag Ritual = data(State);
        return Ritual.contains("Altar") ? BlockPos.of(Ritual.getLong("Altar")) : null;
    }

    public static BlockPos altarOf(FactionSite Site) {
        BlockPos Mark = Site.mark("altar");
        return Mark != null ? Mark : Site.anchor();
    }

    public static void tick(MinecraftServer Server, FactionState State) {
        CompoundTag Ritual = data(State);
        byte Stage = Ritual.getByte("Stage");
        long Now = Server.overworld().getGameTime();
        if (State.phase() != FactionPhase.Awakened) {
            if (Stage != None) {
                finish(Server, Ritual);
                Ritual.putByte("Stage", None);
                State.changed();
            }
            return;
        }
        switch (Stage) {
            case None -> {
                if (State.power() >= DaeviteFaction.MaxPower - 0.01 && Now >= Ritual.getLong("Retry")) {
                    begin(Server, State, Ritual, Now);
                }
            }
            case Waiting -> waiting(Server, State, Ritual, Now);
            case Temple -> run(Server, State, Ritual, Now, TempleTicks);
            case Remote -> run(Server, State, Ritual, Now, RemoteTicks);
            default -> Ritual.putByte("Stage", None);
        }
    }

    public static void force(MinecraftServer Server, FactionState State) {
        CompoundTag Ritual = data(State);
        if (active(State)) {
            return;
        }
        Ritual.putLong("Retry", 0L);
        long Now = Server.overworld().getGameTime();
        FactionSite Site = DaeviteSites.templeSite(State);
        ServerLevel World = Site != null ? Server.getLevel(Site.dimension()) : null;
        if (Site != null && Site.built() && World != null && World.isPositionEntityTicking(altarOf(Site))) {
            startTemple(Server, State, Ritual, Site, World, Now);
        } else {
            startRemote(Server, State, Ritual, Now);
        }
    }

    private static void begin(MinecraftServer Server, FactionState State, CompoundTag Ritual, long Now) {
        FactionSite Site = DaeviteSites.templeSite(State);
        ServerLevel World = Site != null ? Server.getLevel(Site.dimension()) : null;
        if (Site != null && Site.built() && World != null && World.isPositionEntityTicking(altarOf(Site))) {
            startTemple(Server, State, Ritual, Site, World, Now);
            return;
        }
        Ritual.putByte("Stage", Waiting);
        Ritual.putLong("Ends", Now + RemoteDelay);
        State.changed();
    }

    private static void waiting(MinecraftServer Server, FactionState State, CompoundTag Ritual, long Now) {
        FactionSite Site = DaeviteSites.templeSite(State);
        ServerLevel World = Site != null ? Server.getLevel(Site.dimension()) : null;
        if (Site != null && Site.built() && World != null && World.isPositionEntityTicking(altarOf(Site))) {
            startTemple(Server, State, Ritual, Site, World, Now);
            return;
        }
        if (Now >= Ritual.getLong("Ends")) {
            startRemote(Server, State, Ritual, Now);
        }
    }

    private static List<BlockPos> ringAround(BlockPos Centre, double Radius, int Count, double Turn) {
        List<BlockPos> Points = new ArrayList<>();
        for (int Index = 0; Index < Count; Index++) {
            double Angle = Turn + Index * Mth.TWO_PI / Count;
            Points.add(BlockPos.containing(Centre.getX() + 0.5 + Math.cos(Angle) * Radius, Centre.getY(), Centre.getZ() + 0.5 + Math.sin(Angle) * Radius));
        }
        return Points;
    }

    private static void startTemple(MinecraftServer Server, FactionState State, CompoundTag Ritual, FactionSite Site, ServerLevel World, long Now) {
        BlockPos Altar = altarOf(Site);
        List<BlockPos> Points = new ArrayList<>(Site.marks("channel"));
        if (Points.isEmpty()) {
            Points = ringAround(Altar, ChannelRing, Channelers, Mth.HALF_PI * 0.5);
        }
        Vec3 Heart = Vec3.atBottomCenterOf(Altar);
        List<DaeviteOverseer> Found = new ArrayList<>(World.getEntitiesOfClass(DaeviteOverseer.class, new AABB(Altar).inflate(GatherRange),
                Seer -> Seer.isAlive() && !Seer.doomed()));
        Found.sort(Comparator.comparingDouble(Seer -> Seer.distanceToSqr(Heart)));
        while (Found.size() > Channelers) {
            Found.remove(Found.size() - 1);
        }
        int Index = 0;
        while (Found.size() < MinimumChannelers && Index < Points.size() * 2) {
            BlockPos Point = Points.get(Index % Points.size());
            Index++;
            DaeviteMob Summoned = DaeviteSites.spawnMember(World, State, Site, DaeviteFaction.RoleOverseer, Point, true);
            if (Summoned instanceof DaeviteOverseer Seer) {
                Found.add(Seer);
            }
        }
        if (Found.isEmpty()) {
            Ritual.putByte("Stage", None);
            Ritual.putLong("Retry", Now + 1200L);
            State.changed();
            return;
        }
        ListTag Names = new ListTag();
        for (int Slot = 0; Slot < Found.size(); Slot++) {
            DaeviteOverseer Seer = Found.get(Slot);
            Seer.assignChannel(Vec3.atBottomCenterOf(Points.get(Slot % Points.size())), Heart);
            Names.add(StringTag.valueOf(Seer.getUUID().toString()));
        }
        Ritual.put("Channelers", Names);
        Ritual.putByte("Stage", Temple);
        Ritual.putInt("Progress", 0);
        Ritual.putLong("Altar", Altar.asLong());
        Ritual.putString("Dimension", World.dimension().location().toString());
        Ritual.putLong("Start", Now);
        Ritual.putInt("Told", 0);
        Ritual.putInt("Lost", 0);
        int Floor = Altar.getY();
        for (BlockPos Point : Points) {
            Floor = Math.min(Floor, Point.getY());
        }
        circles(World, Ritual, new BlockPos(Altar.getX(), Floor, Altar.getZ()), Points, DaeviteCircle.Ritual);
        Ritual.putInt("Floor", Floor);
        rally(World, Altar, Now + TempleTicks + 200L);
        World.playSound(null, Altar.getX(), Altar.getY(), Altar.getZ(), DaeviteRegistry.RitualBegin.get(), SoundSource.HOSTILE, 6.0f, 0.8f);
        World.sendParticles(DaeviteCollapse.Cinder, Heart.x, Heart.y + 1.5, Heart.z, 80, 1.5, 1.2, 1.5, 0.08);
        tell(World, Altar, SkyRange, "daevite.scp_unleashed.ritual_begin");
        State.changed();
    }

    private static void circles(ServerLevel World, CompoundTag Ritual, BlockPos Centre, List<BlockPos> Points, byte Kind) {
        Vec3 Floor = Vec3.atBottomCenterOf(Centre);
        DaeviteCircle Main = Kind == DaeviteCircle.Summoning ? DaeviteCircle.summoning(World, Floor) : DaeviteCircle.ritual(World, Floor, DaeviteCircle.RitualRadius);
        Ritual.putString("Circle", Main.getUUID().toString());
        ListTag Glyphs = new ListTag();
        for (BlockPos Point : Points) {
            Vec3 Ground = DaeviteCollapse.groundAt(World, Point.getX() + 0.5, Point.getZ() + 0.5, Point.getY()).map(Y -> new Vec3(Point.getX() + 0.5, Y, Point.getZ() + 0.5))
                    .orElse(Vec3.atBottomCenterOf(Point));
            Glyphs.add(StringTag.valueOf(DaeviteCircle.channel(World, Ground).getUUID().toString()));
        }
        Ritual.put("Glyphs", Glyphs);
    }

    private static void rally(ServerLevel World, BlockPos Altar, long Until) {
        for (DaeviteKnight Knight : World.getEntitiesOfClass(DaeviteKnight.class, new AABB(Altar).inflate(RallyRange), Body -> Body.isAlive() && Body.getTarget() == null)) {
            double Angle = World.random.nextDouble() * Mth.TWO_PI;
            double Radius = 8.0 + World.random.nextDouble() * 4.0;
            Knight.setPatrol(new Vec3(Altar.getX() + 0.5 + Math.cos(Angle) * Radius, Altar.getY(), Altar.getZ() + 0.5 + Math.sin(Angle) * Radius), Until);
        }
    }

    @Nullable
    private static ServerPlayer nearestPlayer(ServerLevel World, BlockPos From) {
        ServerPlayer Best = null;
        double BestGap = Double.MAX_VALUE;
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || User.isCreative() || !User.isAlive() || Scp268.isNeglected(User) || !Targeting.isTargetable(User)) {
                continue;
            }
            double Gap = User.blockPosition().distSqr(From);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = User;
            }
        }
        return Best;
    }

    @Nullable
    private static BlockPos summonSpot(ServerLevel World, ServerPlayer Target) {
        BlockPos Found = FactionGround.standable(World, Target.blockPosition(), RemoteMin, RemoteMax, World.random, 40);
        if (Found == null) {
            Found = FactionGround.standable(World, Target.blockPosition(), RemoteMin - 8, RemoteMax + 12, World.random, 40);
        }
        return Found;
    }

    private static void startRemote(MinecraftServer Server, FactionState State, CompoundTag Ritual, long Now) {
        ServerLevel World = Server.getLevel(Level.OVERWORLD);
        if (World == null) {
            return;
        }
        FactionSite Site = DaeviteSites.templeSite(State);
        BlockPos From = Site != null ? Site.anchor() : World.getSharedSpawnPos();
        ServerPlayer Target = nearestPlayer(World, From);
        if (Target == null) {
            Ritual.putByte("Stage", Waiting);
            Ritual.putLong("Ends", Now + 200L);
            State.changed();
            return;
        }
        BlockPos Spot = summonSpot(World, Target);
        if (Spot == null) {
            Ritual.putByte("Stage", Waiting);
            Ritual.putLong("Ends", Now + 100L);
            State.changed();
            return;
        }
        List<BlockPos> Points = ringAround(Spot, RemoteRing, RemoteChannelers, World.random.nextDouble() * Mth.TWO_PI);
        ListTag Names = new ListTag();
        for (BlockPos Point : Points) {
            BlockPos Ground = FactionGround.standable(World, Point, 0, 2, World.random, 8);
            DaeviteMob Summoned = DaeviteSites.spawnMember(World, State, Site, DaeviteFaction.RoleOverseer, Ground != null ? Ground : Point, true);
            if (Summoned instanceof DaeviteOverseer Seer) {
                Seer.assignChannel(Vec3.atBottomCenterOf(Point), Vec3.atBottomCenterOf(Spot));
                Names.add(StringTag.valueOf(Seer.getUUID().toString()));
            }
        }
        if (Names.isEmpty()) {
            Ritual.putByte("Stage", Waiting);
            Ritual.putLong("Ends", Now + 200L);
            State.changed();
            return;
        }
        Ritual.put("Channelers", Names);
        Ritual.remove("Floor");
        Ritual.putByte("Stage", Remote);
        Ritual.putInt("Progress", 0);
        Ritual.putLong("Altar", Spot.asLong());
        Ritual.putString("Dimension", World.dimension().location().toString());
        Ritual.putLong("Start", Now);
        Ritual.putInt("Told", 0);
        Ritual.putInt("Lost", 0);
        circles(World, Ritual, Spot, Points, DaeviteCircle.Summoning);
        World.playSound(null, Spot.getX(), Spot.getY(), Spot.getZ(), DaeviteRegistry.RitualBegin.get(), SoundSource.HOSTILE, 4.0f, 0.9f);
        tell(World, Spot, 96, "daevite.scp_unleashed.ritual_remote");
        State.changed();
    }

    private static ServerLevel levelOf(MinecraftServer Server, CompoundTag Ritual) {
        for (ServerLevel World : Server.getAllLevels()) {
            if (World.dimension().location().toString().equals(Ritual.getString("Dimension"))) {
                return World;
            }
        }
        return Server.overworld();
    }

    private static void run(MinecraftServer Server, FactionState State, CompoundTag Ritual, long Now, int Total) {
        ServerLevel World = levelOf(Server, Ritual);
        BlockPos Centre = BlockPos.of(Ritual.getLong("Altar"));
        boolean Loaded = World.isPositionEntityTicking(Centre);
        int Progress = Ritual.getInt("Progress");
        if (Loaded) {
            mendCircles(World, Ritual, Centre);
            int Lost = checkChannelers(World, State, Ritual, Centre);
            if (Lost > 0) {
                Progress = Math.max(0, Progress - (int) (Total * SetBack * Lost));
                falter(World, Centre);
            }
            if (Ritual.getList("Channelers", Tag.TAG_STRING).isEmpty()) {
                shatter(Server, State, Ritual, World, Centre, Now);
                return;
            }
            effects(World, Ritual, Centre, Progress / (float) Total, Now);
        }
        Progress += 20;
        Ritual.putInt("Progress", Progress);
        State.changed();
        float Share = Progress / (float) Total;
        int Told = Ritual.getInt("Told");
        if (Share >= 0.5f && (Told & 1) == 0) {
            Ritual.putInt("Told", Told | 1);
            tell(World, Centre, SkyRange, "daevite.scp_unleashed.ritual_half");
        } else if (Share >= 0.85f && (Told & 2) == 0) {
            Ritual.putInt("Told", Told | 2);
            tell(World, Centre, SkyRange, "daevite.scp_unleashed.ritual_near");
        }
        if (Progress >= Total) {
            if (!Loaded) {
                ServerPlayer Target = nearestPlayer(World, Centre);
                BlockPos Moved = Target != null ? summonSpot(World, Target) : null;
                if (Moved == null) {
                    Ritual.putInt("Progress", Total - 20);
                    return;
                }
                finish(Server, Ritual);
                Centre = Moved;
                DaeviteCircle Flash = DaeviteCircle.summoning(World, Vec3.atBottomCenterOf(Centre));
                Flash.setPower(1.0f);
                Ritual.putString("Circle", Flash.getUUID().toString());
            }
            complete(Server, State, Ritual, World, Centre, Now);
        }
    }

    @Nullable
    private static Entity find(ServerLevel World, CompoundTag Ritual, String Name) {
        try {
            return Ritual.contains(Name) ? World.getEntity(UUID.fromString(Ritual.getString(Name))) : null;
        } catch (IllegalArgumentException Problem) {
            return null;
        }
    }

    private static void mendCircles(ServerLevel World, CompoundTag Ritual, BlockPos Centre) {
        if (!(find(World, Ritual, "Circle") instanceof DaeviteCircle)) {
            BlockPos Floor = Ritual.contains("Floor") ? new BlockPos(Centre.getX(), Ritual.getInt("Floor"), Centre.getZ()) : Centre;
            DaeviteCircle Main = Ritual.getByte("Stage") == Remote ? DaeviteCircle.summoning(World, Vec3.atBottomCenterOf(Centre))
                    : DaeviteCircle.ritual(World, Vec3.atBottomCenterOf(Floor), DaeviteCircle.RitualRadius);
            Ritual.putString("Circle", Main.getUUID().toString());
        }
    }

    private static int checkChannelers(ServerLevel World, FactionState State, CompoundTag Ritual, BlockPos Centre) {
        ListTag Names = Ritual.getList("Channelers", Tag.TAG_STRING);
        ListTag Kept = new ListTag();
        int Lost = 0;
        for (int Index = 0; Index < Names.size(); Index++) {
            Entity Found;
            try {
                Found = World.getEntity(UUID.fromString(Names.getString(Index)));
            } catch (IllegalArgumentException Problem) {
                Found = null;
            }
            if (Found instanceof DaeviteOverseer Seer && Seer.isAlive() && !Seer.doomed()) {
                if (Seer.channelPoint() == null) {
                    Seer.assignChannel(Seer.position(), Vec3.atBottomCenterOf(Centre));
                }
                Kept.add(StringTag.valueOf(Seer.getUUID().toString()));
            } else {
                Lost++;
            }
        }
        if (Lost > 0) {
            Ritual.put("Channelers", Kept);
            Ritual.putInt("Lost", Ritual.getInt("Lost") + Lost);
            State.setPower(State.power() - ChannelerLoss * Lost, DaeviteFaction.MaxPower);
        }
        return Lost;
    }

    private static void falter(ServerLevel World, BlockPos Centre) {
        World.playSound(null, Centre.getX(), Centre.getY(), Centre.getZ(), DaeviteRegistry.RitualBreak.get(), SoundSource.HOSTILE, 2.0f, 1.3f);
        World.sendParticles(DaeviteCollapse.Shard, Centre.getX() + 0.5, Centre.getY() + 0.4, Centre.getZ() + 0.5, 40, 3.0, 0.2, 3.0, 0.05);
    }

    private static void effects(ServerLevel World, CompoundTag Ritual, BlockPos Centre, float Share, long Now) {
        if (find(World, Ritual, "Circle") instanceof DaeviteCircle Main) {
            Main.setPower(Share);
        }
        float Strength = 0.35f + 0.65f * Share;
        for (ServerPlayer User : World.players()) {
            if (User.blockPosition().distSqr(Centre) <= (double) SkyRange * SkyRange) {
                PacketDistributor.sendToPlayer(User, new DaeviteSkyPayload(Centre, Strength, SkyRadius));
            }
        }
        long Since = Now - Ritual.getLong("Start");
        if (Since % 80L < 20L) {
            World.playSound(null, Centre.getX(), Centre.getY(), Centre.getZ(), DaeviteRegistry.RitualHum.get(), SoundSource.HOSTILE, 3.0f, 0.7f + Share * 0.3f);
        }
        if (Share > 0.5f && World.random.nextFloat() < 0.35f) {
            World.playSound(null, Centre.getX(), Centre.getY() + 12, Centre.getZ(), DaeviteRegistry.RitualRumble.get(), SoundSource.WEATHER, 5.0f, 0.6f + World.random.nextFloat() * 0.2f);
        }
        double X = Centre.getX() + 0.5;
        double Z = Centre.getZ() + 0.5;
        World.sendParticles(DaeviteCollapse.Cinder, X, Centre.getY() + 1.2, Z, 6 + (int) (Share * 20), 0.6, 0.8, 0.6, 0.06);
        if (Share > 0.8f) {
            for (int Step = 0; Step < 10; Step++) {
                World.sendParticles(ParticleTypes.FLAME, X, Centre.getY() + 1.5 + Step * 1.2, Z, 2, 0.15, 0.4, 0.15, 0.01);
            }
        }
    }

    private static void shatter(MinecraftServer Server, FactionState State, CompoundTag Ritual, ServerLevel World, BlockPos Centre, long Now) {
        State.setPower(State.power() - BreakLoss, DaeviteFaction.MaxPower);
        World.playSound(null, Centre.getX(), Centre.getY(), Centre.getZ(), DaeviteRegistry.RitualBreak.get(), SoundSource.HOSTILE, 5.0f, 0.75f);
        World.sendParticles(DaeviteCollapse.Shard, Centre.getX() + 0.5, Centre.getY() + 0.5, Centre.getZ() + 0.5, 160, 4.0, 0.6, 4.0, 0.15);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.getX() + 0.5, Centre.getY() + 0.5, Centre.getZ() + 0.5, 30, 3.0, 0.5, 3.0, 0.02);
        tell(World, Centre, SkyRange, "daevite.scp_unleashed.ritual_broken");
        finish(Server, Ritual);
        Ritual.putByte("Stage", None);
        Ritual.putLong("Retry", Now + RetryDelay);
        State.changed();
    }

    private static void complete(MinecraftServer Server, FactionState State, CompoundTag Ritual, ServerLevel World, BlockPos Centre, long Now) {
        Entity God = null;
        try {
            God = DaeviteSummonEvent.fire(World, Centre);
        } catch (RuntimeException Problem) {
            Log.warn("The Daevite summoning call failed; the faction keeps its power and will try again", Problem);
        }
        if (God != null) {
            State.setPhase(FactionPhase.Ascendant);
            State.setPower(0.0, DaeviteFaction.MaxPower);
            for (int Strike = 0; Strike < 3; Strike++) {
                LightningBolt Bolt = EntityType.LIGHTNING_BOLT.create(World);
                if (Bolt != null) {
                    double Angle = Strike * Mth.TWO_PI / 3.0;
                    Bolt.moveTo(Centre.getX() + 0.5 + Math.cos(Angle) * 4.0, Centre.getY(), Centre.getZ() + 0.5 + Math.sin(Angle) * 4.0);
                    Bolt.setVisualOnly(true);
                    World.addFreshEntity(Bolt);
                }
            }
            World.playSound(null, Centre.getX(), Centre.getY(), Centre.getZ(), DaeviteRegistry.RitualCall.get(), SoundSource.HOSTILE, 8.0f, 0.7f);
            tell(World, Centre, 256, "daevite.scp_unleashed.ritual_answered");
        } else {
            World.playSound(null, Centre.getX(), Centre.getY(), Centre.getZ(), DaeviteRegistry.RitualBreak.get(), SoundSource.HOSTILE, 3.0f, 0.55f);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, Centre.getX() + 0.5, Centre.getY() + 0.5, Centre.getZ() + 0.5, 40, 2.5, 0.8, 2.5, 0.02);
            tell(World, Centre, SkyRange, "daevite.scp_unleashed.ritual_silent");
            Ritual.putLong("Retry", Now + RetryDelay);
        }
        finish(Server, Ritual);
        Ritual.putByte("Stage", None);
        State.changed();
    }

    private static void finish(MinecraftServer Server, CompoundTag Ritual) {
        ServerLevel World = levelOf(Server, Ritual);
        if (find(World, Ritual, "Circle") instanceof DaeviteCircle Main) {
            Main.dismiss();
        }
        ListTag Glyphs = Ritual.getList("Glyphs", Tag.TAG_STRING);
        for (int Index = 0; Index < Glyphs.size(); Index++) {
            try {
                if (World.getEntity(UUID.fromString(Glyphs.getString(Index))) instanceof DaeviteCircle Glyph) {
                    Glyph.dismiss();
                }
            } catch (IllegalArgumentException Problem) {
                Log.debug("bad glyph id in the Daevite ritual data");
            }
        }
        ListTag Names = Ritual.getList("Channelers", Tag.TAG_STRING);
        for (int Index = 0; Index < Names.size(); Index++) {
            try {
                if (World.getEntity(UUID.fromString(Names.getString(Index))) instanceof DaeviteOverseer Seer) {
                    Seer.stopChannel();
                }
            } catch (IllegalArgumentException Problem) {
                Log.debug("bad channeler id in the Daevite ritual data");
            }
        }
        Ritual.remove("Channelers");
        Ritual.remove("Glyphs");
        Ritual.remove("Circle");
        Ritual.putInt("Progress", 0);
    }

    public static void abort(MinecraftServer Server, FactionState State) {
        CompoundTag Ritual = data(State);
        if (Ritual.getByte("Stage") != None) {
            finish(Server, Ritual);
            Ritual.putByte("Stage", None);
            State.changed();
        }
    }

    private static void tell(ServerLevel World, BlockPos Centre, int Range, String Key) {
        for (ServerPlayer User : World.players()) {
            if (User.blockPosition().distSqr(Centre) <= (double) Range * Range) {
                User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            }
        }
    }
}
