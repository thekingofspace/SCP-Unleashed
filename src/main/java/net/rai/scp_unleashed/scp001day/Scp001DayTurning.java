package net.rai.scp_unleashed.scp001day;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFlesh;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayForm;
import net.rai.scp_unleashed.scp001day.entity.Scp001DayFusion;
import net.rai.scp_unleashed.scp001day.network.Scp001DayTurningPayload;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001DayTurning {
    public static final String Key = "scp_unleashed_scp001_day_turning";
    public static final int Duration = 320;
    public static final int SyncTicks = 10;
    public static final double SlowStart = 0.12;
    public static final double SlowRange = 0.5;
    public static final double StumbleOdds = 0.012;
    public static final double StumbleGrowth = 0.05;
    public static final double StumbleStart = 0.12;
    public static final double StumbleRange = 0.3;
    public static final int WetStart = 40;
    public static final int WetEnd = 12;
    public static final int GroanStart = 110;
    public static final int GroanEnd = 40;
    public static final ResourceLocation SlowId = Scp001DayRegistry.location("scp001_day_turning");
    private static final DustParticleOptions Melt = new DustParticleOptions(new Vector3f(0.46f, 0.16f, 0.14f), 1.1f);

    private Scp001DayTurning() {
    }

    public static boolean turning(Player User) {
        return User.getPersistentData().contains(Key);
    }

    public static int ticks(Player User) {
        return User.getPersistentData().getInt(Key);
    }

    public static float progress(Player User) {
        return turning(User) ? Math.min(1.0f, ticks(User) / (float) Duration) : 0.0f;
    }

    public static boolean exempt(Player User) {
        return User.isCreative() || User.isSpectator();
    }

    public static void begin(ServerLevel World, ServerPlayer User) {
        if (turning(User) || exempt(User) || !User.isAlive()) {
            return;
        }
        User.getPersistentData().putInt(Key, 0);
        World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp001DayRegistry.TurningStart.get(), SoundSource.PLAYERS, 1.0f, 0.9f);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp001_day.turning"), true);
        sync(User);
    }

    public static void cure(ServerPlayer User) {
        if (!turning(User)) {
            return;
        }
        User.getPersistentData().remove(Key);
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.removeModifier(SlowId);
        }
        sync(User);
    }

    public static void sync(ServerPlayer User) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, new Scp001DayTurningPayload(User.getId(), turning(User) ? progress(User) : -1.0f));
    }

    public static void tick(ServerPlayer User) {
        if (!turning(User) || !User.isAlive() || !(User.level() instanceof ServerLevel World)) {
            return;
        }
        if (exempt(User)) {
            cure(User);
            return;
        }
        int Ticks = ticks(User) + 1;
        User.getPersistentData().putInt(Key, Ticks);
        float Share = Math.min(1.0f, Ticks / (float) Duration);
        if (Ticks >= Duration) {
            complete(World, User);
            return;
        }
        if (Ticks % SyncTicks == 0) {
            sync(User);
            slow(User, Share);
        }
        if (World.random.nextDouble() < StumbleOdds + StumbleGrowth * Share) {
            double Angle = World.random.nextDouble() * Math.PI * 2.0;
            double Push = StumbleStart + StumbleRange * Share;
            User.push(Math.cos(Angle) * Push, 0.0, Math.sin(Angle) * Push);
            User.hurtMarked = true;
        }
        int Wet = (int) Math.round(WetStart + (WetEnd - WetStart) * Share);
        if (Ticks % Math.max(1, Wet) == 0) {
            World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp001DayRegistry.TurningWet.get(), SoundSource.PLAYERS, 0.5f + 0.5f * Share, 0.8f + 0.3f * World.random.nextFloat());
        }
        int Groan = (int) Math.round(GroanStart + (GroanEnd - GroanStart) * Share);
        if (Ticks % Math.max(1, Groan) == Groan / 2) {
            World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp001DayRegistry.TurningGroan.get(), SoundSource.PLAYERS, 0.7f + 0.3f * Share, 0.65f - 0.15f * Share);
        }
        if (Ticks % 5 == 0) {
            World.sendParticles(Melt, User.getX(), User.getY() + User.getBbHeight() * 0.5, User.getZ(), 1 + (int) (6 * Share), 0.25, 0.5, 0.25, 0.01);
        }
    }

    private static void slow(ServerPlayer User, float Share) {
        AttributeInstance Speed = User.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed == null) {
            return;
        }
        Speed.removeModifier(SlowId);
        Speed.addTransientModifier(new AttributeModifier(SlowId, -(SlowStart + SlowRange * Share), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void complete(ServerLevel World, ServerPlayer User) {
        DamageSource Source = World.damageSources().source(Scp001DayRegistry.SunDamage);
        World.playSound(null, User.getX(), User.getY() + 1.0, User.getZ(), Scp001DayRegistry.TurningEnd.get(), SoundSource.PLAYERS, 1.2f, 0.8f);
        User.hurt(Source, Float.MAX_VALUE);
        if (User.isAlive()) {
            User.setHealth(0.0f);
            User.die(Source);
        }
    }

    public static Scp001DayFlesh rise(ServerLevel World, ServerPlayer User) {
        Vec3 Spot = User.position();
        boolean Room = Scp001DayFusion.areaMass(World, Spot) + Scp001DayForm.Lurker.Weight <= Scp001DayFleshSpawner.NightCap;
        Scp001DayFlesh Made = Scp001DayFusion.make(World, Room ? Scp001DayForm.Lurker.Weight : Scp001DayForm.Crawler.Weight, Spot, User.getUUID());
        if (Made != null) {
            Made.carry(User.getUUID());
        }
        return Made;
    }

    public static void died(ServerPlayer User, DamageSource Source) {
        if (!turning(User)) {
            return;
        }
        boolean Admin = Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !Source.is(Scp001DayRegistry.SunDamage);
        cure(User);
        if (!Admin && User.level() instanceof ServerLevel World) {
            rise(World, User);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User && turning(User)) {
            tick(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && turning(User)) {
            sync(User);
            slow(User, progress(User));
        }
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof ServerPlayer Target && turning(Target) && Event.getEntity() instanceof ServerPlayer Watcher) {
            PacketDistributor.sendToPlayer(Watcher, new Scp001DayTurningPayload(Target.getId(), progress(Target)));
        }
    }
}
