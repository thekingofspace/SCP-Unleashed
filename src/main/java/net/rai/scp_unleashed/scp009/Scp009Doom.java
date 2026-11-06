package net.rai.scp_unleashed.scp009;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.gear.GearTags;
import org.jetbrains.annotations.Nullable;

public final class Scp009Doom {
    public static final int TotalTicks = 1200;
    public static final int Stages = 8;
    public static final int SlowSteps = 12;
    public static final double SlowStart = 0.10;
    public static final double SlowEnd = 0.70;
    public static final int GlintInterval = 5;
    public static final int ChipInterval = 10;
    public static final float ChipStart = 0.25f;
    public static final float GlintGrowth = 5.0f;
    public static final float ChipGrowth = 5.0f;
    public static final float CrackleStart = 0.1f;
    public static final int CrackleSlowOdds = 60;
    public static final int CrackleFastOdds = 12;
    public static final int CarriedInterval = 5;
    public static final int MinFragments = 3;
    public static final int MaxFragments = 6;
    public static final float HealthPerFragment = 8.0f;
    public static final int MaxMobFragments = 6;
    public static final float PlayerBody = 1.08f;
    public static final float MinBody = 0.4f;
    public static final float MaxBody = 4.0f;
    public static final ResourceLocation SlowId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp009_doom_slow");

    private Scp009Doom() {
    }

    public static boolean isExempt(LivingEntity Target) {
        return Target instanceof ArmorStand || Target.isInvulnerable() || Target instanceof Player User && (User.isCreative() || User.isSpectator()) || InfectionImmunity.isImmune(Target);
    }

    public static boolean isProtected(LivingEntity Target) {
        return GearTags.wearsFullHazmatSuit(Target);
    }

    public static boolean isDoomed(LivingEntity Target) {
        return Target.hasEffect(Scp009Registry.Doomed);
    }

    @Nullable
    public static MobEffectInstance instance(LivingEntity Target) {
        return Target.getEffect(Scp009Registry.Doomed);
    }

    public static float progress(@Nullable MobEffectInstance Instance) {
        if (Instance == null || Instance.isInfiniteDuration()) {
            return 0.0f;
        }
        return Mth.clamp(1.0f - Instance.getDuration() / (float) TotalTicks, 0.0f, 1.0f);
    }

    public static int stage(float Progress) {
        return Math.min(Stages, 1 + (int) (Progress * Stages));
    }

    public static double slowFraction(float Progress) {
        int Step = Math.min(SlowSteps, (int) (Progress * SlowSteps));
        return SlowStart + (SlowEnd - SlowStart) * Step / SlowSteps;
    }

    public static float body(LivingEntity Target) {
        return Mth.clamp(Target.getBbWidth() * Target.getBbHeight() / PlayerBody, MinBody, MaxBody);
    }

    public static boolean doom(LivingEntity Target) {
        if (!Target.isAlive() || Target.level().isClientSide) {
            return false;
        }
        if (isExempt(Target)) {
            InfectionImmunity.absorb(Target, Scp009Registry.DocumentId);
            return false;
        }
        if (isDoomed(Target)) {
            return false;
        }
        return Target.addEffect(new MobEffectInstance(Scp009Registry.Doomed, TotalTicks, 0, false, false, true));
    }

    public static void onset(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return;
        }
        double Middle = Target.getY() + Target.getBbHeight() * 0.5;
        World.playSound(null, Target.getX(), Middle, Target.getZ(), Scp009Registry.Exposure.get(), Target.getSoundSource(), 1.0f, 1.0f);
        World.playSound(null, Target.getX(), Middle, Target.getZ(), Scp009Registry.ExposureSting.get(), Target.getSoundSource(), 0.8f, 1.0f);
        float Size = body(Target);
        World.sendParticles(Scp009Registry.Glint.get(), Target.getX(), Middle, Target.getZ(), Math.round(10 + 6 * Size),
                Target.getBbWidth() * 0.45, Target.getBbHeight() * 0.35, Target.getBbWidth() * 0.45, 0.03);
        if (Target instanceof ServerPlayer Victim) {
            Victim.displayClientMessage(Component.translatable("message.scp_unleashed.scp009.doomed").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
        }
        applySlow(Target, 0.0f);
    }

    public static void touch(LivingEntity Bearer) {
        if (!isProtected(Bearer)) {
            doom(Bearer);
        }
    }

    public static boolean carries(LivingEntity Bearer) {
        if (Bearer instanceof Player User) {
            return User.getInventory().hasAnyMatching(Scp009CrystalItem::is) || Scp009CrystalItem.is(User.containerMenu.getCarried());
        }
        for (EquipmentSlot Slot : EquipmentSlot.values()) {
            if (Scp009CrystalItem.is(Bearer.getItemBySlot(Slot))) {
                return true;
            }
        }
        return Bearer instanceof InventoryCarrier Carrier && Carrier.getInventory().hasAnyMatching(Scp009CrystalItem::is);
    }

    public static void checkHeld(LivingEntity Bearer) {
        if (Bearer.level().isClientSide || !Bearer.isAlive() || !carries(Bearer)) {
            return;
        }
        Scp009Sources.carrier(Bearer);
        if (!isDoomed(Bearer)) {
            touch(Bearer);
        }
    }

    public static void checkCarried(ServerPlayer User) {
        if (User.tickCount % CarriedInterval != 0) {
            return;
        }
        ItemStack Carried = User.containerMenu.getCarried();
        if (Scp009CrystalItem.is(Carried)) {
            touch(User);
            Scp009Sources.carrier(User);
        }
    }

    public static boolean clear(LivingEntity Target) {
        boolean Removed = Target.removeEffect(Scp009Registry.Doomed);
        clearSlow(Target);
        return Removed;
    }

    public static boolean tick(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel World) || !Target.isAlive()) {
            return true;
        }
        MobEffectInstance Instance = instance(Target);
        if (Instance == null) {
            return true;
        }
        if (isExempt(Target)) {
            InfectionImmunity.absorb(Target, Scp009Registry.DocumentId);
            clearSlow(Target);
            return false;
        }
        if (!Instance.isInfiniteDuration() && Instance.getDuration() <= 1) {
            crystallize(Target);
            return false;
        }
        float Progress = progress(Instance);
        applySlow(Target, Progress);
        visuals(World, Target, Progress);
        int CrackleOdds = Math.max(CrackleFastOdds, Math.round(Mth.lerp(Progress, CrackleSlowOdds, CrackleFastOdds)));
        if (Progress > CrackleStart && Target.getRandom().nextInt(CrackleOdds) == 0) {
            World.playSound(null, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), Scp009Registry.DoomCrackle.get(), Target.getSoundSource(),
                    0.35f + Progress * 0.45f, 0.85f + Target.getRandom().nextFloat() * 0.3f);
        }
        return true;
    }

    private static void visuals(ServerLevel World, LivingEntity Target, float Progress) {
        int Clock = Target.tickCount + Target.getId();
        float Size = body(Target);
        double Middle = Target.getY() + Target.getBbHeight() * 0.5;
        double Wide = Target.getBbWidth() * 0.4;
        double Tall = Target.getBbHeight() * 0.35;
        if (Clock % GlintInterval == 0) {
            int Count = 1 + Math.round(Progress * GlintGrowth * Size);
            World.sendParticles(Scp009Registry.Glint.get(), Target.getX(), Middle, Target.getZ(), Count, Wide, Tall, Wide, 0.004);
        }
        if (Clock % ChipInterval == 0 && Progress > ChipStart) {
            int Count = Math.max(1, Math.round((Progress - ChipStart) / (1.0f - ChipStart) * ChipGrowth * Size));
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp009Registry.RedIce.get().defaultBlockState()), Target.getX(), Middle, Target.getZ(), Count,
                    Wide, Tall, Wide, 0.02);
        }
    }

    public static void crystallize(LivingEntity Target) {
        clearSlow(Target);
        Target.hurt(Target.damageSources().source(Scp009Registry.CrystallizedDamage), Float.MAX_VALUE);
    }

    public static int fragmentCount(LivingEntity Dead, RandomSource Random) {
        if (Dead instanceof Player) {
            return MinFragments + Random.nextInt(MaxFragments - MinFragments + 1);
        }
        double Share = Dead.getMaxHealth() / HealthPerFragment;
        return Mth.clamp((int) Math.floor(Share + Random.nextDouble()), 1, MaxMobFragments);
    }

    public static void shatter(LivingEntity Dead) {
        if (!(Dead.level() instanceof ServerLevel World)) {
            return;
        }
        RandomSource Random = Dead.getRandom();
        int Count = fragmentCount(Dead, Random);
        double Middle = Dead.getY() + Dead.getBbHeight() * 0.5;
        for (int Index = 0; Index < Count; Index++) {
            ItemEntity Drop = new ItemEntity(World, Dead.getX(), Dead.getY() + Dead.getBbHeight() * (0.3 + Random.nextDouble() * 0.5), Dead.getZ(),
                    new ItemStack(Scp009Registry.Fragment.get()));
            Drop.setDeltaMovement(Random.nextGaussian() * 0.11, 0.18 + Random.nextDouble() * 0.16, Random.nextGaussian() * 0.11);
            Drop.setDefaultPickUpDelay();
            World.addFreshEntity(Drop);
        }
        float Size = body(Dead);
        World.sendParticles(Scp009Registry.Glint.get(), Dead.getX(), Middle, Dead.getZ(), Math.round(16 + 24 * Math.min(Size, 2.0f)),
                Dead.getBbWidth() * 0.5, Dead.getBbHeight() * 0.4, Dead.getBbWidth() * 0.5, 0.08);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Scp009Registry.RedIce.get().defaultBlockState()), Dead.getX(), Middle, Dead.getZ(),
                Math.round(12 + 18 * Math.min(Size, 2.0f)), Dead.getBbWidth() * 0.45, Dead.getBbHeight() * 0.4, Dead.getBbWidth() * 0.45, 0.15);
        World.playSound(null, Dead.getX(), Middle, Dead.getZ(), Scp009Registry.Shatter.get(), Dead.getSoundSource(), 1.0f, 1.0f);
        World.playSound(null, Dead.getX(), Middle, Dead.getZ(), Scp009Registry.ShatterChime.get(), Dead.getSoundSource(), 0.8f, 1.0f);
        clearSlow(Dead);
    }

    private static void applySlow(LivingEntity Target, float Progress) {
        double Amount = -slowFraction(Progress);
        slow(Target.getAttribute(Attributes.MOVEMENT_SPEED), Amount);
        slow(Target.getAttribute(Attributes.FLYING_SPEED), Amount);
    }

    private static void slow(@Nullable AttributeInstance Speed, double Amount) {
        if (Speed == null) {
            return;
        }
        AttributeModifier Current = Speed.getModifier(SlowId);
        if (Current != null && Math.abs(Current.amount() - Amount) < 1.0e-6) {
            return;
        }
        Speed.addOrUpdateTransientModifier(new AttributeModifier(SlowId, Amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    public static void clearSlow(LivingEntity Target) {
        for (AttributeInstance Speed : new AttributeInstance[]{Target.getAttribute(Attributes.MOVEMENT_SPEED), Target.getAttribute(Attributes.FLYING_SPEED)}) {
            if (Speed != null && Speed.hasModifier(SlowId)) {
                Speed.removeModifier(SlowId);
            }
        }
    }
}
