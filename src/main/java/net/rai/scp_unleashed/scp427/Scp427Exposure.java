package net.rai.scp_unleashed.scp427;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp427Exposure {
    public static final int Limit = 6000;
    public static final int FirstWarning = 3600;
    public static final int SecondWarning = 4800;
    public static final int DecayInterval = 30;
    public static final int HealInterval = 20;
    public static final float HealAmount = 1.0f;
    public static final int CleanseInterval = 20;
    public static final int CleanseTicks = 60;
    public static final int MoteInterval = 4;
    public static final int HumInterval = 60;
    public static final int ChimeInterval = 200;
    public static final int ItchChance = 260;
    public static final int HeartbeatInterval = 24;
    public static final int SecondBeat = 6;
    public static final String Prefix = "message.scp_unleashed.scp427.";
    public static final List<Holder<MobEffect>> Cleansed = List.of(MobEffects.POISON, MobEffects.WITHER);

    private Scp427Exposure() {
    }

    public static boolean isLocket(ItemStack Stack) {
        return Scp427LocketItem.is(Stack);
    }

    public static boolean isOpen(LivingEntity Subject) {
        return Subject.isAlive() && Subject.isUsingItem() && Subject.getUsedItemHand() == InteractionHand.MAIN_HAND && isLocket(Subject.getUseItem())
                && isLocket(Subject.getMainHandItem());
    }

    public static boolean isExempt(LivingEntity Subject) {
        if (Subject instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return true;
        }
        return Subject instanceof ArmorStand || Subject instanceof Scp427Flesh || InfectionImmunity.isImmune(Subject);
    }

    @Nullable
    public static Scp427State state(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp427Registry.Exposure.get());
    }

    public static int exposure(LivingEntity Subject) {
        Scp427State State = state(Subject);
        return State == null ? 0 : State.Ticks;
    }

    public static boolean isTransforming(LivingEntity Subject) {
        Scp427State State = state(Subject);
        return State != null && State.transforming();
    }

    public static void setExposure(LivingEntity Subject, int Ticks) {
        Scp427State State = Subject.getData(Scp427Registry.Exposure.get());
        State.Ticks = Math.max(0, Ticks);
        State.Warned = Math.min(State.Warned, stageOf(State.Ticks));
        if (!State.worthSaving()) {
            Subject.removeData(Scp427Registry.Exposure.get());
        }
    }

    public static int stageOf(int Ticks) {
        if (Ticks >= SecondWarning) {
            return 2;
        }
        return Ticks >= FirstWarning ? 1 : 0;
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World)) {
            return;
        }
        Scp427State State = state(Subject);
        boolean Open = isOpen(Subject);
        if (State == null && !Open) {
            return;
        }
        if (Subject.isDeadOrDying() || Subject.isRemoved()) {
            Scp427Light.release(Subject);
            return;
        }
        if (State == null) {
            State = Subject.getData(Scp427Registry.Exposure.get());
        }
        if (State.transforming()) {
            Scp427Light.release(Subject);
            Scp427Transformation.tick(World, Subject, State);
            return;
        }
        boolean Exempt = isExempt(Subject);
        if (Open) {
            State.OpenTicks++;
            if (!Exempt) {
                State.Ticks++;
            }
            soothe(World, Subject, State);
            Scp427Light.refresh(Subject);
            if (!Exempt) {
                warn(World, Subject, State);
            }
        } else {
            State.OpenTicks = 0;
            Scp427Light.release(Subject);
            if (State.Ticks > 0 && State.Ticks <= Limit && Subject.tickCount % DecayInterval == 0) {
                State.Ticks--;
            }
            State.Warned = Math.min(State.Warned, stageOf(State.Ticks));
        }
        State.WasOpen = Open;
        if (State.Ticks > Limit && !Exempt) {
            Scp427Transformation.begin(World, Subject, State);
            return;
        }
        if (!State.worthSaving() && !Open) {
            Subject.removeData(Scp427Registry.Exposure.get());
        }
    }

    private static void soothe(ServerLevel World, LivingEntity Subject, Scp427State State) {
        int Open = State.OpenTicks;
        if (Open % HealInterval == 0 && Subject.getHealth() < Subject.getMaxHealth()) {
            Subject.heal(HealAmount);
        }
        if (Open % CleanseInterval == 0) {
            cleanse(Subject);
        }
        if (Open % MoteInterval == 0) {
            double Angle = World.getRandom().nextDouble() * Math.PI * 2.0;
            double Reach = 0.35 + World.getRandom().nextDouble() * 0.45;
            World.sendParticles(Scp427Registry.MoteParticle.get(), Subject.getX() + Math.cos(Angle) * Reach, Subject.getY() + 0.5 + World.getRandom().nextDouble() * Subject.getBbHeight() * 0.8,
                    Subject.getZ() + Math.sin(Angle) * Reach, 1, 0.02, 0.02, 0.02, 0.005);
        }
        if (Open % HumInterval == 1) {
            sound(World, Subject, Scp427Registry.Hum, 0.32f, 1.0f, false);
        }
        if (Open == 3 || Open % ChimeInterval == 0) {
            sound(World, Subject, Scp427Registry.Chime, 0.3f, 1.0f, false);
        }
    }

    public static void cleanse(LivingEntity Subject) {
        for (Holder<MobEffect> Effect : Cleansed) {
            MobEffectInstance Current = Subject.getEffect(Effect);
            if (Current == null || Current.isInfiniteDuration()) {
                continue;
            }
            int Left = Current.getDuration() - CleanseTicks;
            Subject.removeEffect(Effect);
            if (Left > 0) {
                Subject.addEffect(new MobEffectInstance(Effect, Left, Current.getAmplifier(), Current.isAmbient(), Current.isVisible(), Current.showIcon()));
            }
        }
    }

    private static void warn(ServerLevel World, LivingEntity Subject, Scp427State State) {
        int Stage = stageOf(State.Ticks);
        if (Stage > State.Warned) {
            State.Warned = Stage;
            if (Subject instanceof ServerPlayer User) {
                User.displayClientMessage(Component.translatable(Prefix + "warning." + Stage).withStyle(Stage > 1 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
        if (Stage >= 1 && World.getRandom().nextInt(ItchChance) == 0) {
            sound(World, Subject, Scp427Registry.Itch, 0.45f, 0.9f + World.getRandom().nextFloat() * 0.2f, true);
        }
        if (Stage >= 2) {
            int Beat = State.OpenTicks % HeartbeatInterval;
            if (Beat == 0) {
                sound(World, Subject, Scp427Registry.Heartbeat, 0.8f, 1.0f, true);
            } else if (Beat == SecondBeat) {
                sound(World, Subject, Scp427Registry.Heartbeat, 0.5f, 0.9f, true);
            }
        }
    }

    private static void sound(ServerLevel World, LivingEntity Subject, DeferredHolder<SoundEvent, SoundEvent> Sound, float Volume, float Pitch, boolean Private) {
        if (Private && Subject instanceof ServerPlayer User) {
            User.playNotifySound(Sound.get(), SoundSource.PLAYERS, Volume, Pitch);
            return;
        }
        World.playSound(null, Subject.getX(), Subject.getEyeY() - 0.4, Subject.getZ(), Sound.get(), SoundSource.PLAYERS, Private ? Volume * 0.5f : Volume, Pitch);
    }
}
