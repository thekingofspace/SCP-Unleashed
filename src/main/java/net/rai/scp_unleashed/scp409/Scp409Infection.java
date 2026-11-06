package net.rai.scp_unleashed.scp409;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp409Infection {
    public static final int Stages = 5;
    public static final int[] StageStarts = {0, 4000, 9000, 15000, 21000};
    public static final double[] SlowLevels = {0.05, 0.15, 0.30, 0.45, 0.60};
    public static final double[] AttackLevels = {0.0, 0.0, 0.10, 0.25, 0.40};
    public static final double[] MiningLevels = {0.0, 0.0, 0.20, 0.45, 0.65};
    public static final double SlowStep = 0.025;
    public static final double DyingSlowStep = 0.0025;
    public static final double MaxSlow = 0.75;
    public static final int DamageInterval = 60;
    public static final float DamageBase = 0.025f;
    public static final float DamageStep = 0.0025f;
    public static final float MinDamage = 0.25f;
    public static final float SmallestScale = 0.55f;
    public static final float ScaleSpan = 0.45f;
    public static final float PlayerBody = 0.6f * 1.8f;
    public static final int RefreshTicks = 20;
    public static final int GrowthSteps = 200;
    public static final int[] CrackleOdds = {900, 600, 400, 260, 160};
    public static final int[] GlintOdds = {80, 50, 30, 18, 10};
    public static final ResourceLocation SlowId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_slow");
    public static final ResourceLocation AttackId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_stiff");
    public static final ResourceLocation MiningId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp409_brittle");
    private static final List<Holder<Attribute>> Slowed = List.of(Attributes.MOVEMENT_SPEED, Attributes.FLYING_SPEED);

    private Scp409Infection() {
    }

    public static boolean isInfected(LivingEntity Subject) {
        return Subject.getExistingDataOrNull(Scp409Registry.Crystal.get()) != null;
    }

    public static int elapsed(LivingEntity Subject) {
        Integer Found = Subject.getExistingDataOrNull(Scp409Registry.Crystal.get());
        return Found == null ? 0 : Found;
    }

    public static float scale(LivingEntity Subject) {
        float Body = Subject.getBbWidth() * Subject.getBbHeight() / PlayerBody;
        return Mth.clamp(SmallestScale + ScaleSpan * Body, SmallestScale, 1.0f);
    }

    public static int stageStart(LivingEntity Subject, int Stage) {
        return Mth.ceil(StageStarts[Mth.clamp(Stage, 1, Stages) - 1] * scale(Subject));
    }

    public static int finalStart(LivingEntity Subject) {
        return stageStart(Subject, Stages);
    }

    public static int damageInterval(LivingEntity Subject) {
        return Math.max(20, Math.round(DamageInterval * scale(Subject)));
    }

    public static int stageOf(LivingEntity Subject, int Elapsed) {
        float Normal = Elapsed / scale(Subject);
        int Stage = 1;
        for (int Index = 1; Index < StageStarts.length; Index++) {
            if (Normal >= StageStarts[Index]) {
                Stage = Index + 1;
            }
        }
        return Stage;
    }

    public static int stageOf(LivingEntity Subject) {
        return isInfected(Subject) ? stageOf(Subject, elapsed(Subject)) : 0;
    }

    public static float growth(LivingEntity Subject, int Elapsed) {
        return Mth.clamp(Elapsed / (float) Math.max(1, finalStart(Subject)), 0.0f, 1.0f);
    }

    public static int pulses(LivingEntity Subject, int Elapsed) {
        int Start = finalStart(Subject);
        return Elapsed < Start ? 0 : (Elapsed - Start) / damageInterval(Subject);
    }

    public static double slowFraction(LivingEntity Subject, int Elapsed) {
        float Normal = Elapsed / scale(Subject);
        double Raw;
        if (Normal >= StageStarts[Stages - 1]) {
            Raw = SlowLevels[Stages - 1] + DyingSlowStep * pulses(Subject, Elapsed);
        } else {
            int Index = stageOf(Subject, Elapsed) - 1;
            float Into = (Normal - StageStarts[Index]) / (StageStarts[Index + 1] - StageStarts[Index]);
            Raw = Mth.lerp(Mth.clamp(Into, 0.0f, 1.0f), SlowLevels[Index], SlowLevels[Index + 1]);
        }
        return Math.min(MaxSlow, Math.floor(Raw / SlowStep + 1.0e-6) * SlowStep);
    }

    public static float pulseDamage(LivingEntity Subject, int Pulse) {
        return Math.max(MinDamage, Subject.getMaxHealth() * (DamageBase + DamageStep * Pulse));
    }

    public static boolean isPaused(LivingEntity Subject) {
        return Subject instanceof Player User && (User.isCreative() || User.isSpectator());
    }

    public static boolean infect(LivingEntity Subject) {
        if (Subject.level().isClientSide || !Subject.isAlive() || isInfected(Subject)) {
            return false;
        }
        if (Scp409Exposure.isExempt(Subject)) {
            InfectionImmunity.absorb(Subject, Scp409Registry.DocumentId);
            return false;
        }
        Subject.setData(Scp409Registry.Crystal.get(), 0);
        Subject.getData(Scp409Registry.Trace.get()).LastStage = 1;
        onset(Subject);
        applyModifiers(Subject, 0);
        Scp409Sync.update(Subject);
        return true;
    }

    public static boolean cure(LivingEntity Subject) {
        boolean Was = isInfected(Subject);
        Subject.removeData(Scp409Registry.Crystal.get());
        boolean Modified = hasModifiers(Subject);
        clearModifiers(Subject);
        Scp409Sync.update(Subject);
        return Was || Modified;
    }

    public static boolean setStage(LivingEntity Subject, int Stage) {
        if (!isInfected(Subject) && !infect(Subject)) {
            return false;
        }
        int Elapsed = stageStart(Subject, Stage);
        Subject.setData(Scp409Registry.Crystal.get(), Elapsed);
        Subject.getData(Scp409Registry.Trace.get()).LastStage = stageOf(Subject, Elapsed);
        applyModifiers(Subject, Elapsed);
        Scp409Sync.update(Subject);
        return true;
    }

    public static void setElapsed(LivingEntity Subject, int Elapsed) {
        Subject.setData(Scp409Registry.Crystal.get(), Math.max(0, Elapsed));
        Subject.getData(Scp409Registry.Trace.get()).LastStage = stageOf(Subject, Elapsed);
        applyModifiers(Subject, Elapsed);
        Scp409Sync.update(Subject);
    }

    public static void tick(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World) || !Subject.isAlive()) {
            return;
        }
        if (isPaused(Subject)) {
            Scp409Sync.update(Subject);
            return;
        }
        if (Scp409Exposure.isExempt(Subject)) {
            InfectionImmunity.absorb(Subject, Scp409Registry.DocumentId);
            cure(Subject);
            return;
        }
        int Now = elapsed(Subject) + 1;
        Subject.setData(Scp409Registry.Crystal.get(), Now);
        Scp409Trace Trace = Subject.getData(Scp409Registry.Trace.get());
        int Stage = stageOf(Subject, Now);
        if (Stage != Trace.LastStage) {
            boolean Grew = Stage > Trace.LastStage;
            Trace.LastStage = Stage;
            applyModifiers(Subject, Now);
            if (Grew) {
                advance(World, Subject, Stage);
            }
        } else if (Math.floorMod(Now + Subject.getId(), RefreshTicks) == 0) {
            applyModifiers(Subject, Now);
        }
        Scp409Sync.update(Subject);
        ambience(World, Subject, Stage);
        Scp409Exposure.spread(World, Subject);
        int Start = finalStart(Subject);
        if (Now >= Start && (Now - Start) % damageInterval(Subject) == 0) {
            int Pulse = (Now - Start) / damageInterval(Subject);
            Subject.hurt(Subject.damageSources().source(Scp409Registry.OvergrownDamage), pulseDamage(Subject, Pulse));
        }
    }

    private static void onset(LivingEntity Subject) {
        if (!(Subject.level() instanceof ServerLevel World)) {
            return;
        }
        double Middle = Subject.getY() + Subject.getBbHeight() * 0.5;
        play(World, Subject, Scp409Registry.InfectLayers, 1.0f, 1.0f);
        World.sendParticles(Scp409Registry.ShardParticle.get(), Subject.getX(), Middle, Subject.getZ(), 6 + Math.round(4 * Subject.getBbWidth()),
                Subject.getBbWidth() * 0.4, Subject.getBbHeight() * 0.3, Subject.getBbWidth() * 0.4, 0.02);
        tell(Subject, 1);
    }

    private static void advance(ServerLevel World, LivingEntity Subject, int Stage) {
        play(World, Subject, Scp409Registry.GrowthLayers, 0.7f + 0.1f * Stage, 0.95f - 0.05f * Stage);
        double Middle = Subject.getY() + Subject.getBbHeight() * 0.5;
        World.sendParticles(Scp409Registry.ShardParticle.get(), Subject.getX(), Middle, Subject.getZ(), 4 + 3 * Stage,
                Subject.getBbWidth() * 0.45, Subject.getBbHeight() * 0.35, Subject.getBbWidth() * 0.45, 0.015);
        tell(Subject, Stage);
    }

    private static void tell(LivingEntity Subject, int Stage) {
        if (Subject instanceof ServerPlayer Victim) {
            Victim.displayClientMessage(Component.translatable("message.scp_unleashed.scp409.stage." + Stage).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), true);
        }
    }

    private static void ambience(ServerLevel World, LivingEntity Subject, int Stage) {
        int Index = Mth.clamp(Stage, 1, Stages) - 1;
        if (Subject.getRandom().nextInt(CrackleOdds[Index]) == 0) {
            play(World, Subject, Scp409Registry.GrowthLayers, 0.25f + 0.08f * Stage, 0.9f + Subject.getRandom().nextFloat() * 0.25f);
        }
        if (Subject.getRandom().nextInt(GlintOdds[Index]) == 0) {
            World.sendParticles(Scp409Registry.ShardParticle.get(), Subject.getX(), Subject.getY() + Subject.getBbHeight() * 0.55, Subject.getZ(), 1,
                    Subject.getBbWidth() * 0.4, Subject.getBbHeight() * 0.3, Subject.getBbWidth() * 0.4, 0.005);
        }
    }

    public static void play(ServerLevel World, LivingEntity Subject, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        double Middle = Subject.getY() + Subject.getBbHeight() * 0.5;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Subject.getX(), Middle, Subject.getZ(), Layer.get(), Subject.getSoundSource(), Volume, Pitch);
        }
    }

    public static void applyModifiers(LivingEntity Subject, int Elapsed) {
        int Index = stageOf(Subject, Elapsed) - 1;
        double Slow = -slowFraction(Subject, Elapsed);
        for (Holder<Attribute> Speed : Slowed) {
            modify(Subject.getAttribute(Speed), SlowId, Slow);
        }
        modify(Subject.getAttribute(Attributes.ATTACK_SPEED), AttackId, -AttackLevels[Index]);
        modify(Subject.getAttribute(Attributes.BLOCK_BREAK_SPEED), MiningId, -MiningLevels[Index]);
    }

    private static void modify(@Nullable AttributeInstance Instance, ResourceLocation Id, double Amount) {
        if (Instance == null) {
            return;
        }
        AttributeModifier Current = Instance.getModifier(Id);
        if (Amount == 0.0) {
            if (Current != null) {
                Instance.removeModifier(Id);
            }
            return;
        }
        if (Current != null && Math.abs(Current.amount() - Amount) < 1.0e-6) {
            return;
        }
        Instance.addOrUpdateTransientModifier(new AttributeModifier(Id, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static boolean hasModifiers(LivingEntity Subject) {
        for (Holder<Attribute> Speed : Slowed) {
            if (has(Subject.getAttribute(Speed), SlowId)) {
                return true;
            }
        }
        return has(Subject.getAttribute(Attributes.ATTACK_SPEED), AttackId) || has(Subject.getAttribute(Attributes.BLOCK_BREAK_SPEED), MiningId);
    }

    private static boolean has(@Nullable AttributeInstance Instance, ResourceLocation Id) {
        return Instance != null && Instance.hasModifier(Id);
    }

    public static void clearModifiers(LivingEntity Subject) {
        for (Holder<Attribute> Speed : Slowed) {
            remove(Subject.getAttribute(Speed), SlowId);
        }
        remove(Subject.getAttribute(Attributes.ATTACK_SPEED), AttackId);
        remove(Subject.getAttribute(Attributes.BLOCK_BREAK_SPEED), MiningId);
    }

    private static void remove(@Nullable AttributeInstance Instance, ResourceLocation Id) {
        if (Instance != null && Instance.hasModifier(Id)) {
            Instance.removeModifier(Id);
        }
    }

    public static void forget(LivingEntity Subject) {
        Subject.removeData(Scp409Registry.Crystal.get());
        clearModifiers(Subject);
        Scp409Sync.update(Subject);
    }
}
