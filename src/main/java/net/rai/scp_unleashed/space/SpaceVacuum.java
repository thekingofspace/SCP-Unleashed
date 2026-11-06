package net.rai.scp_unleashed.space;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearSounds;
import net.rai.scp_unleashed.notice.Notice;
import net.rai.scp_unleashed.notice.NoticeIcon;
import net.rai.scp_unleashed.notice.Notices;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SpaceVacuum {
    public static final int BreathTicks = 20;
    public static final int WarningTicks = 60;
    public static final int DamageInterval = 20;
    public static final float ExposureDamage = 5.0F;
    public static final int LowAir = AirCanisterItem.Capacity / 4;
    public static final int CriticalAir = AirCanisterItem.Capacity / 10;
    public static final int AlarmInterval = 5;
    public static final int WarnNone = 0;
    public static final int WarnLow = 1;
    public static final int WarnCritical = 2;
    public static final int WarnEmpty = 3;
    public static final NoticeIcon AirIcon = NoticeIcon.texture(SpaceRegistry.location("textures/gui/space/notice_air.png"));
    private static final Map<UUID, Breather> Players = new HashMap<>();
    private static final Map<LivingEntity, Integer> Creatures = new WeakHashMap<>();

    private SpaceVacuum() {
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            tickPlayer(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof Mob Creature) || !(Creature.level() instanceof ServerLevel World) || Creature.tickCount % DamageInterval != 0) {
            return;
        }
        if (!SpaceAir.isAirless(World) || !Creature.isAlive() || !susceptible(Creature)) {
            Creatures.remove(Creature);
            return;
        }
        if (SpaceAir.inPressurizedVehicle(Creature) || SpaceAir.sheltered(Creature)) {
            Creatures.remove(Creature);
            return;
        }
        if (SpaceAir.breathing(Creature)) {
            Creatures.remove(Creature);
            SpaceAir.drain(Creature, 1);
            return;
        }
        int Seconds = Creatures.merge(Creature, 1, Integer::sum);
        if (Seconds * DamageInterval > WarningTicks) {
            Creature.hurt(World.damageSources().source(SpaceRegistry.VacuumDamage), ExposureDamage);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Players.remove(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Players.clear();
        Creatures.clear();
    }

    public static boolean susceptible(LivingEntity Target) {
        if (Target.getType().is(SpaceTags.VacuumImmune)) {
            return false;
        }
        if (Target instanceof Player || ControlBodies.isPersonLike(Target)) {
            return true;
        }
        if (Target.getType().is(EntityTypeTags.UNDEAD)) {
            return false;
        }
        return !Scp_unleashed.ModId.equals(BuiltInRegistries.ENTITY_TYPE.getKey(Target.getType()).getNamespace());
    }

    public static int exposure(Player User) {
        Breather State = Players.get(User.getUUID());
        return State == null ? 0 : State.Exposure;
    }

    public static void tickPlayer(ServerPlayer User) {
        Breather State = Players.computeIfAbsent(User.getUUID(), Id -> new Breather());
        boolean Sealed = SpaceAir.wearsFullSuit(User);
        if (Sealed && !State.Sealed && User.isAlive()) {
            play(User, SpaceRegistry.SuitSeal.get(), 0.9F, 1.0F);
            play(User, GearSounds.HazmatHiss.get(), 0.7F, 1.1F);
        }
        State.Sealed = Sealed;
        ServerLevel World = User.serverLevel();
        if (!SpaceAir.isAirless(World) || User.isCreative() || User.isSpectator() || !User.isAlive() || !susceptible(User)) {
            State.Exposure = 0;
            return;
        }
        if (SpaceAir.inPressurizedVehicle(User) || SpaceAir.sheltered(User)) {
            State.Exposure = 0;
            return;
        }
        if (SpaceAir.breathing(User)) {
            State.Exposure = 0;
            if (++State.Breath >= BreathTicks) {
                State.Breath = 0;
                SpaceAir.drain(User, 1);
                warn(User, State, SpaceAir.suitAir(User));
            }
            return;
        }
        expose(User, World, State);
    }

    private static void warn(ServerPlayer User, Breather State, int Air) {
        int Level = Air <= 0 ? WarnEmpty : Air <= CriticalAir ? WarnCritical : Air <= LowAir ? WarnLow : WarnNone;
        if (Level < State.Warned) {
            State.Warned = Level;
            return;
        }
        if (Level > State.Warned) {
            State.Warned = Level;
            switch (Level) {
                case WarnLow -> alert(User, "low", SpaceRegistry.AirBeep.get(), ChatFormatting.GOLD);
                case WarnCritical -> {
                    alert(User, "critical", SpaceRegistry.AirAlarm.get(), ChatFormatting.RED);
                    play(User, SpaceRegistry.AirBeep.get(), 0.8F, 1.0F);
                }
                case WarnEmpty -> {
                    alert(User, "empty", SpaceRegistry.AirAlarm.get(), ChatFormatting.DARK_RED);
                    play(User, SpaceRegistry.AirBeep.get(), 0.8F, 0.8F);
                }
                default -> {
                }
            }
            return;
        }
        if (Level == WarnCritical && Air % AlarmInterval == 0) {
            play(User, SpaceRegistry.AirBeep.get(), 0.6F, 1.25F);
        }
    }

    private static void alert(ServerPlayer User, String Key, SoundEvent Cue, ChatFormatting Colour) {
        play(User, Cue, 1.0F, 1.0F);
        Notices.send(User, Notice.alert(Component.translatable("notice.scp_unleashed.space.air_" + Key).withStyle(Colour))
                .withDetail(Component.translatable("notice.scp_unleashed.space.air_" + Key + ".detail"))
                .withIcon(AirIcon));
    }

    private static void expose(ServerPlayer User, ServerLevel World, Breather State) {
        State.Exposure++;
        State.Breath = 0;
        if (State.Exposure == 1) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.space.no_air").withStyle(ChatFormatting.RED), true);
            Notices.send(User, Notice.alert(Component.translatable("notice.scp_unleashed.space.vacuum").withStyle(ChatFormatting.RED))
                    .withDetail(Component.translatable("notice.scp_unleashed.space.vacuum.detail"))
                    .withIcon(AirIcon));
        }
        if (State.Exposure % DamageInterval != 1) {
            return;
        }
        play(User, SpaceRegistry.VacuumGasp.get(), 1.0F, 0.9F + User.getRandom().nextFloat() * 0.2F);
        User.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, false, true));
        User.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 1, false, false, true));
        User.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false, true));
        if (State.Exposure > WarningTicks) {
            User.hurt(World.damageSources().source(SpaceRegistry.VacuumDamage), ExposureDamage);
        }
    }

    private static void play(ServerPlayer User, SoundEvent Cue, float Volume, float Pitch) {
        User.playNotifySound(Cue, SoundSource.PLAYERS, Volume, Pitch);
    }

    private static final class Breather {
        private int Exposure;
        private int Breath;
        private int Warned;
        private boolean Sealed = true;
    }
}
