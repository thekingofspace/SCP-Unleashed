package net.rai.scp_unleashed.scp143;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class Scp143Bleeding {
    public static final float PetalDamage = 1.0f;
    public static final float TickDamage = 1.0f;
    public static final int MaxLevel = 2;
    public static final int BaseTicks = 120;
    public static final int StepTicks = 20;
    public static final int[] Intervals = {40, 30, 24};
    public static final float HelmetGuard = 0.18f;
    public static final float HelmetCap = 0.6f;

    private Scp143Bleeding() {
    }

    public static int interval(int Amplifier) {
        return Intervals[Math.max(0, Math.min(MaxLevel, Amplifier))];
    }

    public static boolean cut(LivingEntity Target, Vec3 Where) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return false;
        }
        boolean Hurt = Target.hurt(Target.damageSources().source(Scp143Registry.PetalDamage), PetalDamage);
        World.playSound(null, Where.x, Where.y, Where.z, Scp143Registry.Slice.get(), SoundSource.HOSTILE, 0.55f, 1.15f + World.getRandom().nextFloat() * 0.3f);
        if (!Hurt) {
            return false;
        }
        if (World.getRandom().nextFloat() < openChance(Target)) {
            afflict(Target);
        }
        return true;
    }

    public static float openChance(LivingEntity Target) {
        ItemStack Helmet = Target.getItemBySlot(EquipmentSlot.HEAD);
        if (Helmet.isEmpty() || !(Helmet.getItem() instanceof ArmorItem Armour)) {
            return 1.0f;
        }
        return 1.0f - Math.min(HelmetCap, Armour.getDefense() * HelmetGuard);
    }

    public static void afflict(LivingEntity Target) {
        MobEffectInstance Existing = Target.getEffect(Scp143Registry.Bleeding);
        int Stage = Existing == null ? 0 : Math.min(MaxLevel, Existing.getAmplifier() + 1);
        int Duration = Math.max(BaseTicks + Stage * StepTicks, Existing == null ? 0 : Existing.getDuration());
        Target.addEffect(new MobEffectInstance(Scp143Registry.Bleeding, Duration, Stage, false, false, true));
    }

    public static void tick(LivingEntity Target, int Amplifier) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return;
        }
        if (Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return;
        }
        Target.hurt(Target.damageSources().source(Scp143Registry.BleedingDamage), TickDamage);
        int Stage = Math.max(0, Math.min(MaxLevel, Amplifier));
        World.sendParticles(Scp143Registry.Drip.get(), Target.getX(), Target.getY() + Target.getBbHeight() * 0.55, Target.getZ(), 2 + Stage,
                Target.getBbWidth() * 0.3, Target.getBbHeight() * 0.2, Target.getBbWidth() * 0.3, 0.0);
        World.playSound(null, Target.getX(), Target.getY() + Target.getBbHeight() * 0.5, Target.getZ(), Scp143Registry.DripSound.get(), Target.getSoundSource(),
                1.0f, 0.9f + World.getRandom().nextFloat() * 0.25f);
    }
}
