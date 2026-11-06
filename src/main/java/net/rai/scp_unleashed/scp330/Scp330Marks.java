package net.rai.scp_unleashed.scp330;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp330.network.Scp330MarkPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class Scp330Marks {
    public static final byte Greedy = 1;
    public static final byte Shielded = 2;
    private static final Map<LivingEntity, Byte> Sent = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<LivingEntity> Tracked = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private Scp330Marks() {
    }

    public static byte flags(LivingEntity Target) {
        byte Flags = 0;
        if (Scp330Greed.isGreedy(Target)) {
            Flags |= Greedy;
        }
        if (Scp330Barrier.pool(Target) > 0.0f) {
            Flags |= Shielded;
        }
        return Flags;
    }

    public static boolean needsTicking(LivingEntity Target) {
        return Scp330Greed.isGreedy(Target) || Target.hasData(Scp330Registry.Shell);
    }

    public static void track(LivingEntity Target) {
        if (Target.level().isClientSide) {
            return;
        }
        if (!(Target instanceof Player)) {
            Tracked.add(Target);
        }
        update(Target);
    }

    public static void joined(Entity Joined) {
        if (Joined instanceof LivingEntity Living && !(Living instanceof Player) && !Living.level().isClientSide && needsTicking(Living)) {
            Tracked.add(Living);
        }
    }

    public static void update(LivingEntity Target) {
        if (Target.level().isClientSide) {
            return;
        }
        byte Now = flags(Target);
        Byte Before = Sent.get(Target);
        if (Before == null ? Now == 0 : Before == Now) {
            return;
        }
        if (Now == 0) {
            Sent.remove(Target);
        } else {
            Sent.put(Target, Now);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(Target, new Scp330MarkPayload(Target.getId(), Now));
    }

    public static void resend(ServerPlayer User) {
        byte Now = flags(User);
        if (Now == 0) {
            Sent.remove(User);
        } else {
            Sent.put(User, Now);
        }
        PacketDistributor.sendToPlayer(User, new Scp330MarkPayload(User.getId(), Now));
    }

    public static void startTracking(ServerPlayer Viewer, Entity Target) {
        if (Target instanceof LivingEntity Living) {
            byte Now = flags(Living);
            if (Now != 0) {
                PacketDistributor.sendToPlayer(Viewer, new Scp330MarkPayload(Living.getId(), Now));
            }
        }
    }

    public static void levelTick(ServerLevel World) {
        for (ServerPlayer User : List.copyOf(World.players())) {
            if (Scp330Greed.isGreedy(User)) {
                Scp330Greed.tick(User, World);
            }
            if (User.hasData(Scp330Registry.Shell) && !User.hasEffect(Scp330Registry.Barrier)) {
                Scp330Barrier.clear(User);
            }
            update(User);
        }
        List<LivingEntity> Snapshot;
        synchronized (Tracked) {
            Snapshot = new ArrayList<>(Tracked);
        }
        for (LivingEntity Each : Snapshot) {
            if (Each.level() != World) {
                continue;
            }
            if (Each.isRemoved()) {
                Tracked.remove(Each);
                Sent.remove(Each);
                continue;
            }
            if (Scp330Greed.isGreedy(Each)) {
                Scp330Greed.tick(Each, World);
            }
            if (Each.hasData(Scp330Registry.Shell) && !Each.hasEffect(Scp330Registry.Barrier)) {
                Scp330Barrier.clear(Each);
            }
            update(Each);
            if (!needsTicking(Each)) {
                Tracked.remove(Each);
            }
        }
    }

    public static void clear() {
        Sent.clear();
        Tracked.clear();
    }
}
