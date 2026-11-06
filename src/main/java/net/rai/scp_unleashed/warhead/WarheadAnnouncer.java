package net.rai.scp_unleashed.warhead;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.warhead.network.WarheadAlertPayload;
import net.rai.scp_unleashed.warhead.network.WarheadAnnouncePayload;
import net.rai.scp_unleashed.discord.DiscordNotices;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class WarheadAnnouncer {
    public static final int[] Milestones = {1200, 600, 400};
    public static final double ClearMargin = 96.0;

    private WarheadAnnouncer() {
    }

    private static List<BlockPos> points(WarheadSequence Sequence) {
        List<BlockPos> Points = new ArrayList<>(Sequence.getTargets());
        Points.add(Sequence.getPanel());
        return Points;
    }

    private static double nearest(ServerPlayer Listener, WarheadSequence Sequence) {
        double Best = Double.MAX_VALUE;
        for (BlockPos Point : points(Sequence)) {
            Best = Math.min(Best, Listener.distanceToSqr(Vec3.atCenterOf(Point)));
        }
        return Math.sqrt(Best);
    }

    public static Optional<WarheadSequence> urgentFor(ServerPlayer Listener, Collection<WarheadSequence> Running, long Now) {
        WarheadSequence Best = null;
        for (WarheadSequence Sequence : Running) {
            if (nearest(Listener, Sequence) <= WarheadNetwork.AlertRange && (Best == null || Sequence.ticksLeft(Now) < Best.ticksLeft(Now))) {
                Best = Sequence;
            }
        }
        return Optional.ofNullable(Best);
    }

    private static void announce(ServerLevel World, WarheadSequence Sequence, int Kind, int Seconds) {
        WarheadAnnouncePayload Payload = new WarheadAnnouncePayload(Kind, Seconds);
        for (ServerPlayer Listener : World.players()) {
            if (nearest(Listener, Sequence) <= WarheadNetwork.AlertRange) {
                PacketDistributor.sendToPlayer(Listener, Payload);
            }
        }
    }

    private static void announceUrgent(ServerLevel World, WarheadSequence Sequence, Collection<WarheadSequence> Running, int Kind, int Seconds) {
        WarheadAnnouncePayload Payload = new WarheadAnnouncePayload(Kind, Seconds);
        long Now = World.getGameTime();
        for (ServerPlayer Listener : World.players()) {
            if (urgentFor(Listener, Running, Now).filter(Urgent -> Urgent == Sequence).isPresent()) {
                PacketDistributor.sendToPlayer(Listener, Payload);
            }
        }
    }

    private static void alertOne(ServerPlayer Listener, WarheadSequence Sequence, int Left) {
        BlockPos Closest = Sequence.getPanel();
        double Danger = Double.MAX_VALUE;
        for (BlockPos Target : Sequence.getTargets()) {
            double Distance = Listener.distanceToSqr(Vec3.atCenterOf(Target));
            if (Distance < Danger) {
                Danger = Distance;
                Closest = Target;
            }
        }
        boolean Inside = Danger <= WarheadNetwork.KillRange * WarheadNetwork.KillRange;
        PacketDistributor.sendToPlayer(Listener, new WarheadAlertPayload(Closest, Left, Sequence.isLocked(), true, Inside));
    }

    public static void alertAll(ServerLevel World, Collection<WarheadSequence> Running) {
        long Now = World.getGameTime();
        for (ServerPlayer Listener : World.players()) {
            urgentFor(Listener, Running, Now).ifPresent(Urgent -> alertOne(Listener, Urgent, Urgent.ticksLeft(Now)));
        }
    }

    public static void start(ServerLevel World, WarheadSequence Sequence, Collection<WarheadSequence> Running) {
        announce(World, Sequence, WarheadAnnouncePayload.Start, WarheadNetwork.CountdownTicks / 20);
        alertAll(World, Running);
    }

    public static void cue(ServerLevel World, WarheadSequence Sequence, Collection<WarheadSequence> Running, int Left) {
        for (int Milestone : Milestones) {
            if (Left == Milestone) {
                announceUrgent(World, Sequence, Running, WarheadAnnouncePayload.Remaining, Left / 20);
                return;
            }
        }
        if (Left <= WarheadNetwork.LockTicks && Left % 20 == 0) {
            announceUrgent(World, Sequence, Running, WarheadAnnouncePayload.Final, Left / 20);
        }
    }

    private static void clear(ServerLevel World, WarheadSequence Ended, Collection<WarheadSequence> Running) {
        long Now = World.getGameTime();
        WarheadAlertPayload Quiet = new WarheadAlertPayload(Ended.getPanel(), 0, false, false, false);
        for (ServerPlayer Listener : World.players()) {
            if (nearest(Listener, Ended) > WarheadNetwork.AlertRange + ClearMargin) {
                continue;
            }
            Optional<WarheadSequence> Other = urgentFor(Listener, Running, Now);
            if (Other.isPresent()) {
                alertOne(Listener, Other.get(), Other.get().ticksLeft(Now));
            } else {
                PacketDistributor.sendToPlayer(Listener, Quiet);
            }
        }
    }

    public static void cancelled(ServerLevel World, WarheadSequence Sequence, Collection<WarheadSequence> Running) {
        announce(World, Sequence, WarheadAnnouncePayload.Cancelled, Sequence.ticksLeft(World.getGameTime()) / 20);
        clear(World, Sequence, Running);
    }

    public static void detonation(ServerLevel World, WarheadSequence Sequence, Collection<WarheadSequence> Running) {
        announce(World, Sequence, WarheadAnnouncePayload.Detonation, 0);
        DiscordNotices.warheadEnded(World, Sequence, true);
        clear(World, Sequence, Running);
    }

    public static void failed(ServerLevel World, WarheadSequence Sequence, Collection<WarheadSequence> Running) {
        announce(World, Sequence, WarheadAnnouncePayload.Failed, 0);
        DiscordNotices.warheadEnded(World, Sequence, false);
        clear(World, Sequence, Running);
    }
}
