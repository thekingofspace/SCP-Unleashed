package net.rai.scp_unleashed.scp149;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.gear.GearTags;

import java.util.Optional;
import java.util.UUID;

public final class Scp149Host {
    public static final int Clear = 0;
    public static final int Incubating = 1;
    public static final int Bursting = 2;
    public static final int IncubationMin = 1800;
    public static final int IncubationMax = 3000;
    public static final int BroodMin = 4;
    public static final int BroodMax = 6;
    public static final int WarnTicks = 90;
    public static final int CueInterval = 10;
    public static final float BurstDamage = 12.0f;
    public static final double TremorStart = 0.03;
    public static final double TremorEnd = 0.09;
    public static final int[] Jolts = {18, 36, 50, 62, 71, 78, 83, 87};
    public static final int ItchMin = 300;
    public static final int ItchMax = 600;
    public static final int ItchLines = 4;
    public static final String Prefix = "message.scp_unleashed.scp149.";

    private Scp149Host() {
    }

    public record State(int Stage, int Ticks, int Target, int Brood, int Shed, int Next, UUID BroodId, Optional<BlockPos> Nest) {
        public static final State None = new State(Clear, 0, 0, 0, 0, 0, new UUID(0L, 0L), Optional.empty());
        public static final Codec<State> StateCodec = RecordCodecBuilder.create(Instance -> Instance.group(
                Codec.INT.optionalFieldOf("stage", Clear).forGetter(State::Stage),
                Codec.INT.optionalFieldOf("ticks", 0).forGetter(State::Ticks),
                Codec.INT.optionalFieldOf("target", IncubationMin).forGetter(State::Target),
                Codec.INT.optionalFieldOf("brood", BroodMin).forGetter(State::Brood),
                Codec.INT.optionalFieldOf("shed", 0).forGetter(State::Shed),
                Codec.INT.optionalFieldOf("next", 0).forGetter(State::Next),
                UUIDUtil.CODEC.fieldOf("brood_id").forGetter(State::BroodId),
                BlockPos.CODEC.optionalFieldOf("nest").forGetter(State::Nest)
        ).apply(Instance, State::new));

        public boolean active() {
            return Stage != Clear;
        }

        public State with(int NewStage, int NewTicks, int NewNext) {
            return new State(NewStage, NewTicks, Target, Brood, Shed, NewNext, BroodId, Nest);
        }
    }

    public static State stateOf(LivingEntity Subject) {
        State Found = Subject.getExistingDataOrNull(Scp149Registry.Host.get());
        return Found == null ? State.None : Found;
    }

    public static boolean isHost(LivingEntity Subject) {
        return stateOf(Subject).active();
    }

    public static boolean isPerson(LivingEntity Subject) {
        if (Subject instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return ControlBodies.isPersonLike(Subject) && !InfectionImmunity.isImmune(Subject);
    }

    public static boolean shielded(LivingEntity Subject) {
        return GearTags.wearsFullHazmatSuit(Subject);
    }

    public static boolean canHost(LivingEntity Subject) {
        return !Subject.isRemoved() && isPerson(Subject) && !isHost(Subject) && !InfectionImmunity.isImmune(Subject);
    }

    public static boolean infect(LivingEntity Subject) {
        if (!canHost(Subject)) {
            return false;
        }
        RandomSource Random = Subject.getRandom();
        int Target = IncubationMin + Random.nextInt(IncubationMax - IncubationMin + 1);
        int Brood = BroodMin + Random.nextInt(BroodMax - BroodMin + 1);
        store(Subject, new State(Incubating, 0, Target, Brood, 0, roll(Random, ItchMin, ItchMax), UUID.randomUUID(), Optional.empty()));
        return true;
    }

    public static boolean beginBursting(LivingEntity Subject) {
        State Current = stateOf(Subject);
        if (!Current.active()) {
            return false;
        }
        store(Subject, Current.with(Bursting, 0, 0));
        tell(Subject, "burst.warn", ChatFormatting.RED);
        return true;
    }

    public static boolean cure(LivingEntity Subject) {
        boolean Was = isHost(Subject);
        Subject.removeData(Scp149Registry.Host.get());
        return Was;
    }

    private static void store(LivingEntity Subject, State Next) {
        if (Next.active()) {
            Subject.setData(Scp149Registry.Host.get(), Next);
        } else {
            Subject.removeData(Scp149Registry.Host.get());
        }
    }

    private static int roll(RandomSource Random, int Low, int High) {
        return Low + Random.nextInt(High - Low + 1);
    }

    public static void tick(LivingEntity Subject) {
        State Current = stateOf(Subject);
        if (!Current.active() || !Subject.isAlive() || !(Subject.level() instanceof ServerLevel World)) {
            return;
        }
        if (Subject instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return;
        }
        RandomSource Random = Subject.getRandom();
        int Ticks = Current.Ticks() + 1;
        if (Current.Stage() == Incubating) {
            if (Ticks >= Current.Target()) {
                beginBursting(Subject);
                return;
            }
            int Next = Current.Next() - 1;
            if (Next <= 0) {
                itch(Subject);
                Next = roll(Random, ItchMin, ItchMax);
            }
            store(Subject, Current.with(Incubating, Ticks, Next));
            return;
        }
        if (Ticks >= WarnTicks) {
            burst(World, Subject, Current);
            return;
        }
        store(Subject, Current.with(Bursting, Ticks, 0));
        warn(World, Subject, Ticks);
    }

    private static void warn(ServerLevel World, LivingEntity Subject, int Ticks) {
        float Share = Ticks / (float) WarnTicks;
        RandomSource Random = Subject.getRandom();
        boolean Jolt = false;
        for (int At : Jolts) {
            Jolt |= At == Ticks;
        }
        if (Jolt) {
            float Yaw = Random.nextFloat() * 360.0f;
            if (Subject instanceof ServerPlayer User) {
                User.connection.send(new ClientboundHurtAnimationPacket(User.getId(), Yaw));
            } else {
                double Shake = Mth.lerp(Share, TremorStart, TremorEnd);
                Subject.push((Random.nextDouble() - 0.5) * Shake * 2.0, 0.0, (Random.nextDouble() - 0.5) * Shake * 2.0);
            }
        }
        if (Ticks % 4 == 0) {
            Vec3 Low = Subject.position().add(0.0, Subject.getBbHeight() * (0.2 + 0.4 * Share), 0.0);
            World.sendParticles(Scp149Registry.Mote.get(), Low.x, Low.y, Low.z, 1 + (int) (Share * 5.0f), Subject.getBbWidth() * 0.45, Subject.getBbHeight() * 0.25,
                    Subject.getBbWidth() * 0.45, 0.01 + Share * 0.03);
        }
        if (Ticks % CueInterval == 1) {
            float Pitch = 0.75f + Share * 0.55f;
            float Volume = 0.25f + Share * 0.75f;
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp149Registry.StirLayers) {
                World.playSound(null, Subject.getX(), Subject.getY() + 0.8, Subject.getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
            }
        }
        if (Ticks == WarnTicks / 2) {
            tell(Subject, "burst.near", ChatFormatting.DARK_RED);
        }
    }

    public static void burst(ServerLevel World, LivingEntity Subject, State Current) {
        cure(Subject);
        BlockPos Nest = Current.Nest().orElseGet(() -> Scp149Brood.nestNear(World, Subject.blockPosition(), Subject.getRandom()));
        int Born = Scp149Brood.burstFrom(World, Subject, Current.BroodId(), Nest, Mth.clamp(Current.Brood(), 1, BroodMax));
        Vec3 Middle = Subject.position().add(0.0, Subject.getBbHeight() * 0.45, 0.0);
        World.sendParticles(Scp149Registry.Mote.get(), Middle.x, Middle.y, Middle.z, 26 + Born * 4, Subject.getBbWidth() * 0.5, Subject.getBbHeight() * 0.35,
                Subject.getBbWidth() * 0.5, 0.12);
        Scp149Brood.play(World, Middle, Scp149Registry.BurstLayers, 1.15f);
        Scp149Brood.play(World, Middle, Scp149Registry.ShedLayers, 0.9f);
        tell(Subject, "burst.now", ChatFormatting.DARK_RED);
        Subject.invulnerableTime = 0;
        Subject.hurt(World.damageSources().source(Scp149Registry.BroodDamage), BurstDamage);
    }

    private static void itch(LivingEntity Subject) {
        if (Subject instanceof ServerPlayer User) {
            User.playNotifySound(Scp149Registry.Itch.get(), SoundSource.HOSTILE, 0.35f, 0.9f + User.getRandom().nextFloat() * 0.2f);
            int Line = User.getRandom().nextInt(ItchLines);
            User.displayClientMessage(Component.translatable(Prefix + "itch." + Line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    private static void tell(LivingEntity Subject, String Key, ChatFormatting Colour) {
        if (Subject instanceof ServerPlayer User) {
            User.displayClientMessage(Component.translatable(Prefix + Key).withStyle(Colour, ChatFormatting.ITALIC), true);
        }
    }

    public static void onDeath(LivingEntity Fallen) {
        State Current = stateOf(Fallen);
        if (!Current.active()) {
            return;
        }
        cure(Fallen);
        if (Current.Stage() != Bursting || !(Fallen.level() instanceof ServerLevel World)) {
            return;
        }
        BlockPos Nest = Current.Nest().orElseGet(() -> Scp149Brood.nestNear(World, Fallen.blockPosition(), Fallen.getRandom()));
        Scp149Brood.burstFrom(World, Fallen, Current.BroodId(), Nest, Mth.clamp(Current.Brood(), 1, BroodMax));
        Scp149Brood.play(World, Fallen.position().add(0.0, 0.6, 0.0), Scp149Registry.BurstLayers, 1.15f);
    }

    public static int secondsLeft(State Current) {
        if (Current.Stage() == Incubating) {
            return Math.max(0, Current.Target() - Current.Ticks()) / 20;
        }
        return Math.max(0, WarnTicks - Current.Ticks()) / 20;
    }
}
