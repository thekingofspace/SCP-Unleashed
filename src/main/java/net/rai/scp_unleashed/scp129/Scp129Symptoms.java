package net.rai.scp_unleashed.scp129;

import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.ArrayList;
import java.util.List;

public final class Scp129Symptoms {
    public static final int None = 0;
    public static final int Cough = 1;
    public static final int Blur = 2;
    public static final int Freeze = 3;
    public static final int Clear = 4;
    public static final int Gap = 160;
    public static final int CoughMean = 900;
    public static final int MildCoughMean = 1800;
    public static final int BlurMean = 1400;
    public static final int FreezeMean = 2000;
    public static final int[] CourseCoughMean = {700, 480, 320, 200};
    public static final int[] CourseBlurMean = {1300, 950, 700, 480};
    public static final int[] CourseFreezeMean = {1900, 1400, 1000, 700};
    public static final int CoughSpacing = 9;
    public static final int CoughJitter = 6;
    public static final int BlurShortest = 70;
    public static final int BlurJitter = 60;
    public static final int BlurPerStage = 20;
    public static final float BlurStagePower = 0.15f;
    public static final int FreezeShortest = 50;
    public static final int FreezeJitter = 30;
    public static final int FreezePerStage = 15;
    public static final int BlindLinger = 8;
    public static final float MildVolume = 0.5f;
    public static final float CoughVolume = 0.9f;
    public static final float HardVolume = 1.1f;
    public static final int JoltTicks = 7;
    public static final double MouthReach = 0.35;
    public static final double PuffSpeed = 0.09;
    public static final double PuffJitter = 0.025;
    public static final int MildPuff = 3;
    public static final int CoughPuff = 6;
    public static final int HardPuff = 9;
    public static final ResourceLocation FreezeModifier = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_freeze");

    private Scp129Symptoms() {
    }

    public record Episode(int Kind, long Ends, long CalmUntil, int CoughsLeft, long NextCough, float Strength) {
        public static final Episode Idle = new Episode(None, 0L, 0L, 0, 0L, 0.0f);

        public boolean active() {
            return Kind != None;
        }
    }

    public static Episode episodeOf(LivingEntity Subject) {
        Episode Found = Subject.getExistingDataOrNull(Scp129Registry.EpisodeState.get());
        return Found == null ? Episode.Idle : Found;
    }

    public static int activeKind(LivingEntity Subject) {
        return episodeOf(Subject).Kind();
    }

    public static float voice(LivingEntity Subject) {
        return 0.92f + Math.floorMod(Subject.getUUID().hashCode(), 17) / 16.0f * 0.16f;
    }

    private static float chance(int Mean) {
        return Mean > 0 ? Scp129Infection.Beat / (float) Mean : 0.0f;
    }

    private static int smallest(int... Means) {
        int Best = 0;
        for (int Mean : Means) {
            if (Mean > 0 && (Best == 0 || Mean < Best)) {
                Best = Mean;
            }
        }
        return Best;
    }

    public static void tick(LivingEntity Subject, ServerLevel World) {
        Episode Current = Subject.getExistingDataOrNull(Scp129Registry.EpisodeState.get());
        if (Current == null || !Current.active()) {
            return;
        }
        long Now = World.getGameTime();
        if (Current.Kind() == Cough && Current.CoughsLeft() > 0 && Now >= Current.NextCough()) {
            cough(Subject, World, Current.Strength());
            int Spacing = CoughSpacing + Subject.getRandom().nextInt(CoughJitter);
            Current = new Episode(Cough, Math.max(Current.Ends(), Now + Spacing), Current.CalmUntil(), Current.CoughsLeft() - 1, Now + Spacing, Current.Strength());
            Subject.setData(Scp129Registry.EpisodeState.get(), Current);
        }
        if (Current.Kind() == Freeze && Subject instanceof Mob Body) {
            Body.getNavigation().stop();
        }
        if (Now >= Current.Ends() && (Current.Kind() != Cough || Current.CoughsLeft() <= 0)) {
            finish(Subject, World, Current, Now);
        }
    }

    public static void roll(LivingEntity Subject, ServerLevel World, Scp129Infection.State Current) {
        Episode Running = episodeOf(Subject);
        long Now = World.getGameTime();
        if (Running.active() || Now < Running.CalmUntil() || Current.Strains() == 0) {
            return;
        }
        int Stage = Scp129Course.symptomStage(Current);
        boolean Lethal = Current.isLethal();
        int CoughEvery = smallest(Current.symptomatic(Scp129Strain.One) ? CoughMean : 0, Current.symptomatic(Scp129Strain.Four) && !Lethal ? MildCoughMean : 0,
                Stage > 0 ? CourseCoughMean[Stage - 1] : 0);
        int BlurEvery = smallest(Current.symptomatic(Scp129Strain.Two) ? BlurMean : 0, Stage > 0 ? CourseBlurMean[Stage - 1] : 0);
        int FreezeEvery = smallest(Current.symptomatic(Scp129Strain.Three) ? FreezeMean : 0, Stage > 0 ? CourseFreezeMean[Stage - 1] : 0);
        float CoughChance = chance(CoughEvery);
        float BlurChance = chance(BlurEvery);
        float FreezeChance = chance(FreezeEvery);
        float Roll = Subject.getRandom().nextFloat();
        if (Roll < CoughChance) {
            boolean Mild = !Current.symptomatic(Scp129Strain.One) && Stage == 0;
            begin(Subject, World, Cough, Stage, Mild);
        } else if (Roll < CoughChance + BlurChance) {
            begin(Subject, World, Blur, Stage, false);
        } else if (Roll < CoughChance + BlurChance + FreezeChance) {
            begin(Subject, World, Freeze, Stage, false);
        }
    }

    public static boolean begin(LivingEntity Subject, ServerLevel World, int Kind, int Stage, boolean Mild) {
        if (!Subject.isAlive() || Scp129Infection.isPaused(Subject)) {
            return false;
        }
        Episode Running = episodeOf(Subject);
        if (Running.active()) {
            finish(Subject, World, Running, World.getGameTime());
        }
        RandomSource Random = Subject.getRandom();
        long Now = World.getGameTime();
        if (Kind == Cough) {
            int Coughs = Mild ? 1 + Random.nextInt(2) : 2 + Random.nextInt(3) + (Stage >= 3 ? 1 : 0);
            float Volume = Mild ? MildVolume : Stage >= 3 ? HardVolume : CoughVolume;
            Subject.setData(Scp129Registry.EpisodeState.get(), new Episode(Cough, Now + 2, 0L, Coughs, Now, Volume));
            return true;
        }
        if (Kind == Blur) {
            int Ticks = BlurShortest + Random.nextInt(BlurJitter + 1) + Stage * BlurPerStage;
            float Strength = 1.0f + Stage * BlurStagePower;
            Subject.setData(Scp129Registry.EpisodeState.get(), new Episode(Blur, Now + Ticks, 0L, 0, 0L, Strength));
            if (Subject instanceof ServerPlayer User) {
                Scp129Sync.episode(User, Blur, Ticks, Strength);
            }
            return true;
        }
        if (Kind == Freeze) {
            int Ticks = FreezeShortest + Random.nextInt(FreezeJitter + 1) + Stage * FreezePerStage;
            Subject.setData(Scp129Registry.EpisodeState.get(), new Episode(Freeze, Now + Ticks, 0L, 0, 0L, 1.0f));
            Subject.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Ticks + BlindLinger, 0, false, false, false));
            stiffen(Subject);
            World.playSound(null, Subject.getX(), Subject.getEyeY(), Subject.getZ(), Scp129Registry.Seize.get(), channel(Subject), 0.8f, voice(Subject));
            if (Subject instanceof ServerPlayer User) {
                Scp129Sync.episode(User, Freeze, Ticks, 1.0f);
            }
            return true;
        }
        return false;
    }

    private static void finish(LivingEntity Subject, ServerLevel World, Episode Ending, long Now) {
        if (Ending.Kind() == Freeze) {
            loosen(Subject);
        }
        if (Ending.Kind() == Cough && Ending.Strength() >= HardVolume) {
            World.playSound(null, Subject.getX(), Subject.getEyeY(), Subject.getZ(), Scp129Registry.Wheeze.get(), channel(Subject), 0.55f, voice(Subject));
        }
        Subject.setData(Scp129Registry.EpisodeState.get(), new Episode(None, 0L, Now + Gap, 0, 0L, 0.0f));
    }

    public static void interrupt(LivingEntity Subject) {
        Episode Running = Subject.getExistingDataOrNull(Scp129Registry.EpisodeState.get());
        if (Running != null && Running.Kind() == Freeze) {
            loosen(Subject);
            MobEffectInstance Blind = Subject.getEffect(MobEffects.BLINDNESS);
            if (Blind != null && !Blind.isVisible() && !Blind.showIcon() && !Blind.isAmbient()) {
                Subject.removeEffect(MobEffects.BLINDNESS);
            }
        }
        Subject.removeData(Scp129Registry.EpisodeState.get());
        if (Subject instanceof ServerPlayer User) {
            Scp129Sync.episode(User, Clear, 0, 0.0f);
        }
    }

    private static void stiffen(LivingEntity Subject) {
        if (Subject instanceof Player) {
            return;
        }
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null && !Speed.hasModifier(FreezeModifier)) {
            Speed.addTransientModifier(new AttributeModifier(FreezeModifier, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        if (Subject instanceof Mob Body) {
            Body.getNavigation().stop();
        }
    }

    private static void loosen(LivingEntity Subject) {
        AttributeInstance Speed = Subject.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Speed != null) {
            Speed.removeModifier(FreezeModifier);
        }
    }

    public static SoundSource channel(LivingEntity Subject) {
        return Subject instanceof Player ? SoundSource.PLAYERS : SoundSource.NEUTRAL;
    }

    private static void cough(LivingEntity Subject, ServerLevel World, float Volume) {
        Vec3 Look = Subject.getLookAngle();
        Vec3 Mouth = Subject.getEyePosition().add(Look.scale(MouthReach)).add(0.0, -0.08, 0.0);
        RandomSource Random = Subject.getRandom();
        float Pitch = voice(Subject) * (0.95f + Random.nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp129Registry.CoughLayers) {
            World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Layer.get(), channel(Subject), Volume, Pitch);
        }
        if (!Scp129Infection.isProtected(Subject)) {
            puff(Subject, World, Mouth, Look, Volume <= MildVolume ? MildPuff : Volume >= HardVolume ? HardPuff : CoughPuff);
        }
        if (Subject instanceof ServerPlayer User) {
            Scp129Sync.episode(User, Cough, JoltTicks, Volume);
        }
        Scp129Spread.cough(Subject, World);
    }

    private static void puff(LivingEntity Subject, ServerLevel World, Vec3 Mouth, Vec3 Look, int Count) {
        List<Integer> Colours = new ArrayList<>();
        int Strains = Scp129Infection.strainsOf(Subject);
        for (Scp129Strain Strain : Scp129Strain.values()) {
            if (Strain.in(Strains)) {
                Colours.add(Strain.mistColour());
            }
        }
        if (Colours.isEmpty()) {
            Colours.add(Scp129Strain.One.mistColour());
        }
        RandomSource Random = Subject.getRandom();
        for (int Index = 0; Index < Count; Index++) {
            ColorParticleOption Tint = ColorParticleOption.create(Scp129Registry.Spore.get(), 0xFF000000 | Colours.get(Index % Colours.size()));
            double Speed = PuffSpeed * (0.6 + Random.nextDouble() * 0.8);
            World.sendParticles(Tint, Mouth.x, Mouth.y, Mouth.z, 0,
                    Look.x * Speed + (Random.nextDouble() - 0.5) * PuffJitter,
                    Look.y * Speed + (Random.nextDouble() - 0.5) * PuffJitter,
                    Look.z * Speed + (Random.nextDouble() - 0.5) * PuffJitter, 1.0);
        }
    }
}
