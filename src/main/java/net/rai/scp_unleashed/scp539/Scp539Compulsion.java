package net.rai.scp_unleashed.scp539;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.scp539.network.Scp539GazePayload;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Scp539Compulsion {
    public static final double Radius = 16.0;
    public static final int Duration = 600;
    public static final int Linger = 50;
    public static final int Hold = 8;
    public static final int SendEvery = 4;
    public static final float NoticeVolume = 0.5f;
    public static final ResourceLocation StareId = Scp539Registry.location("scp539_stare");
    public static final AttributeModifier Rooted = new AttributeModifier(StareId, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private static final Map<UUID, Long> Until = new HashMap<>();
    private static final Map<UUID, Gaze> Gazes = new HashMap<>();
    private static final Map<UUID, Gaze> Stares = new HashMap<>();
    private static final Map<UUID, Source> Sources = new HashMap<>();

    public record Gaze(ResourceKey<Level> Dimension, int TargetId, Vec3 Spot, long Ends, long Sent) {
        public Gaze retarget(int NewTarget, Vec3 NewSpot, long NewEnds) {
            return new Gaze(Dimension, NewTarget, NewSpot, NewEnds, Long.MIN_VALUE);
        }
    }

    public record Source(ResourceKey<Level> Dimension, int CarrierId) {
    }

    private Scp539Compulsion() {
    }

    public static long now(ServerLevel World) {
        return World.getServer().getTickCount();
    }

    public static long now(MinecraftServer Server) {
        return Server.getTickCount();
    }

    public static boolean canBeCompelled(ServerPlayer User) {
        return User.isAlive() && !User.isSpectator() && !Senses.isBlindfolded(User);
    }

    public static boolean canStare(DClass Subject) {
        return Subject.isAlive() && !Subject.isRestingPose() && !Senses.isBlindfolded(Subject);
    }

    public static void thrown(ServerLevel World, Scp539Stone Stone) {
        Scp539Registry.playLayers(World, Stone.position(), Scp539Registry.NoticeLayers, SoundSource.NEUTRAL, NoticeVolume, 0.9f + World.getRandom().nextFloat() * 0.15f);
        flight(World, Stone);
    }

    public static void flight(ServerLevel World, Scp539Stone Stone) {
        long Now = now(World);
        long Ends = Now + Duration - Math.max(0, Stone.flight() - 1);
        Vec3 Spot = Stone.position();
        double Reach = Radius * Radius;
        for (ServerPlayer User : World.players()) {
            if (canBeCompelled(User) && User.distanceToSqr(Spot) <= Reach) {
                compel(User, World, Stone.getId(), Spot, Now, Ends, Now + Hold);
            }
        }
        for (DClass Subject : World.getEntitiesOfClass(DClass.class, new AABB(Spot, Spot).inflate(Radius), Scp539Compulsion::canStare)) {
            if (Subject.distanceToSqr(Spot) <= Reach) {
                Stares.put(Subject.getUUID(), new Gaze(World.dimension(), Stone.getId(), Spot, Now + Hold, Now));
            }
        }
    }

    public static void compel(ServerPlayer User, ServerLevel World, int TargetId, Vec3 Spot, long Now, long Ends, long GazeEnds) {
        Until.merge(User.getUUID(), Ends, Math::max);
        Sources.put(User.getUUID(), new Source(World.dimension(), TargetId));
        Gaze Previous = Gazes.get(User.getUUID());
        boolean Fresh = Previous == null || Previous.TargetId() != TargetId || !Previous.Dimension().equals(World.dimension());
        long Sent = Fresh ? Long.MIN_VALUE : Previous.Sent();
        Gaze Next = new Gaze(World.dimension(), TargetId, Spot, Math.max(GazeEnds, Fresh ? GazeEnds : Previous.Ends()), Sent);
        if (Fresh || Now - Sent >= SendEvery) {
            send(User, Next, Now);
            Next = new Gaze(Next.Dimension(), Next.TargetId(), Next.Spot(), Next.Ends(), Now);
        }
        Gazes.put(User.getUUID(), Next);
    }

    public static void landed(ServerLevel World, Scp539Stone Stone, ItemEntity Dropped) {
        long Now = now(World);
        int Old = Stone.getId();
        Vec3 Spot = Dropped.position();
        for (Map.Entry<UUID, Gaze> Entry : Gazes.entrySet()) {
            Gaze Current = Entry.getValue();
            if (Current.TargetId() == Old && Current.Dimension().equals(World.dimension())) {
                Gaze Next = Current.retarget(Dropped.getId(), Spot, Now + Linger);
                ServerPlayer User = World.getServer().getPlayerList().getPlayer(Entry.getKey());
                if (User != null) {
                    send(User, Next, Now);
                    Next = new Gaze(Next.Dimension(), Next.TargetId(), Next.Spot(), Next.Ends(), Now);
                }
                Entry.setValue(Next);
            }
        }
        for (Map.Entry<UUID, Gaze> Entry : Stares.entrySet()) {
            Gaze Current = Entry.getValue();
            if (Current.TargetId() == Old && Current.Dimension().equals(World.dimension())) {
                Entry.setValue(Current.retarget(Dropped.getId(), Spot, Now + Linger));
            }
        }
        for (Map.Entry<UUID, Source> Entry : Sources.entrySet()) {
            if (Entry.getValue().CarrierId() == Old && Entry.getValue().Dimension().equals(World.dimension())) {
                Entry.setValue(new Source(World.dimension(), Dropped.getId()));
            }
        }
    }

    public static void pickedUp(ServerLevel World, ItemEntity Dropped) {
        int Carrier = Dropped.getId();
        Iterator<Map.Entry<UUID, Source>> Players = Sources.entrySet().iterator();
        while (Players.hasNext()) {
            Map.Entry<UUID, Source> Entry = Players.next();
            if (Entry.getValue().CarrierId() != Carrier || !Entry.getValue().Dimension().equals(World.dimension())) {
                continue;
            }
            Until.remove(Entry.getKey());
            if (Gazes.remove(Entry.getKey()) != null) {
                ServerPlayer User = World.getServer().getPlayerList().getPlayer(Entry.getKey());
                if (User != null) {
                    PacketDistributor.sendToPlayer(User, new Scp539GazePayload(-1, 0.0, 0.0, 0.0, 0));
                }
            }
            Players.remove();
        }
        Iterator<Map.Entry<UUID, Gaze>> Subjects = Stares.entrySet().iterator();
        while (Subjects.hasNext()) {
            Map.Entry<UUID, Gaze> Entry = Subjects.next();
            if (Entry.getValue().TargetId() != Carrier || !Entry.getValue().Dimension().equals(World.dimension())) {
                continue;
            }
            if (World.getEntity(Entry.getKey()) instanceof DClass Subject) {
                release(Subject);
            }
            Subjects.remove();
        }
    }

    public static void forget(ServerLevel World, Scp539Stone Stone) {
        long Now = now(World);
        for (Map.Entry<UUID, Gaze> Entry : Gazes.entrySet()) {
            if (Entry.getValue().TargetId() == Stone.getId() && Entry.getValue().Dimension().equals(World.dimension())) {
                Entry.setValue(new Gaze(Entry.getValue().Dimension(), Entry.getValue().TargetId(), Entry.getValue().Spot(), Now, Entry.getValue().Sent()));
            }
        }
    }

    private static void send(ServerPlayer User, Gaze Current, long Now) {
        int Ticks = (int) Mth.clamp(Current.Ends() - Now, 0L, 1200L);
        PacketDistributor.sendToPlayer(User, new Scp539GazePayload(Current.TargetId(), Current.Spot().x, Current.Spot().y, Current.Spot().z, Ticks));
    }

    public static boolean isAffected(ServerPlayer User) {
        return isAffected(User.getUUID(), now(User.server));
    }

    public static boolean isAffected(UUID Id, long Now) {
        Long Ends = Until.get(Id);
        return Ends != null && Now < Ends;
    }

    public static long affectedUntil(UUID Id) {
        return Until.getOrDefault(Id, Long.MIN_VALUE);
    }

    public static boolean isGazing(UUID Id, long Now) {
        Gaze Current = Gazes.get(Id);
        return Current != null && Now < Current.Ends();
    }

    @Nullable
    public static Gaze gaze(UUID Id) {
        return Gazes.get(Id);
    }

    public static boolean isStaring(UUID Id, long Now) {
        Gaze Current = Stares.get(Id);
        return Current != null && Now < Current.Ends();
    }

    public static void setUntil(UUID Id, long Ends) {
        Until.put(Id, Ends);
    }

    public static void clear(UUID Id) {
        Until.remove(Id);
        Sources.remove(Id);
        Gazes.remove(Id);
        Stares.remove(Id);
    }

    public static void reset() {
        Until.clear();
        Sources.clear();
        Gazes.clear();
        Stares.clear();
    }

    public static void hold(DClass Subject, ServerLevel World) {
        if (Stares.isEmpty()) {
            return;
        }
        Gaze Stare = Stares.get(Subject.getUUID());
        if (Stare == null || !Stare.Dimension().equals(World.dimension()) || now(World) >= Stare.Ends()) {
            return;
        }
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null && !Speed.hasModifier(StareId)) {
            Speed.addTransientModifier(Rooted);
        }
        Subject.getNavigation().stop();
        Vec3 Motion = Subject.getDeltaMovement();
        Subject.setDeltaMovement(0.0, Motion.y, 0.0);
        Entity Target = World.getEntity(Stare.TargetId());
        Vec3 Spot = Target != null ? Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0) : Stare.Spot();
        Vec3 Toward = Spot.subtract(Subject.getEyePosition());
        double Across = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        float Yaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
        float Pitch = Mth.clamp((float) -(Mth.atan2(Toward.y, Across) * Mth.RAD_TO_DEG), -Subject.getMaxHeadXRot(), Subject.getMaxHeadXRot());
        Subject.setYRot(Yaw);
        Subject.setYHeadRot(Yaw);
        Subject.yBodyRot = Yaw;
        Subject.setXRot(Pitch);
        Subject.getLookControl().setLookAt(Spot.x, Spot.y, Spot.z, 90.0f, 90.0f);
    }

    public static void tick(MinecraftServer Server) {
        if (Until.isEmpty() && Gazes.isEmpty() && Stares.isEmpty() && Sources.isEmpty()) {
            return;
        }
        long Now = now(Server);
        Until.values().removeIf(Ends -> Ends <= Now);
        Sources.keySet().removeIf(Id -> !Until.containsKey(Id));
        Iterator<Map.Entry<UUID, Gaze>> Players = Gazes.entrySet().iterator();
        while (Players.hasNext()) {
            Map.Entry<UUID, Gaze> Entry = Players.next();
            if (Entry.getValue().Ends() > Now) {
                continue;
            }
            ServerPlayer User = Server.getPlayerList().getPlayer(Entry.getKey());
            if (User != null) {
                PacketDistributor.sendToPlayer(User, new Scp539GazePayload(-1, 0.0, 0.0, 0.0, 0));
            }
            Players.remove();
        }
        Iterator<Map.Entry<UUID, Gaze>> Subjects = Stares.entrySet().iterator();
        while (Subjects.hasNext()) {
            Map.Entry<UUID, Gaze> Entry = Subjects.next();
            if (Entry.getValue().Ends() > Now) {
                continue;
            }
            ServerLevel World = Server.getLevel(Entry.getValue().Dimension());
            Entity Found = World == null ? null : World.getEntity(Entry.getKey());
            if (Found instanceof DClass Subject) {
                release(Subject);
            }
            Subjects.remove();
        }
    }

    public static void release(DClass Subject) {
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.removeModifier(StareId);
        }
    }
}
