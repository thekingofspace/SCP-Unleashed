package net.rai.scp_unleashed.ultralaser;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.guns.GunItem;
import net.rai.scp_unleashed.guns.GunTimer;
import net.rai.scp_unleashed.guns.GunsRegistry;
import net.rai.scp_unleashed.ultralaser.network.UltralaserPrimingPayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class UltralaserPriming {
    public static final String MessagePrefix = "message.scp_unleashed.ultralaser.";
    public static final int CooldownTicks = 200;
    public static final int VentTicks = 76;
    public static final int VentSoundDelay = 16;
    public static final float InterruptDamage = 6.0F;
    public static final double HearingRange = UltralaserRegistry.PrimingRange + 8.0;
    public static final double NanosPerTick = 5.0E7;
    private static final Map<UUID, Session> Sessions = new HashMap<>();
    private static final Map<UUID, Long> Vents = new HashMap<>();

    private UltralaserPriming() {
    }

    private static final class Session {
        private final long Start;
        private final long StartNanos;

        private Session(long Start, long StartNanos) {
            this.Start = Start;
            this.StartNanos = StartNanos;
        }

        private double clipTicks() {
            return (System.nanoTime() - StartNanos) / NanosPerTick;
        }
    }

    public static boolean owns(ServerPlayer User, GunTimer Charging) {
        Session Active = Sessions.get(User.getUUID());
        return Active != null && Active.Start == Charging.Start();
    }

    public static boolean isPriming(ServerPlayer User) {
        return Sessions.containsKey(User.getUUID());
    }

    public static void press(ServerPlayer User) {
        ItemStack Stack = User.getMainHandItem();
        if (!UltralaserItem.isUltralaser(Stack) || User.isSpectator() || !User.isAlive() || isPriming(User) || GunItem.isBusy(Stack) || GunItem.getAmmo(Stack) <= 0) {
            return;
        }
        if (User.getCooldowns().isOnCooldown(Stack.getItem())) {
            User.displayClientMessage(Component.translatable(MessagePrefix + "venting").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return;
        }
        ServerLevel World = User.serverLevel();
        long Now = World.getGameTime();
        Session Active = new Session(Now, System.nanoTime());
        Sessions.put(User.getUUID(), Active);
        Stack.set(GunsRegistry.Charging.get(), new GunTimer(Now, Now + UltralaserScore.PrimeTicks));
        User.setSprinting(false);
        UltralaserSounds.play(World, User.getEyePosition(), UltralaserSounds.PowerUp);
        broadcast(User, true);
    }

    public static void release(ServerPlayer User) {
        stop(User, "cancelled");
    }

    public static void interrupt(ServerPlayer User, float Damage) {
        if (Damage >= InterruptDamage && isPriming(User)) {
            stop(User, "interrupted");
        }
    }

    public static void forget(UUID Id) {
        Sessions.remove(Id);
        Vents.remove(Id);
    }

    public static void forgetAll() {
        Sessions.clear();
        Vents.clear();
    }

    public static void tick(ServerPlayer User) {
        Session Active = Sessions.get(User.getUUID());
        if (Active != null) {
            ItemStack Stack = User.getMainHandItem();
            GunTimer Charging = UltralaserItem.isUltralaser(Stack) ? GunItem.getCharging(Stack) : null;
            if (Charging == null || Charging.Start() != Active.Start || !User.isAlive() || User.isSpectator() || User.isSleeping()) {
                stop(User, "cancelled");
            } else {
                long Elapsed = User.serverLevel().getGameTime() - Active.Start;
                if (Elapsed >= 1 && Active.clipTicks() >= UltralaserScore.fireTicks()) {
                    Sessions.remove(User.getUUID());
                    if (UltralaserBlast.fire(User, Stack)) {
                        Vents.put(User.getUUID(), User.serverLevel().getGameTime());
                    }
                }
            }
        }
        vent(User);
    }

    public static MutableComponent cue(UltralaserScore.Cue Entry) {
        MutableComponent Text = Component.translatable(MessagePrefix + Entry.Key());
        if (!Entry.Loud()) {
            return Text.withStyle(ChatFormatting.AQUA);
        }
        boolean Pink = Entry.Key().equals("two") || Entry.Key().equals("ready") || Entry.Key().equals("fire");
        return Text.withStyle(Pink ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    private static void stop(ServerPlayer User, String Reason) {
        Session Removed = Sessions.remove(User.getUUID());
        if (Removed == null) {
            return;
        }
        ItemStack Stack = User.getMainHandItem();
        GunTimer Charging = UltralaserItem.isUltralaser(Stack) ? GunItem.getCharging(Stack) : null;
        if (Charging != null && Charging.Start() == Removed.Start) {
            Stack.remove(GunsRegistry.Charging.get());
        }
        broadcast(User, false);
        UltralaserSounds.play(User.serverLevel(), User.getEyePosition(), UltralaserSounds.PowerDown);
        User.displayClientMessage(Component.translatable(MessagePrefix + Reason).withStyle(ChatFormatting.GRAY), true);
    }

    private static void broadcast(ServerPlayer User, boolean Playing) {
        UltralaserPrimingPayload Payload = new UltralaserPrimingPayload(User.getId(), Playing);
        for (ServerPlayer Listener : User.serverLevel().players()) {
            if (Listener == User || Listener.distanceToSqr(User) <= HearingRange * HearingRange) {
                PacketDistributor.sendToPlayer(Listener, Payload);
            }
        }
    }

    private static void vent(ServerPlayer User) {
        Long Fired = Vents.get(User.getUUID());
        if (Fired == null) {
            return;
        }
        ServerLevel World = User.serverLevel();
        long Elapsed = World.getGameTime() - Fired;
        ItemStack Stack = User.getMainHandItem();
        boolean Holding = UltralaserItem.isUltralaser(Stack);
        if (Elapsed < 0 || !User.getCooldowns().isOnCooldown(UltralaserRegistry.Ultralaser.get())) {
            Vents.remove(User.getUUID());
            if (Holding && User.isAlive()) {
                UltralaserSounds.ready(World, User.getEyePosition());
                User.displayClientMessage(Component.translatable(MessagePrefix + "vented").withStyle(ChatFormatting.AQUA), true);
            }
            return;
        }
        if (!Holding || Elapsed >= VentTicks) {
            return;
        }
        Vec3 Port = ventPort(User);
        if (Elapsed == VentSoundDelay) {
            UltralaserSounds.play(World, Port, UltralaserSounds.Vent);
        }
        if (Elapsed >= VentSoundDelay && Elapsed % 2 == 0) {
            float Strength = 1.0F - (float) Elapsed / VentTicks;
            Vec3 Up = new Vec3(0.0, 0.05 + 0.05 * Strength, 0.0);
            World.sendParticles(ParticleTypes.WHITE_SMOKE, Port.x, Port.y, Port.z, 1 + Math.round(3 * Strength), 0.05, 0.03, 0.05, 0.02);
            World.sendParticles(ParticleTypes.CLOUD, Port.x, Port.y + 0.05, Port.z, 0, Up.x, Up.y, Up.z, 1.0);
            if (Elapsed % 6 == 0) {
                World.sendParticles(UltralaserRegistry.MotePink.get(), Port.x, Port.y, Port.z, 2, 0.06, 0.04, 0.06, 0.02);
            }
        }
    }

    public static Vec3 ventPort(ServerPlayer User) {
        Vec3 Look = User.getLookAngle();
        Vec3 Flat = new Vec3(Look.x, 0.0, Look.z);
        Flat = Flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : Flat.normalize();
        Vec3 Right = new Vec3(-Flat.z, 0.0, Flat.x);
        double Side = User.getMainArm() == HumanoidArm.RIGHT ? 0.34 : -0.34;
        return User.getEyePosition().add(Look.scale(0.55)).add(Right.scale(Side)).add(0.0, -0.18, 0.0);
    }
}
