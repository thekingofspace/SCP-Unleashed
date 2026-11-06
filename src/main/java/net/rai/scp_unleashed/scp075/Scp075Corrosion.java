package net.rai.scp_unleashed.scp075;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class Scp075Corrosion {
    public static final int Ticks = 160;
    public static final int MaxLevel = 2;
    public static final int[] Intervals = {25, 20, 15};
    public static final float[] Damage = {1.0f, 1.0f, 1.5f};
    public static final int[] Wear = {4, 5, 6};
    public static final int ImpactWear = 6;
    public static final EquipmentSlot[] Worn = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.BODY};

    private Scp075Corrosion() {
    }

    public static void afflict(LivingEntity Target, @Nullable LivingEntity Source) {
        MobEffectInstance Existing = Target.getEffect(Scp075Registry.Corrosion);
        int Stage = Existing == null ? 0 : Math.min(MaxLevel, Existing.getAmplifier() + 1);
        int Duration = Math.max(Ticks, Existing == null ? 0 : Existing.getDuration());
        if (Target.addEffect(new MobEffectInstance(Scp075Registry.Corrosion, Duration, Stage), Source)) {
            corrode(Target, ImpactWear);
        }
    }

    public static boolean tick(LivingEntity Target, int Amplifier) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return true;
        }
        if (Target.isInWaterOrBubble()) {
            World.sendParticles(ParticleTypes.BUBBLE_POP, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
            Scp075Glob.playAt(World, Target.position(), Scp075Registry.RinseLayers, 0.6f, 1.2f);
            return false;
        }
        int Stage = Math.min(MaxLevel, Math.max(0, Amplifier));
        if (Target.tickCount % Intervals[Stage] != 0) {
            return true;
        }
        Target.hurt(Target.damageSources().source(Scp075Registry.CorrosionDamage), Damage[Stage]);
        corrode(Target, Wear[Stage]);
        World.sendParticles(Scp075Registry.Drip.get(), Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 3, Target.getBbWidth() * 0.35, Target.getBbHeight() * 0.25,
                Target.getBbWidth() * 0.35, 0.01);
        Scp075Glob.playAt(World, Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0), Scp075Registry.SizzleLayers, 0.35f, 1.1f + World.getRandom().nextFloat() * 0.3f);
        return true;
    }

    public static void corrode(LivingEntity Target, int Amount) {
        for (EquipmentSlot Slot : Worn) {
            ItemStack Stack = Target.getItemBySlot(Slot);
            if (!Stack.isEmpty() && Stack.isDamageableItem()) {
                Stack.hurtAndBreak(Amount, Target, Slot);
            }
        }
    }
}
