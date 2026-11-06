package net.rai.scp_unleashed.scp055;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp055.network.Scp055EpisodePayload;
import net.rai.scp_unleashed.scp055.network.Scp055WakePayload;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp055Episodes {
    public static final int DistortTicks = 40;
    public static final int StaticTicks = 30;
    public static final int MinWalk = 90;
    public static final int MaxWalk = 118;
    public static final int CooldownTicks = 60;
    public static final int FiddleClick = 9;
    public static final float SafeFall = 2.5f;
    public static final float WakePitchLow = -6.0f;
    public static final float WakePitchSpread = 16.0f;
    public static final String MessageKey = "message.scp_unleashed.scp055.forgot";
    private static final Map<UUID, Episode> Active = new ConcurrentHashMap<>();

    private Scp055Episodes() {
    }

    public static boolean isActive(Player User) {
        return Active.containsKey(User.getUUID());
    }

    public static boolean canBegin(Player User) {
        return User.isAlive() && !User.isSpectator() && !isActive(User) && !User.getCooldowns().isOnCooldown(Scp055Registry.CaseItem.get());
    }

    public static boolean begin(ServerPlayer User) {
        if (!canBegin(User)) {
            return false;
        }
        long Seed = User.getRandom().nextLong();
        RandomSource Random = RandomSource.create(Seed);
        int Budget = MinWalk + Random.nextInt(MaxWalk - MinWalk + 1);
        List<Scp055Step> Steps = Scp055Wander.plan(User.serverLevel(), User, Random, Budget);
        Episode Each = new Episode(User.level().dimension(), Scp055Step.total(Steps), clickAge(Steps));
        Active.put(User.getUUID(), Each);
        User.stopUsingItem();
        User.setSprinting(false);
        User.level().playSound(null, User.getX(), User.getY() + 0.8, User.getZ(), Scp055Registry.Clasp.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        PacketDistributor.sendToPlayer(User, new Scp055EpisodePayload(Seed, DistortTicks, StaticTicks, Steps));
        return true;
    }

    private static int clickAge(List<Scp055Step> Steps) {
        int Start = DistortTicks;
        for (Scp055Step Each : Steps) {
            if (Each.Kind() == Scp055Step.Fiddle) {
                return Start + Math.min(FiddleClick, Each.Ticks() - 1);
            }
            Start += Each.Ticks();
        }
        return -1;
    }

    public static void tick(ServerPlayer User) {
        Episode Each = Active.get(User.getUUID());
        if (Each == null) {
            return;
        }
        if (!User.isAlive() || User.level().dimension() != Each.Dimension) {
            end(User, Each, true);
            return;
        }
        Each.Age++;
        if (User.isInLava() || User.isOnFire() || User.fallDistance > SafeFall) {
            end(User, Each, true);
            return;
        }
        if (Each.Age == Each.ClickAge) {
            User.swing(InteractionHand.MAIN_HAND, true);
            User.level().playSound(null, User.getX(), User.getY() + 0.6, User.getZ(), Scp055Registry.Clasp.get(), SoundSource.PLAYERS, 0.55f, 0.9f);
        }
        if (Each.Age >= DistortTicks + Each.Walk + StaticTicks) {
            end(User, Each, false);
        }
    }

    public static void interrupt(Player User) {
        Episode Each = Active.get(User.getUUID());
        if (Each != null && User instanceof ServerPlayer Viewer) {
            end(Viewer, Each, true);
        }
    }

    private static void end(ServerPlayer User, Episode Each, boolean Interrupted) {
        if (Active.remove(User.getUUID()) == null) {
            return;
        }
        if (User.isAlive()) {
            if (!Interrupted) {
                RandomSource Random = User.getRandom();
                float Yaw = Random.nextFloat() * 360.0f - 180.0f;
                float Pitch = WakePitchLow + Random.nextFloat() * WakePitchSpread;
                User.connection.teleport(User.getX(), User.getY(), User.getZ(), Yaw, Pitch, EnumSet.of(RelativeMovement.X, RelativeMovement.Y, RelativeMovement.Z));
            }
            User.displayClientMessage(Component.translatable(MessageKey).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            User.getCooldowns().addCooldown(Scp055Registry.CaseItem.get(), CooldownTicks);
        }
        PacketDistributor.sendToPlayer(User, new Scp055WakePayload(Interrupted));
    }

    public static void forget(Player User) {
        Active.remove(User.getUUID());
    }

    public static void clear() {
        Active.clear();
    }

    private static final class Episode {
        private final ResourceKey<Level> Dimension;
        private final int Walk;
        private final int ClickAge;
        private int Age;

        private Episode(ResourceKey<Level> Dimension, int Walk, int ClickAge) {
            this.Dimension = Dimension;
            this.Walk = Walk;
            this.ClickAge = ClickAge;
        }
    }
}
