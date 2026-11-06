package net.rai.scp_unleashed.scp079;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.cameras.CameraViews;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp079.network.Scp079LurchPayload;
import net.rai.scp_unleashed.scp268.Scp268;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp079Exposure {
    public static final float MaxExposure = 100.0f;
    public static final float WhisperStage = 15.0f;
    public static final float SicknessStage = 35.0f;
    public static final float HarmStage = 60.0f;
    public static final float TurnStage = 85.0f;
    public static final double HearingRange = 10.0;
    public static final double ClearRange = 3.0;
    public static final double ScreenRange = 12.0;
    public static final double ScreenCone = 0.94;
    public static final float SpeakerGain = 8.0f;
    public static final float ScreenGain = 12.0f;
    public static final float MuffledShare = 0.5f;
    public static final float FarShare = 0.3f;
    public static final float CycleCap = 10.0f;
    public static final int Cycle = 10;
    public static final int DecayDelay = 200;
    public static final float DecayPerSecond = 1.0f;
    public static final float ShieldedDecayPerSecond = 4.0f;
    public static final float HarmDamage = 1.5f;
    public static final int HarmInterval = 100;
    public static final int WhisperMin = 120;
    public static final int WhisperMax = 240;
    public static final int SicknessMin = 160;
    public static final int SicknessMax = 280;
    public static final int NauseaTicks = 140;
    public static final int DarknessTicks = 40;
    public static final int TurnMin = 80;
    public static final int TurnMax = 140;
    public static final double AllyReach = 3.5;
    public static final double FrenzyRange = 10.0;
    public static final int FrenzyTicks = 120;
    public static final int FrenzySwing = 20;
    public static final double FrenzyStrike = 2.0;
    private static final Map<ServerLevel, List<Emitter>> Emitters = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<UUID>> Afflicted = new WeakHashMap<>();
    private static final Map<UUID, Victim> Victims = new HashMap<>();
    private static final Map<UUID, Frenzy> Frenzies = new HashMap<>();

    private Scp079Exposure() {
    }

    public static void addSpeaker(ServerLevel World, BlockPos Owner, Vec3 Mouth, long Until) {
        add(World, new Emitter(Owner.immutable(), Mouth, null, Until));
    }

    public static void addScreen(ServerLevel World, BlockPos Owner, Vec3 Centre, Direction Front, long Until) {
        add(World, new Emitter(Owner.immutable(), Centre, new Vec3(Front.getStepX(), 0.0, Front.getStepZ()), Until));
    }

    private static void add(ServerLevel World, Emitter Entry) {
        List<Emitter> Active = Emitters.computeIfAbsent(World, Key -> new ArrayList<>());
        Active.removeIf(Other -> Other.Owner().equals(Entry.Owner()) && (Other.Normal() == null) == (Entry.Normal() == null));
        Active.add(Entry);
    }

    public static float exposure(Entity Target) {
        return Target.hasData(Scp079Registry.Exposure) ? Target.getData(Scp079Registry.Exposure) : 0.0f;
    }

    public static int stage(float Value) {
        if (Value >= TurnStage) {
            return 4;
        }
        if (Value >= HarmStage) {
            return 3;
        }
        if (Value >= SicknessStage) {
            return 2;
        }
        return Value >= WhisperStage ? 1 : 0;
    }

    public static void set(LivingEntity Target, float Value) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return;
        }
        float Clamped = Mth.clamp(Value, 0.0f, MaxExposure);
        if (Clamped <= 0.0f) {
            Target.removeData(Scp079Registry.Exposure);
            Victims.remove(Target.getUUID());
            return;
        }
        Target.setData(Scp079Registry.Exposure, Clamped);
        Afflicted.computeIfAbsent(World, Key -> new HashSet<>()).add(Target.getUUID());
        victim(Target.getUUID()).LastExposed = World.getGameTime();
    }

    public static boolean isSubject(Entity Target) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Living.isSpectator() || !Targeting.canWitness(Living)) {
            return false;
        }
        if (Living instanceof Player User) {
            return !User.isCreative() && !Scp268.isNeglected(User);
        }
        return Living instanceof Mob && ControlBodies.isPersonLike(Living);
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getEntity().hasData(Scp079Registry.Exposure)) {
            Afflicted.computeIfAbsent(World, Key -> new HashSet<>()).add(Event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (!(Event.getLevel() instanceof ServerLevel World)) {
            return;
        }
        long Now = World.getGameTime();
        tickFrenzies(World, Now);
        if (Now % Cycle != 0) {
            return;
        }
        List<Emitter> Active = Emitters.get(World);
        if (Active != null) {
            Active.removeIf(Entry -> Now >= Entry.Until());
            if (!Active.isEmpty()) {
                expose(World, Active, Now);
            }
        }
        Set<UUID> Ids = Afflicted.get(World);
        if (Ids == null || Ids.isEmpty()) {
            return;
        }
        Iterator<UUID> Each = Ids.iterator();
        while (Each.hasNext()) {
            UUID Id = Each.next();
            Entity Found = World.getEntity(Id);
            if (!(Found instanceof LivingEntity Target) || !Target.isAlive()) {
                Victims.remove(Id);
                Each.remove();
                continue;
            }
            if (!Target.hasData(Scp079Registry.Exposure)) {
                Victims.remove(Id);
                Each.remove();
                continue;
            }
            if (!afflict(World, Target, Now)) {
                Each.remove();
            }
        }
    }

    private static void expose(ServerLevel World, List<Emitter> Active, long Now) {
        Map<LivingEntity, Float> Gains = new HashMap<>();
        float Seconds = Cycle / 20.0f;
        for (Emitter Entry : Active) {
            boolean Screen = Entry.Normal() != null;
            double Range = Screen ? ScreenRange : HearingRange;
            AABB Area = new AABB(Entry.Pos(), Entry.Pos()).inflate(Range);
            for (LivingEntity Target : CameraViews.withViewers(World, Area, World.getEntitiesOfClass(LivingEntity.class, Area, Scp079Exposure::isSubject), LivingEntity.class, Scp079Exposure::isSubject)) {
                float Gain = Screen ? screenGain(World, Entry, Target) : speakerGain(World, Entry, Target);
                if (Gain > 0.0f) {
                    Gains.merge(Target, Gain * Seconds, Float::sum);
                }
            }
        }
        Gains.forEach((Target, Gain) -> {
            if (AntiMemetic.isProtected(Target)) {
                return;
            }
            set(Target, exposure(Target) + Math.min(CycleCap, Gain));
        });
    }

    private static float speakerGain(ServerLevel World, Emitter Entry, LivingEntity Target) {
        Vec3 Eye = Target.getEyePosition();
        double Distance = Eye.distanceTo(Entry.Pos());
        if (Distance > HearingRange || Senses.isDeafened(Target)) {
            return 0.0f;
        }
        float Share = Distance <= ClearRange ? 1.0f : (float) Mth.lerp((Distance - ClearRange) / (HearingRange - ClearRange), 1.0, FarShare);
        if (!clear(World, Eye, Entry.Pos(), Target)) {
            Share *= MuffledShare;
        }
        return SpeakerGain * Share;
    }

    private static float screenGain(ServerLevel World, Emitter Entry, LivingEntity Target) {
        Vec3 Eye = CameraViews.eye(Target);
        Vec3 Towards = Entry.Pos().subtract(Eye);
        double Distance = Towards.length();
        if (Distance > ScreenRange || Distance < 1.0E-3 || Senses.isBlindfolded(Target)) {
            return 0.0f;
        }
        Vec3 Heading = Towards.scale(1.0 / Distance);
        if (Entry.Normal().dot(Heading.scale(-1.0)) < 0.2 || CameraViews.look(Target).dot(Heading) < ScreenCone) {
            return 0.0f;
        }
        if (!clear(World, Eye, Entry.Pos().add(Entry.Normal().scale(0.2)), Target)) {
            return 0.0f;
        }
        return ScreenGain * (float) (1.0 - 0.5 * Distance / ScreenRange);
    }

    private static boolean clear(ServerLevel World, Vec3 From, Vec3 To, Entity Viewer) {
        Vec3 Short = To.add(From.subtract(To).normalize().scale(0.35));
        return World.clip(new ClipContext(From, Short, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Viewer)).getType() == HitResult.Type.MISS;
    }

    private static boolean afflict(ServerLevel World, LivingEntity Target, long Now) {
        Victim State = victim(Target.getUUID());
        float Value = exposure(Target);
        boolean Shielded = AntiMemetic.isProtected(Target);
        if (Shielded || Now - State.LastExposed > DecayDelay) {
            Value -= (Shielded ? ShieldedDecayPerSecond : DecayPerSecond) * Cycle / 20.0f;
        }
        if (Value <= 0.0f) {
            Target.removeData(Scp079Registry.Exposure);
            Victims.remove(Target.getUUID());
            return false;
        }
        Target.setData(Scp079Registry.Exposure, Value);
        if (Shielded || !isSubject(Target)) {
            return true;
        }
        int Stage = stage(Value);
        RandomSource Random = World.getRandom();
        if (Stage >= 1 && Now >= State.NextWhisper) {
            State.NextWhisper = Now + Mth.nextInt(Random, WhisperMin, WhisperMax) / Math.max(1, Stage - 1);
            whisper(World, Target, Random);
        }
        if (Stage >= 2 && Now >= State.NextSickness) {
            State.NextSickness = Now + Mth.nextInt(Random, SicknessMin, SicknessMax);
            if (Target instanceof Player) {
                Target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NauseaTicks, 0, false, false, false));
                Target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DarknessTicks, 0, false, false, false));
            } else {
                Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NauseaTicks, 0, false, false, false));
            }
        }
        if (Stage >= 3 && Now >= State.NextHarm) {
            if (State.NextHarm == 0L) {
                State.NextHarm = Now + HarmInterval / 2;
            } else {
                State.NextHarm = Now + HarmInterval;
                Target.hurt(damage(World), HarmDamage);
            }
        }
        if (Stage >= 4 && Now >= State.NextTurn) {
            boolean Acted = Target instanceof ServerPlayer User ? lurch(World, User) : frenzy(World, Target, Now);
            State.NextTurn = Now + (Acted ? Mth.nextInt(Random, TurnMin, TurnMax) : Cycle * 2);
        }
        return true;
    }

    public static DamageSource damage(ServerLevel World) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp079Registry.AntiMemeticDamage));
    }

    private static void whisper(ServerLevel World, LivingEntity Target, RandomSource Random) {
        if (!(Target instanceof ServerPlayer User)) {
            if (Target instanceof Mob Body) {
                Body.getLookControl().setLookAt(Body.getX() + Random.nextGaussian() * 4.0, Body.getEyeY(), Body.getZ() + Random.nextGaussian() * 4.0);
            }
            return;
        }
        double Angle = Random.nextDouble() * Math.PI * 2.0;
        double X = User.getX() + Math.cos(Angle) * 1.6;
        double Z = User.getZ() + Math.sin(Angle) * 1.6;
        double Y = User.getEyeY() + (Random.nextDouble() - 0.5) * 0.6;
        long Seed = Random.nextLong();
        send(User, Scp079Registry.Whisper.get(), X, Y, Z, 0.7f, 0.85f + Random.nextFloat() * 0.25f, Seed);
        send(User, Scp079Registry.WhisperLow.get(), X, Y, Z, 0.5f, 0.75f + Random.nextFloat() * 0.15f, Seed + 1L);
    }

    private static void send(ServerPlayer User, SoundEvent Sound, double X, double Y, double Z, float Volume, float Pitch, long Seed) {
        User.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(Sound), SoundSource.AMBIENT, X, Y, Z, Volume, Pitch, Seed));
    }

    private static boolean lurch(ServerLevel World, ServerPlayer User) {
        LivingEntity Ally = nearestAlly(World, User);
        if (Ally == null) {
            return false;
        }
        User.swing(InteractionHand.MAIN_HAND, true);
        User.attack(Ally);
        User.resetAttackStrengthTicker();
        PacketDistributor.sendToPlayer(User, new Scp079LurchPayload(Ally.getId()));
        User.displayClientMessage(Component.translatable("message.scp_unleashed.scp079.lurch"), true);
        return true;
    }

    @Nullable
    private static LivingEntity nearestAlly(ServerLevel World, ServerPlayer User) {
        AABB Area = User.getBoundingBox().inflate(AllyReach);
        LivingEntity Best = null;
        double BestDistance = AllyReach * AllyReach;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Area, Other -> Other != User && Other.isAlive() && !Other.isSpectator() && Targeting.isTargetable(Other))) {
            boolean Ally = Candidate instanceof Player Other ? !Other.isCreative() && !Scp268.isNeglected(Other) : ControlBodies.isPersonLike(Candidate)
                    || Candidate instanceof OwnableEntity Pet && User.getUUID().equals(Pet.getOwnerUUID());
            if (!Ally) {
                continue;
            }
            double Distance = User.getEyePosition().distanceToSqr(Candidate.getBoundingBox().getCenter());
            if (Distance < BestDistance && User.hasLineOfSight(Candidate)) {
                Best = Candidate;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    private static boolean frenzy(ServerLevel World, LivingEntity Target, long Now) {
        if (!(Target instanceof PathfinderMob Body)) {
            return false;
        }
        AABB Area = Body.getBoundingBox().inflate(FrenzyRange);
        LivingEntity Best = null;
        double BestDistance = FrenzyRange * FrenzyRange;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Area, Other -> Other != Body && Other.isAlive() && !Other.isSpectator() && Targeting.isTargetable(Other))) {
            boolean Person = Candidate instanceof Player Other ? !Other.isCreative() && !Scp268.isNeglected(Other) : ControlBodies.isPersonLike(Candidate);
            if (!Person) {
                continue;
            }
            double Distance = Body.distanceToSqr(Candidate);
            if (Distance < BestDistance && Body.hasLineOfSight(Candidate)) {
                Best = Candidate;
                BestDistance = Distance;
            }
        }
        if (Best == null) {
            return false;
        }
        Frenzies.put(Body.getUUID(), new Frenzy(Best.getUUID(), Now + FrenzyTicks));
        Body.setTarget(Best);
        return true;
    }

    private static void tickFrenzies(ServerLevel World, long Now) {
        if (Frenzies.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Frenzy>> Each = Frenzies.entrySet().iterator();
        while (Each.hasNext()) {
            Map.Entry<UUID, Frenzy> Entry = Each.next();
            Entity Found = World.getEntity(Entry.getKey());
            if (Found == null) {
                continue;
            }
            Frenzy State = Entry.getValue();
            Entity Hunted = World.getEntity(State.Target);
            if (!(Found instanceof PathfinderMob Body) || !Body.isAlive() || !Targeting.isTargetable(Body) || !(Hunted instanceof LivingEntity Prey) || !Prey.isAlive() || Now >= State.Until
                    || Prey.distanceToSqr(Body) > FrenzyRange * FrenzyRange * 4.0 || Scp268.isNeglected(Prey) || !Targeting.isTargetable(Prey)) {
                if (Found instanceof Mob Stopped && Hunted != null && Stopped.getTarget() == Hunted) {
                    Stopped.setTarget(null);
                }
                Each.remove();
                continue;
            }
            Body.getLookControl().setLookAt(Prey, 30.0f, 30.0f);
            if (Body.distanceToSqr(Prey) > FrenzyStrike * FrenzyStrike) {
                if (Now % 5 == 0) {
                    Body.getNavigation().moveTo(Prey, 1.2);
                }
            } else if (Now >= State.NextSwing) {
                State.NextSwing = Now + FrenzySwing;
                Body.getNavigation().stop();
                Body.swing(InteractionHand.MAIN_HAND);
                Body.doHurtTarget(Prey);
            }
        }
    }

    public static void clear(LivingEntity Target) {
        Target.removeData(Scp079Registry.Exposure);
        Victims.remove(Target.getUUID());
        Frenzies.remove(Target.getUUID());
    }

    public static boolean calm(LivingEntity Target) {
        Frenzy State = Frenzies.remove(Target.getUUID());
        if (State == null) {
            return false;
        }
        if (Target instanceof Mob Body && Body.getTarget() != null && State.Target.equals(Body.getTarget().getUUID())) {
            Body.setTarget(null);
        }
        return true;
    }

    private static Victim victim(UUID Id) {
        return Victims.computeIfAbsent(Id, Key -> new Victim());
    }

    private record Emitter(BlockPos Owner, Vec3 Pos, @Nullable Vec3 Normal, long Until) {
    }

    private static final class Victim {
        private long LastExposed;
        private long NextWhisper;
        private long NextSickness;
        private long NextHarm;
        private long NextTurn;
    }

    private static final class Frenzy {
        private final UUID Target;
        private final long Until;
        private long NextSwing;

        private Frenzy(UUID Target, long Until) {
            this.Target = Target;
            this.Until = Until;
        }
    }
}
